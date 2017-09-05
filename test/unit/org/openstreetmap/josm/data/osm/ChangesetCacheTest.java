// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.UserIdentityManager;

/**
 * Unit test of {@link ChangesetCache}
 */
public class ChangesetCacheTest {

    /**
     * Clears cache after each unit test.
     */
    @After
    public void after() {
        ChangesetCache.getInstance().clear();
    }

    /**
     * Unit test of {@link ChangesetCache#ChangesetCache}
     */
    @Test
    public void testConstructor() {
        assertNotNull(ChangesetCache.getInstance());
    }

    @SuppressWarnings("unchecked")
    private static List<ChangesetCacheListener> getListeners(ChangesetCache cache) throws ReflectiveOperationException {
        return (List<ChangesetCacheListener>) TestUtils.getPrivateField(cache, "listeners");
    }

    @Test
    public void testAddAndRemoveListeners() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();

        // should work
        cache.addChangesetCacheListener(null);

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {}
        };
        cache.addChangesetCacheListener(listener);
        // adding a second time - should work too
        cache.addChangesetCacheListener(listener);
        assertEquals(1, getListeners(cache).size()); // ... but only added once

        cache.removeChangesetCacheListener(null);

        cache.removeChangesetCacheListener(listener);
        assertTrue(getListeners(cache).isEmpty());
    }

    @Test
    public void testUpdateGetRemoveCycle() {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();

        cache.update(new Changeset(1));
        assertEquals(1, cache.size());
        assertNotNull(cache.get(1));
        assertEquals(1, cache.get(1).getId());
        cache.remove(1);
        assertEquals(0, cache.size());
    }

    @Test
    public void testUpdateTwice() {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();

        Changeset cs = new Changeset(1);
        cs.setIncomplete(false);
        cs.put("key1", "value1");
        cs.setOpen(true);
        cache.update(cs);

        Changeset cs2 = new Changeset(cs);
        assertNotNull(cs2);
        cs2.put("key2", "value2");
        cs2.setOpen(false);
        cache.update(cs2);

        assertEquals(1, cache.size());
        assertNotNull(cache.get(1));

        cs = cache.get(1);
        assertEquals("value1", cs.get("key1"));
        assertEquals("value2", cs.get("key2"));
        assertFalse(cs.isOpen());
    }

    @Test
    public void testContains() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        getListeners(cache).clear();
        cache.clear();

        Changeset cs = new Changeset(1);
        cache.update(cs);

        assertTrue(cache.contains(1));
        assertTrue(cache.contains(cs));
        assertTrue(cache.contains(new Changeset(cs)));

        assertFalse(cache.contains(2));
        assertFalse(cache.contains(new Changeset(2)));
        assertFalse(cache.contains(null));
    }

    @Test
    public void testFireingEventsAddAChangeset() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();
        getListeners(cache).clear();

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {
                assertNotNull(event);
                assertEquals(1, event.getAddedChangesets().size());
                assertTrue(event.getRemovedChangesets().isEmpty());
                assertTrue(event.getUpdatedChangesets().isEmpty());
                assertEquals(cache, event.getSource());
            }
        };
        cache.addChangesetCacheListener(listener);
        cache.update(new Changeset(1));
        cache.removeChangesetCacheListener(listener);
    }

    @Test
    public void testFireingEventsUpdateChangeset() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();
        getListeners(cache).clear();

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {
                assertNotNull(event);
                assertTrue(event.getAddedChangesets().isEmpty());
                assertTrue(event.getRemovedChangesets().isEmpty());
                assertEquals(1, event.getUpdatedChangesets().size());
                assertEquals(cache, event.getSource());
            }
        };
        cache.update(new Changeset(1));

        cache.addChangesetCacheListener(listener);
        cache.update(new Changeset(1));
        cache.removeChangesetCacheListener(listener);
    }

    @Test
    public void testFireingEventsRemoveChangeset() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();
        getListeners(cache).clear();

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {
                assertNotNull(event);
                assertTrue(event.getAddedChangesets().isEmpty());
                assertEquals(1, event.getRemovedChangesets().size());
                assertTrue(event.getUpdatedChangesets().isEmpty());
                assertEquals(cache, event.getSource());
            }
        };
        cache.update(new Changeset(1));

        cache.addChangesetCacheListener(listener);
        cache.remove(1);
        cache.removeChangesetCacheListener(listener);
    }

    /**
     * Unit test of methods {@link ChangesetCache#getOpenChangesets} / {@link ChangesetCache#getChangesets}.
     */
    @Test
    public void testGetOpenChangesets() {
        final ChangesetCache cache = ChangesetCache.getInstance();
        // empty cache => empty list
        assertTrue(
                "Empty cache should produce an empty list.",
                cache.getOpenChangesets().isEmpty()
        );
        assertTrue(
                "Empty cache should produce an empty list.",
                cache.getChangesets().isEmpty()
        );

        // cache with only closed changesets => empty list
        Changeset closedCs = new Changeset(1);
        closedCs.setOpen(false);
        cache.update(closedCs);
        assertTrue(
                "Cache with only closed changesets should produce an empty list.",
                cache.getOpenChangesets().isEmpty()
        );
        assertEquals(1, cache.getChangesets().size());

        // cache with open and closed changesets => list with only the open ones
        Changeset openCs = new Changeset(2);
        openCs.setOpen(true);
        cache.update(openCs);
        assertEquals(
                Collections.singletonList(openCs),
                cache.getOpenChangesets()
        );
        assertEquals(2, cache.getChangesets().size());
    }

    /**
     * Unit test of method {@link ChangesetCache#getOpenChangesetsForCurrentUser}.
     */
    @Test
    public void testGetOpenChangesetsForCurrentUser() {
        final ChangesetCache cache = ChangesetCache.getInstance();
        // empty cache => empty list
        assertTrue(
                "Empty cache should produce an empty list.",
                cache.getOpenChangesetsForCurrentUser().isEmpty()
        );

        Changeset openCs1 = new Changeset(1);
        openCs1.setOpen(true);
        openCs1.setUser(User.getAnonymous());
        cache.update(openCs1);

        Changeset openCs2 = new Changeset(2);
        openCs2.setOpen(true);
        openCs2.setUser(User.createLocalUser("foo"));
        cache.update(openCs2);

        Changeset closedCs = new Changeset(3);
        closedCs.setOpen(false);
        cache.update(closedCs);

        assertEquals(3, cache.getChangesets().size());

        UserIdentityManager.getInstance().setAnonymous();
        assertEquals(2, cache.getOpenChangesetsForCurrentUser().size());

        UserIdentityManager.getInstance().setPartiallyIdentified("foo");
        assertEquals(1, cache.getOpenChangesetsForCurrentUser().size());
    }

    /**
     * Unit test of methods {@link ChangesetCache#remove}.
     */
    @Test
    public void testRemove() {
        final ChangesetCache cache = ChangesetCache.getInstance();
        Changeset cs1 = new Changeset(1);
        cache.update(cs1);
        assertEquals(1, cache.getChangesets().size());

        cache.remove((Changeset) null);
        cache.remove(cs1);
        assertTrue(cache.getChangesets().isEmpty());

        Changeset cs2 = new Changeset(2);
        cache.update((Collection<Changeset>) null);
        cache.update(Arrays.asList(cs1, cs2));
        assertEquals(2, cache.getChangesets().size());

        cache.remove((Collection<Changeset>) null);
        cache.remove(Arrays.asList(cs1, cs2));
        assertTrue(cache.getChangesets().isEmpty());
    }
}
