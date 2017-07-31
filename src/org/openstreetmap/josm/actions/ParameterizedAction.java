// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

/**
 * Interface for (toolbar-)actions that have additional parameters which need
 * to be saved to the preferences (and loaded back).
 */
public interface ParameterizedAction extends AdaptableAction {

    /**
     * Get the list of parameters that describe the action.
     * @return the list of parameters that describe the action
     */
    List<ActionParameter<?>> getActionParameters();

    /**
     * Invoke action using the given parameters.
     * @param e the ActionEvent
     * @param parameters parameter map
     */
    void actionPerformed(ActionEvent e, Map<String, Object> parameters);
}
