// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit test of {@link ChangesetCache}
 */
public class ChangesetCacheTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static final ChangesetCache cache = ChangesetCache.getInstance();

    /**
     * Clears cache before/after each unit test.
     */
    @After
    @Before
    public void clearCache() {
        cache.listeners.clear();
        cache.clear();
    }

    abstract class TestListener implements ChangesetCacheListener {

        protected final CountDownLatch latch = new CountDownLatch(1);
        protected ChangesetCacheEvent event;

        @Override
        public void changesetCacheUpdated(ChangesetCacheEvent event) {
            this.event = event;
            latch.countDown();
        }

        protected final void await() {
            try {
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Logging.error(e);
            }
        }

        abstract void test();
    }

    /**
     * Unit test of {@link ChangesetCache#ChangesetCache}
     */
    @Test
    public void testConstructor() {
        assertNotNull(ChangesetCache.getInstance());
    }

    @Test
    public void testAddAndRemoveListeners() {
        // should work
        cache.addChangesetCacheListener(null);

        ChangesetCacheListener listener = event -> {
            // should work
        };
        cache.addChangesetCacheListener(listener);
        // adding a second time - should work too
        cache.addChangesetCacheListener(listener);
        assertEquals(1, cache.listeners.size()); // ... but only added once

        cache.removeChangesetCacheListener(null);

        cache.removeChangesetCacheListener(listener);
        assertTrue(cache.listeners.isEmpty());
    }

    @Test
    public void testUpdateGetRemoveCycle() {
        cache.update(new Changeset(1));
        assertEquals(1, cache.size());
        assertNotNull(cache.get(1));
        assertEquals(1, cache.get(1).getId());
        cache.remove(1);
        assertEquals(0, cache.size());
    }

    @Test
    public void testUpdateTwice() {
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
    public void testContains() {
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
    public void testFireingEventsAddAChangeset() {
        TestListener listener = new TestListener() {
            @Override
            public void test() {
                await();
                assertNotNull(event);
                assertEquals(1, event.getAddedChangesets().size());
                assertTrue(event.getRemovedChangesets().isEmpty());
                assertTrue(event.getUpdatedChangesets().isEmpty());
                assertEquals(cache, event.getSource());
            }
        };
        cache.addChangesetCacheListener(listener);
        cache.update(new Changeset(1));
        listener.test();
        cache.removeChangesetCacheListener(listener);
    }

    @Test
    public void testFireingEventsUpdateChangeset() {
        // Waiter listener to ensure the second listener does not receive the first event
        TestListener waiter = new TestListener() {
            @Override
            void test() {
                await();
            }
        };
        cache.addChangesetCacheListener(waiter);
        Changeset cs = new Changeset(1);
        cache.update(cs);
        waiter.test();
        cache.removeChangesetCacheListener(waiter);

        TestListener listener = new TestListener() {
            @Override
            void test() {
                await();
                assertNotNull(event);
                assertTrue(event.getAddedChangesets().isEmpty());
                assertTrue(event.getRemovedChangesets().isEmpty());
                assertEquals(1, event.getUpdatedChangesets().size());
                assertEquals(cache, event.getSource());
            }
        };
        cache.addChangesetCacheListener(listener);
        cache.update(cs);
        listener.test();
        cache.removeChangesetCacheListener(listener);
    }

    @Test
    public void testFireingEventsRemoveChangeset() {
        // Waiter listener to ensure the second listener does not receive the first event
        TestListener waiter = new TestListener() {
            @Override
            void test() {
                await();
            }
        };
        cache.addChangesetCacheListener(waiter);
        cache.update(new Changeset(1));
        waiter.test();
        cache.removeChangesetCacheListener(waiter);

        TestListener listener = new TestListener() {
            @Override
            void test() {
                await();
                assertNotNull(event);
                assertTrue(event.getAddedChangesets().isEmpty());
                assertEquals(1, event.getRemovedChangesets().size());
                assertTrue(event.getUpdatedChangesets().isEmpty());
                assertEquals(cache, event.getSource());
            }
        };
        cache.addChangesetCacheListener(listener);
        cache.remove(1);
        listener.test();
        cache.removeChangesetCacheListener(listener);
    }

    /**
     * Unit test of methods {@link ChangesetCache#getOpenChangesets} / {@link ChangesetCache#getChangesets}.
     */
    @Test
    public void testGetOpenChangesets() {
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
