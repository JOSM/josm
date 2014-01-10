// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;

/**
 * Connection preferences, including authentication and proxy sub-preferences.
 */
public final class ServerAccessPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code ServerAccessPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ServerAccessPreference();
        }
    }

    private ServerAccessPreference() {
        super("connection", tr("Connection Settings"), tr("Connection Settings for the OSM server."), false, new JTabbedPane());
    }

    /** indicates whether to use the default OSM URL or not */
    private OsmApiUrlInputPanel pnlApiUrlPreferences;

    /**
     * Builds the tabbed pane with the server preferences
     *
     * @return panel with server preferences tabs
     */
    protected JPanel buildTabbedServerPreferences() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(getTabPane(), BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Builds the panel for entering the server access preferences
     *
     * @return preferences panel for server settings
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // the checkbox for the default UL
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 1.0;
        gc.insets = new Insets(0,0,0,0);
        pnlApiUrlPreferences = new OsmApiUrlInputPanel();
        pnl.add(pnlApiUrlPreferences, gc);

        // the remaining access properties
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.insets = new Insets(10,0,3,3);
        pnl.add(buildTabbedServerPreferences(), gc);

        HelpUtil.setHelpContext(pnl, HelpUtil.ht("/Preferences/Connection"));
        return pnl;
    }

    /**
     * Adds a listener that will be notified of API URL change.
     * @param listener the listener
     * @since 6523
     */
    public final void addApiUrlChangeListener(PropertyChangeListener listener) {
        pnlApiUrlPreferences.addPropertyChangeListener(listener);
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gui.createPreferenceTab(this).add(buildContentPanel(), gc);

        pnlApiUrlPreferences.initFromPreferences();
    }

    /**
     * Saves the values to the preferences
     */
    @Override
    public boolean ok() {
        pnlApiUrlPreferences.saveToPreferences();
        return false;
    }
}
