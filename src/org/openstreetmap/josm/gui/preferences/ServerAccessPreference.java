// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class ServerAccessPreference implements PreferenceSetting {

    /**
     * Editfield for the Base url to the REST API from OSM.
     */
    private JTextField osmDataServer = new JTextField(20);
    /**
     * Editfield for the username to the OSM account.
     */
    private JTextField osmDataUsername = new JTextField(20);
    /**
     * Passwordfield for the userpassword of the REST API.
     */
    private JPasswordField osmDataPassword = new JPasswordField(20);

    public void addGui(PreferenceDialog gui) {
        osmDataServer.setText(Main.pref.get("osm-server.url"));
        osmDataUsername.setText(Main.pref.get("osm-server.username"));
        osmDataPassword.setText(Main.pref.get("osm-server.password"));

        osmDataServer.setToolTipText(tr("The base URL for the OSM server (REST API)"));
        osmDataUsername.setToolTipText(tr("Login name (e-mail) to the OSM account."));
        osmDataPassword.setToolTipText(tr("Login password to the OSM account. Leave blank to not store any password."));

        gui.connection.add(new JLabel(tr("Base Server URL")), GBC.std());
        gui.connection.add(osmDataServer, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        gui.connection.add(new JLabel(tr("OSM username (e-mail)")), GBC.std());
        gui.connection.add(osmDataUsername, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        gui.connection.add(new JLabel(tr("OSM password")), GBC.std());
        gui.connection.add(osmDataPassword, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,0));
        JLabel warning = new JLabel(tr("<html>" +
                "WARNING: The password is stored in plain text in the preferences file.<br>" +
                "The password is transferred in plain text to the server, encoded in the URL.<br>" +
        "<b>Do not use a valuable Password.</b></html>"));
        warning.setFont(warning.getFont().deriveFont(Font.ITALIC));
        gui.connection.add(warning, GBC.eop().fill(GBC.HORIZONTAL));
    }

    public boolean ok() {
        Main.pref.put("osm-server.url", osmDataServer.getText());
        Main.pref.put("osm-server.username", osmDataUsername.getText());
        Main.pref.put("osm-server.password", String.valueOf(osmDataPassword.getPassword()));
        return false;
    }
}
