// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Puwg;

public class PuwgProjectionChoice extends ListProjectionChoice implements Alias {

    public PuwgProjectionChoice() {
        super("core:puwg", tr("PUWG (Poland)"), Puwg.Zones, tr("PUWG Zone"));
    }

    @Override
    public Projection getProjection() {
        return new Puwg(index);
    }

    @Override
    public String[] allCodes() {
        String[] zones = new String[Puwg.Zones.length];
        for (int zone = 0; zone < Puwg.Zones.length; zone++) {
            zones[zone] = Puwg.Zones[zone].toCode();
        }
        return zones;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (Puwg.PuwgData p : Puwg.Zones) {
            if (code.equals(p.toCode()))
                return Collections.singleton(code);
        }
        return null;
    }

    @Override
    public String getAlias() {
        return Puwg.class.getName();
    }

    @Override
    protected int indexToZone(int index) {
        return index;
    }

    @Override
    protected int zoneToIndex(int zone) {
        return zone;
    }

}
