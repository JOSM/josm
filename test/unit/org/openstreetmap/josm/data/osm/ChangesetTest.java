// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openstreetmap.josm.data.osm.Changeset.MAX_CHANGESET_TAG_LENGTH;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link Changeset}.
 */
class ChangesetTest {
    /**
     * Unit test of method {@link Changeset#setKeys}.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    void testSetKeys() {
        final Changeset cs = new Changeset();
        // Cannot add null map => IllegalArgumentException
        try {
            cs.setKeys(null);
            fail("Should have thrown an IllegalArgumentException as we gave a null argument.");
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            // Was expected
        }

        // Add a map with no values
        // => the key list is empty
        Map<String, String> keys = new HashMap<>();

        // Add a map with valid values : null and short texts
        // => all the items are in the keys
        keys.put("empty", null);
        keys.put("test", "test");
        cs.setKeys(keys);
        assertEquals(2, cs.getKeys().size(), "Both valid keys should have been put in the ChangeSet.");

        // Add a map with too long values => IllegalArgumentException
        keys = new HashMap<>();
        keys.put("test", "x".repeat(MAX_CHANGESET_TAG_LENGTH + 1));
        try {
            cs.setKeys(keys);
            fail("Should have thrown an IllegalArgumentException as we gave a too long value.");
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            // Was expected
        }
    }

    /**
     * Unit test of method {@link Changeset#compareTo}.
     */
    @Test
    void testCompareTo() {
        Changeset cs1 = new Changeset(1);
        Changeset cs2 = new Changeset(2);
        assertEquals(0, cs1.compareTo(cs1));
        assertEquals(-1, cs1.compareTo(cs2));
        assertEquals(+1, cs2.compareTo(cs1));
    }

    /**
     * Unit test of method {@link Changeset#getBounds}.
     */
    @Test
    void testGetBounds() {
        Changeset cs = new Changeset();
        assertNull(cs.getBounds());
        cs.setMin(LatLon.NORTH_POLE);
        cs.setMax(null);
        assertNull(cs.getBounds());
        cs.setMin(null);
        cs.setMax(LatLon.SOUTH_POLE);
        assertNull(cs.getBounds());
        cs.setMin(LatLon.NORTH_POLE);
        cs.setMax(LatLon.SOUTH_POLE);
        assertEquals(new Bounds(90, 0, -90, 0), cs.getBounds());
    }

    /**
     * Unit test of methods {@link Changeset#getContent} / {@link Changeset#setContent} / {@link Changeset#hasContent}.
     */
    @Test
    void testGetSetHasContent() {
        Changeset cs = new Changeset();
        assertNull(cs.getContent());
        assertFalse(cs.hasContent());
        ChangesetDataSet cds = new ChangesetDataSet();
        cs.setContent(cds);
        assertEquals(cds, cs.getContent());
        assertTrue(cs.hasContent());
    }

    /**
     * Unit test of method {@link Changeset#getDisplayName}.
     */
    @Test
    void testGetDisplayName() {
        assertEquals("Changeset 0", new Changeset().getDisplayName(DefaultNameFormatter.getInstance()));
    }

    /**
     * Unit test of method {@link Changeset#getName}.
     */
    @Test
    void testGetName() {
        assertEquals("changeset 0", new Changeset().getName());
    }

    /**
     * Unit test of method {@link Changeset#hasEqualSemanticAttributes}.
     */
    @Test
    void testHasEqualSemanticAttributes() {
        Instant today = Instant.now();
        Instant yesterday = today.minus(Duration.ofDays(1));
        Changeset cs1 = new Changeset();
        Changeset cs2 = new Changeset();
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        assertFalse(cs1.hasEqualSemanticAttributes(null));
        // Closed At
        cs1.setClosedAt(null);
        cs2.setClosedAt(today);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setClosedAt(yesterday);
        cs2.setClosedAt(today);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setClosedAt(today);
        cs2.setClosedAt(today);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Created At
        cs1.setCreatedAt(null);
        cs2.setCreatedAt(today);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setCreatedAt(yesterday);
        cs2.setCreatedAt(today);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setCreatedAt(today);
        cs2.setCreatedAt(today);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Id
        cs1.setId(1);
        cs2.setId(2);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setId(1);
        cs2.setId(1);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Max
        cs1.setMax(null);
        cs2.setMax(null);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setMax(null);
        cs2.setMax(LatLon.NORTH_POLE);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setMax(LatLon.SOUTH_POLE);
        cs2.setMax(LatLon.NORTH_POLE);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setMax(LatLon.SOUTH_POLE);
        cs2.setMax(LatLon.SOUTH_POLE);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Min
        cs1.setMin(null);
        cs2.setMin(null);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setMin(null);
        cs2.setMin(LatLon.SOUTH_POLE);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setMin(LatLon.NORTH_POLE);
        cs2.setMin(LatLon.SOUTH_POLE);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setMin(LatLon.NORTH_POLE);
        cs2.setMin(LatLon.NORTH_POLE);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Open
        cs1.setOpen(false);
        cs2.setOpen(true);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setOpen(false);
        cs2.setOpen(false);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Tags
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "bar");
        cs2.setKeys(tags);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setKeys(new HashMap<>(tags));
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // User
        cs1.setUser(null);
        cs2.setUser(User.createLocalUser("foo"));
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setUser(null);
        cs2.setUser(null);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setUser(User.createLocalUser("foo"));
        cs2.setUser(User.createLocalUser("foo"));
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Comment count
        cs1.setCommentsCount(1);
        cs2.setCommentsCount(2);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setCommentsCount(1);
        cs2.setCommentsCount(1);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
    }

    /**
     * Unit test of methods {@link Changeset#keySet} / {@link Changeset#put} / {@link Changeset#remove} / {@link Changeset#removeAll}.
     */
    @Test
    void testKeySet() {
        Changeset cs = new Changeset();
        assertTrue(cs.keySet().isEmpty());
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "bar");
        cs.setKeys(tags);
        Collection<String> set = cs.keySet();
        assertEquals(1, set.size());
        assertEquals("foo", set.iterator().next());
        cs.remove("foo");
        assertTrue(cs.keySet().isEmpty());
        cs.put("foo", "bar");
        cs.put("bar", "foo");
        assertEquals(2, cs.keySet().size());
        cs.removeAll();
        assertTrue(cs.keySet().isEmpty());
    }
}
