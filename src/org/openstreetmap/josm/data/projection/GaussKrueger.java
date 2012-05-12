// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.NTV2Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator;

public class GaussKrueger extends AbstractProjection {

    public static final int DEFAULT_ZONE = 2;
    private final int zone;

    private static Bounds[] bounds = {
        new Bounds(new LatLon(-5, 3.5), new LatLon(85, 8.5), false),
        new Bounds(new LatLon(-5, 6.5), new LatLon(85, 11.5), false),
        new Bounds(new LatLon(-5, 9.5), new LatLon(85, 14.5), false),
        new Bounds(new LatLon(-5, 12.5), new LatLon(85, 17.5), false),
    };

    public GaussKrueger() {
        this(DEFAULT_ZONE);
    }

    public GaussKrueger(int zone) {
        if (zone < 2 || zone > 5)
            throw new IllegalArgumentException();
        this.zone = zone;
        ellps = Ellipsoid.Bessel1841;
        datum = new NTV2Datum("BETA2007", null, ellps, NTV2GridShiftFileWrapper.BETA2007);
        ////less acurrate datum (errors up to 3m):
        //datum = new SevenParameterDatum(
        //        tr("Deutsches Hauptdreiecksnetz"), null, ellps,
        //        598.1, 73.7, 418.2, 0.202, 0.045, -2.455, 6.70);
        proj = new TransverseMercator();
        try {
            proj.initialize(new ProjParameters() {{ ellps = GaussKrueger.this.ellps; }});
        } catch (ProjectionConfigurationException e) {
            throw new RuntimeException(e);
        }
        x_0 = 1000000 * zone + 500000;
        lon_0 = 3 * zone;
    }

    @Override
    public String toString() {
        return tr("Gau\u00DF-Kr\u00FCger Zone {0}", zone);
    }

    @Override
    public Integer getEpsgCode() {
        return 31464 + zone;
    }

    @Override
    public String getCacheDirectoryName() {
        return "gausskrueger"+zone;
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return bounds[zone-2];
    }

}
