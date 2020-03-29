// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;

/**
 * Utility methods that display an option dialog with an additional help button that links to the JOSM help
 */
public final class HelpAwareOptionPane {

    private HelpAwareOptionPane() {
        // Hide default constructor for utils classes
    }

    /**
     * A specification of a button that should be added to the options dialog
     */
    public static class ButtonSpec {
        /**
         * the button text
         */
        public final String text;
        /**
         * the icon to display. Can be <code>null</code>
         */
        public final Icon icon;
        /**
         * The tooltip to display when hovering the button
         */
        public final String tooltipText;
        /**
         * The help topic to link
         */
        public final String helpTopic;
        private boolean enabled;

        private final Collection<ChangeListener> listeners = new HashSet<>();

        /**
         * Constructs a new {@code ButtonSpec}.
         * @param text the button text
         * @param imageProvider provides the icon to display. Can be null
         * @param tooltipText the tooltip text. Can be null.
         * @param helpTopic the help topic. Can be null.
         * @since 13842
         */
        public ButtonSpec(String text, ImageProvider imageProvider, String tooltipText, String helpTopic) {
            this(text, imageProvider != null ? imageProvider.setSize(ImageSizes.LARGEICON).get() : null, tooltipText, helpTopic, true);
        }

        /**
         * Constructs a new {@code ButtonSpec}.
         * @param text the button text
         * @param icon the icon to display. Can be null
         * @param tooltipText the tooltip text. Can be null.
         * @param helpTopic the help topic. Can be null.
         */
        public ButtonSpec(String text, Icon icon, String tooltipText, String helpTopic) {
            this(text, icon, tooltipText, helpTopic, true);
        }

        /**
         * Constructs a new {@code ButtonSpec}.
         * @param text the button text
         * @param icon the icon to display. Can be null
         * @param tooltipText the tooltip text. Can be null.
         * @param helpTopic the help topic. Can be null.
         * @param enabled the enabled status
         * @since 5951
         */
        public ButtonSpec(String text, Icon icon, String tooltipText, String helpTopic, boolean enabled) {
            this.text = text;
            this.icon = icon;
            this.tooltipText = tooltipText;
            this.helpTopic = helpTopic;
            setEnabled(enabled);
        }

        /**
         * Determines if this button spec is enabled
         * @return {@code true} if this button spec is enabled, {@code false} otherwise
         * @since 6051
         */
        public final boolean isEnabled() {
            return enabled;
        }

        /**
         * Enables or disables this button spec, depending on the value of the parameter {@code b}.
         * @param enabled if {@code true}, this button spec is enabled; otherwise this button spec is disabled
         * @since 6051
         */
        public final void setEnabled(boolean enabled) {
            if (this.enabled != enabled) {
                this.enabled = enabled;
                ChangeEvent event = new ChangeEvent(this);
                for (ChangeListener listener : listeners) {
                    listener.stateChanged(event);
                }
            }
        }

        private boolean addChangeListener(ChangeListener listener) {
            return listener != null && listeners.add(listener);
        }
    }

    private static class DefaultAction extends AbstractAction {
        private final JDialog dialog;
        private final JOptionPane pane;
        private final int value;

        DefaultAction(JDialog dialog, JOptionPane pane, int value) {
            this.dialog = dialog;
            this.pane = pane;
            this.value = value;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setValue(value);
            dialog.setVisible(false);
        }
    }

    /**
     * Creates the list buttons to be displayed in the option pane dialog.
     *
     * @param options the option. If null, just creates an OK button and a help button
     * @param helpTopic the help topic. The context sensitive help of all buttons is equal
     * to the context sensitive help of the whole dialog
     * @return the list of buttons
     */
    private static List<JButton> createOptionButtons(ButtonSpec[] options, String helpTopic) {
        List<JButton> buttons = new ArrayList<>();
        if (options == null) {
            JButton b = new JButton(tr("OK"));
            b.setIcon(ImageProvider.get("ok"));
            b.setToolTipText(tr("Click to close the dialog"));
            b.setFocusable(true);
            buttons.add(b);
        } else {
            for (final ButtonSpec spec: options) {
                final JButton b = new JButton(spec.text);
                b.setIcon(spec.icon);
                b.setToolTipText(spec.tooltipText == null ? "" : spec.tooltipText);
                if (helpTopic != null) {
                    HelpUtil.setHelpContext(b, helpTopic);
                }
                b.setFocusable(true);
                b.setEnabled(spec.isEnabled());
                spec.addChangeListener(e -> b.setEnabled(spec.isEnabled()));
                buttons.add(b);
            }
        }
        return buttons;
    }

    /**
     * Creates the help button
     *
     * @param helpTopic the help topic
     * @return the help button
     */
    private static JButton createHelpButton(String helpTopic) {
        JButton b = new JButton(new HelpAction(helpTopic));
        HelpUtil.setHelpContext(b, helpTopic);
        InputMapUtils.enableEnter(b);
        return b;
    }

    private static class HelpAction extends JosmAction {
        private final String helpTopic;

        HelpAction(String helpTopic) {
            super(tr("Help"), "help", tr("Show help information"), null, false, false);
            this.helpTopic = helpTopic;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            HelpBrowser.setUrlForHelpTopic(helpTopic);
        }
    }

    /**
     * Displays an option dialog which is aware of a help context. If <code>helpTopic</code> isn't null,
     * the dialog includes a "Help" button and launches the help browser if the user presses F1. If the
     * user clicks on the "Help" button the option dialog remains open and JOSM launches the help
     * browser.
     *
     * <code>helpTopic</code> is the trailing part of a JOSM online help URL, i.e. the part after the leading
     * <code>https://josm.openstreetmap.de/wiki/Help</code>. It should start with a leading '/' and it
     * may include an anchor after a '#'.
     *
     * <strong>Examples</strong>
     * <ul>
     *    <li>/Dialogs/RelationEditor</li>
     *    <li>/Dialogs/RelationEditor#ConflictInData</li>
     * </ul>
     *
     * In addition, the option buttons display JOSM icons, similar to ExtendedDialog.
     *
     * @param parentComponent the parent component
     * @param msg the message
     * @param title the title
     * @param messageType the message type (see {@link JOptionPane})
     * @param icon the icon to display. Can be null.
     * @param options the list of options to display. Can be null.
     * @param defaultOption the default option. Can be null.
     * @param helpTopic the help topic. Can be null.
     * @return the index of the selected option or {@link JOptionPane#CLOSED_OPTION}
     */
    public static int showOptionDialog(Component parentComponent, Object msg, String title, int messageType,
            Icon icon, final ButtonSpec[] options, final ButtonSpec defaultOption, final String helpTopic) {
        final List<JButton> buttons = createOptionButtons(options, helpTopic);
        if (helpTopic != null) {
            buttons.add(createHelpButton(helpTopic));
        }

        JButton defaultButton = null;
        if (options != null && defaultOption != null) {
            for (int i = 0; i < options.length; i++) {
                if (options[i] == defaultOption) {
                    defaultButton = buttons.get(i);
                    break;
                }
            }
        }
        final Object content;
        if (msg instanceof String) {
            String msgStr = (String) msg;
            content = new HtmlPanel(msgStr.startsWith("<html>") ? msgStr : "<html>" + msgStr + "</html>");
        } else {
            content = msg;
        }
        final JOptionPane pane = new JOptionPane(
                content,
                messageType,
                JOptionPane.DEFAULT_OPTION,
                icon,
                buttons.toArray(),
                defaultButton
        );

        // Log message. Useful for bug reports and unit tests
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                Logging.error(title + " - " + msg);
                break;
            case JOptionPane.WARNING_MESSAGE:
                Logging.warn(title + " - " + msg);
                break;
            default:
                Logging.info(title + " - " + msg);
        }

        if (!GraphicsEnvironment.isHeadless()) {
            doShowOptionDialog(parentComponent, title, options, defaultOption, helpTopic, buttons, pane);
        }
        return pane.getValue() instanceof Integer ? (Integer) pane.getValue() : JOptionPane.OK_OPTION;
    }

    private static void doShowOptionDialog(Component parentComponent, String title, final ButtonSpec[] options,
            final ButtonSpec defaultOption, final String helpTopic, final List<JButton> buttons,
            final JOptionPane pane) {
        final JDialog dialog = new JDialog(
                GuiHelper.getFrameForComponent(parentComponent),
                title,
                ModalityType.DOCUMENT_MODAL
        );
        dialog.setContentPane(pane);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                pane.setValue(JOptionPane.CLOSED_OPTION);
                super.windowClosed(e);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                if (defaultOption != null && options != null && options.length > 0) {
                    int i;
                    for (i = 0; i < options.length; i++) {
                        if (options[i] == defaultOption) {
                            break;
                        }
                    }
                    if (i >= options.length) {
                        buttons.get(0).requestFocusInWindow();
                    }
                    buttons.get(i).requestFocusInWindow();
                } else {
                    buttons.get(0).requestFocusInWindow();
                }
            }
        });
        InputMapUtils.addEscapeAction(dialog.getRootPane(), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pane.setValue(JOptionPane.CLOSED_OPTION);
                dialog.setVisible(false);
            }
        });

        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                final DefaultAction action = new DefaultAction(dialog, pane, i);
                buttons.get(i).addActionListener(action);
                InputMapUtils.addEnterAction(buttons.get(i), action);
            }
        } else {
            final DefaultAction action = new DefaultAction(dialog, pane, 0);
            buttons.get(0).addActionListener(action);
            InputMapUtils.addEnterAction(buttons.get(0), action);
        }

        dialog.pack();
        WindowGeometry.centerOnScreen(dialog.getSize()).applySafe(dialog);
        if (helpTopic != null) {
            HelpUtil.setHelpContext(dialog.getRootPane(), helpTopic);
        }
        dialog.setVisible(true);
    }

    /**
     * Displays an option dialog which is aware of a help context.
     *
     * @param parentComponent the parent component
     * @param msg the message
     * @param title the title
     * @param messageType the message type (see {@link JOptionPane})
     * @param helpTopic the help topic. Can be null.
     * @return the index of the selected option or {@link JOptionPane#CLOSED_OPTION}
     * @see #showOptionDialog(Component, Object, String, int, Icon, ButtonSpec[], ButtonSpec, String)
     */
    public static int showOptionDialog(Component parentComponent, Object msg, String title, int messageType, String helpTopic) {
        return showOptionDialog(parentComponent, msg, title, messageType, null, null, null, helpTopic);
    }

    /**
     * Run it in Event Dispatch Thread.
     * This version does not return anything, so it is more like {@code showMessageDialog}.
     *
     * It can be used, when you need to show a message dialog from a worker thread,
     * e.g. from {@code PleaseWaitRunnable}.
     *
     * @param parentComponent the parent component
     * @param msg the message
     * @param title the title
     * @param messageType the message type (see {@link JOptionPane})
     * @param helpTopic the help topic. Can be null.
     */
    public static void showMessageDialogInEDT(final Component parentComponent, final Object msg, final String title,
            final int messageType, final String helpTopic) {
        GuiHelper.runInEDT(() -> showOptionDialog(parentComponent, msg, title, messageType, null, null, null, helpTopic));
    }
}
