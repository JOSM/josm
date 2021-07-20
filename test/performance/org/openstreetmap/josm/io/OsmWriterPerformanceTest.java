// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.PerformanceTestUtils.PerformanceTestTimer;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * This test tests how fast we are at writing an OSM file.
 * <p>
 * For this, we use the neubrandenburg-file, which is a good real world example of an OSM file.
 */
@Timeout(value = 15, unit = TimeUnit.MINUTES)
@BasicPreferences
class OsmWriterPerformanceTest {
    private static final int TIMES = 4;
    private DataSet neubrandenburgDataSet;

    /**
     * Setup test
     * @throws Exception if an error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        neubrandenburgDataSet = PerformanceTestUtils.getNeubrandenburgDataSet();
    }

    /**
     * Tests writing OSM data
     * @throws Exception if an error occurs
     */
    @Test
    void testWriter() throws Exception {
        PerformanceTestTimer timer = PerformanceTestUtils.startTimer("write .osm-file " + TIMES + " times");
        for (int i = 0; i < TIMES; i++) {
            try (StringWriter stringWriter = new StringWriter();
                 OsmWriter osmWriter = OsmWriterFactory.createOsmWriter(new PrintWriter(stringWriter), true, OsmWriter.DEFAULT_API_VERSION)) {
                osmWriter.write(neubrandenburgDataSet);
            }
        }
        timer.done();
    }

}
