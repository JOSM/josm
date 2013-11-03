// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.WindowGeometry;

public final class HelpAwareOptionPane {

    private HelpAwareOptionPane() {
        // Hide default constructor for utils classes
    }
    
    public static class ButtonSpec {
        public final String text;
        public final Icon icon;
        public final String tooltipText;
        public final String helpTopic;
        private boolean enabled;

        private final Collection<ChangeListener> listeners = new HashSet<ChangeListener>();

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

        private final boolean addChangeListener(ChangeListener listener) {
            return listener != null ? listeners.add(listener) : false;
        }
    }

    static private class DefaultAction extends AbstractAction {
        private JDialog dialog;
        private JOptionPane pane;
        private int value;

        public DefaultAction(JDialog dialog, JOptionPane pane, int value) {
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
    static private List<JButton> createOptionButtons(ButtonSpec[] options, String helpTopic) {
        List<JButton> buttons = new ArrayList<JButton>();
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
                b.setToolTipText(spec.tooltipText == null? "" : spec.tooltipText);
                if (helpTopic != null) {
                    HelpUtil.setHelpContext(b, helpTopic);
                }
                b.setFocusable(true);
                b.setEnabled(spec.isEnabled());
                spec.addChangeListener(new ChangeListener() {
                    @Override public void stateChanged(ChangeEvent e) {
                        b.setEnabled(spec.isEnabled());
                    }
                });
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
    static private JButton createHelpButton(final String helpTopic) {
        JButton b = new JButton(tr("Help"));
        b.setIcon(ImageProvider.get("help"));
        b.setToolTipText(tr("Show help information"));
        HelpUtil.setHelpContext(b, helpTopic);
        Action a = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HelpBrowser.setUrlForHelpTopic(helpTopic);
            }
        };
        b.addActionListener(a);
        InputMapUtils.enableEnter(b);
        return b;
    }

    /**
     * Displays an option dialog which is aware of a help context. If <code>helpTopic</code> isn't null,
     * the dialog includes a "Help" button and launches the help browser if the user presses F1. If the
     * user clicks on the "Help" button the option dialog remains open and JOSM launches the help
     * browser.
     *
     * <code>helpTopic</code> is the trailing part of a JOSM online help URL, i.e. the part after the leading
     * <code>http://josm.openstreetmap.de/wiki/Help</code>. It should start with a leading '/' and it
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
    static public int showOptionDialog(Component parentComponent, Object msg, String title, int messageType, Icon icon, final ButtonSpec[] options, final ButtonSpec defaultOption, final String helpTopic)  {
        final List<JButton> buttons = createOptionButtons(options, helpTopic);
        if (helpTopic != null) {
            buttons.add(createHelpButton(helpTopic));
        }

        JButton defaultButton = null;
        if (options != null && defaultOption != null) {
            for (int i=0; i< options.length; i++) {
                if (options[i] == defaultOption) {
                    defaultButton = buttons.get(i);
                    break;
                }
            }
        }

        if (msg instanceof String) {
            JosmEditorPane pane = new JosmEditorPane("text/html", (String) msg);
            pane.setEditable(false);
            pane.setOpaque(false);
            msg = pane;
        }

        final JOptionPane pane = new JOptionPane(
                msg,
                messageType,
                JOptionPane.DEFAULT_OPTION,
                icon,
                buttons.toArray(),
                defaultButton
        );

        pane.getValue();
        final JDialog dialog = new JDialog(
                JOptionPane.getFrameForComponent(parentComponent),
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
                    for (i=0; i<options.length;i++) {
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
        dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "close");
        dialog.getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pane.setValue(JOptionPane.CLOSED_OPTION);
                dialog.setVisible(false);
            }}
        );

        if (options != null) {
            for (int i=0; i < options.length;i++) {
                final DefaultAction action = new DefaultAction(dialog, pane, i);
                buttons.get(i).addActionListener(action);
                buttons.get(i).getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "enter");
                buttons.get(i).getActionMap().put("enter", action);
            }
        } else {
            final DefaultAction action = new DefaultAction(dialog, pane, 0);
            buttons.get(0).addActionListener(action);
            buttons.get(0).getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "enter");
            buttons.get(0).getActionMap().put("enter", action);
        }

        dialog.pack();
        WindowGeometry.centerOnScreen(dialog.getSize()).applySafe(dialog);
        if (helpTopic != null) {
            HelpUtil.setHelpContext(dialog.getRootPane(), helpTopic);
        }
        dialog.setVisible(true);
        return (Integer)pane.getValue();
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
    static public int showOptionDialog(Component parentComponent, Object msg, String title, int messageType,final String helpTopic)  {
        return showOptionDialog(parentComponent, msg, title, messageType, null,null,null, helpTopic);
    }

    /**
     * Run it in Event Dispatch Thread.
     * This version does not return anything, so it is more like showMessageDialog.
     *
     * It can be used, when you need to show a message dialog from a worker thread,
     * e.g. from PleaseWaitRunnable
     */
    static public void showMessageDialogInEDT(final Component parentComponent, final Object msg, final String title, final int messageType, final String helpTopic)  {
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                showOptionDialog(parentComponent, msg, title, messageType, null, null, null, helpTopic);
            }
        });
    }
}
