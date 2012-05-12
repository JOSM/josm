// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;

/**
 *
 * @author Dirk Stöcker
 * code based on JavaScript from Chuck Taylor
 *
 */
public class UTM extends AbstractProjection {

    private static final int DEFAULT_ZONE = 30;
    private int zone;

    public enum Hemisphere { North, South }
    private static final Hemisphere DEFAULT_HEMISPHERE = Hemisphere.North;
    private Hemisphere hemisphere;

    /**
     * Applies an additional false easting of 3000000 m if true.
     */
    private boolean offset;

    public UTM() {
        this(DEFAULT_ZONE, DEFAULT_HEMISPHERE, false);
    }

    public UTM(int zone, Hemisphere hemisphere, boolean offset) {
        ellps = Ellipsoid.WGS84;
        proj = new org.openstreetmap.josm.data.projection.proj.TransverseMercator();
        try {
            proj.initialize(new ProjParameters() {{ ellps = UTM.this.ellps; }});
        } catch (ProjectionConfigurationException e) {
            throw new RuntimeException(e);
        }
        datum = WGS84Datum.INSTANCE;
        this.zone = zone;
        this.hemisphere = hemisphere;
        this.offset = offset;
        x_0 = 500000 + (offset ? 3000000 : 0);
        y_0 = hemisphere == Hemisphere.North ? 0 : 10000000;
        lon_0 = getUtmCentralMeridianDeg(zone);
        k_0 = 0.9996;
    }

    /*
     * UTMCentralMeridian
     *
     * Determines the central meridian for the given UTM zone.
     *
     * Inputs:
     *     zone - An integer value designating the UTM zone, range [1,60].
     *
     * Returns:
     *   The central meridian for the given UTM zone, in radians, or zero
     *   if the UTM zone parameter is outside the range [1,60].
     *   Range of the central meridian is the radian equivalent of [-177,+177].
     *
     */
    private double getUtmCentralMeridianDeg(int zone)
    {
        return -183.0 + (zone * 6.0);
    }

    public int getzone() {
        return zone;
    }

    @Override
    public String toString() {
        return tr("UTM");
    }

    @Override
    public Integer getEpsgCode() {
        return ((offset?325800:32600) + getzone() + (hemisphere == Hemisphere.South?100:0));
    }

    @Override
    public int hashCode() {
        return toCode().hashCode();
    }

    @Override
    public String getCacheDirectoryName() {
        return "epsg"+ getEpsgCode();
    }

    @Override
    public Bounds getWorldBoundsLatLon()
    {
        if (hemisphere == Hemisphere.North)
            return new Bounds(
                    new LatLon(-5.0, getUtmCentralMeridianDeg(getzone())-5.0),
                    new LatLon(85.0, getUtmCentralMeridianDeg(getzone())+5.0));
        else
            return new Bounds(
                    new LatLon(-85.0, getUtmCentralMeridianDeg(getzone())-5.0),
                    new LatLon(5.0, getUtmCentralMeridianDeg(getzone())+5.0));
    }

}
