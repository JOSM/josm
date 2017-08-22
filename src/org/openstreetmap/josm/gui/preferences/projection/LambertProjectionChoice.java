// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * ProjectionChoice for 4 zone Lambert (1920, EPSG:27561-27564).
 * <p>
 * @see <a href="https://fr.wikipedia.org/wiki/Projection_conique_conforme_de_Lambert#Lambert_zone">Lambert zone</a>
 */
public class LambertProjectionChoice extends ListProjectionChoice {

    private static final String[] LAMBERT_4_ZONES = {
        tr("{0} ({1} to {2} degrees)", 1, "51.30", "48.15"),
        tr("{0} ({1} to {2} degrees)", 2, "48.15", "45.45"),
        tr("{0} ({1} to {2} degrees)", 3, "45.45", "42.76"),
        tr("{0} (Corsica)", 4)
    };

    /**
     * Constructs a new {@code LambertProjectionChoice}.
     */
    public LambertProjectionChoice() {
        super(tr("Lambert 4 Zones (France)"), /* NO-ICON */ "core:lambert", LAMBERT_4_ZONES, tr("Lambert CC Zone"));
    }

    private static class LambertCBPanel extends CBPanel {
        LambertCBPanel(String[] entries, int initialIndex, String label, ActionListener listener) {
            super(entries, initialIndex, label, listener);
            this.add(new JLabel(ImageProvider.get("data/projection", "Departements_Lambert4Zones")), GBC.eol().fill(GBC.HORIZONTAL));
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
                if (zoneval >= 1 && zoneval <= 4)
                    return Collections.singleton(zonestring);
            } catch (NumberFormatException e) {
                Logging.warn(e);
            }
        }
        return null;
    }

    @Override
    protected String indexToZone(int idx) {
        return Integer.toString(idx + 1);
    }

    @Override
    protected int zoneToIndex(String zone) {
        try {
            return Integer.parseInt(zone) - 1;
        } catch (NumberFormatException e) {
            Logging.warn(e);
        }
        return defaultIndex;
    }
}
