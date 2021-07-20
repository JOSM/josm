// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;

/**
 * Integration tests of {@link Territories} class.
 */
@IntegrationTest
class TerritoriesTestIT {

    /**
     * Test rules.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().projection();


    /**
     * Test of {@link Territories#initialize} method.
     */
    @Test
    void testUtilityClass() {
        Logging.clearLastErrorAndWarnings();
        Territories.initialize();
        assertEquals(Collections.emptyList(), Logging.getLastErrorAndWarnings(), "no errors or warnings");
        assertFalse(Territories.customTagsCache.isEmpty(), "customTagsCache is non empty");
        assertFalse(Territories.iso3166Cache.isEmpty(), "iso3166Cache is non empty");
        assertFalse(Territories.taginfoCache.isEmpty(), "taginfoCache is non empty");
        assertFalse(Territories.taginfoGeofabrikCache.isEmpty(), "taginfoGeofabrikCache is non empty");
    }
}
