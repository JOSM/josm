// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of Multipolygon validation test.
 */
public class MultipolygonTestTest {


    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().mapStyles().presets().main().preferences();

    /**
     * Test all error cases manually created in multipolygon.osm.
     * @throws Exception in case of error
     */
    @Test
    public void testMultipolygonFile() throws Exception {
        final MultipolygonTest MULTIPOLYGON_TEST = new MultipolygonTest();
        final RelationChecker RELATION_TEST = new RelationChecker();
        ValidatorTestUtils.testSampleFile("data_nodist/multipolygon.osm",
                ds -> ds.getRelations().stream().filter(Relation::isMultipolygon).collect(Collectors.toList()),
                name -> name.startsWith("06") || name.startsWith("07") || name.startsWith("08"), MULTIPOLYGON_TEST, RELATION_TEST);
    }

    /**
     * Non-regression test for ticket #17768.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket17768TouchingInner() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(17768, "touching-inner.osm")) {
            MultipolygonTest mpTest = new MultipolygonTest();
            mpTest.makeFromWays(OsmReader.parseDataSet(is, null).getWays());
            // inner touches inner, is considered OK in OSM
            assertTrue(mpTest.getErrors().isEmpty());
        }
    }

    /**
     * Non-regression test for ticket #17768.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket17768TouchingInnerOuter() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(17768, "touching-inner-outer.osm")) {
            MultipolygonTest mpTest = new MultipolygonTest();
            mpTest.makeFromWays(OsmReader.parseDataSet(is, null).getWays());
            // inner touches outer, should return error
            assertEquals(1, mpTest.getErrors().size());
        }
    }
}
