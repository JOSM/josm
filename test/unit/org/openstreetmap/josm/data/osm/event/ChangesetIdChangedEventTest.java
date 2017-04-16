// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ChangesetIdChangedEvent} class.
 */
public class ChangesetIdChangedEventTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link ChangesetIdChangedEvent#toString}.
     */
    @Test
    public void testToString() {
        assertEquals("CHANGESET_ID_CHANGED", new ChangesetIdChangedEvent(null, null, 0, 0).toString());
    }
}
