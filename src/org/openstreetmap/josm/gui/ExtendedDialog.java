package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
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
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

public class ExtendedDialog extends JDialog {
    private int result = 0;
    public static final int DialogClosedOtherwise = 0;
    private boolean toggleable = false;
    private String rememberSizePref = "";
    private WindowGeometry defaultWindowGeometry = null;
    private String togglePref = "";
    private int toggleValue = -1;
    private String toggleCheckboxText = tr("Do not show again (remembers choice)");
    private JCheckBox toggleCheckbox = null;
    private Component parent;
    private Component content;
    private final String[] bTexts;
    private String[] bToolTipTexts;
    private String[] bIcons;
    private int cancelButtonIdx = -1;
    private int defaultButtonIdx = -1;
    private JButton defaultButton = null;

    /** true, if the dialog should include a help button */
    private boolean showHelpButton;
    /** the help topic */
    private String helpTopic;

    /**
     * set to true if the content of the extended dialog should
     * be placed in a {@see JScrollPane}
     */
    private boolean placeContentInScrollPane;

    // For easy access when inherited
    protected Object contentConstraints = GBC.eol().anchor(GBC.CENTER).fill(GBC.BOTH).insets(5,10,5,0);
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
     *   <li><code>setRememberWindowGeometry</code></li>
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
     * Allows decorating the buttons with icons. Expects an String[] with paths
     * to images relative to JOSM/images.
     * @param buttonIcons
     */
    public ExtendedDialog setButtonIcons(String[] buttonIcons) {
        this.bIcons = buttonIcons;
        return this;
    }

    /**
     * Allows decorating the buttons with tooltips. Expects an String[] with translated
     * tooltip texts.
     *
     * @param toolTipTexts the tool tip texts. Ignored, if null.
     */
    public ExtendedDialog setToolTipTexts(String[] toolTipTexts) {
        this.bToolTipTexts = toolTipTexts;
        return this;
    }

    /**
     * Sets the content that will be displayed in the message dialog.
     *
     * Note that depending on your other settings more UI elements may appear.
     * The content is played on top of the other elements though.
     *
     * @param content Any element that can be displayed in the message dialog
     */
    public ExtendedDialog setContent(Component content) {
        return setContent(content, true);
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
    public ExtendedDialog setContent(Component content, boolean placeContentInScrollPane) {
        this.content = content;
        this.placeContentInScrollPane = placeContentInScrollPane;
        return this;
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
    public ExtendedDialog setContent(String message) {
        return setContent(string2label(message), false);
    }

    /**
     * Show the dialog to the user. Call this after you have set all options
     * for the dialog. You can retrieve the result using <code>getValue</code>
     */
    public ExtendedDialog showDialog() {
        // Check if the user has set the dialog to not be shown again
        if(toggleCheckState(togglePref)) {
            result = toggleValue;
            return this;
        }

        setupDialog();
        if (defaultButton != null) {
            getRootPane().setDefaultButton(defaultButton);
        }
        setVisible(true);
        toggleSaveState();
        return this;
    }

    /**
     * @return int * The selected button. The count starts with 1.
     *             * A return value of ExtendedDialog.DialogClosedOtherwise means the dialog has been closed otherwise.
     */
    public int getValue() {
        return result;
    }

    private boolean setupDone = false;

    /**
     * This is called by showDialog().
     * Only invoke from outside if you need to modify the contentPane
     */
    public void setupDialog() {
        if (setupDone)
            return;
        setupDone = true;

        setupEscListener();

        JButton button;
        JPanel buttonsPanel = new JPanel(new GridBagLayout());

        for(int i=0; i < bTexts.length; i++) {
            final int final_i = i;
            Action action = new AbstractAction(bTexts[i]) {
                public void actionPerformed(ActionEvent evt) {
                    buttonAction(final_i, evt);
                }
            };

            button = new JButton(action);
            if (i == defaultButtonIdx-1) {
                defaultButton = button;
            }
            if(bIcons != null && bIcons[i] != null) {
                button.setIcon(ImageProvider.get(bIcons[i]));
            }
            if (bToolTipTexts != null && i < bToolTipTexts.length && bToolTipTexts[i] != null) {
                button.setToolTipText(bToolTipTexts[i]);
            }

            if(i == 0) {
                rootPane.setDefaultButton(button);
            }
            buttonsPanel.add(button, GBC.std().insets(2,2,2,2));
            buttons.add(button);
        }
        if (showHelpButton) {
            buttonsPanel.add(new JButton(new HelpAction()), GBC.std().insets(2,2,2,2));
            HelpUtil.setHelpContext(getRootPane(),helpTopic);
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
    protected void buttonAction(int i, ActionEvent evt) {
        String a = evt.getActionCommand();
        result = i+1;
        setVisible(false);
    }

    /**
     * Tries to find a good value of how large the dialog should be
     * @return Dimension Size of the parent Component or 2/3 of screen size if not available
     */
    protected Dimension findMaxDialogSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension x = new Dimension(screenSize.width*2/3, screenSize.height*2/3);
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

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", actionListener);
    }

    /**
     * Override setVisible to be able to save the window geometry if required
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            repaint();
        }

        // Ensure all required variables are available
        if(rememberSizePref.length() != 0 && defaultWindowGeometry != null) {
            if(visible) {
                new WindowGeometry(rememberSizePref,
                        defaultWindowGeometry).applySafe(this);
            } else {
                new WindowGeometry(this).remember(rememberSizePref);
            }
        }
        super.setVisible(visible);
    }

    /**
     * Call this if you want the dialog to remember the size set by the user.
     * Set the pref to <code>null</code> or to an empty string to disable again.
     * By default, it's disabled.
     *
     * Note: If you want to set the width of this dialog directly use the usual
     * setSize, setPreferredSize, setMaxSize, setMinSize
     *
     * @param pref  The preference to save the dimension to
     * @param wg    The default window geometry that should be used if no
     *              existing preference is found (only takes effect if
     *              <code>pref</code> is not null or empty
     *
     */
    public ExtendedDialog setRememberWindowGeometry(String pref, WindowGeometry wg) {
        rememberSizePref = pref == null ? "" : pref;
        defaultWindowGeometry = wg;
        return this;
    }

    /**
     * Calling this will offer the user a "Do not show again" checkbox for the
     * dialog. Default is to not offer the choice; the dialog will be shown
     * every time.
     * @param togglePref  The preference to save the checkbox state to
     */
    public ExtendedDialog toggleEnable(String togglePref) {
        this.toggleable = true;
        this.togglePref = togglePref;
        return this;
    }

    /**
     * Call this if you "accidentally" called toggleEnable. This doesn't need
     * to be called for every dialog, as it's the default anyway.
     */
    public ExtendedDialog toggleDisable() {
        this.toggleable = false;
        return this;
    }

    /**
     * Overwrites the default "Don't show again" text of the toggle checkbox
     * if you want to give more information. Only has an effect if
     * <code>toggleEnable</code> is set.
     * @param text
     */
    public ExtendedDialog setToggleCheckboxText(String text) {
        this.toggleCheckboxText = text;
        return this;
    }

    /**
     * Sets the button that will react to ENTER.
     */
    public ExtendedDialog setDefaultButton(int defaultButtonIdx) {
        this.defaultButtonIdx = defaultButtonIdx;
        return this;
    }

    /**
     * Used in combination with toggle:
     * If the user presses 'cancel' the toggle settings are ignored and not saved to the pref
     * @param cancelButton index of the button that stands for cancel
     */
    public ExtendedDialog setCancelButton(int cancelButtonIdx) {
        this.cancelButtonIdx = cancelButtonIdx;
        return this;
    }

    /**
     * This function returns true if the dialog has been set to "do not show again"
     * @return true if dialog should not be shown again
     */
    private boolean toggleCheckState(String togglePref) {
        toggleable = togglePref != null && !togglePref.equals("");

        toggleValue = Main.pref.getInteger("message."+togglePref+".value", -1);
        // No identifier given, so return false (= show the dialog)
        if(!toggleable || toggleValue == -1)
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
        if(!toggleable || toggleCheckbox == null || result == cancelButtonIdx || result == ExtendedDialog.DialogClosedOtherwise)
            return;
        Main.pref.put("message."+ togglePref, !toggleCheckbox.isSelected());
        Main.pref.putInteger("message."+togglePref+".value", result);
    }

    /**
     * Convenience function that converts a given string into a JMultilineLabel
     * @param msg
     * @return JMultilineLabel
     */
    private static JMultilineLabel string2label(String msg) {
        JMultilineLabel lbl = new JMultilineLabel(msg);
        // Make it not wider than 1/2 of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        lbl.setMaxWidth(screenSize.width/2);
        return lbl;
    }

    /**
     * Configures how this dialog support for context sensitive help.
     * <ul>
     *  <li>if helpTopic is null, the dialog doesn't provide context sensitive help</li>
     *  <li>if helpTopic != null, the dialog redirect user to the help page for this helpTopic when
     *  the user clicks F1 in the dialog</li>
     *  <li>if showHelpButton is true, the dialog displays "Help" button (rightmost button in
     *  the button row)</li>
     * </ul>
     *
     * @param helpTopic the help topic
     * @param showHelpButton true, if the dialog displays a help button
     */
    public ExtendedDialog configureContextsensitiveHelp(String helpTopic, boolean showHelpButton) {
        this.helpTopic = helpTopic;
        this.showHelpButton = showHelpButton;
        return this;
    }

    class HelpAction extends AbstractAction {
        public HelpAction() {
            putValue(SHORT_DESCRIPTION, tr("Show help information"));
            putValue(NAME, tr("Help"));
            putValue(SMALL_ICON, ImageProvider.get("help"));
        }

        public void actionPerformed(ActionEvent e) {
            HelpBrowser.setUrlForHelpTopic(helpTopic);
        }
    }
}
