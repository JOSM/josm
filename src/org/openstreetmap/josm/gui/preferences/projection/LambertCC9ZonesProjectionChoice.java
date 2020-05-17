// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.IntStream;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * ProjectionChoice for Lambert CC (9 zones, EPSG:3942-3950).
 * <p>
 * @see <a href="https://fr.wikipedia.org/wiki/Projection_conique_conforme_de_Lambert#Lambert_zone_CC">Lambert CC</a>
 */
public class LambertCC9ZonesProjectionChoice extends ListProjectionChoice {

    private static String[] lambert9zones = {
        tr("{0} ({1} to {2} degrees)", 1, 41, 43),
        tr("{0} ({1} to {2} degrees)", 2, 42, 44),
        tr("{0} ({1} to {2} degrees)", 3, 43, 45),
        tr("{0} ({1} to {2} degrees)", 4, 44, 46),
        tr("{0} ({1} to {2} degrees)", 5, 45, 47),
        tr("{0} ({1} to {2} degrees)", 6, 46, 48),
        tr("{0} ({1} to {2} degrees)", 7, 47, 49),
        tr("{0} ({1} to {2} degrees)", 8, 48, 50),
        tr("{0} ({1} to {2} degrees)", 9, 49, 51)
    };

    /**
     * Constructs a new {@code LambertCC9ZonesProjectionChoice}.
     */
    public LambertCC9ZonesProjectionChoice() {
        super(tr("Lambert CC9 Zone (France)"), /* NO-ICON */ "core:lambertcc9", lambert9zones, tr("Lambert CC Zone"));
    }

    private static class LambertCC9CBPanel extends CBPanel {
        LambertCC9CBPanel(String[] entries, int initialIndex, String label, ActionListener listener) {
            super(entries, initialIndex, label, listener);
            this.add(new JLabel(ImageProvider.get("data/projection", "LambertCC9Zones")), GBC.eol().fill(GBC.HORIZONTAL));
            this.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new LambertCC9CBPanel(entries, index, label, listener);
    }

    @Override
    public String getCurrentCode() {
        return "EPSG:" + Integer.toString(3942+index); //CC42 is EPSG:3942 (up to EPSG:3950 for CC50)
    }

    @Override
    public String getProjectionName() {
        return tr("Lambert CC9 Zone (France)");
    }

    @Override
    public String[] allCodes() {
        return IntStream.range(0, 9).mapToObj(zone -> "EPSG:" + (3942 + zone)).toArray(String[]::new);
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        //zone 1=CC42=EPSG:3942 up to zone 9=CC50=EPSG:3950
        if (code.startsWith("EPSG:39") && code.length() == 9) {
            try {
                String zonestring = code.substring(5, 9);
                int zoneval = Integer.parseInt(zonestring)-3942;
                if (zoneval >= 0 && zoneval <= 8)
                    return Collections.singleton(String.valueOf(zoneval+1));
            } catch (NumberFormatException ex) {
                Logging.warn(ex);
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
