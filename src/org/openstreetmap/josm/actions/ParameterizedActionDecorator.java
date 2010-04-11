// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;

public class ParameterizedActionDecorator implements Action {

    private final ParameterizedAction action;
    private final Map<String, Object> parameters;

    public ParameterizedActionDecorator(ParameterizedAction action, Map<String, Object> parameters) {
        this.action = action;
        this.parameters = new HashMap<String, Object>(parameters);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        action.addPropertyChangeListener(listener);
    }
    public Object getValue(String key) {
        return action.getValue(key);
    }
    public boolean isEnabled() {
        return action.isEnabled();
    }
    public void putValue(String key, Object value) {
        action.putValue(key, value);
    }
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        action.removePropertyChangeListener(listener);
    }
    public void setEnabled(boolean b) {
        action.setEnabled(b);
    }
    public void actionPerformed(ActionEvent e) {
        action.actionPerformed(e, parameters);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

}
