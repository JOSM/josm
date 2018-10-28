// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * ConditionalOptionPaneUtil provides static utility methods for displaying modal message dialogs
 * which can be enabled/disabled by the user.
 *
 * They wrap the methods provided by {@link JOptionPane}. Within JOSM you should use these
 * methods rather than the bare methods from {@link JOptionPane} because the methods provided
 * by ConditionalOptionPaneUtil ensure that a dialog window is always on top and isn't hidden by one of the
 * JOSM windows for detached dialogs, relation editors, history browser and the like.
 *
 */
public final class ConditionalOptionPaneUtil {
    public static final int DIALOG_DISABLED_OPTION = Integer.MIN_VALUE;

    /** (preference key =&gt; return value) mappings valid for the current operation (no, those two maps cannot be combined) */
    private static final Map<String, Integer> sessionChoices = new HashMap<>();
    /** (preference key =&gt; return value) mappings valid for the current session */
    private static final Map<String, Integer> immediateChoices = new HashMap<>();
    /** a set indication that (preference key) is or may be stored for the currently active bulk operation */
    private static final Set<String> immediateActive = new HashSet<>();

    /**
     * this is a static utility class only
     */
    private ConditionalOptionPaneUtil() {
        // Hide default constructor for utility classes
    }

    /**
     * Returns the preference value for the preference key "message." + <code>prefKey</code> + ".value".
     * The default value if the preference key is missing is -1.
     *
     * @param  prefKey the preference key
     * @return the preference value for the preference key "message." + <code>prefKey</code> + ".value"
     */
    public static int getDialogReturnValue(String prefKey) {
        return Utils.firstNonNull(immediateChoices.get(prefKey),
                sessionChoices.get(prefKey),
                !Config.getPref().getBoolean("message." + prefKey, true) ? Config.getPref().getInt("message." + prefKey + ".value", -1) : -1
        );
    }

    /**
     * Marks the beginning of a bulk operation in order to provide a "Do not show again (this operation)" option.
     * @param prefKey the preference key
     */
    public static void startBulkOperation(final String prefKey) {
        immediateActive.add(prefKey);
    }

    /**
     * Determines whether the key has been marked to be part of a bulk operation
     * (in order to provide a "Do not show again (this operation)" option).
     * @param prefKey the preference key
     * @return {@code true} if the key has been marked to be part of a bulk operation
     */
    public static boolean isInBulkOperation(final String prefKey) {
        return immediateActive.contains(prefKey);
    }

    /**
     * Marks the ending of a bulk operation. Removes the "Do not show again (this operation)" result value.
     * @param prefKey the preference key
     */
    public static void endBulkOperation(final String prefKey) {
        immediateActive.remove(prefKey);
        immediateChoices.remove(prefKey);
    }

    /**
     * Displays an confirmation dialog with some option buttons given by <code>optionType</code>.
     * It is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     *
     * Set <code>optionType</code> to {@link JOptionPane#YES_NO_OPTION} for a dialog with a YES and
     * a NO button.

     * Set <code>optionType</code> to {@link JOptionPane#YES_NO_CANCEL_OPTION} for a dialog with a YES,
     * a NO and a CANCEL button
     *
     * Returns one of the constants JOptionPane.YES_OPTION, JOptionPane.NO_OPTION,
     * JOptionPane.CANCEL_OPTION or JOptionPane.CLOSED_OPTION depending on the action chosen by
     * the user.
     *
     * @param preferenceKey the preference key
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * @param optionType  the option type
     * @param messageType the message type
     * @param options a list of options
     * @param defaultOption the default option; only meaningful if options is used; can be null
     *
     * @return the option selected by user.
     *         {@link JOptionPane#CLOSED_OPTION} if the dialog was closed.
     */
    public static int showOptionDialog(String preferenceKey, Component parent, Object message, String title, int optionType,
            int messageType, Object[] options, Object defaultOption) {
        int ret = getDialogReturnValue(preferenceKey);
        if (isYesOrNo(ret))
            return ret;
        MessagePanel pnl = new MessagePanel(message, isInBulkOperation(preferenceKey));
        ret = JOptionPane.showOptionDialog(parent, pnl, title, optionType, messageType, null, options, defaultOption);
        if (isYesOrNo(ret)) {
            pnl.getNotShowAgain().store(preferenceKey, ret);
        }
        return ret;
    }

    /**
     * Displays a confirmation dialog with some option buttons given by <code>optionType</code>.
     * It is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     *
     * Set <code>optionType</code> to {@link JOptionPane#YES_NO_OPTION} for a dialog with a YES and
     * a NO button.

     * Set <code>optionType</code> to {@link JOptionPane#YES_NO_CANCEL_OPTION} for a dialog with a YES,
     * a NO and a CANCEL button
     *
     * Replies true, if the selected option is equal to <code>trueOption</code>, otherwise false.
     * Replies true, if the dialog is not displayed because the respective preference option
     * <code>preferenceKey</code> is set to false and the user has previously chosen
     * <code>trueOption</code>.
     *
     * @param preferenceKey the preference key
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * @param optionType  the option type
     * @param messageType the message type
     * @param trueOption if this option is selected the method replies true
     *
     *
     * @return true, if the selected option is equal to <code>trueOption</code>, otherwise false.
     *
     * @see JOptionPane#INFORMATION_MESSAGE
     * @see JOptionPane#WARNING_MESSAGE
     * @see JOptionPane#ERROR_MESSAGE
     */
    public static boolean showConfirmationDialog(String preferenceKey, Component parent, Object message, String title,
            int optionType, int messageType, int trueOption) {
        int ret = getDialogReturnValue(preferenceKey);
        if (isYesOrNo(ret))
            return ret == trueOption;
        MessagePanel pnl = new MessagePanel(message, isInBulkOperation(preferenceKey));
        ret = JOptionPane.showConfirmDialog(parent, pnl, title, optionType, messageType);
        if (isYesOrNo(ret)) {
            pnl.getNotShowAgain().store(preferenceKey, ret);
        }
        return ret == trueOption;
    }

    private static boolean isYesOrNo(int returnCode) {
        return (returnCode == JOptionPane.YES_OPTION) || (returnCode == JOptionPane.NO_OPTION);
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
    public static void showMessageDialog(String preferenceKey, Component parent, Object message, String title, int messageType) {
        if (getDialogReturnValue(preferenceKey) == Integer.MAX_VALUE)
            return;
        MessagePanel pnl = new MessagePanel(message, isInBulkOperation(preferenceKey));
        JOptionPane.showMessageDialog(parent, pnl, title, messageType);
        pnl.getNotShowAgain().store(preferenceKey, Integer.MAX_VALUE);
    }

    /**
     * An enum designating how long to not show this message again, i.e., for how long to store
     */
    enum NotShowAgain {
        NO, OPERATION, SESSION, PERMANENT;

        /**
         * Stores the dialog result {@code value} at the corresponding place.
         * @param prefKey the preference key
         * @param value the dialog result
         */
        void store(String prefKey, Integer value) {
            switch (this) {
                case NO:
                    break;
                case OPERATION:
                    immediateChoices.put(prefKey, value);
                    break;
                case SESSION:
                    sessionChoices.put(prefKey, value);
                    break;
                case PERMANENT:
                    Config.getPref().putBoolean("message." + prefKey, false);
                    Config.getPref().putInt("message." + prefKey + ".value", value);
                    break;
            }
        }

        String getLabel() {
            switch (this) {
                case NO:
                    return tr("Show this dialog again the next time");
                case OPERATION:
                    return tr("Do not show again (this operation)");
                case SESSION:
                    return tr("Do not show again (this session)");
                case PERMANENT:
                    return tr("Do not show again (remembers choice)");
            }
            throw new IllegalStateException();
        }
    }

    /**
     * This is a message panel used in dialogs which can be enabled/disabled with a preference setting.
     * In addition to the normal message any {@link JOptionPane} would display it includes
     * a checkbox for enabling/disabling this particular dialog.
     *
     */
    public static class MessagePanel extends JPanel {
        private final JRadioButton cbShowPermanentDialog = new JRadioButton(NotShowAgain.PERMANENT.getLabel());
        private final JRadioButton cbShowSessionDialog = new JRadioButton(NotShowAgain.SESSION.getLabel());
        private final JRadioButton cbShowImmediateDialog = new JRadioButton(NotShowAgain.OPERATION.getLabel());
        private final JRadioButton cbStandard = new JRadioButton(NotShowAgain.NO.getLabel());

        /**
         * Constructs a new panel.
         * @param message the the message (null to add no message, Component instances are added directly,
         *                otherwise a JLabel with the string representation is added)
         * @param displayImmediateOption whether to provide "Do not show again (this session)"
         */
        MessagePanel(Object message, boolean displayImmediateOption) {
            cbStandard.setSelected(true);
            ButtonGroup group = new ButtonGroup();
            group.add(cbShowPermanentDialog);
            group.add(cbShowSessionDialog);
            group.add(cbShowImmediateDialog);
            group.add(cbStandard);

            setLayout(new GridBagLayout());
            if (message instanceof Component) {
                add((Component) message, GBC.eop());
            } else if (message != null) {
                add(new JMultilineLabel(message.toString()), GBC.eop());
            }
            add(cbShowPermanentDialog, GBC.eol());
            add(cbShowSessionDialog, GBC.eol());
            if (displayImmediateOption) {
                add(cbShowImmediateDialog, GBC.eol());
            }
            add(cbStandard, GBC.eol());
        }

        NotShowAgain getNotShowAgain() {
            return cbStandard.isSelected()
                    ? NotShowAgain.NO
                    : cbShowImmediateDialog.isSelected()
                    ? NotShowAgain.OPERATION
                    : cbShowSessionDialog.isSelected()
                    ? NotShowAgain.SESSION
                    : cbShowPermanentDialog.isSelected()
                    ? NotShowAgain.PERMANENT
                    : null;
        }
    }
}
