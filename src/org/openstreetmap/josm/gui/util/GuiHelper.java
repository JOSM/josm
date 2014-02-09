// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.FilteredImageSource;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.ImageProvider;

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
            if(child instanceof Container) {
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

    public static void runInEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public static void runInEDTAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException e) {
                Main.error(e);
            } catch (InvocationTargetException e) {
                Main.error(e);
            }
        }
    }

    /**
     * returns true if the user wants to cancel, false if they
     * want to continue
     */
    public static final boolean warnUser(String title, String content, ImageIcon baseActionIcon, String continueToolTip) {
        ExtendedDialog dlg = new ExtendedDialog(Main.parent,
                title, new String[] {tr("Cancel"), tr("Continue")});
        dlg.setContent(content);
        dlg.setButtonIcons(new Icon[] {
                ImageProvider.get("cancel"),
                ImageProvider.overlay(
                        ImageProvider.get("upload"),
                        new ImageIcon(ImageProvider.get("warning-small").getImage().getScaledInstance(10 , 10, Image.SCALE_SMOOTH)),
                        ImageProvider.OverlayPosition.SOUTHEAST)});
        dlg.setToolTipTexts(new String[] {
                tr("Cancel"),
                continueToolTip});
        dlg.setIcon(JOptionPane.WARNING_MESSAGE);
        dlg.setCancelButton(1);
        return dlg.showDialog().getValue() != 2;
    }

    /**
     * Replies the disabled (grayed) version of the specified image.
     * @param image The image to disable
     * @return The disabled (grayed) version of the specified image, brightened by 20%.
     * @since 5484
     */
    public static final Image getDisabledImage(Image image) {
        return Toolkit.getDefaultToolkit().createImage(
                new FilteredImageSource(image.getSource(), new GrayFilter(true, 20)));
    }

    /**
     * Replies the disabled (grayed) version of the specified icon.
     * @param icon The icon to disable
     * @return The disabled (grayed) version of the specified icon, brightened by 20%.
     * @since 5484
     */
    public static final ImageIcon getDisabledIcon(ImageIcon icon) {
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
    public static final Component prepareResizeableOptionPane(final Component pane, final Dimension minDimension) {
        if (pane != null) {
            pane.addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    Window window = SwingUtilities.getWindowAncestor(pane);
                    if (window instanceof Dialog) {
                        Dialog dialog = (Dialog)window;
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
    public static final Timer scheduleTimer(int initialDelay, ActionListener actionListener, boolean repeats) {
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

        if (s.length==0) return new BasicStroke();
        float w;
        try {
            w = Float.parseFloat(s[0]);
        } catch (NumberFormatException ex) {
            w = 1.0f;
        }
        if (s.length>1) {
            float[] dash= new float[s.length-1];
            float sumAbs = 0;
            try {
                for (int i=0; i<s.length-1; i++) {
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
            if (w>1) {
                // thick stroke
                return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            } else {
                // thin stroke
                return new BasicStroke(w);
            }
        }
    }

    /**
     * Gets the font used to display JOSM title in about dialog and splash screen.
     * @return By order or priority, the first font available in local fonts:
     *         1. Helvetica Bold 20
     *         2. Calibri Bold 23
     *         3. Arial Bold 20
     *         4. SansSerif Bold 20
     * @since 5797
     */
    public static Font getTitleFont() {
        List<String> fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        // Helvetica is the preferred choice but is not available by default on Windows
        // (http://www.microsoft.com/typography/fonts/product.aspx?pid=161)
        if (fonts.contains("Helvetica")) {
            return new Font("Helvetica", Font.BOLD, 20);
        // Calibri is the default Windows font since Windows Vista but is not available on older versions of Windows, where Arial is preferred
        } else if (fonts.contains("Calibri")) {
            return new Font("Calibri", Font.BOLD, 23);
        } else if (fonts.contains("Arial")) {
            return new Font("Arial", Font.BOLD, 20);
        // No luck, nothing found, fallback to one of the 5 fonts provided with Java (Serif, SansSerif, Monospaced, Dialog, and DialogInput)
        } else {
            return new Font("SansSerif", Font.BOLD, 20);
        }
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
}
