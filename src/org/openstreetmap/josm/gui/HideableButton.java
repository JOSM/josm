// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import javax.swing.Icon;

/**
 * An interface to provide showing/hiding method for buttons,
 * when hidden state is stored in preferences
 */
interface HideableButton {
    void applyButtonHiddenPreferences();
    void setButtonHidden(boolean b);
    void showButton();
    void hideButton();
    String getActionName();
    Icon getIcon();
    boolean isButtonVisible();
    void setShowHideButtonListener(ShowHideButtonListener l);
}
