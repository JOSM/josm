// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * JUnit Test of {@link Coastlines} validation test.
 */
@BasicPreferences
class CoastlinesTest {

    private static final Coastlines COASTLINES = new Coastlines();
    private static final WronglyOrderedWays WRONGLY_ORDERED_WAYS = new WronglyOrderedWays();
    /**
     * Test all error cases manually created in coastline.osm.
     * @throws Exception in case of error
     */
    @Test
    void testCoastlineFile() throws Exception {
        ValidatorTestUtils.testSampleFile("nodist/data/coastlines.osm",
                ds -> ds.getWays().stream().filter(
                        w -> "coastline".equals(w.get("natural"))).collect(Collectors.toList()),
                null, COASTLINES, WRONGLY_ORDERED_WAYS);
    }
}
