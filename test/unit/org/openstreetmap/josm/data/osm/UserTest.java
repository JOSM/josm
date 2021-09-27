// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link User}.
 */
class UserTest {
    @BeforeEach
    void setUp() {
        User.clearUserMap();
    }

    @AfterEach
    void tearDown() {
        this.setUp();
    }

    /**
     * Test method for {@link User#createOsmUser}.
     */
    @Test
    void testCreateOsmUser() {
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
