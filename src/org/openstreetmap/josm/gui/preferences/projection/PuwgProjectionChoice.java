// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Puwg;

public class PuwgProjectionChoice extends ListProjectionChoice {

    public PuwgProjectionChoice() {
        super("core:puwg", tr("PUWG (Poland)"), Puwg.zones, tr("PUWG Zone"));
    }

    @Override
    public Projection getProjection() {
        return new Puwg(index);
    }

    @Override
    public String[] allCodes() {
        String[] zones = new String[Puwg.zones.length];
        for (int index = 0; index < Puwg.zones.length; index++) {
            zones[index] = Puwg.zones[index].toCode();
        }
        return zones;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (Puwg.PuwgData p : Puwg.zones) {
            if (code.equals(p.toCode()))
                return Collections.singleton(code);
        }
        return null;
    }

    @Override
    protected String indexToZone(int index) {
        return Puwg.zones[index].toCode();
    }

    @Override
    protected int zoneToIndex(String zone) {
        for (int i=0; i<Puwg.zones.length; i++) {
            if (zone.equals(Puwg.zones[i].toCode())) {
                return i;
            }
        }
        return defaultIndex;
    }

}
