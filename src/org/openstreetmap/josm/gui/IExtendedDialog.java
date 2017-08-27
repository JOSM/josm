// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.util.WindowGeometry;

/**
 * Extracted interface of {@link ExtendedDialog} class.
 * @since 11945
 */
public interface IExtendedDialog {

    /**
     * Allows decorating the buttons with icons.
     * @param buttonIcons The button icons
     * @return {@code this}
     */
    ExtendedDialog setButtonIcons(Icon... buttonIcons);

    /**
     * Convenience method to provide image names instead of images.
     * @param buttonIcons The button icon names
     * @return {@code this}
     */
    ExtendedDialog setButtonIcons(String... buttonIcons);

    /**
     * Allows decorating the buttons with tooltips. Expects a String array with
     * translated tooltip texts.
     *
     * @param toolTipTexts the tool tip texts. Ignored, if null.
     * @return {@code this}
     */
    ExtendedDialog setToolTipTexts(String... toolTipTexts);

    /**
     * Sets the content that will be displayed in the message dialog.
     *
     * Note that depending on your other settings more UI elements may appear.
     * The content is played on top of the other elements though.
     *
     * @param content Any element that can be displayed in the message dialog
     * @return {@code this}
     */
    ExtendedDialog setContent(Component content);

    /**
     * Sets the content that will be displayed in the message dialog.
     *
     * Note that depending on your other settings more UI elements may appear.
     * The content is played on top of the other elements though.
     *
     * @param content Any element that can be displayed in the message dialog
     * @param placeContentInScrollPane if true, places the content in a JScrollPane
     * @return {@code this}
     */
    ExtendedDialog setContent(Component content, boolean placeContentInScrollPane);

    /**
     * Sets the message that will be displayed. The String will be automatically
     * wrapped if it is too long.
     *
     * Note that depending on your other settings more UI elements may appear.
     * The content is played on top of the other elements though.
     *
     * @param message The text that should be shown to the user
     * @return {@code this}
     */
    ExtendedDialog setContent(String message);

    /**
     * Decorate the dialog with an icon that is shown on the left part of
     * the window area. (Similar to how it is done in {@link JOptionPane})
     * @param icon The icon to display
     * @return {@code this}
     */
    ExtendedDialog setIcon(Icon icon);

    /**
     * Convenience method to allow values that would be accepted by {@link JOptionPane} as messageType.
     * @param messageType The {@link JOptionPane} messageType
     * @return {@code this}
     */
    ExtendedDialog setIcon(int messageType);

    /**
     * Show the dialog to the user. Call this after you have set all options
     * for the dialog. You can retrieve the result using {@link #getValue()}.
     * @return {@code this}
     */
    ExtendedDialog showDialog();

    /**
     * Retrieve the user choice after the dialog has been closed.
     *
     * @return <ul> <li>The selected button. The count starts with 1.</li>
     *              <li>A return value of {@link ExtendedDialog#DialogClosedOtherwise} means the dialog has been closed otherwise.</li>
     *         </ul>
     */
    int getValue();

    /**
     * This is called by {@link #showDialog()}.
     * Only invoke from outside if you need to modify the contentPane
     */
    void setupDialog();

    /**
     * Call this if you want the dialog to remember the geometry (size and position) set by the user.
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
     * @return {@code this}
     */
    ExtendedDialog setRememberWindowGeometry(String pref, WindowGeometry wg);

    /**
     * Calling this will offer the user a "Do not show again" checkbox for the
     * dialog. Default is to not offer the choice; the dialog will be shown
     * every time.
     * Currently, this is not supported for non-modal dialogs.
     * @param togglePref  The preference to save the checkbox state to
     * @return {@code this}
     */
    ExtendedDialog toggleEnable(String togglePref);

    /**
     * Sets the button that will react to ENTER.
     * @param defaultButtonIdx The button index (starts to 1)
     * @return {@code this}
     */
    ExtendedDialog setDefaultButton(int defaultButtonIdx);

    /**
     * Used in combination with toggle:
     * If the user presses 'cancel' the toggle settings are ignored and not saved to the pref
     * @param cancelButtonIdx index of the button that stands for cancel, accepts multiple values
     * @return {@code this}
     */
    ExtendedDialog setCancelButton(Integer... cancelButtonIdx);

    /**
     * Makes default button request initial focus or not.
     * @param focus {@code true} to make default button request initial focus
     * @since 7407
     */
    void setFocusOnDefaultButton(boolean focus);

    /**
     * This function returns true if the dialog has been set to "do not show again"
     * @return true if dialog should not be shown again
     */
    boolean toggleCheckState();

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
     * @return {@code this}
     */
    ExtendedDialog configureContextsensitiveHelp(String helpTopic, boolean showHelpButton);
}
