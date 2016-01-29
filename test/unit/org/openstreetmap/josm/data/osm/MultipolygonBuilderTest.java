// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertNull;

import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Unit tests of the {@code MultipolygonBuilder} class.
 */
public class MultipolygonBuilderTest {

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(15);

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for ticket #12060.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket12376() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(12376, "multipolygon_hang.osm.bz2")) {
            for (Relation r : OsmReader.parseDataSet(is, null).getRelations()) {
                assertNull(new MultipolygonBuilder().makeFromWays(r.getMemberPrimitives(Way.class)));
            }
        }
    }
}
