// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.*;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Implementation of the Lambert Conformal Conic projection.
 *
 * @author Pieren
 */
public class LambertConformalConic implements Proj {
    
    protected Ellipsoid ellps;
    protected double e;

    /**
     * projection exponent
     */
    protected double n;
    /**
     * projection factor
     */
    protected double F;
    /**
     * radius of the parallel of latitude of the false origin (2SP) or at 
     * natural origin (1SP)
     */
    protected double r0; 
    
    /**
     * precision in iterative schema
     */
    protected static final double epsilon = 1e-12;

    /**
     * Constructor.
     * Call one of the updateParameters... methods for initialization.
     */
    public LambertConformalConic() {
    }

    /**
     * Initialize for LCC with 2 standard parallels.
     * 
     * @param ellps the ellipsoid
     * @param lat_0 latitude of false origin (in degrees)
     * @param lat_1 latitude of first standard parallel (in degrees)
     * @param lat_2 latitude of second standard parallel (in degrees)
     */
    public void updateParameters2SP(Ellipsoid ellps, double lat_0, double lat_1, double lat_2) {
        this.ellps = ellps;
        this.e = ellps.e;
        
        final double m1 = m(toRadians(lat_1));
        final double m2 = m(toRadians(lat_2));
        
        final double t1 = t(toRadians(lat_1));
        final double t2 = t(toRadians(lat_2));
        final double tf = t(toRadians(lat_0));
        
        n  = (log(m1) - log(m2)) / (log(t1) - log(t2));
        F  = m1 / (n * pow(t1, n));
        r0 = F * pow(tf, n);
    }
    
    /**
     * Initialize for LCC with 1 standard parallel.
     * 
     * @param ellps the ellipsoid
     * @param lat_0 latitude of natural origin (in degrees)
     */
    public void updateParameters1SP(Ellipsoid ellps, double lat_0) {
        this.ellps = ellps;
        this.e = ellps.e;
        final double lat_0_rad = toRadians(lat_0);
        
        final double m0 = m(lat_0_rad);
        final double t0 = t(lat_0_rad);
        
        n = sin(lat_0_rad);
        F  = m0 / (n * pow(t0, n));
        r0 = F * pow(t0, n);
    }

    /**
     * Initialize LCC by providing the projection parameters directly.
     * 
     * @param ellps the ellipsoid
     * @param n see field n
     * @param F see field F
     * @param r0 see field r0
     */
    public void updateParametersDirect(Ellipsoid ellps, double n, double F, double r0) {
        this.ellps = ellps;
        this.e = ellps.e;
        this.n = n;
        this.F = F;
        this.r0 = r0;
    }

    /**
     * auxiliary function t
     */
    protected double t(double lat_rad) {
        return tan(PI/4 - lat_rad / 2.0)
            / pow(( (1.0 - e * sin(lat_rad)) / (1.0 + e * sin(lat_rad))) , e/2);
    }

    /**
     * auxiliary function m
     */
    protected double m(double lat_rad) {
        return cos(lat_rad) / (sqrt(1 - e * e * pow(sin(lat_rad), 2)));
    }
    
    @Override
    public String getName() {
        return tr("Lambert Conformal Conic");
    }

    @Override
    public String getProj4Id() {
        return "lcc";
    }

    @Override
    public double[] project(double phi, double lambda) {
        double sinphi = sin(phi);
        double L = (0.5*log((1+sinphi)/(1-sinphi))) - e/2*log((1+e*sinphi)/(1-e*sinphi));
        double r = F*exp(-n*L);
        double gamma = n*lambda;
        double X = r*sin(gamma);
        double Y = r0 - r*cos(gamma);
        return new double[] { X, Y };
    }
    
    @Override
    public double[] invproject(double east, double north) {
        double r = sqrt(pow(east,2) + pow(north-r0, 2));
        double gamma = atan(east / (r0-north));
        double lambda = gamma/n;
        double latIso = (-1/n) * log(abs(r/F));
        double phi = ellps.latitude(latIso, e, epsilon);
        return new double[] { phi, lambda };
    }
    
}
