// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Territories} class.
 */
public class TerritoriesTestIT {

    /**
     * Test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().projection();


    /**
     * Test of {@link Territories#initialize} method.
     */
    @Test
    public void testUtilityClass() {
        Logging.clearLastErrorAndWarnings();
        Territories.initialize();
        assertEquals("no errors or warnings", Collections.emptyList(), Logging.getLastErrorAndWarnings());
        assertFalse("taginfoCache is non empty", Territories.taginfoCache.isEmpty());
        assertFalse("taginfoGeofabrikCache is non empty", Territories.taginfoGeofabrikCache.isEmpty());
    }
}
