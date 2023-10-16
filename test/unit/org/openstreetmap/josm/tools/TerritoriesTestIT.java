// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Integration tests of {@link Territories} class.
 */
@Projection
class TerritoriesTestIT {
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
