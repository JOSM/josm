// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of the {@code MultipolygonBuilder} class.
 */
@BasicPreferences
@Projection
@Timeout(15)
class MultipolygonBuilderTest {
    /**
     * Non-regression test for ticket #12376.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket12376() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(12376, "multipolygon_hang.osm.bz2")) {
            for (Relation r : OsmReader.parseDataSet(is, null).getRelations()) {
                assertNull(new MultipolygonBuilder().makeFromWays(r.getMemberPrimitives(Way.class)));
            }
        }
    }
}
