// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;

/**
 * Unit tests of {@link AbstractProj}
 */
class AbstractProjTest {

    private final AbstractProj proj = new AbstractProj() {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getProj4Id() {
            return null;
        }

        @Override
        public double[] project(double latRad, double lonRad) {
            return null;
        }

        @Override
        public double[] invproject(double east, double north) {
            return null;
        }

        @Override
        public Bounds getAlgorithmBounds() {
            return null;
        }};

    @Test
    void testCphi2NaN() {
        assertTrue(Double.isNaN(proj.cphi2(Double.NaN)));
    }
}
