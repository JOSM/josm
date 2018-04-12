// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Equidistant cylindrical projection (EPSG code 9823).
 * In the particular case where the {@code standard_parallel_1} is 0°, this projection is also called Plate Carree or Equirectangular.
 * This is used in, for example, <cite>WGS84 / Plate Carree</cite> (EPSG:32662).
 * <p>
 * <b>References:</b>
 * <ul>
 *   <li>John P. Snyder (Map Projections - A Working Manual,<br>
 *       U.S. Geological Survey Professional Paper 1395, 1987)</li>
 *   <li>"Coordinate Conversions and Transformations including Formulas",<br>
 *       EPSG Guidence Note Number 7 part 2, Version 24.</li>
 * </ul>
 *
 * @author John Grange
 * @author Martin Desruisseaux
 *
 * @see <A HREF="http://mathworld.wolfram.com/CylindricalEquidistantProjection.html">Cylindrical Equidistant projection on MathWorld</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/equirectangular.html">"Equirectangular" on RemoteSensing.org</A>
 * @since 13598
 */
public class EquidistantCylindrical extends AbstractProj {

    /**
     * Cosinus of the {@code "standard_parallel_1"} parameter.
     */
    private double cosStandardParallel;

    @Override
    public String getName() {
        return tr("Equidistant Cylindrical (Plate Caree)");
    }

    @Override
    public String getProj4Id() {
        return "eqc";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        if (params.lat_ts != null) {
            cosStandardParallel = Math.cos(Utils.toRadians(Math.abs(params.lat_ts)));
        } else {
            // standard parallel is the equator (Plate Carree or Equirectangular)
            cosStandardParallel = 1.0;
        }
    }

    @Override
    public double[] project(double latRad, double lonRad) {
        return new double[] {lonRad * cosStandardParallel, latRad};
    }

    @Override
    public double[] invproject(double east, double north) {
        return new double[] {north, east / cosStandardParallel};
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -180, 89, 180, false);
    }
}
