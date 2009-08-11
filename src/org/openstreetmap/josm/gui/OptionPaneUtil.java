// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.Icon;
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

        // always on top can be disabled in a configuration option. This is necessary
        // for some WM on Linux, i.e. fluxbox. There, always-on-top propagates to the
        // parent window, i.e. the JOSM window itself.
        //
        // FIXME: this is a temporary solution. I'm still looking for an approach which
        // works across all OS and WMs. Can we get rid of "always-on-top" in JOSM
        // completely?
        //
        if (Main.pref.getBoolean("window-handling.option-pane-always-on-top", true)) {
            try {
                dialog.setAlwaysOnTop(true);
            } catch(SecurityException e) {
                System.out.println(tr("Warning: failed to put option pane dialog always on top. Exception was: {0}", e.toString()));
            }
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

    /**
     * Shows a dialog requesting input from the user parented to
     * <code>parentComponent</code> with the dialog having the title
     * <code>title</code> and message type <code>messageType</code>.
     *
     * @param parentComponent  the parent <code>Component</code> for the
     *          dialog
     * @param message  the <code>Object</code> to display
     * @param title    the <code>String</code> to display in the dialog
     *          title bar
     * @param messageType the type of message that is to be displayed:
     *                  <code>ERROR_MESSAGE</code>,
     *          <code>INFORMATION_MESSAGE</code>,
     *          <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *          or <code>PLAIN_MESSAGE</code>
     * @exception HeadlessException if
     *   <code>GraphicsEnvironment.isHeadless</code> returns
     *   <code>true</code>
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static String showInputDialog(Component parentComponent,
            Object message, String title, int messageType)
    throws HeadlessException {
        return (String)showInputDialog(parentComponent, message, title,
                messageType, null, null, null);
    }

    /**
     * Prompts the user for input in a blocking dialog where the
     * initial selection, possible selections, and all other options can
     * be specified. The user will able to choose from
     * <code>selectionValues</code>, where <code>null</code> implies the
     * user can input
     * whatever they wish, usually by means of a <code>JTextField</code>.
     * <code>initialSelectionValue</code> is the initial value to prompt
     * the user with. It is up to the UI to decide how best to represent
     * the <code>selectionValues</code>, but usually a
     * <code>JComboBox</code>, <code>JList</code>, or
     * <code>JTextField</code> will be used.
     *
     * @param parentComponent  the parent <code>Component</code> for the
     *          dialog
     * @param message  the <code>Object</code> to display
     * @param title    the <code>String</code> to display in the
     *          dialog title bar
     * @param messageType the type of message to be displayed:
     *                  <code>ERROR_MESSAGE</code>,
     *          <code>INFORMATION_MESSAGE</code>,
     *          <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *          or <code>PLAIN_MESSAGE</code>
     * @param icon     the <code>Icon</code> image to display
     * @param selectionValues an array of <code>Object</code>s that
     *          gives the possible selections
     * @param initialSelectionValue the value used to initialize the input
     *                 field
     * @return user's input, or <code>null</code> meaning the user
     *          canceled the input
     * @exception HeadlessException if
     *   <code>GraphicsEnvironment.isHeadless</code> returns
     *   <code>true</code>
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static Object showInputDialog(Component parentComponent,
            Object message, String title, int messageType, Icon icon,
            Object[] selectionValues, Object initialSelectionValue)
    throws HeadlessException {
        JOptionPane    pane = new JOptionPane(message, messageType,
                JOptionPane.OK_CANCEL_OPTION, icon,
                null, null);

        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        pane.setComponentOrientation(((parentComponent == null) ?
                JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

        JDialog dialog = pane.createDialog(parentComponent, title);
        // this shows the dialog always on top and brings it to front
        //
        prepareDialog(dialog);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();

        Object value = pane.getInputValue();
        if (value == JOptionPane.UNINITIALIZED_VALUE)
            return null;
        return value;
    }
}
