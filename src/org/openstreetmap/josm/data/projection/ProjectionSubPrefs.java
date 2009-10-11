// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.Collection;

import javax.swing.JPanel;

public interface ProjectionSubPrefs {
    /**
     * Generates the GUI for the given preference and packs them in a JPanel
     * so they may be displayed if the projection is selected.
     *
     * Implementation hints:
     * <ul>
     *      <li>Do not return <code>null</code> as it is assumed that if this
     *      interface is implemented the projection actually has prefs to
     *      display/save.</li>
     *      <li>Cache the JPanel in a local variable so that changes are
     *      persistent even if the user chooses another projection in between.
     *      Destroy the panel on destroyCachedPanel() so that the pre-selected
     *      settings may update the preferences are updated from the outside</li>
     *      </li>
     * @return
     */
    public JPanel getPreferencePanel();

    /**
     * Will be called if the preference dialog is dismissed.
     */
    public Collection<String> getPreferences();

    /**
     * Return null when code is not part of this projection.
     */
    public Collection<String> getPreferencesFromCode(String code);

    /**
     * Will be called if the preference dialog is dismissed.
     */
    public void setPreferences(Collection<String> args);

    /**
     * Resets all variables related to the projection preferences so they may
     * update the next time getPreferencePanel is called.
     */
    public void destroyCachedPanel();
}
