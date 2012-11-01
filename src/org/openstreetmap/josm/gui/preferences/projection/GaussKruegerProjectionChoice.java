// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.projection.GaussKrueger;
import org.openstreetmap.josm.data.projection.Projection;

public class GaussKruegerProjectionChoice extends ListProjectionChoice {

    private static String[] zones = { "2", "3", "4", "5" };

    public GaussKruegerProjectionChoice() {
        super("core:gauss-krueger", tr("Gau\u00DF-Kr\u00FCger"), zones, tr("GK Zone"));
    }

    @Override
    public Projection getProjection() {
        return new GaussKrueger(index + 2);
    }

    @Override
    protected String indexToZone(int index) {
        return Integer.toString(index + 2);
    }

    @Override
    protected int zoneToIndex(String zone) {
        try {
            return Integer.parseInt(zone) - 2;
        } catch(NumberFormatException e) {}
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
    public Collection<String> getPreferencesFromCode(String code)
    {
        //zone 2 = EPSG:31466 up to zone 5 = EPSG:31469
        for (int zone = 2; zone <= 5; zone++) {
            String epsg = "EPSG:" + (31464 + zone);
            if (epsg.equals(code))
                return Collections.singleton(String.valueOf(zone));
        }
        return null;
    }
    
}
