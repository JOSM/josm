// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.PerformanceTest;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.Territories;

/**
 * Performance test of {@code MapCSSTagChecker}.
 */
@BasicPreferences
@PerformanceTest
@Projection
@Territories
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
