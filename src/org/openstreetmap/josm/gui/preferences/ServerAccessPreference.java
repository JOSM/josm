// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.io.CredentialsManager.PreferenceAdditions;

public class ServerAccessPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ServerAccessPreference();
        }
    }

    /**
     * Editfield for the Base url to the REST API from OSM.
     */
    private JTextField osmDataServer = new JTextField(20);
    /**
     * Provide username and password input editfields.
     * Store the values if user hits OK.
     */
    private PreferenceAdditions credentialsPA = OsmConnection.credentialsManager.newPreferenceAdditions();

    public void addGui(PreferenceDialog gui) {
        osmDataServer.setText(Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api"));
        osmDataServer.setToolTipText(tr("The base URL for the OSM server (REST API)"));
        gui.connection.add(new JLabel(tr("Base Server URL")), GBC.std());
        gui.connection.add(osmDataServer, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        credentialsPA.addPreferenceOptions(gui.connection);
    }

    public boolean ok() {
        Main.pref.put("osm-server.url", osmDataServer.getText());
        credentialsPA.preferencesChanged();
        return false;
    }
}
