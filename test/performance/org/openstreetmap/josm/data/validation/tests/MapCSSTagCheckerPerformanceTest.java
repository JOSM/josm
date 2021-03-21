// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Performance test of {@code MapCSSTagChecker}.
 */
class MapCSSTagCheckerPerformanceTest {

    private MapCSSTagChecker tagChecker;
    private DataSet dsCity;

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().territories().preferences();

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
