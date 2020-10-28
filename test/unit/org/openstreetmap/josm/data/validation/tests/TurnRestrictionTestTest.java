// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of turn restriction validation test.
 */
class TurnRestrictionTestTest {

    private static final TurnrestrictionTest TURNRESTRICTION_TEST = new TurnrestrictionTest();
    private static final RelationChecker RELATION_TEST = new RelationChecker();

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().mapStyles().presets().main();

    /**
     * Test all error cases manually created in restriction.osm.
     * @throws Exception in case of error
     */
    @Test
    void testTurnrestrictionFile() throws Exception {
        ValidatorTestUtils.testSampleFile("nodist/data/restriction.osm",
                ds -> ds.getRelations(),
                name -> name.startsWith("E"), TURNRESTRICTION_TEST, RELATION_TEST);
    }
}
