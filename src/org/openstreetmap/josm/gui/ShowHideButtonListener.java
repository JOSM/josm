// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

/**
 * When some component (ToggleDialog, for example) is linked to button
 * and needs information about button showing/hiding events, this interface
 * is used, setting the listener should be implemented by @class HideableButton
 */
public interface ShowHideButtonListener {
    /**
     * Called when the button is shown
     */
    void buttonShown();

    /**
     * Called when the button is hidden
     */
    void buttonHidden();
}
