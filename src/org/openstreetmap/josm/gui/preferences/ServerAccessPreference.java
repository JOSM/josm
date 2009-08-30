// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import org.openstreetmap.josm.io.CredentialsManager;
import org.openstreetmap.josm.io.OsmConnection;

public class ServerAccessPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ServerAccessPreference();
        }
    }

    /**
     * Provide username and password input editfields.
     * Store the values if user hits OK.
     */
    private CredentialsManager.PreferenceAdditions credentialsPA = OsmConnection.credentialsManager.newPreferenceAdditions();

    public void addGui(PreferenceDialog gui) {
        credentialsPA.addPreferenceOptions(gui.connection);
    }

    public boolean ok() {
        credentialsPA.preferencesChanged();
        return false;
    }
}
