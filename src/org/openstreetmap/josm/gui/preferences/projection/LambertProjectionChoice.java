// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class LambertProjectionChoice extends ListProjectionChoice {

    private static final String[] lambert4zones = {
        tr("{0} ({1} to {2} degrees)", 1,"51.30","48.15"),
        tr("{0} ({1} to {2} degrees)", 2,"48.15","45.45"),
        tr("{0} ({1} to {2} degrees)", 3,"45.45","42.76"),
        tr("{0} (Corsica)", 4)
    };

    /**
     * Constructs a new {@code LambertProjectionChoice}.
     */
    public LambertProjectionChoice() {
        super(tr("Lambert 4 Zones (France)"), "core:lambert", lambert4zones, tr("Lambert CC Zone"));
    }

    private class LambertCBPanel extends CBPanel {
        public LambertCBPanel(Object[] entries, int initialIndex, String label, ActionListener listener) {
            super(entries, initialIndex, label, listener);
            this.add(new JLabel(ImageProvider.get("data/projection", "Departements_Lambert4Zones.png")), GBC.eol().fill(GBC.HORIZONTAL));
            this.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new LambertCBPanel(entries, index, label, listener);
    }

    @Override
    public String getCurrentCode() {
        return "EPSG:" + Integer.toString(27561+index);
    }

    @Override
    public String getProjectionName() {
        return tr("Lambert 4 Zones (France)");
    }

    @Override
    public String[] allCodes() {
        String[] codes = new String[4];
        for (int zone = 0; zone < 4; zone++) {
            codes[zone] = "EPSG:"+(27561+zone);
        }
        return codes;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        if (code.startsWith("EPSG:2756") && code.length() == 10) {
            try {
                String zonestring = code.substring(9);
                int zoneval = Integer.parseInt(zonestring);
                if(zoneval >= 1 && zoneval <= 4)
                    return Collections.singleton(zonestring);
            } catch(NumberFormatException e) {
                Main.warn(e);
            }
        }
        return null;
    }

    @Override
    protected String indexToZone(int index) {
        return Integer.toString(index + 1);
    }

    @Override
    protected int zoneToIndex(String zone) {
        try {
            return Integer.parseInt(zone) - 1;
        } catch(NumberFormatException e) {
            Main.warn(e);
        }
        return defaultIndex;
    }
}
