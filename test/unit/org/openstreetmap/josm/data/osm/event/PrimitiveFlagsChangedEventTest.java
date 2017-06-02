// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PrimitiveFlagsChangedEvent} class.
 */
public class PrimitiveFlagsChangedEventTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link PrimitiveFlagsChangedEvent#toString}.
     */
    @Test
    public void testToString() {
        assertEquals("PRIMITIVE_FLAGS_CHANGED", new PrimitiveFlagsChangedEvent(null, null).toString());
    }
}
