// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSourceChangeEvent;

/**
 * Test class for {@link DataSourceAddedEvent}
 *
 * @author Taylor Smock
 */
class DataSourceAddedEventTest {
    /**
     * Get getting the originating data source
     */
    @Test
    void testGetDataEventSource() {
        DataSource fakeAdd = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        DataSet ds = new DataSet();
        assertSame(ds, new DataSourceAddedEvent(ds, Collections.emptySet(), Stream.of(fakeAdd)).getSource());
    }

    /**
     * Test that added sources are processed properly
     */
    @Test
    void testGetAddedSource() {
        DataSource fakeAdd = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        assertTrue(
                new DataSourceAddedEvent(new DataSet(), Collections.emptySet(), Stream.empty()).getAdded().isEmpty());
        DataSourceChangeEvent event = new DataSourceAddedEvent(new DataSet(), Collections.emptySet(),
                Stream.of(fakeAdd));
        assertSame(event.getAdded(), event.getAdded());
        assertSame(fakeAdd, new DataSourceAddedEvent(new DataSet(), Collections.emptySet(), Stream.of(fakeAdd))
                .getAdded().iterator().next());
    }

    /**
     * Test that there are no removed sources
     */
    @Test
    void testGetRemovedSource() {
        DataSource fakeAdd = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        assertTrue(
                new DataSourceAddedEvent(new DataSet(), Collections.emptySet(), Stream.empty()).getRemoved().isEmpty());
        DataSourceChangeEvent event = new DataSourceAddedEvent(new DataSet(), Collections.emptySet(),
                Stream.of(fakeAdd));
        assertSame(event.getRemoved(), event.getRemoved());
        assertTrue(new DataSourceAddedEvent(new DataSet(), Collections.emptySet(), Stream.of(fakeAdd)).getRemoved()
                .isEmpty());
    }

    /**
     * Check that the sources include newly added data
     */
    @Test
    void testGetDataSources() {
        DataSource fakeAdd = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        DataSourceChangeEvent event = new DataSourceAddedEvent(new DataSet(), Collections.emptySet(),
                Stream.of(fakeAdd));
        assertSame(event.getDataSources(), event.getDataSources());
        assertSame(fakeAdd, event.getDataSources().iterator().next());
    }

    /**
     * Check that a string is returned with added/current/deleted
     */
    @Test
    void testToString() {
        String toString = new DataSourceAddedEvent(new DataSet(), Collections.emptySet(), Stream.empty()).toString();
        assertTrue(toString.contains("added"));
        assertTrue(toString.contains("current"));
        assertTrue(toString.contains("removed"));
    }
}
