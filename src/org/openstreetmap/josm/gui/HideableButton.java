// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import javax.swing.Icon;

/**
 * An interface to provide showing/hiding method for buttons,
 * when hidden state is stored in preferences
 */
interface HideableButton {
    public void applyButtonHiddenPreferences();
    public void setButtonHidden(boolean b);
    public void showButton();
    public void hideButton();
    public String getActionName();
    public Icon getIcon();
    public boolean isButtonVisible();
    public void setShowHideButtonListener(ShowHideButtonListener l);
}
