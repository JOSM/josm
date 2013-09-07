// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;
import javax.swing.Icon;

import org.openstreetmap.josm.tools.Shortcut;

/**
 * Abtract class for Toggle Actions.
 * @since 6220
 */
public abstract class ToggleAction extends JosmAction {

    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();

    /**
     * Constructs a {@code ToggleAction}.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param icon the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     * @param toolbarId identifier for the toolbar preferences. The iconName is used, if this parameter is null
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public ToggleAction(String name, Icon icon, String tooltip, Shortcut shortcut, boolean registerInToolbar, String toolbarId, boolean installAdapters) {
        super(name, icon, tooltip, shortcut, registerInToolbar, toolbarId, installAdapters);
    }

    /**
     * Constructs a {@code ToggleAction}.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the name of icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     */
    public ToggleAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);
    }

    protected final void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    /**
     * Determines if this action is currently being selected.
     * @return {@code true} if this action is currently being selected, {@code false} otherwise
     */
    public final boolean isSelected() {
        Object selected = getValue(SELECTED_KEY);
        if (selected instanceof Boolean) {
            return (Boolean) selected;
        } else {
            return false;
        }
    }
    
    /**
     * Adds a button model
     * @param model The button model to add
     */
    public final void addButtonModel(ButtonModel model) {
        if (model != null && !buttonModels.contains(model)) {
            buttonModels.add(model);
            model.setSelected(isSelected());
        }
    }

    /**
     * Removes a button model
     * @param model The button model to remove
     */
    public final void removeButtonModel(ButtonModel model) {
        if (model != null && buttonModels.contains(model)) {
            buttonModels.remove(model);
        }
    }
    
    protected void notifySelectedState() {
        boolean selected = isSelected();
        for (ButtonModel model: buttonModels) {
            if (model.isSelected() != selected) {
                model.setSelected(selected);
            }
        }
    }

    protected final void toggleSelectedState() {
        setSelected(!isSelected());
    }
}
