// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.GBC;

/**
 * Preferences related to Overpass API servers.
 *
 * @since 9142
 */
public class OverpassServerPreference implements SubPreferenceSetting {

    private final HistoryComboBox overpassServer = new HistoryComboBox();
    private final JCheckBox forMultiFetch = new JCheckBox(tr("Use Overpass server for object downloads"));

    /**
     * Factory used to create a new {@link OverpassServerPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new OverpassServerPreference();
        }
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getServerPreference();
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        final JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(new JLabel(tr("Overpass server: ")), GBC.std().insets(5, 5, 5, 5));
        panel.add(overpassServer, GBC.eop().fill(GBC.HORIZONTAL));
        overpassServer.setPossibleItems(OverpassDownloadReader.OVERPASS_SERVER_HISTORY.get());
        overpassServer.setText(OverpassDownloadReader.OVERPASS_SERVER.get());

        panel.add(forMultiFetch, GBC.eop());
        forMultiFetch.setSelected(OverpassDownloadReader.FOR_MULTI_FETCH.get());

        panel.add(Box.createVerticalGlue(), GBC.eol().fill());

        getTabPreferenceSetting(gui).addSubTab(this, tr("Overpass server"), panel);
    }

    @Override
    public boolean ok() {
        OverpassDownloadReader.OVERPASS_SERVER.put(overpassServer.getText());
        OverpassDownloadReader.OVERPASS_SERVER_HISTORY.put(overpassServer.getHistory());
        OverpassDownloadReader.FOR_MULTI_FETCH.put(forMultiFetch.isSelected());
        return false;
    }

    @Override
    public boolean isExpert() {
        return true;
    }
}
