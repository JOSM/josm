// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Datum based of NTV2 grid shift file.
 */
public class NTV2Datum extends AbstractDatum {

    protected NTV2GridShiftFile nadgrids;

    public NTV2Datum(String name, String proj4Id, Ellipsoid ellps, NTV2GridShiftFile nadgrids) {
        super(name, proj4Id, ellps);
        this.nadgrids = nadgrids;
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        NTV2GridShift gs = new NTV2GridShift(ll);
        nadgrids.gridShiftForward(gs);
        return new LatLon(ll.lat() + gs.getLatShiftDegrees(), ll.lon() + gs.getLonShiftPositiveEastDegrees());
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        NTV2GridShift gs = new NTV2GridShift(ll);
        nadgrids.gridShiftReverse(gs);
        return new LatLon(ll.lat() + gs.getLatShiftDegrees(), ll.lon() + gs.getLonShiftPositiveEastDegrees());
    }
}
