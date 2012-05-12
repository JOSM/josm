//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;
import org.openstreetmap.josm.data.projection.proj.SwissObliqueMercator;

/**
 * SwissGrid CH1903 / L03, see http://de.wikipedia.org/wiki/Swiss_Grid.
 *
 * Actually, what we have here, is CH1903+ (EPSG:2056), but without
 * the additional false easting of 2000km and false northing 1000 km.
 *
 * To get to CH1903, a shift file is required. So currently, there are errors
 * up to 1.6m (depending on the location).
 *
 * This projection does not have any parameters, it only implements
 * ProjectionSubPrefs to show a warning that the grid file correction is not done.
 */
public class SwissGrid extends AbstractProjection {

    public SwissGrid() {
        ellps = Ellipsoid.Bessel1841;
        datum = new ThreeParameterDatum("SwissGrid Datum", null, ellps, 674.374, 15.056, 405.346);
        x_0 = 600000;
        y_0 = 200000;
        lon_0 = 7.0 + 26.0/60 + 22.50/3600;
        proj = new SwissObliqueMercator();
        try {
            proj.initialize(new ProjParameters() {{
                ellps = SwissGrid.this.ellps;
                lat_0 = 46.0 + 57.0/60 + 8.66/3600;
            }});
        } catch (ProjectionConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return tr("Swiss Grid (Switzerland)");
    }

    @Override
    public Integer getEpsgCode() {
        return 21781;
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    @Override
    public String getCacheDirectoryName() {
        return "swissgrid";
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(new LatLon(45.7, 5.7), new LatLon(47.9, 10.6));
    }

}
