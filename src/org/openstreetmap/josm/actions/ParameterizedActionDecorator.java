// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;

/**
 * Action wrapper that delegates to a {@link ParameterizedAction} object using
 * a specific set of parameters.
 */
public class ParameterizedActionDecorator implements Action {

    private final ParameterizedAction action;
    private final Map<String, Object> parameters;

    /**
     * Constructs a new ParameterizedActionDecorator.
     * @param action the action that is invoked by this wrapper
     * @param parameters parameters used for invoking the action
     */
    public ParameterizedActionDecorator(ParameterizedAction action, Map<String, Object> parameters) {
        this.action = action;
        this.parameters = new HashMap<>(parameters);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        action.addPropertyChangeListener(listener);
    }

    @Override
    public Object getValue(String key) {
        return action.getValue(key);
    }

    @Override
    public boolean isEnabled() {
        return action.isEnabled();
    }

    @Override
    public void putValue(String key, Object value) {
        action.putValue(key, value);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        action.removePropertyChangeListener(listener);
    }

    @Override
    public void setEnabled(boolean b) {
        action.setEnabled(b);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        action.actionPerformed(e, parameters);
    }

    /**
     * Get the parameters used to invoke the wrapped action.
     * @return the parameters used to invoke the wrapped action
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
}
