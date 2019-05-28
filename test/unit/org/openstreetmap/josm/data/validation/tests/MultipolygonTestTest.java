// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of Multipolygon validation test.
 */
public class MultipolygonTestTest {

    private static final MultipolygonTest MULTIPOLYGON_TEST = new MultipolygonTest();
    private static final RelationChecker RELATION_TEST = new RelationChecker();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().mapStyles().presets().main();

    /**
     * Test all error cases manually created in multipolygon.osm.
     * @throws Exception in case of error
     */
    @Test
    public void testMultipolygonFile() throws Exception {
        ValidatorTestUtils.testSampleFile("data_nodist/multipolygon.osm",
                ds -> ds.getRelations().stream().filter(Relation::isMultipolygon).collect(Collectors.toList()),
                name -> name.startsWith("06") || name.startsWith("07") || name.startsWith("08"), MULTIPOLYGON_TEST, RELATION_TEST);
    }
}
