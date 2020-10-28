// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.security.SecureRandom;
import java.util.Random;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests for class {@link Ellipsoid}.
 */
class EllipsoidTest {

    private static final double EPSILON = 1e-8;

    /**
     * convert latlon to cartesian coordinates back and forth
     */
    @Test
    void testLatLon2Cart2LatLon() {
        Random r = new SecureRandom();
        double maxErrLat = 0, maxErrLon = 0;
        Ellipsoid ellips = Ellipsoid.WGS84;
        for (int num = 0; num < 1000; ++num) {

            double lat = r.nextDouble() * 180.0 - 90.0;
            double lon = r.nextDouble() * 360.0 - 180.0;
            LatLon ll = new LatLon(lat, lon);

            for (int i = 0; i < 1000; ++i) {
                double[] cart = ellips.latLon2Cart(ll);
                ll = ellips.cart2LatLon(cart);

                if (!(Math.abs(lat - ll.lat()) < EPSILON && Math.abs(lon - ll.lon()) < EPSILON)) {
                    String error = String.format("point: %s iterations: %s current: %s errorLat: %s errorLon %s",
                            new LatLon(lat, lon), i, ll, Math.abs(lat - ll.lat()), Math.abs(lon - ll.lon()));
                    System.err.println(error);
                    Assert.fail();
                }
            }

            maxErrLat = Math.max(maxErrLat, Math.abs(lat - ll.lat()));
            maxErrLon = Math.max(maxErrLon, Math.abs(lon - ll.lon()));
        }
    }
}
