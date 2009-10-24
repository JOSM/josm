// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpBrowserProxy;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

public class HelpAwareOptionPane {

    public static class ButtonSpec {
        public String text;
        public Icon icon;
        public String tooltipText;
        public String helpTopic;

        /**
         * 
         * @param text  the button text
         * @param icon  the icon to display. Can be null
         * @param tooltipText  the tooltip text. Can be null.
         * @param helpTopic the help topic. Can be null.
         */
        public ButtonSpec(String text, Icon icon, String tooltipText, String helpTopic) {
            this.text = text;
            this.icon = icon;
            this.tooltipText = tooltipText;
            this.helpTopic = helpTopic;
        }
    }

    static private class DefaultAction implements ActionListener {
        private JDialog dialog;
        private JOptionPane pane;
        private int value;

        public DefaultAction(JDialog dialog, JOptionPane pane, int value) {
            this.dialog = dialog;
            this.pane = pane;
            this.value = value;
        }
        public void actionPerformed(ActionEvent e) {
            pane.setValue(value);
            dialog.setVisible(false);
        }
    }

    static private List<JButton> createOptionButtons(ButtonSpec[] options, String helpTopic) {
        List<JButton> buttons = new ArrayList<JButton>();
        if (options == null) {
            buttons.add(new JButton(tr("OK")));
        } else {
            for (ButtonSpec spec: options) {
                JButton b = new JButton(spec.text);
                b.setIcon(spec.icon);
                b.setToolTipText(spec.tooltipText == null? "" : spec.tooltipText);
                if (helpTopic != null) {
                    HelpUtil.setHelpContext(b, helpTopic);
                }
                b.setFocusable(true);
                buttons.add(b);

            }
        }
        return buttons;
    }

    static private JButton createHelpButton(final String helpTopic) {
        JButton b = new JButton(tr("Help"));
        b.setIcon(ImageProvider.get("help"));
        b.setToolTipText(tr("Show help information"));
        HelpUtil.setHelpContext(b, helpTopic);
        b.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        HelpBrowserProxy.getInstance().setUrlForHelpTopic(completeHelpTopic(helpTopic));
                    }
                }
        );
        return b;
    }

    static private String completeHelpTopic(String helpTopic) {
        if (helpTopic == null) return null;
        if (! helpTopic.startsWith("/")) {
            helpTopic = "/" + helpTopic;
        }
        return "Help" + helpTopic;
    }

    /**
     * Displays an option dialog which is aware of a help context. If <code>helpTopic</code> isn't null,
     * the dialog includes a "Help" button and launches the help browser if the user presses F1. If the
     * user clicks on the "Help" button the option dialog remains open and JOSM launches the help
     * browser.
     * 
     * <code>helpTopic</code> is the trailing part of a JOSM online help URL, i.e. the part after the leading
     * <code>http://josm.openstreetmap.de/wiki/Help</code>. It should start with a leading '/' and it
     * may include and anchor after a '#'.
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
     * @param messageType the message type (see {@see JOptionPane})
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
            msg = new JLabel((String)msg);
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
                true
        );
        dialog.setContentPane(pane);
        dialog.addWindowListener(
                new WindowAdapter() {
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
                            if (i >= options.length) return; // default option not an option?

                            buttons.get(i).requestFocusInWindow();
                        }
                    }
                }
        );
        if (options != null) {
            for (int i=0; i < options.length;i++) {
                buttons.get(i).addActionListener(new DefaultAction(dialog, pane, i));
            }
        }
        dialog.pack();
        WindowGeometry.centerOnScreen(dialog.getSize()).applySafe(dialog);
        if (helpTopic != null) {
            HelpUtil.setHelpContext(dialog.getRootPane(), helpTopic);
        }
        dialog.setVisible(true);
        return (Integer)pane.getValue();
    }
}
