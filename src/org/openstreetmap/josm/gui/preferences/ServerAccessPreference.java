// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.server.AuthenticationPreferencesPanel;
import org.openstreetmap.josm.gui.preferences.server.BackupPreferencesPanel;
import org.openstreetmap.josm.gui.preferences.server.OsmApiUrlInputPanel;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferencesPanel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
public class ServerAccessPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ServerAccessPreference();
        }
    }

    private OsmApiUrlInputPanel pnlApiUrlPreferences;

    private JTabbedPane tpServerPreferences;
    /** indicates whether to use the default OSM URL or not */
    /** panel for configuring authentication preferences */
    private AuthenticationPreferencesPanel pnlAuthPreferences;
    /** panel for configuring proxy preferences */
    private ProxyPreferencesPanel pnlProxyPreferences;
    /** panel for backup preferences */
    private BackupPreferencesPanel pnlBackupPreferences;

    /**
     * Embeds a vertically scrollable panel in a {@see JScrollPane}
     * @param panel the panel
     * @return the scroll pane
     */
    protected JScrollPane wrapVerticallyScrollablePanel(VerticallyScrollablePanel panel) {
        JScrollPane sp = new JScrollPane(panel);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return sp;
    }

    /**
     * Builds the tabbed pane with the server preferences
     * 
     * @return
     */
    protected JPanel buildTabbedServerPreferences() {
        JPanel pnl = new JPanel(new BorderLayout());

        tpServerPreferences = new JTabbedPane();
        pnlAuthPreferences = new AuthenticationPreferencesPanel();
        tpServerPreferences.add(wrapVerticallyScrollablePanel(pnlAuthPreferences));
        pnlProxyPreferences = new ProxyPreferencesPanel();
        tpServerPreferences.add(wrapVerticallyScrollablePanel(pnlProxyPreferences));
        pnlBackupPreferences = new BackupPreferencesPanel();
        tpServerPreferences.add(wrapVerticallyScrollablePanel(pnlBackupPreferences));

        tpServerPreferences.setTitleAt(0, tr("Authentication"));
        tpServerPreferences.setTitleAt(1, tr("Proxy settings"));
        tpServerPreferences.setTitleAt(2, tr("File backup"));
        tpServerPreferences.setToolTipTextAt(0, tr("Configure your identity and how to authenticate at the OSM server"));
        tpServerPreferences.setToolTipTextAt(1, tr("Configure whether to use a proxy server"));
        tpServerPreferences.setToolTipTextAt(2, tr("Configure whether to create backup files"));

        pnl.add(tpServerPreferences, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Builds the panel for entering the server access preferences
     * 
     * @return
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // the checkbox for the default UL
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 1.0;
        gc.insets = new Insets(0,0,0,0);
        pnl.add(pnlApiUrlPreferences = new OsmApiUrlInputPanel(), gc);

        // the remaining access properties
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.insets = new Insets(10,0,3,3);
        pnl.add(buildTabbedServerPreferences(), gc);

        // let the AuthPreferencesPanel know when the API URL changes
        //
        pnlApiUrlPreferences.addPropertyChangeListener(pnlAuthPreferences);

        HelpUtil.setHelpContext(pnl, HelpUtil.ht("/Preferences/Connection"));
        return pnl;
    }

    public void addGui(PreferenceTabbedPane gui) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gui.connection.add(buildContentPanel(), gc);

        initFromPreferences();
    }

    /**
     * Initializes the configuration panel with values from the preferences
     */
    public void initFromPreferences() {
        pnlApiUrlPreferences.initFromPreferences();
        pnlAuthPreferences.initFromPreferences();
        pnlProxyPreferences.initFromPreferences();
        pnlBackupPreferences.initFromPreferences();
    }

    /**
     * Saves the values to the preferences
     */
    public boolean ok() {
        pnlApiUrlPreferences.saveToPreferences();
        pnlAuthPreferences.saveToPreferences();
        pnlProxyPreferences.saveToPreferences();
        pnlBackupPreferences.saveToPreferences();
        return false;
    }
}
