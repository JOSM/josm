// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.data.osm.Changeset.MAX_CHANGESET_TAG_LENGTH;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link Changeset}.
 */
public class ChangesetTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of method {@link Changeset#setKeys}.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public void testSetKeys() {
        final Changeset cs = new Changeset();
        // Cannot add null map => IllegalArgumentException
        try {
            cs.setKeys(null);
            Assert.fail("Should have thrown an IllegalArgumentException as we gave a null argument.");
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
        Assert.assertEquals("Both valid keys should have been put in the ChangeSet.", 2, cs.getKeys().size());

        // Add a map with too long values => IllegalArgumentException
        keys = new HashMap<>();
        StringBuilder b = new StringBuilder(MAX_CHANGESET_TAG_LENGTH + 1);
        for (int i = 0; i < MAX_CHANGESET_TAG_LENGTH + 1; i++) {
           b.append("x");
        }
        keys.put("test", b.toString());
        try {
            cs.setKeys(keys);
            Assert.fail("Should have thrown an IllegalArgumentException as we gave a too long value.");
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            // Was expected
        }
    }

    /**
     * Unit test of method {@link Changeset#compareTo}.
     */
    @Test
    public void testCompareTo() {
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
    public void testGetBounds() {
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
    public void testGetSetHasContent() {
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
    public void testGetDisplayName() {
        assertEquals("Changeset 0", new Changeset().getDisplayName(DefaultNameFormatter.getInstance()));
    }

    /**
     * Unit test of method {@link Changeset#getName}.
     */
    @Test
    public void testGetName() {
        assertEquals("changeset 0", new Changeset().getName());
    }

    private static Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    /**
     * Unit test of method {@link Changeset#hasEqualSemanticAttributes}.
     */
    @Test
    public void testHasEqualSemanticAttributes() {
        Date today = new Date();
        Changeset cs1 = new Changeset();
        Changeset cs2 = new Changeset();
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        assertFalse(cs1.hasEqualSemanticAttributes(null));
        // Closed At
        cs1.setClosedAt(null);
        cs2.setClosedAt(today);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setClosedAt(yesterday());
        cs2.setClosedAt(today);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setClosedAt(today);
        cs2.setClosedAt(today);
        assertTrue(cs1.hasEqualSemanticAttributes(cs2));
        // Created At
        cs1.setCreatedAt(null);
        cs2.setCreatedAt(today);
        assertFalse(cs1.hasEqualSemanticAttributes(cs2));
        cs1.setCreatedAt(yesterday());
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
    public void testKeySet() {
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
