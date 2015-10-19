// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.image.FilteredImageSource;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageOverlay;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * basic gui utils
 */
public final class GuiHelper {

    private GuiHelper() {
        // Hide default constructor for utils classes
    }

    /**
     * disable / enable a component and all its child components
     */
    public static void setEnabledRec(Container root, boolean enabled) {
        root.setEnabled(enabled);
        Component[] children = root.getComponents();
        for (Component child : children) {
            if (child instanceof Container) {
                setEnabledRec((Container) child, enabled);
            } else {
                child.setEnabled(enabled);
            }
        }
    }

    public static void executeByMainWorkerInEDT(final Runnable task) {
        Main.worker.submit(new Runnable() {
            @Override
            public void run() {
                runInEDTAndWait(task);
            }
        });
    }

    /**
     * Executes asynchronously a runnable in
     * <a href="http://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html">Event Dispatch Thread</a>.
     * @param task The runnable to execute
     * @see SwingUtilities#invokeLater
     */
    public static void runInEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /**
     * Executes synchronously a runnable in
     * <a href="http://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html">Event Dispatch Thread</a>.
     * @param task The runnable to execute
     * @see SwingUtilities#invokeAndWait
     */
    public static void runInEDTAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException | InvocationTargetException e) {
                Main.error(e);
            }
        }
    }

    /**
     * Executes synchronously a callable in
     * <a href="http://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html">Event Dispatch Thread</a>
     * and return a value.
     * @param callable The callable to execute
     * @return The computed result
     * @since 7204
     */
    public static <V> V runInEDTAndWaitAndReturn(Callable<V> callable) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                Main.error(e);
                return null;
            }
        } else {
            FutureTask<V> task = new FutureTask<V>(callable);
            SwingUtilities.invokeLater(task);
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                Main.error(e);
                return null;
            }
        }
    }

    /**
     * Warns user about a dangerous action requiring confirmation.
     * @param title Title of dialog
     * @param content Content of dialog
     * @param baseActionIcon Unused? FIXME why is this parameter unused?
     * @param continueToolTip Tooltip to display for "continue" button
     * @return true if the user wants to cancel, false if they want to continue
     */
    public static boolean warnUser(String title, String content, ImageIcon baseActionIcon, String continueToolTip) {
        ExtendedDialog dlg = new ExtendedDialog(Main.parent,
                title, new String[] {tr("Cancel"), tr("Continue")});
        dlg.setContent(content);
        dlg.setButtonIcons(new Icon[] {
                    new ImageProvider("cancel").setMaxSize(ImageSizes.LARGEICON).get(),
                    new ImageProvider("upload").setMaxSize(ImageSizes.LARGEICON).addOverlay(
                            new ImageOverlay(new ImageProvider("warning-small"), 0.5, 0.5, 1.0, 1.0)).get()});
        dlg.setToolTipTexts(new String[] {
                tr("Cancel"),
                continueToolTip});
        dlg.setIcon(JOptionPane.WARNING_MESSAGE);
        dlg.setCancelButton(1);
        return dlg.showDialog().getValue() != 2;
    }

    /**
     * Notifies user about an error received from an external source as an HTML page.
     * @param parent Parent component
     * @param title Title of dialog
     * @param message Message displayed at the top of the dialog
     * @param html HTML content to display (real error message)
     * @since 7312
     */
    public static void notifyUserHtmlError(Component parent, String title, String message, String html) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(message), GBC.eol());
        p.add(new JLabel(tr("Received error page:")), GBC.eol());
        JScrollPane sp = embedInVerticalScrollPane(new HtmlPanel(html));
        sp.setPreferredSize(new Dimension(640, 240));
        p.add(sp, GBC.eol().fill(GBC.BOTH));

        ExtendedDialog ed = new ExtendedDialog(parent, title, new String[] {tr("OK")});
        ed.setButtonIcons(new String[] {"ok.png"});
        ed.setContent(p);
        ed.showDialog();
    }

    /**
     * Replies the disabled (grayed) version of the specified image.
     * @param image The image to disable
     * @return The disabled (grayed) version of the specified image, brightened by 20%.
     * @since 5484
     */
    public static Image getDisabledImage(Image image) {
        return Toolkit.getDefaultToolkit().createImage(
                new FilteredImageSource(image.getSource(), new GrayFilter(true, 20)));
    }

    /**
     * Replies the disabled (grayed) version of the specified icon.
     * @param icon The icon to disable
     * @return The disabled (grayed) version of the specified icon, brightened by 20%.
     * @since 5484
     */
    public static ImageIcon getDisabledIcon(ImageIcon icon) {
        return new ImageIcon(getDisabledImage(icon.getImage()));
    }

    /**
     * Attaches a {@code HierarchyListener} to the specified {@code Component} that
     * will set its parent dialog resizeable. Use it before a call to JOptionPane#showXXXXDialog
     * to make it resizeable.
     * @param pane The component that will be displayed
     * @param minDimension The minimum dimension that will be set for the dialog. Ignored if null
     * @return {@code pane}
     * @since 5493
     */
    public static Component prepareResizeableOptionPane(final Component pane, final Dimension minDimension) {
        if (pane != null) {
            pane.addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    Window window = SwingUtilities.getWindowAncestor(pane);
                    if (window instanceof Dialog) {
                        Dialog dialog = (Dialog) window;
                        if (!dialog.isResizable()) {
                            dialog.setResizable(true);
                            if (minDimension != null) {
                                dialog.setMinimumSize(minDimension);
                            }
                        }
                    }
                }
            });
        }
        return pane;
    }

    /**
     * Schedules a new Timer to be run in the future (once or several times).
     * @param initialDelay milliseconds for the initial and between-event delay if repeatable
     * @param actionListener an initial listener; can be null
     * @param repeats specify false to make the timer stop after sending its first action event
     * @return The (started) timer.
     * @since 5735
     */
    public static Timer scheduleTimer(int initialDelay, ActionListener actionListener, boolean repeats) {
        Timer timer = new Timer(initialDelay, actionListener);
        timer.setRepeats(repeats);
        timer.start();
        return timer;
    }

    /**
     * Return s new BasicStroke object with given thickness and style
     * @param code = 3.5 -&gt; thickness=3.5px; 3.5 10 5 -&gt; thickness=3.5px, dashed: 10px filled + 5px empty
     * @return stroke for drawing
     */
    public static Stroke getCustomizedStroke(String code) {
        String[] s = code.trim().split("[^\\.0-9]+");

        if (s.length == 0) return new BasicStroke();
        float w;
        try {
            w = Float.parseFloat(s[0]);
        } catch (NumberFormatException ex) {
            w = 1.0f;
        }
        if (s.length > 1) {
            float[] dash = new float[s.length-1];
            float sumAbs = 0;
            try {
                for (int i = 0; i < s.length-1; i++) {
                   dash[i] = Float.parseFloat(s[i+1]);
                   sumAbs += Math.abs(dash[i]);
                }
            } catch (NumberFormatException ex) {
                Main.error("Error in stroke preference format: "+code);
                dash = new float[]{5.0f};
            }
            if (sumAbs < 1e-1) {
                Main.error("Error in stroke dash fomat (all zeros): "+code);
                return new BasicStroke(w);
            }
            // dashed stroke
            return new BasicStroke(w, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
        } else {
            if (w > 1) {
                // thick stroke
                return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            } else {
                // thin stroke
                return new BasicStroke(w);
            }
        }
    }

    /**
     * Gets the font used to display monospaced text in a component, if possible.
     * @param component The component
     * @return the font used to display monospaced text in a component, if possible
     * @since 7896
     */
    public static Font getMonospacedFont(JComponent component) {
        // Special font for Khmer script
        if ("km".equals(LanguageInfo.getJOSMLocaleCode())) {
            return component.getFont();
        } else {
            return new Font("Monospaced", component.getFont().getStyle(), component.getFont().getSize());
        }
    }

    /**
     * Gets the font used to display JOSM title in about dialog and splash screen.
     * @since 5797
     */
    public static Font getTitleFont() {
        return new Font("SansSerif", Font.BOLD, 23);
    }

    /**
     * Embeds the given component into a new vertical-only scrollable {@code JScrollPane}.
     * @param panel The component to embed
     * @return the vertical scrollable {@code JScrollPane}
     * @since 6666
     */
    public static JScrollPane embedInVerticalScrollPane(Component panel) {
        return new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * Returns extended modifier key used as the appropriate accelerator key for menu shortcuts.
     * It is advised everywhere to use {@link Toolkit#getMenuShortcutKeyMask()} to get the cross-platform modifier, but:
     * <ul>
     * <li>it returns KeyEvent.CTRL_MASK instead of KeyEvent.CTRL_DOWN_MASK. We used the extended
     *    modifier for years, and Oracle recommends to use it instead, so it's best to keep it</li>
     * <li>the method throws a HeadlessException ! So we would need to handle it for unit tests anyway</li>
     * </ul>
     * @return extended modifier key used as the appropriate accelerator key for menu shortcuts
     * @since 7539
     */
    public static int getMenuShortcutKeyMaskEx() {
        return Main.isPlatformOsx() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
    }

    /**
     * Sets a global font for all UI, replacing default font of current look and feel.
     * @param name Font name. It is up to the caller to make sure the font exists
     * @throws IllegalArgumentException if name is null
     * @since 7896
     */
    public static void setUIFont(String name) {
        CheckParameterUtil.ensureParameterNotNull(name, "name");
        Main.info("Setting "+name+" as the default UI font");
        Enumeration<?> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource fui = (FontUIResource) value;
                UIManager.put(key, new FontUIResource(name, fui.getStyle(), fui.getSize()));
            }
        }
    }
}
