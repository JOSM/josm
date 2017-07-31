// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import javax.swing.Action;

/**
 * Interface to indicate that name (tooltip) and icon may be changed for an entry
 * in the toolbar.
 * <p>
 * The name and icon of an {@link org.openstreetmap.josm.gui.preferences.ToolbarPreferences.ActionDefinition}
 * is saved to the preferences when the wrapped action implements AdaptableAction.
 * <p>
 * The user will have options to change the name and icon in the
 * {@link org.openstreetmap.josm.gui.preferences.ToolbarPreferences} when the action
 * for the toolbar entry implements AdaptableAction.
 */
public interface AdaptableAction extends Action {
}
