// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;

/**
 * OptionPaneUtil provides static utility methods for displaying modal message dialogs.
 * 
 * They wrap the methods provided by {@see JOptionPane}. Within JOSM you should use these
 * methods rather than the bare methods from {@see JOptionPane} because the methods provided
 * by OptionPaneUtil ensure that a dialog window is always on top and isn't hidden by one of the
 * JOSM windows for detached dialogs, relation editors, history browser and the like.
 *
 * Typical usage for a message dialog:
 * <pre>
 *    OptionPaneUtil.showMessageDialog(Main.parent, tr("My message"), tr("My title"), JOptionPane.WARNING_MESSAGE);
 * </pre>
 * 
 */
public class OptionPaneUtil {

    /**
     * this is static utility class, no instances allowed
     */
    private OptionPaneUtil () {}

    /**
     * prepares a dialog as message dialog which is always on the top of the windows
     * stack
     * 
     * @param dialog the dialog
     */
    static protected void prepareDialog(JDialog dialog) {
        try {
            dialog.setAlwaysOnTop(true);
        } catch(SecurityException e) {
            System.out.println(tr("Warning: failed to put option pane dialog always on top. Exception was: {0}", e.toString()));
        }
        dialog.setModal(true);
        dialog.toFront();
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    /**
     * Displays an message in modal dialog with an OK button. Makes sure the dialog
     * is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     * 
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * @param messageType the message type
     * 
     * @see JOptionPane#INFORMATION_MESSAGE
     * @see JOptionPane#WARNING_MESSAGE
     * @see JOptionPane#ERROR_MESSAGE
     */
    static public void showMessageDialog(Component parent, Object message, String title, int messageType) {
        JOptionPane op = new JOptionPane(
                message,
                messageType
        );
        JDialog dialog = op.createDialog(Main.parent, title);
        prepareDialog(dialog);
        dialog.setVisible(true);
    }


    /**
     * Displays an confirmation dialog with two or three buttons. Makes sure the dialog
     * is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     * 
     * Set <code>optionType</code> to {@see JOptionPane#YES_NO_OPTION} for a dialog with a YES and
     * a NO button.

     * Set <code>optionType</code> to {@see JOptionPane#YES_NO_CANCEL_OPTION} for a dialog with a YES,
     * a NO and a CANCEL button
     * 
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * @param optionType  the option type
     * @param messageType the message type
     * 
     * @see JOptionPane#INFORMATION_MESSAGE
     * @see JOptionPane#WARNING_MESSAGE
     * @see JOptionPane#ERROR_MESSAGE
     */
    static public int showConfirmationDialog(Component parent, Object message, String title, int optionType, int messageType) throws HeadlessException {
        JOptionPane op = new JOptionPane(
                message,
                messageType,
                optionType
        );
        JDialog dialog = op.createDialog(Main.parent, title);
        prepareDialog(dialog);
        dialog.setVisible(true);
        Object value = op.getValue();
        if (value == null || ! (value instanceof Integer))
            return JOptionPane.CLOSED_OPTION;
        return (Integer)value;
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
     * 
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
    static public boolean showConfirmationDialog(Component parent, Object message, String title, int optionType, int messageType, int trueOption) throws HeadlessException {
        int ret = showConfirmationDialog(parent, message, title, optionType, messageType);
        return ret == trueOption;
    }

    /**
     * Displays an confirmation dialog with three buttons: YES, NO and CANCEL. Makes sure the dialog
     * is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like. The dialog is of type {@see JOptionPane#QUESTION_MESSAGE}
     * 
     * @param parent  the parent component
     * @param message  the message
     * @param title the title
     * 
     */
    static public int showConfirmationDialog(Component parent, Object message, String title) throws HeadlessException {
        return showConfirmationDialog(parent, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Displays a confirmation dialog with arbitrary confirmation options. Makes sure the dialog
     * is always on top even if there are other open windows like detached dialogs,
     * relation editors, history browsers and the like.
     * 
     * Provide an array of option labels in <code>options</code>, i.e.
     * <pre>
     *    options = new String[] {"Yes, do it!", "No, not yet", "Abort"};
     * </pre>
     * 
     * Replies the index of the selected option or {@see JOptionPane#CLOSED_OPTION} if
     * the dialog was closed.
     * 
     * @param parent the parent component
     * @param message the message
     * @param title  the title
     * @param optionType
     * @param messageType
     * @param options
     * @param initialValue
     * @return  the index of the selected option or {@see JOptionPane#CLOSED_OPTION}
     */
    static public int showOptionDialog(Component parent,
            Object message,
            String title,
            int optionType,
            int messageType,
            Object[] options,
            Object initialValue) {

        JOptionPane op = new JOptionPane(
                message,
                messageType,
                optionType,
                null /* no icon */,
                options,
                initialValue
        );
        JDialog dialog = op.createDialog(Main.parent, title);
        prepareDialog(dialog);
        dialog.setVisible(true);
        Object value = op.getValue();
        if (value == null)
            return JOptionPane.CLOSED_OPTION;
        if (options == null) {
            if (value instanceof Integer)
                return (Integer)value;
            return JOptionPane.CLOSED_OPTION;
        }
        for(int i = 0; i < options.length; i++) {
            if(options[i].equals(value))
                return i;
        }
        return JOptionPane.CLOSED_OPTION;
    }
}
