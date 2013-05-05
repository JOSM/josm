// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

public interface ParameterizedAction extends AdaptableAction {

    List<ActionParameter<?>> getActionParameters();
    void actionPerformed(ActionEvent e, Map<String, Object> parameters);

}
