// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class PuwgProjectionChoice extends ListProjectionChoice {

    public static final String[] CODES = {
        "EPSG:2180",
        "EPSG:2176",
        "EPSG:2177",
        "EPSG:2178",
        "EPSG:2179"
    };
    public static final String[] NAMES = {
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
        super(tr("PUWG (Poland)"), "core:puwg", NAMES, tr("PUWG Zone"));
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
        return Arrays.copyOf(CODES, CODES.length);
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
        for (int i=0; i<CODES.length; i++) {
            if (zone.equals(CODES[i])) {
                return i;
            }
        }
        return defaultIndex;
    }

}
