// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Territories;
import org.openstreetmap.josm.testutils.annotations.Users;

/**
 * Performance test of {@code MapCSSTagChecker}.
 */
@Main
@Territories
@Users
class MapCSSTagCheckerPerformanceTest {

    private MapCSSTagChecker tagChecker;
    private DataSet dsCity;

    /**
     * Setup test.
     *
     * @throws Exception if any error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        tagChecker = new MapCSSTagChecker();
        tagChecker.initialize();
        dsCity = PerformanceTestUtils.getNeubrandenburgDataSet();
    }

    @Test
    void testCity() {
        PerformanceTestUtils.runPerformanceTest("MapCSSTagChecker on " + dsCity.getName(),
                () -> tagChecker.visit(dsCity.allPrimitives()));
    }
}
