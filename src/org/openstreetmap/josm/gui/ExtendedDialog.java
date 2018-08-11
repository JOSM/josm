// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * General configurable dialog window.
 *
 * If dialog is modal, you can use {@link #getValue()} to retrieve the
 * button index. Note that the user can close the dialog
 * by other means. This is usually equivalent to cancel action.
 *
 * For non-modal dialogs, {@link #buttonAction(int, ActionEvent)} can be overridden.
 *
 * There are various options, see below.
 *
 * Note: The button indices are counted from 1 and upwards.
 * So for {@link #getValue()}, {@link #setDefaultButton(int)} and
 * {@link #setCancelButton} the first button has index 1.
 *
 * Simple example:
 * <pre>
 *  ExtendedDialog ed = new ExtendedDialog(
 *          Main.parent, tr("Dialog Title"),
 *          new String[] {tr("Ok"), tr("Cancel")});
 *  ed.setButtonIcons(new String[] {"ok", "cancel"});   // optional
 *  ed.setIcon(JOptionPane.WARNING_MESSAGE);            // optional
 *  ed.setContent(tr("Really proceed? Interesting things may happen..."));
 *  ed.showDialog();
 *  if (ed.getValue() == 1) { // user clicked first button "Ok"
 *      // proceed...
 *  }
 * </pre>
 */
public class ExtendedDialog extends JDialog implements IExtendedDialog {
    private final boolean disposeOnClose;
    private volatile int result;
    public static final int DialogClosedOtherwise = 0;
    private boolean toggleable;
    private String rememberSizePref = "";
    private transient WindowGeometry defaultWindowGeometry;
    private String togglePref = "";
    private int toggleValue = -1;
    private ConditionalOptionPaneUtil.MessagePanel togglePanel;
    private Component parent;
    private Component content;
    private final String[] bTexts;
    private String[] bToolTipTexts;
    private transient Icon[] bIcons;
    private Set<Integer> cancelButtonIdx = Collections.emptySet();
    private int defaultButtonIdx = 1;
    protected JButton defaultButton;
    private transient Icon icon;
    private boolean modal;
    private boolean focusOnDefaultButton;

    /** true, if the dialog should include a help button */
    private boolean showHelpButton;
    /** the help topic */
    private String helpTopic;

    /**
     * set to true if the content of the extended dialog should
     * be placed in a {@link JScrollPane}
     */
    private boolean placeContentInScrollPane;

    // For easy access when inherited
    protected transient Insets contentInsets = new Insets(10, 5, 0, 5);
    protected transient List<JButton> buttons = new ArrayList<>();

    /**
     * This method sets up the most basic options for the dialog. Add more
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
    public ExtendedDialog(Component parent, String title, String... buttonTexts) {
        this(parent, title, buttonTexts, true, true);
    }

    /**
     * Same as above but lets you define if the dialog should be modal.
     * @param parent The parent element that will be used for position and maximum size
     * @param title The text that will be shown in the window titlebar
     * @param buttonTexts String Array of the text that will appear on the buttons. The first button is the default one.
     * @param modal Set it to {@code true} if you want the dialog to be modal
     */
    public ExtendedDialog(Component parent, String title, String[] buttonTexts, boolean modal) {
        this(parent, title, buttonTexts, modal, true);
    }

    /**
     * Same as above but lets you define if the dialog should be disposed on close.
     * @param parent The parent element that will be used for position and maximum size
     * @param title The text that will be shown in the window titlebar
     * @param buttonTexts String Array of the text that will appear on the buttons. The first button is the default one.
     * @param modal Set it to {@code true} if you want the dialog to be modal
     * @param disposeOnClose whether to call {@link #dispose} when closing the dialog
     */
    public ExtendedDialog(Component parent, String title, String[] buttonTexts, boolean modal, boolean disposeOnClose) {
        super(searchRealParent(parent), title, modal ? ModalityType.DOCUMENT_MODAL : ModalityType.MODELESS);
        this.parent = parent;
        this.modal = modal;
        bTexts = Utils.copyArray(buttonTexts);
        if (disposeOnClose) {
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        }
        this.disposeOnClose = disposeOnClose;
    }

    private static Frame searchRealParent(Component parent) {
        if (parent == null) {
            return null;
        } else {
            return GuiHelper.getFrameForComponent(parent);
        }
    }

    @Override
    public ExtendedDialog setButtonIcons(Icon... buttonIcons) {
        this.bIcons = Utils.copyArray(buttonIcons);
        return this;
    }

    @Override
    public ExtendedDialog setButtonIcons(String... buttonIcons) {
        bIcons = new Icon[buttonIcons.length];
        for (int i = 0; i < buttonIcons.length; ++i) {
            bIcons[i] = ImageProvider.get(buttonIcons[i], ImageSizes.LARGEICON);
        }
        return this;
    }

    @Override
    public ExtendedDialog setToolTipTexts(String... toolTipTexts) {
        this.bToolTipTexts = Utils.copyArray(toolTipTexts);
        return this;
    }

    @Override
    public ExtendedDialog setContent(Component content) {
        return setContent(content, true);
    }

    @Override
    public ExtendedDialog setContent(Component content, boolean placeContentInScrollPane) {
        this.content = content;
        this.placeContentInScrollPane = placeContentInScrollPane;
        return this;
    }

    @Override
    public ExtendedDialog setContent(String message) {
        return setContent(string2label(message), false);
    }

    @Override
    public ExtendedDialog setIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public ExtendedDialog setIcon(int messageType) {
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.errorIcon"));
            case JOptionPane.INFORMATION_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.informationIcon"));
            case JOptionPane.WARNING_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.warningIcon"));
            case JOptionPane.QUESTION_MESSAGE:
                return setIcon(UIManager.getIcon("OptionPane.questionIcon"));
            case JOptionPane.PLAIN_MESSAGE:
                return setIcon(null);
            default:
                throw new IllegalArgumentException("Unknown message type!");
        }
    }

    @Override
    public ExtendedDialog showDialog() {
        // Check if the user has set the dialog to not be shown again
        if (toggleCheckState()) {
            result = toggleValue;
            return this;
        }

        setupDialog();
        if (defaultButton != null) {
            getRootPane().setDefaultButton(defaultButton);
        }
        // Don't focus the "do not show this again" check box, but the default button.
        if (toggleable || focusOnDefaultButton) {
            requestFocusToDefaultButton();
        }
        setVisible(true);
        toggleSaveState();
        return this;
    }

    @Override
    public int getValue() {
        return result;
    }

    private boolean setupDone;

    @Override
    public void setupDialog() {
        if (setupDone)
            return;
        setupDone = true;

        setupEscListener();

        JButton button;
        JPanel buttonsPanel = new JPanel(new GridBagLayout());

        for (int i = 0; i < bTexts.length; i++) {
            button = new JButton(createButtonAction(i));
            if (i == defaultButtonIdx-1) {
                defaultButton = button;
            }
            if (bIcons != null && bIcons[i] != null) {
                button.setIcon(bIcons[i]);
            }
            if (bToolTipTexts != null && i < bToolTipTexts.length && bToolTipTexts[i] != null) {
                button.setToolTipText(bToolTipTexts[i]);
            }

            buttonsPanel.add(button, GBC.std().insets(2, 2, 2, 2));
            buttons.add(button);
        }
        if (showHelpButton) {
            buttonsPanel.add(new JButton(new HelpAction()), GBC.std().insets(2, 2, 2, 2));
            HelpUtil.setHelpContext(getRootPane(), helpTopic);
        }

        JPanel cp = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        int y = 0;
        gc.gridy = y++;
        gc.weightx = 0.0;
        gc.weighty = 0.0;

        if (icon != null) {
            JLabel iconLbl = new JLabel(icon);
            gc.insets = new Insets(10, 10, 10, 10);
            gc.anchor = GridBagConstraints.NORTH;
            gc.weighty = 1.0;
            cp.add(iconLbl, gc);
            gc.anchor = GridBagConstraints.CENTER;
            gc.gridx = 1;
        }

        gc.fill = GridBagConstraints.BOTH;
        gc.insets = contentInsets;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        cp.add(content, gc);

        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;

        if (toggleable) {
            togglePanel = new ConditionalOptionPaneUtil.MessagePanel(null, ConditionalOptionPaneUtil.isInBulkOperation(togglePref));
            gc.gridx = icon != null ? 1 : 0;
            gc.gridy = y++;
            gc.anchor = GridBagConstraints.LINE_START;
            gc.insets = new Insets(5, contentInsets.left, 5, contentInsets.right);
            cp.add(togglePanel, gc);
        }

        gc.gridy = y;
        gc.anchor = GridBagConstraints.CENTER;
            gc.insets = new Insets(5, 5, 5, 5);
        cp.add(buttonsPanel, gc);
        if (placeContentInScrollPane) {
            JScrollPane pane = new JScrollPane(cp);
            GuiHelper.setDefaultIncrement(pane);
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

        if (x.width > 0 && d.width > x.width) {
            d.width = x.width;
        }
        if (x.height > 0 && d.height > x.height) {
            d.height = x.height;
        }

        // We have a vertical scrollbar and enough space to prevent a horizontal one
        if (!limitedInWidth && limitedInHeight) {
            d.width += new JScrollBar().getPreferredSize().width;
        }

        setSize(d);
        setLocationRelativeTo(parent);
    }

    protected Action createButtonAction(final int i) {
        return new AbstractAction(bTexts[i]) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                buttonAction(i, evt);
            }
        };
    }

    /**
     * This gets performed whenever a button is clicked or activated
     * @param buttonIndex the button index (first index is 0)
     * @param evt the button event
     */
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        result = buttonIndex+1;
        setVisible(false);
    }

    /**
     * Tries to find a good value of how large the dialog should be
     * @return Dimension Size of the parent component if visible or 2/3 of screen size if not available or hidden
     */
    protected Dimension findMaxDialogSize() {
        Dimension screenSize = GuiHelper.getScreenSize();
        Dimension x = new Dimension(screenSize.width*2/3, screenSize.height*2/3);
        if (parent != null && parent.isVisible()) {
            x = GuiHelper.getFrameForComponent(parent).getSize();
        }
        return x;
    }

    /**
     * Makes the dialog listen to ESC keypressed
     */
    private void setupEscListener() {
        Action actionListener = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // 0 means that the dialog has been closed otherwise.
                // We need to set it to zero again, in case the dialog has been re-used
                // and the result differs from its default value
                result = ExtendedDialog.DialogClosedOtherwise;
                if (Logging.isDebugEnabled()) {
                    Logging.debug("{0} ESC action performed ({1}) from {2}",
                            getClass().getName(), actionEvent, new Exception().getStackTrace()[1]);
                }
                setVisible(false);
            }
        };

        InputMapUtils.addEscapeAction(getRootPane(), actionListener);
    }

    protected final void rememberWindowGeometry(WindowGeometry geometry) {
        if (geometry != null) {
            geometry.remember(rememberSizePref);
        }
    }

    protected final WindowGeometry initWindowGeometry() {
        return new WindowGeometry(rememberSizePref, defaultWindowGeometry);
    }

    /**
     * Override setVisible to be able to save the window geometry if required
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            repaint();
        }

        if (Logging.isDebugEnabled()) {
            Logging.debug(getClass().getName()+".setVisible("+visible+") from "+new Exception().getStackTrace()[1]);
        }

        // Ensure all required variables are available
        if (!rememberSizePref.isEmpty() && defaultWindowGeometry != null) {
            if (visible) {
                initWindowGeometry().applySafe(this);
            } else if (isShowing()) { // should fix #6438, #6981, #8295
                rememberWindowGeometry(new WindowGeometry(this));
            }
        }
        super.setVisible(visible);

        if (!visible && disposeOnClose) {
            dispose();
        }
    }

    @Override
    public ExtendedDialog setRememberWindowGeometry(String pref, WindowGeometry wg) {
        rememberSizePref = pref == null ? "" : pref;
        defaultWindowGeometry = wg;
        return this;
    }

    @Override
    public ExtendedDialog toggleEnable(String togglePref) {
        if (!modal) {
            throw new IllegalStateException();
        }
        this.toggleable = true;
        this.togglePref = togglePref;
        return this;
    }

    @Override
    public ExtendedDialog setDefaultButton(int defaultButtonIdx) {
        this.defaultButtonIdx = defaultButtonIdx;
        return this;
    }

    @Override
    public ExtendedDialog setCancelButton(Integer... cancelButtonIdx) {
        this.cancelButtonIdx = new HashSet<>(Arrays.<Integer>asList(cancelButtonIdx));
        return this;
    }

    @Override
    public void setFocusOnDefaultButton(boolean focus) {
        focusOnDefaultButton = focus;
    }

    private void requestFocusToDefaultButton() {
        if (defaultButton != null) {
            GuiHelper.runInEDT(defaultButton::requestFocusInWindow);
        }
    }

    @Override
    public final boolean toggleCheckState() {
        toggleable = togglePref != null && !togglePref.isEmpty();
        toggleValue = ConditionalOptionPaneUtil.getDialogReturnValue(togglePref);
        return toggleable && toggleValue != -1;
    }

    /**
     * This function checks the state of the "Do not show again" checkbox and
     * writes the corresponding pref.
     */
    protected void toggleSaveState() {
        if (!toggleable ||
                togglePanel == null ||
                cancelButtonIdx.contains(result) ||
                result == ExtendedDialog.DialogClosedOtherwise)
            return;
        togglePanel.getNotShowAgain().store(togglePref, result);
    }

    /**
     * Convenience function that converts a given string into a JMultilineLabel
     * @param msg the message to display
     * @return JMultilineLabel displaying {@code msg}
     */
    private static JMultilineLabel string2label(String msg) {
        JMultilineLabel lbl = new JMultilineLabel(msg);
        // Make it not wider than 1/2 of the screen
        Dimension screenSize = GuiHelper.getScreenSize();
        lbl.setMaxWidth(screenSize.width/2);
        // Disable default Enter key binding to allow dialog's one (then enables to hit default button from here)
        lbl.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new Object());
        return lbl;
    }

    @Override
    public ExtendedDialog configureContextsensitiveHelp(String helpTopic, boolean showHelpButton) {
        this.helpTopic = helpTopic;
        this.showHelpButton = showHelpButton;
        return this;
    }

    class HelpAction extends AbstractAction {
        /**
         * Constructs a new {@code HelpAction}.
         */
        HelpAction() {
            putValue(SHORT_DESCRIPTION, tr("Show help information"));
            putValue(NAME, tr("Help"));
            new ImageProvider("help").getResource().attachImageIcon(this, true);
            setEnabled(!NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            HelpBrowser.setUrlForHelpTopic(helpTopic);
        }
    }
}
