// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests of {@link User}.
 */
public class UserTest {

    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test method for {@link User#createOsmUser}.
     */
    @Test
    public void testCreateOsmUser() {
        User user1 = User.createOsmUser(1, "name1");
        assertEquals(1, user1.getId());
        assertEquals("name1", user1.getName());
        User user2 = User.createOsmUser(1, "name2");
        assertSame(user1, user2);
        assertEquals(1, user2.getId());
        assertEquals("name2", user2.getName());
        assertEquals(2, user2.getNames().size());
        assertTrue(user2.getNames().contains("name1"));
        assertTrue(user2.getNames().contains("name2"));
    }
}
