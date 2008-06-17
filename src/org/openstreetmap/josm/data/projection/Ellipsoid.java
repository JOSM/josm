/*
 * Import from fr.geo.convert package, a geographic coordinates converter.
 * (http://www.i3s.unice.fr/~johan/gps/)
 * License: GPL. For details, see LICENSE file.
 * Copyright (C) 2002 Johan Montagnat (johan@creatis.insa-lyon.fr)
 */

package org.openstreetmap.josm.data.projection;

/**
 * the reference ellipsoids
 */
class Ellipsoid {
  /**
   * Clarke's elipsoid (NTF system)
   */
  public static final Ellipsoid clarke = new Ellipsoid(6378249.2, 6356515.0);
  /**
   * Hayford's ellipsoid (ED50 system)
   */
  public static final Ellipsoid hayford =
    new Ellipsoid(6378388.0, 6356911.9461);
  /**
   * WGS84 elipsoid
   */
  public static final Ellipsoid GRS80 = new Ellipsoid(6378137.0, 6356752.314);

  /**
   * half long axis
   */
  public final double a;
  /**
   * half short axis
   */
  public final double b;
  /**
   * first excentricity
   */
  public final double e;
  /**
   * first excentricity squared
   */
  public final double e2;

  /**
   * create a new ellipsoid and precompute its parameters
   *
   * @param a ellipsoid long axis (in meters)
   * @param b ellipsoid short axis (in meters)
   */
  public Ellipsoid(double a, double b) {
    this.a = a;
    this.b = b;
    e2 = (a*a - b*b) / (a*a);
    e = Math.sqrt(e2);
  }
}


