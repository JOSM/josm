// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of coastline validation test.
 */
public class CoastlinesTest {

    private static final Coastlines COASTLINES = new Coastlines();
    private static final WronglyOrderedWays WRONGLY_ORDERED_WAYS = new WronglyOrderedWays();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test all error cases manually created in coastline.osm.
     * @throws Exception in case of error
     */
    @Test
    public void testCoastlineFile() throws Exception {
        ValidatorTestUtils.testSampleFile("data_nodist/coastlines.osm",
                ds -> ds.getWays().stream().filter(
                        w -> "coastline".equals(w.get("natural"))).collect(Collectors.toList()),
                null, COASTLINES, WRONGLY_ORDERED_WAYS);
    }
}
