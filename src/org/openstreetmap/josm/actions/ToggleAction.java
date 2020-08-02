// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Abtract class for Toggle Actions.
 * @since 6220
 */
public abstract class ToggleAction extends JosmAction {

    private final transient Set<ButtonModel> buttonModels = new HashSet<>();

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
    protected ToggleAction(String name, ImageProvider icon, String tooltip, Shortcut shortcut, boolean registerInToolbar,
            String toolbarId, boolean installAdapters) {
        super(name, icon, tooltip, shortcut, registerInToolbar, toolbarId, installAdapters);
        // It is required to set the SELECTED_KEY to a non-null value in order to let Swing components update it
        setSelected(false);
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
    protected ToggleAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);
        // It is required to set the SELECTED_KEY to a non-null value in order to let Swing components update it
        setSelected(false);
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
            Logging.warn(getClass().getName() + " does not define a boolean for SELECTED_KEY but " + selected +
                    ". You should report it to JOSM developers.");
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
        if (model != null) {
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

    /**
     * Toggles the selcted action state, if needed according to the ActionEvent that trigerred the action.
     * This method will do nothing if the action event comes from a Swing component supporting the SELECTED_KEY property because
     * the component already set the selected state.
     * This method needs to be called especially if the action is associated with a keyboard shortcut to ensure correct selected state.
     * @param e ActionEvent that trigerred the action
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/javax/swing/Action.html">Interface Action</a>
     */
    protected final void toggleSelectedState(ActionEvent e) {
        if (e == null || !(e.getSource() instanceof JToggleButton ||
                           e.getSource() instanceof JCheckBox ||
                           e.getSource() instanceof JRadioButton ||
                           e.getSource() instanceof JCheckBoxMenuItem ||
                           e.getSource() instanceof JRadioButtonMenuItem
                           )) {
            setSelected(!isSelected());
        }
    }
}
