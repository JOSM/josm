package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;


public class ExtendedDialog extends JDialog {
    private int result = 0;
    public static final int DialogNotShown = -99;
    public static final int DialogClosedOtherwise = 0;
    private boolean toggleable = false;
    private String togglePref = "";
    private String toggleCheckboxText = tr("Do not show again");
    private JCheckBox toggleCheckbox = null;
    private Component parent;
    private Component content;
    private final String[] bTexts;
    private String[] bIcons;
    /**
     * set to true if the content of the extended dialog should
     * be placed in a {@see JScrollPane}
     */
    private boolean placeContentInScrollPane;

    // For easy access when inherited
    protected Object contentConstraints = GBC.eol().anchor(GBC.CENTER).fill(GBC.HORIZONTAL).insets(5,10,5,0);
    protected ArrayList<JButton> buttons = new ArrayList<JButton>();

    /**
     * This method sets up the most basic options for the dialog. Add all more
     * advanced features with dedicated methods.
     * Possible features:
     * <ul>
     *   <li><code>setButtonIcons</code></li>
     *   <li><code>setContent</code></li>
     *   <li><code>toggleEnable</code></li>
     *   <li><code>toggleDisable</code></li>
     *   <li><code>setToggleCheckboxText</code></li>
     * </ul>
     * 
     * When done, call <code>showDialog</code> to display it. You can receive
     * the user's choice using <code>getValue</code>. Have a look at this function
     * for possible return values.
     * 
     * @param parent       The parent element that will be used for position and maximum size
     * @param title        The text that will be shown in the window titlebar
     * @param buttonTexts  String Array of the text that will appear on the buttons. The first button is the default one.
     */
    public ExtendedDialog(Component parent, String title, String[] buttonTexts) {
        super(JOptionPane.getFrameForComponent(parent), title, true);
        this.parent = parent;
        bTexts = buttonTexts;
    }

    /**
     * Same as above but lets you define if the dialog should be modal.
     */
    public ExtendedDialog(Component parent, String title, String[] buttonTexts,
            boolean modal) {
        super(JOptionPane.getFrameForComponent(parent), title, modal);
        this.parent = parent;
        bTexts = buttonTexts;
    }

    /**
     * Sets up the dialog. The first button is always the default.
     * @param parent The parent element that will be used for position and maximum size
     * @param title The text that will be shown in the window titlebar
     * @param content Any component that should be show above the buttons (e.g. JLabel)
     * @param buttonTexts The labels that will be displayed on the buttons
     * @param buttonIcons The path to the icons that will be displayed on the buttons. Path is relative to JOSM's image directory. File extensions need to be included. If a button should not have an icon pass null.
     */
    @Deprecated public ExtendedDialog(Component parent, String title, Component content,
            String[] buttonTexts, String[] buttonIcons) {
        super(JOptionPane.getFrameForComponent(parent), title, true /* modal */);
        this.parent = parent;
        bTexts = buttonTexts;
        this.content = content;
        this.bIcons = buttonIcons;
        setupDialog();
        setVisible(true);
    }

    @Deprecated public ExtendedDialog(Component parent, String title, Component content,
            String[] buttonTexts) {
        this(parent, title, content, buttonTexts, null);
    }

    /**
     * Sets up the dialog and displays the given message in a breakable label
     */
    @Deprecated public ExtendedDialog(Component parent, String title, String message,
            String[] buttonTexts, String[] buttonIcons) {
        this(parent, title, string2label(message), buttonTexts, buttonIcons);
    }

    @Deprecated public ExtendedDialog(Component parent, String title, String message,
            String[] buttonTexts) {
        this(parent, title, message, buttonTexts, null);
    }

    /**
     * Allows decorating the buttons with icons. Expects an String[] with paths
     * to images relative to JOSM/images.
     * @param buttonIcons
     */
    public void setButtonIcons(String[] buttonIcons) {
        this.bIcons = buttonIcons;
    }

    /**
     * Sets the content that will be displayed in the message dialog.
     * 
     * Note that depending on your other settings more UI elements may appear.
     * The content is played on top of the other elements though.
     * 
     * @param content Any element that can be displayed in the message dialog
     */
    public void setContent(Component content) {
        setContent(content, true);
    }

    /**
     * Sets the content that will be displayed in the message dialog.
     * 
     * Note that depending on your other settings more UI elements may appear.
     * The content is played on top of the other elements though.
     * 
     * @param content Any element that can be displayed in the message dialog
     * @param placeContentInScrollPane if  true, places  the content in a JScrollPane
     * 
     */
    public void setContent(Component content, boolean placeContentInScrollPane) {
        this.content = content;
        this.placeContentInScrollPane = placeContentInScrollPane;
    }

    /**
     * Sets the message that will be displayed. The String will be automatically
     * wrapped if it is too long.
     * 
     * Note that depending on your other settings more UI elements may appear.
     * The content is played on top of the other elements though.
     * 
     * @param message The text that should be shown to the user
     */
    public void setContent(String message) {
        setContent(string2label(message), true);
    }

    /**
     * Show the dialog to the user. Call this after you have set all options
     * for the dialog. You can retrieve the result using <code>getValue</code>
     */
    public void showDialog() {
        // Check if the user has set the dialog to not be shown again
        if(toggleCheckState(togglePref)) {
            result = ExtendedDialog.DialogNotShown;
            return;
        }

        setupDialog();
        setVisible(true);
        toggleSaveState();
    }

    /**
     * @return int * The selected button. The count starts with 1.
     *             * A return value of ExtendedDialog.DialogClosedOtherwise means the dialog has been closed otherwise.
     *             * A return value of ExtendedDialog.DialogNotShown means the
     *               dialog has been toggled off in the past
     */
    public int getValue() {
        return result;
    }

    @Deprecated protected void setupDialog(Component content, String[] buttonIcons) {
        this.setContent(content);
        this.setButtonIcons(buttonIcons);
        this.setupDialog();
    }

    protected void setupDialog() {
        setupEscListener();

        JButton button;
        JPanel buttonsPanel = new JPanel(new GridBagLayout());

        for(int i=0; i < bTexts.length; i++) {
            Action action = new AbstractAction(bTexts[i]) {
                public void actionPerformed(ActionEvent evt) {
                    buttonAction(evt);
                }
            };

            button = new JButton(action);
            if(bIcons != null && bIcons[i] != null) {
                button.setIcon(ImageProvider.get(bIcons[i]));
            }

            if(i == 0) {
                rootPane.setDefaultButton(button);
            }
            buttonsPanel.add(button, GBC.std().insets(2,2,2,2));
            buttons.add(button);
        }

        JPanel cp = new JPanel(new GridBagLayout());
        cp.add(content, contentConstraints);

        if(toggleable) {
            toggleCheckbox = new JCheckBox(toggleCheckboxText);
            boolean showDialog = Main.pref.getBoolean("message."+ togglePref, true);
            toggleCheckbox.setSelected(!showDialog);
            cp.add(toggleCheckbox, GBC.eol().anchor(GBC.LINE_START).insets(5,5,5,5));
        }

        cp.add(buttonsPanel, GBC.eol().anchor(GBC.CENTER).insets(5,5,5,5));
        if (placeContentInScrollPane) {
            JScrollPane pane = new JScrollPane(cp);
            pane.setBorder(null);
            setContentPane(pane);
        } else {
            setContentPane(cp);
        }
        pack();

        // Try to make it not larger than the parent window or at least not larger than 2/3 of the screen
        Dimension d = getSize();
        Dimension x = findMaxDialogSize();

        boolean limitedInWidth = d.width > x.width;
        boolean limitedInHeight = d.height > x.height;

        if(x.width  > 0 && d.width  > x.width) {
            d.width  = x.width;
        }
        if(x.height > 0 && d.height > x.height) {
            d.height = x.height;
        }

        // We have a vertical scrollbar and enough space to prevent a horizontal one
        if(!limitedInWidth && limitedInHeight) {
            d.width += new JScrollBar().getPreferredSize().width;
        }

        setSize(d);
        setLocationRelativeTo(parent);
    }

    /**
     * This gets performed whenever a button is clicked or activated
     * @param evt the button event
     */
    protected void buttonAction(ActionEvent evt) {
        String a = evt.getActionCommand();
        for(int i=0; i < bTexts.length; i++)
            if(bTexts[i].equals(a)) {
                result = i+1;
                break;
            }

        setVisible(false);
    }

    /**
     * Tries to find a good value of how large the dialog should be
     * @return Dimension Size of the parent Component or 2/3 of screen size if not available
     */
    protected Dimension findMaxDialogSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension x = new Dimension(Math.round(screenSize.width*2/3),
                Math.round(screenSize.height*2/3));
        try {
            if(parent != null) {
                x = JOptionPane.getFrameForComponent(parent).getSize();
            }
        } catch(NullPointerException e) { }
        return x;
    }

    /**
     * Makes the dialog listen to ESC keypressed
     */
    private void setupEscListener() {
        Action actionListener = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                // 0 means that the dialog has been closed otherwise.
                // We need to set it to zero again, in case the dialog has been re-used
                // and the result differs from its default value
                result = ExtendedDialog.DialogClosedOtherwise;
                setVisible(false);
            }
        };

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", actionListener);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            repaint();
        }
    }

    /**
     * Calling this will offer the user a "Do not show again" checkbox for the
     * dialog. Default is to not offer the choice; the dialog will be shown
     * every time. If the dialog is not shown due to the previous choice of the
     * user, the result <code>ExtendedDialog.DialogNotShown</code> is returned
     * @param togglePref  The preference to save the checkbox state to
     */
    public void toggleEnable(String togglePref) {
        this.toggleable = true;
        this.togglePref = togglePref;
    }

    /**
     * Call this if you "accidentally" called toggleEnable. This doesn't need
     * to be called for every dialog, as it's the default anyway.
     */
    public void toggleDisable() {
        this.toggleable = false;
    }

    /**
     * Overwrites the default "Don't show again" text of the toggle checkbox
     * if you want to give more information. Only has an effect if
     * <code>toggleEnable</code> is set.
     * @param text
     */
    public void setToggleCheckboxText(String text) {
        this.toggleCheckboxText = text;
    }

    /**
     * This function returns true if the dialog has been set to "do not show again"
     * @return true if dialog should not be shown again
     */
    private boolean toggleCheckState(String togglePref) {
        toggleable = togglePref != null && !togglePref.equals("");

        // No identifier given, so return false (= show the dialog)
        if(!toggleable)
            return false;

        this.togglePref = togglePref;
        // The pref is true, if the dialog should be shown.
        return !(Main.pref.getBoolean("message."+ togglePref, true));
    }

    /**
     * This function checks the state of the "Do not show again" checkbox and
     * writes the corresponding pref
     */
    private void toggleSaveState() {
        if(!toggleable || toggleCheckbox == null)
            return;
        Main.pref.put("message."+ togglePref, !toggleCheckbox.isSelected());
    }

    /**
     * Convenience function that converts a given string into a JMultilineLabel
     * @param msg
     * @return JMultilineLabel
     */
    private static JMultilineLabel string2label(String msg) {
        JMultilineLabel lbl = new JMultilineLabel(msg);
        // Make it not wider than 2/3 of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        lbl.setMaxWidth(Math.round(screenSize.width*2/3));
        return lbl;
    }
}