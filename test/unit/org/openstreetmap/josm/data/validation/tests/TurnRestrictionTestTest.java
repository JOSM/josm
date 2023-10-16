// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.MapPaintStyles;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.TaggingPresets;

/**
 * JUnit Test of turn restriction validation test.
 */
@Main
@MapPaintStyles
@Projection
@TaggingPresets
class TurnRestrictionTestTest {

    private static final TurnrestrictionTest TURNRESTRICTION_TEST = new TurnrestrictionTest();
    private static final RelationChecker RELATION_TEST = new RelationChecker();

    /**
     * Test all error cases manually created in restriction.osm.
     * @throws Exception in case of error
     */
    @Test
    void testTurnrestrictionFile() throws Exception {
        ValidatorTestUtils.testSampleFile("nodist/data/restriction.osm",
                DataSet::getRelations,
                name -> name.startsWith("E"), TURNRESTRICTION_TEST, RELATION_TEST);
    }
}
