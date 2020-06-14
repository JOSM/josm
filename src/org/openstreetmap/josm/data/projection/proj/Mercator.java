// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mercator Cylindrical Projection. The parallels and the meridians are straight lines and
 * cross at right angles; this projection thus produces rectangular charts. The scale is true
 * along the equator (by default) or along two parallels equidistant of the equator (if a scale
 * factor other than 1 is used). This projection is used to represent areas close to the equator.
 * It is also often used for maritime navigation because all the straight lines on the chart are
 * <em>loxodrome</em> lines, i.e. a ship following this line would keep a constant azimuth on its
 * compass.
 * <p>
 * This implementation handles both the 1 and 2 stardard parallel cases.
 * For 1 SP (EPSG code 9804), the line of contact is the equator.
 * For 2 SP (EPSG code 9805) lines of contact are symmetrical
 * about the equator.
 * <p>
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.Mercator
 * at the time of migration.
 * <p>
 * <b>References:</b>
 * <ul>
 *   <li>John P. Snyder (Map Projections - A Working Manual,<br>
 *       U.S. Geological Survey Professional Paper 1395, 1987)</li>
 *   <li>"Coordinate Conversions and Transformations including Formulas",<br>
 *       EPSG Guidence Note Number 7, Version 19.</li>
 * </ul>
 *
 * @author André Gosselin
 * @author Martin Desruisseaux (PMO, IRD)
 * @author Rueben Schulz
 * @author Simone Giannecchini
 *
 * @see <A HREF="http://mathworld.wolfram.com/MercatorProjection.html">Mercator projection on MathWorld</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/mercator_1sp.html">"mercator_1sp" on RemoteSensing.org</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/mercator_2sp.html">"mercator_2sp" on RemoteSensing.org</A>
 */
public class Mercator extends AbstractProj implements IScaleFactorProvider {
    /**
     * Maximum difference allowed when comparing real numbers.
     */
    private static final double EPSILON = 1E-6;

    protected double scaleFactor;

    @Override
    public String getName() {
        return tr("Mercator");
    }

    @Override
    public String getProj4Id() {
        return "merc";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        scaleFactor = 1;
        if (params.lat_ts != null) {
            /*
             * scaleFactor is not a parameter in the 2 SP case and is computed from
             * the standard parallel.
             */
            double standardParallel = Utils.toRadians(params.lat_ts);
            if (spherical) {
                scaleFactor *= Math.cos(standardParallel);
            } else {
                scaleFactor *= msfn(Math.sin(standardParallel), Math.cos(standardParallel));
            }
        }
        /*
         * A correction that allows us to employs a latitude of origin that is not
         * correspondent to the equator. See Snyder and al. for reference, page 47.
         */
        if (params.lat0 != null) {
            final double lat0 = Utils.toRadians(params.lat0);
            final double sinPhi = Math.sin(lat0);
            scaleFactor *= Math.cos(lat0) / Math.sqrt(1 - e2 * sinPhi * sinPhi);
        }
    }

    @Override
    public double[] project(double y, double x) {
        if (Math.abs(y) > (Math.PI/2 - EPSILON)) {
            return new double[] {0, 0}; // this is an error and should be handled somehow
        }
        if (spherical) {
            y = Math.log(Math.tan(Math.PI/4 + 0.5*y));
        } else {
            y = -Math.log(tsfn(y, Math.sin(y)));
        }
        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double x, double y) {
        if (spherical) {
            y = Math.PI/2 - 2.0*Math.atan(Math.exp(-y));
        } else {
            y = Math.exp(-y);
            y = cphi2(y);
        }
        return new double[] {y, x};
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -180, 89, 180, false);
    }

    @Override
    public double getScaleFactor() {
        return scaleFactor;
    }

    @Override
    public boolean lonIsLinearToEast() {
        return true;
    }
}
