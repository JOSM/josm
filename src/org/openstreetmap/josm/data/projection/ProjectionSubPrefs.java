// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JPanel;

public interface ProjectionSubPrefs extends Projection {
    /**
     * Generates the GUI for the given preference and packs them in a JPanel
     * so they may be displayed if the projection is selected.
     * 
     * @param listener   listener for any change of preferences
     */
    public void setupPreferencePanel(JPanel p, ActionListener listener);

    /**
     * Will be called if the preference dialog is dismissed.
     */
    public Collection<String> getPreferences(JPanel p);

    /**
     * Return all projection codes supported by this projection class.
     */
    public String[] allCodes();

    /**
     * Return null when code is not part of this projection.
     */
    public Collection<String> getPreferencesFromCode(String code);

    /**
     * Will be called if the preference dialog is dismissed.
     * argument may be null to reset everything
     */
    public void setPreferences(Collection<String> args);
}
