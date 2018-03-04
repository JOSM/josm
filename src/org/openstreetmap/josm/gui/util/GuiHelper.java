// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.FilteredImageSource;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.StrokeProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageOverlay;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

/**
 * basic gui utils
 */
public final class GuiHelper {

    /* Localization keys for file chooser (and color chooser). */
    private static final String[] JAVA_INTERNAL_MESSAGE_KEYS = new String[] {
        /* JFileChooser windows laf */
        "FileChooser.detailsViewActionLabelText",
        "FileChooser.detailsViewButtonAccessibleName",
        "FileChooser.detailsViewButtonToolTipText",
        "FileChooser.fileAttrHeaderText",
        "FileChooser.fileDateHeaderText",
        "FileChooser.fileNameHeaderText",
        "FileChooser.fileNameLabelText",
        "FileChooser.fileSizeHeaderText",
        "FileChooser.fileTypeHeaderText",
        "FileChooser.filesOfTypeLabelText",
        "FileChooser.homeFolderAccessibleName",
        "FileChooser.homeFolderToolTipText",
        "FileChooser.listViewActionLabelText",
        "FileChooser.listViewButtonAccessibleName",
        "FileChooser.listViewButtonToolTipText",
        "FileChooser.lookInLabelText",
        "FileChooser.newFolderAccessibleName",
        "FileChooser.newFolderActionLabelText",
        "FileChooser.newFolderToolTipText",
        "FileChooser.refreshActionLabelText",
        "FileChooser.saveInLabelText",
        "FileChooser.upFolderAccessibleName",
        "FileChooser.upFolderToolTipText",
        "FileChooser.viewMenuLabelText",

        /* JFileChooser gtk laf */
        "FileChooser.acceptAllFileFilterText",
        "FileChooser.cancelButtonText",
        "FileChooser.cancelButtonToolTipText",
        "FileChooser.deleteFileButtonText",
        "FileChooser.filesLabelText",
        "FileChooser.filterLabelText",
        "FileChooser.foldersLabelText",
        "FileChooser.newFolderButtonText",
        "FileChooser.newFolderDialogText",
        "FileChooser.openButtonText",
        "FileChooser.openButtonToolTipText",
        "FileChooser.openDialogTitleText",
        "FileChooser.pathLabelText",
        "FileChooser.renameFileButtonText",
        "FileChooser.renameFileDialogText",
        "FileChooser.renameFileErrorText",
        "FileChooser.renameFileErrorTitle",
        "FileChooser.saveButtonText",
        "FileChooser.saveButtonToolTipText",
        "FileChooser.saveDialogTitleText",

        /* JFileChooser motif laf */
        //"FileChooser.cancelButtonText",
        //"FileChooser.cancelButtonToolTipText",
        "FileChooser.enterFileNameLabelText",
        //"FileChooser.filesLabelText",
        //"FileChooser.filterLabelText",
        //"FileChooser.foldersLabelText",
        "FileChooser.helpButtonText",
        "FileChooser.helpButtonToolTipText",
        //"FileChooser.openButtonText",
        //"FileChooser.openButtonToolTipText",
        //"FileChooser.openDialogTitleText",
        //"FileChooser.pathLabelText",
        //"FileChooser.saveButtonText",
        //"FileChooser.saveButtonToolTipText",
        //"FileChooser.saveDialogTitleText",
        "FileChooser.updateButtonText",
        "FileChooser.updateButtonToolTipText",

        /* gtk color chooser */
        "GTKColorChooserPanel.blueText",
        "GTKColorChooserPanel.colorNameText",
        "GTKColorChooserPanel.greenText",
        "GTKColorChooserPanel.hueText",
        "GTKColorChooserPanel.nameText",
        "GTKColorChooserPanel.redText",
        "GTKColorChooserPanel.saturationText",
        "GTKColorChooserPanel.valueText",

        /* JOptionPane */
        "OptionPane.okButtonText",
        "OptionPane.yesButtonText",
        "OptionPane.noButtonText",
        "OptionPane.cancelButtonText"
    };

    private GuiHelper() {
        // Hide default constructor for utils classes
    }

    /**
     * disable / enable a component and all its child components
     * @param root component
     * @param enabled enabled state
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

    /**
     * Add a task to the main worker that will block the worker and run in the GUI thread.
     * @param task The task to run
     */
    public static void executeByMainWorkerInEDT(final Runnable task) {
        MainApplication.worker.submit(() -> runInEDTAndWait(task));
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
                Logging.error(e);
            }
        }
    }

    /**
     * Executes synchronously a runnable in
     * <a href="http://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html">Event Dispatch Thread</a>.
     * <p>
     * Passes on the exception that was thrown to the thread calling this.
     * The exception is wrapped using a {@link ReportedException}.
     * @param task The runnable to execute
     * @see SwingUtilities#invokeAndWait
     * @since 10271
     */
    public static void runInEDTAndWaitWithException(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException | InvocationTargetException e) {
                throw BugReport.intercept(e).put("task", task);
            }
        }
    }

    /**
     * Executes synchronously a callable in
     * <a href="http://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html">Event Dispatch Thread</a>
     * and return a value.
     * @param <V> the result type of method <code>call</code>
     * @param callable The callable to execute
     * @return The computed result
     * @since 7204
     */
    public static <V> V runInEDTAndWaitAndReturn(Callable<V> callable) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return callable.call();
            } catch (Exception e) { // NOPMD
                Logging.error(e);
                return null;
            }
        } else {
            FutureTask<V> task = new FutureTask<>(callable);
            SwingUtilities.invokeLater(task);
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                Logging.error(e);
                return null;
            }
        }
    }

    /**
     * This function fails if it was not called from the EDT thread.
     * @throws IllegalStateException if called from wrong thread.
     * @since 10271
     */
    public static void assertCallFromEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "Needs to be called from the EDT thread, not from " + Thread.currentThread().getName());
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
                title, tr("Cancel"), tr("Continue"));
        dlg.setContent(content);
        dlg.setButtonIcons(
                    new ImageProvider("cancel").setMaxSize(ImageSizes.LARGEICON).get(),
                    new ImageProvider("upload").setMaxSize(ImageSizes.LARGEICON).addOverlay(
                            new ImageOverlay(new ImageProvider("warning-small"), 0.5, 0.5, 1.0, 1.0)).get());
        dlg.setToolTipTexts(tr("Cancel"), continueToolTip);
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

        ExtendedDialog ed = new ExtendedDialog(parent, title, tr("OK"));
        ed.setButtonIcons("ok");
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
            pane.addHierarchyListener(e -> {
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
     * @see StrokeProperty
     */
    public static Stroke getCustomizedStroke(String code) {
        return StrokeProperty.getFromString(code);
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
     * @return title font
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
     * Set the default unit increment for a {@code JScrollPane}.
     *
     * This fixes slow mouse wheel scrolling when the content of the {@code JScrollPane}
     * is a {@code JPanel} or other component that does not implement the {@link Scrollable}
     * interface.
     * The default unit increment is 1 pixel. Multiplied by the number of unit increments
     * per mouse wheel "click" (platform dependent, usually 3), this makes a very
     * sluggish mouse wheel experience.
     * This methods sets the unit increment to a larger, more reasonable value.
     * @param sp the scroll pane
     * @return the scroll pane (same object) with fixed unit increment
     * @throws IllegalArgumentException if the component inside of the scroll pane
     * implements the {@code Scrollable} interface ({@code JTree}, {@code JLayer},
     * {@code JList}, {@code JTextComponent} and {@code JTable})
     */
    public static JScrollPane setDefaultIncrement(JScrollPane sp) {
        if (sp.getViewport().getView() instanceof Scrollable) {
            throw new IllegalArgumentException();
        }
        sp.getVerticalScrollBar().setUnitIncrement(10);
        sp.getHorizontalScrollBar().setUnitIncrement(10);
        return sp;
    }

    /**
     * Sets a global font for all UI, replacing default font of current look and feel.
     * @param name Font name. It is up to the caller to make sure the font exists
     * @throws IllegalArgumentException if name is null
     * @since 7896
     */
    public static void setUIFont(String name) {
        CheckParameterUtil.ensureParameterNotNull(name, "name");
        Logging.info("Setting "+name+" as the default UI font");
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

    /**
     * Sets the background color for this component, and adjust the foreground color so the text remains readable.
     * @param c component
     * @param background background color
     * @since 9223
     */
    public static void setBackgroundReadable(JComponent c, Color background) {
        c.setBackground(background);
        c.setForeground(ColorHelper.getForegroundColor(background));
    }

    /**
     * Gets the size of the screen. On systems with multiple displays, the primary display is used.
     * This method returns always 800x600 in headless mode (useful for unit tests).
     * @return the size of this toolkit's screen, in pixels, or 800x600
     * @see Toolkit#getScreenSize
     * @since 9576
     */
    public static Dimension getScreenSize() {
        return GraphicsEnvironment.isHeadless() ? new Dimension(800, 600) : Toolkit.getDefaultToolkit().getScreenSize();
    }

    /**
     * Gets the size of the screen. On systems with multiple displays,
     * contrary to {@link #getScreenSize()}, the biggest display is used.
     * This method returns always 800x600 in headless mode (useful for unit tests).
     * @return the size of maximum screen, in pixels, or 800x600
     * @see Toolkit#getScreenSize
     * @since 10470
     */
    public static Dimension getMaximumScreenSize() {
        if (GraphicsEnvironment.isHeadless()) {
            return new Dimension(800, 600);
        }

        int height = 0;
        int width = 0;
        for (GraphicsDevice gd: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            DisplayMode dm = gd.getDisplayMode();
            height = Math.max(height, dm.getHeight());
            width = Math.max(width, dm.getWidth());
        }
        if (height == 0 || width == 0) {
            return new Dimension(800, 600);
        }
        return new Dimension(width, height);
    }

    /**
     * Returns the first <code>Window</code> ancestor of event source, or
     * {@code null} if event source is not a component contained inside a <code>Window</code>.
     * @param e event object
     * @return a Window, or {@code null}
     * @since 9916
     */
    public static Window getWindowAncestorFor(EventObject e) {
        if (e != null) {
            Object source = e.getSource();
            if (source instanceof Component) {
                Window ancestor = SwingUtilities.getWindowAncestor((Component) source);
                if (ancestor != null) {
                    return ancestor;
                } else {
                    Container parent = ((Component) source).getParent();
                    if (parent instanceof JPopupMenu) {
                        Component invoker = ((JPopupMenu) parent).getInvoker();
                        return SwingUtilities.getWindowAncestor(invoker);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extends tooltip dismiss delay to a default value of 1 minute for the given component.
     * @param c component
     * @since 10024
     */
    public static void extendTooltipDelay(Component c) {
        extendTooltipDelay(c, 60_000);
    }

    /**
     * Extends tooltip dismiss delay to the specified value for the given component.
     * @param c component
     * @param delay tooltip dismiss delay in milliseconds
     * @see <a href="http://stackoverflow.com/a/6517902/2257172">http://stackoverflow.com/a/6517902/2257172</a>
     * @since 10024
     */
    public static void extendTooltipDelay(Component c, final int delay) {
        final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent me) {
                ToolTipManager.sharedInstance().setDismissDelay(delay);
            }

            @Override
            public void mouseExited(MouseEvent me) {
                ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
            }
        });
    }

    /**
     * Returns the specified component's <code>Frame</code> without throwing exception in headless mode.
     *
     * @param parentComponent the <code>Component</code> to check for a <code>Frame</code>
     * @return the <code>Frame</code> that contains the component, or <code>getRootFrame</code>
     *         if the component is <code>null</code>, or does not have a valid <code>Frame</code> parent
     * @see JOptionPane#getFrameForComponent
     * @see GraphicsEnvironment#isHeadless
     * @since 10035
     */
    public static Frame getFrameForComponent(Component parentComponent) {
        try {
            return JOptionPane.getFrameForComponent(parentComponent);
        } catch (HeadlessException e) {
            Logging.debug(e);
            return null;
        }
    }

    /**
     * Localizations for file chooser dialog.
     * For some locales (e.g. de, fr) translations are provided
     * by Java, but not for others (e.g. ru, uk).
     * @since 12644 (moved from I18n)
     */
    public static void translateJavaInternalMessages() {
        Locale l = Locale.getDefault();

        AbstractFileChooser.setDefaultLocale(l);
        JFileChooser.setDefaultLocale(l);
        JColorChooser.setDefaultLocale(l);
        for (String key : JAVA_INTERNAL_MESSAGE_KEYS) {
            String us = UIManager.getString(key, Locale.US);
            String loc = UIManager.getString(key, l);
            // only provide custom translation if it is not already localized by Java
            if (us != null && us.equals(loc)) {
                UIManager.put(key, tr(us));
            }
        }
    }

    /**
     * Setup special font for Khmer script, as the default Java fonts do not display these characters.
     * @since 12644 (moved from I18n)
     * @since 8282
     */
    public static void setupLanguageFonts() {
        // Use special font for Khmer script, as the default Java font do not display these characters
        if ("km".equals(LanguageInfo.getJOSMLocaleCode())) {
            Collection<String> fonts = Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String f : new String[]{"Khmer UI", "DaunPenh", "MoolBoran"}) {
                if (fonts.contains(f)) {
                    setUIFont(f);
                    break;
                }
            }
        }
    }
}
