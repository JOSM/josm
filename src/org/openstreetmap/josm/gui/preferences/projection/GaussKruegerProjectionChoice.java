// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.tools.Logging;

/**
 * ProjectionChoice for Gauß-Krüger coordinate system (zones 2-5, EPSG:31466-31469).
 * <p>
 * @see <a href="https://de.wikipedia.org/wiki/Gauß-Krüger-Koordinatensystem">Gauß-Krüger</a>
 */
public class GaussKruegerProjectionChoice extends ListProjectionChoice {

    private static final String[] zones = {"2", "3", "4", "5"};

    /**
     * Constructs a new {@code GaussKruegerProjectionChoice}.
     */
    public GaussKruegerProjectionChoice() {
        super(tr("Gau\u00DF-Kr\u00FCger"), /* NO-ICON */ "core:gauss-krueger", zones, tr("GK Zone"));
    }

    @Override
    public String getCurrentCode() {
        return "EPSG:"+Integer.toString(31466 + index);
    }

    @Override
    protected String indexToZone(int index) {
        return Integer.toString(index + 2);
    }

    @Override
    protected int zoneToIndex(String zone) {
        try {
            return Integer.parseInt(zone) - 2;
        } catch (NumberFormatException e) {
            Logging.warn(e);
        }
        return defaultIndex;
    }

    @Override
    public String[] allCodes() {
        String[] codes = new String[4];
        for (int zone = 2; zone <= 5; zone++) {
            codes[zone-2] = "EPSG:" + (31464 + zone);
        }
        return codes;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        //zone 2 = EPSG:31466 up to zone 5 = EPSG:31469
        for (int zone = 2; zone <= 5; zone++) {
            String epsg = "EPSG:" + (31464 + zone);
            if (epsg.equals(code))
                return Collections.singleton(String.valueOf(zone));
        }
        return null;
    }

    @Override
    public String getProjectionName() {
        return tr("Gau\u00DF-Kr\u00FCger Zone {0}", index + 2);
    }

}
