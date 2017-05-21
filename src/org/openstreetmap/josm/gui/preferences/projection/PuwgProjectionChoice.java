// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.tools.Utils;

/**
 * ProjectionChoice for PUWG 1992 (EPSG:2180) and PUWG 2000 for Poland (Zone 5-8, EPSG:2176-2179).
 * <p>
 * @see <a href="https://pl.wikipedia.org/wiki/Układ_współrzędnych_1992">PUWG 1992</a>
 * @see <a href="https://pl.wikipedia.org/wiki/Układ_współrzędnych_2000">PUWG 2000</a>
 */
public class PuwgProjectionChoice extends ListProjectionChoice {

    private static final String[] CODES = {
        "EPSG:2180",
        "EPSG:2176",
        "EPSG:2177",
        "EPSG:2178",
        "EPSG:2179"
    };

    private static final String[] NAMES = {
        tr("PUWG 1992 (Poland)"),
        tr("PUWG 2000 Zone {0} (Poland)", 5),
        tr("PUWG 2000 Zone {0} (Poland)", 6),
        tr("PUWG 2000 Zone {0} (Poland)", 7),
        tr("PUWG 2000 Zone {0} (Poland)", 8)
    };

    /**
     * Constructs a new {@code PuwgProjectionChoice}.
     */
    public PuwgProjectionChoice() {
        super(tr("PUWG (Poland)"), /* NO-ICON */ "core:puwg", NAMES, tr("PUWG Zone"));
    }

    @Override
    public String getCurrentCode() {
        return CODES[index];
    }

    @Override
    public String getProjectionName() {
        return NAMES[index];
    }

    @Override
    public String[] allCodes() {
        return Utils.copyArray(CODES);
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (String code2 : CODES) {
            if (code.equals(code2))
                return Collections.singleton(code2);
        }
        return null;
    }

    @Override
    protected String indexToZone(int index) {
        return CODES[index];
    }

    @Override
    protected int zoneToIndex(String zone) {
        for (int i = 0; i < CODES.length; i++) {
            if (zone.equals(CODES[i])) {
                return i;
            }
        }
        return defaultIndex;
    }
}
