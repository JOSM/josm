// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

/**
 * ConditionalOptionPaneUtil provides static utility methods for displaying modal message dialogs
 * which can be enabled/disabled by the user.
 *
 * They wrap the methods provided by {@see JOptionPane}. Within JOSM you should use these
 * methods rather than the bare methods from {@see JOptionPane} because the methods provided
 * by ConditionalOptionPaneUtil ensure that a dialog window is always on top and isn't hidden by one of the
 * JOSM windows for detached dialogs, relation editors, history browser and the like.
 *
 */
public class ConditionalOptionPaneUtil {
    static public final int DIALOG_DISABLED_OPTION = Integer.MIN_VALUE;

    /**
     * this is a static utility class only
     */
    private ConditionalOptionPaneUtil() {}

    /**
     * Replies the preference value for the preference key "message." + <code>prefKey</code>.
     * The default value if the preference key is missing is true.
     *
     * @param  the preference key
     * @return prefKey the preference value for the preference key "message." + <code>prefKey</code>
     */
    public static boolean getDialogShowingEnabled(String prefKey) {
        return Main.pref.getBoolean("message."+prefKey, true);
    }

    /**
     * sets the value for the preference key "message." + <code>prefKey</code>.
     *
     * @param prefKey the key
     * @param enabled the value
     */
    public static void setDialogShowingEnabled(String prefKey, boolean enabled) {
        Main.pref.put("message."+prefKey, enabled);
    }

    /**
     * Returns the preference value for the preference key "message." + <code>prefKey</code> + ".value".
     * The default value if the preference key is missing is -1.
     *
     * @param  the preference key
     * @return prefKey the preference value for the preference key "message." + <code>prefKey</code> + ".value"
     */
    public static Integer getDialogReturnValue(String prefKey) {
        return Main.pref.getInteger("message."+prefKey+".value", -1);
    }

    /**
     * sets the value for the preference key "message." + <code>prefKey</code> + ".value".
     *
     * @param prefKey the key
     * @param value the value
     */
    public static void setDialogReturnValue(String prefKey, Integer value) {
        Main.pref.putInteger("message."+prefKey+".value", value);
    }

    /**
     * Displays an confirmation dialog with some option buttons given by <code>optionType</code>.
     * It is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     *
     * Set <code>optionType</code> to {@see JOptionPane#YES_NO_OPTION} for a dialog with a YES and
     * a NO button.

     * Set <code>optionType</code> to {@see JOptionPane#YES_NO_CANCEL_OPTION} for a dialog with a YES,
     * a NO and a CANCEL button
     *
     * Replies true, if the selected option is equal to <code>trueOption</code>, otherwise false.
     * Replies true, if the dialog is not displayed because the respective preference option
     * <code>preferenceKey</code> is set to false.
     *
     * @param preferenceKey the preference key
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * @param optionType  the option type
     * @param messageType the message type
     * @param options a list of options
     * @param defaultOption the default option
     *
     *
     * @return the index of the selected option. {@see JOptionPane#CLOSED_OPTION} if the dialog was closed.
     * {@see ConditionalOptionPaneUtil#DIALOG_DISABLED_OPTION} if the dialog is disabled.
     *
     */
    static public int showOptionDialog(String preferenceKey, Component parent, Object message, String title, int optionType, int messageType, Object [] options, Object defaultOption) throws HeadlessException {
        if (!getDialogShowingEnabled(preferenceKey))
            return DIALOG_DISABLED_OPTION;
        MessagePanel pnl = new MessagePanel(false, message);
        int ret = JOptionPane.showOptionDialog(parent, pnl, title, optionType, messageType, null,options,defaultOption);

        if(!pnl.getDialogShowingEnabled())
            setDialogShowingEnabled(preferenceKey, false);
        return ret;
    }

    /**
     * Displays an confirmation dialog with some option buttons given by <code>optionType</code>.
     * It is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     *
     * Set <code>optionType</code> to {@see JOptionPane#YES_NO_OPTION} for a dialog with a YES and
     * a NO button.

     * Set <code>optionType</code> to {@see JOptionPane#YES_NO_CANCEL_OPTION} for a dialog with a YES,
     * a NO and a CANCEL button
     *
     * Replies true, if the selected option is equal to <code>trueOption</code>, otherwise false.
     * Replies true, if the dialog is not displayed because the respective preference option
     * <code>preferenceKey</code> is set to false.
     *
     * @param preferenceKey the preference key
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * @param optionType  the option type
     * @param messageType the message type
     * @param trueOption  if this option is selected the method replies true
     *
     *
     * @return true, if the selected option is equal to <code>trueOption</code>, otherwise false.
     *
     * @see JOptionPane#INFORMATION_MESSAGE
     * @see JOptionPane#WARNING_MESSAGE
     * @see JOptionPane#ERROR_MESSAGE
     */
    static public boolean showConfirmationDialog(String preferenceKey, Component parent, Object message, String title, int optionType, int messageType, int trueOption) throws HeadlessException {
        if (!getDialogShowingEnabled(preferenceKey) && (getDialogReturnValue(preferenceKey) >= 0))
            return getDialogReturnValue(preferenceKey) == trueOption;
        MessagePanel pnl = new MessagePanel(false, message);
        int ret = JOptionPane.showConfirmDialog(parent, pnl, title, optionType, messageType);
        if ((ret >= 0) && !pnl.getDialogShowingEnabled()) {
            setDialogShowingEnabled(preferenceKey, false);
            setDialogReturnValue(preferenceKey, ret);
        }
        return ret == trueOption;
    }

    /**
     * Displays an message in modal dialog with an OK button. Makes sure the dialog
     * is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     *
     * If there is a preference with key <code>preferenceKey</code> and value <code>false</code>
     * the dialog is not show.
     *
     * @param preferenceKey the preference key
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * @param messageType the message type
     *
     * @see JOptionPane#INFORMATION_MESSAGE
     * @see JOptionPane#WARNING_MESSAGE
     * @see JOptionPane#ERROR_MESSAGE
     */
    static public void showMessageDialog(String preferenceKey, Component parent, Object message, String title,int messageType) {
        if (!getDialogShowingEnabled(preferenceKey))
            return;
        MessagePanel pnl = new MessagePanel(false, message);
        JOptionPane.showMessageDialog(parent, pnl, title, messageType);
        if(!pnl.getDialogShowingEnabled())
            setDialogShowingEnabled(preferenceKey, false);
    }

    /**
     * This is a message panel used in dialogs which can be enabled/disabled with a preference
     * setting.
     * In addition to the normal message any {@see JOptionPane} would display it includes
     * a checkbox for enabling/disabling this particular dialog.
     *
     */
    private static class MessagePanel extends JPanel {
        JCheckBox cbShowDialog;

        public MessagePanel(boolean donotshow, Object message) {
            cbShowDialog = new JCheckBox(tr("Do not show again (remembers choice)"));
            cbShowDialog.setSelected(donotshow);
            setLayout(new GridBagLayout());

            if (message instanceof Component) {
                add((Component)message, GBC.eop());
            } else {
                add(new JLabel(message.toString()),GBC.eop());
            }
            add(cbShowDialog, GBC.eol());
        }

        public boolean getDialogShowingEnabled() {
            return !cbShowDialog.isSelected();
        }
    }
}
