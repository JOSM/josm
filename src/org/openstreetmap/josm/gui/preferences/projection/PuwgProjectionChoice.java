// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.IntStream;

import org.openstreetmap.josm.tools.Utils;

/**
 * ProjectionChoice for PUWG 1992 (EPSG:2180) and PUWG 2000 for Poland (Zone 5-8, EPSG:2176-2179).
 * <p>
 * @see <a href="https://pl.wikipedia.org/wiki/Uk%C5%82ad_wsp%C3%B3%C5%82rz%C4%99dnych_1992">PUWG 1992</a>
 * @see <a href="https://pl.wikipedia.org/wiki/Uk%C5%82ad_wsp%C3%B3%C5%82rz%C4%99dnych_2000">PUWG 2000</a>
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
        return Arrays.stream(CODES).filter(code::equals).findFirst().map(Collections::singleton).orElse(null);
    }

    @Override
    protected String indexToZone(int index) {
        return CODES[index];
    }

    @Override
    protected int zoneToIndex(String zone) {
        return IntStream.range(0, CODES.length)
                .filter(i -> zone.equals(CODES[i]))
                .findFirst().orElse(defaultIndex);
    }
}
