// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.IOException;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Datum based of NTV2 grid shift file.
 * @since 5073
 */
public class NTV2Datum extends AbstractDatum {

    private final NTV2GridShiftFileWrapper nadgrids;

    /**
     * Constructs a new {@code NTV2Datum}.
     * @param name datum name
     * @param proj4Id PROJ.4 id
     * @param ellps ellipsoid
     * @param nadgrids NTV2 grid shift file wrapper
     */
    public NTV2Datum(String name, String proj4Id, Ellipsoid ellps, NTV2GridShiftFileWrapper nadgrids) {
        super(name, proj4Id, ellps);
        this.nadgrids = nadgrids;
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        NTV2GridShift gs = new NTV2GridShift(ll);
        try {
            nadgrids.getShiftFile().gridShiftForward(gs);
            return new LatLon(ll.lat() + gs.getLatShiftDegrees(), ll.lon() + gs.getLonShiftPositiveEastDegrees());
        } catch (IOException e) {
            throw new JosmRuntimeException(e);
        }
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        NTV2GridShift gs = new NTV2GridShift(ll);
        try {
            nadgrids.getShiftFile().gridShiftReverse(gs);
            return new LatLon(ll.lat() + gs.getLatShiftDegrees(), ll.lon() + gs.getLonShiftPositiveEastDegrees());
        } catch (IOException e) {
            throw new JosmRuntimeException(e);
        }
    }
}
