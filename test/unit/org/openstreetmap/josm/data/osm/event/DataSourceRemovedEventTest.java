// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSourceChangeEvent;

/**
 * Test class for {@link DataSourceRemovedEvent}
 *
 * @author Taylor Smock
 */

public class DataSourceRemovedEventTest {
    /**
     * Get getting the originating data source
     */
    @Test
    public void testGetDataEventSource() {
        DataSource fakeRemove = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        DataSet ds = new DataSet();
        assertSame(ds, new DataSourceRemovedEvent(ds, Collections.emptySet(), Stream.of(fakeRemove)).getSource());
    }

    /**
     * Test that no sources are added
     */
    @Test
    public void testGetAddedSource() {
        DataSource fakeRemove = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        assertTrue(
                new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(), Stream.empty()).getAdded().isEmpty());
        DataSourceChangeEvent event = new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(),
                Stream.of(fakeRemove));
        assertSame(event.getAdded(), event.getAdded());
        assertTrue(new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(), Stream.of(fakeRemove)).getAdded()
                .isEmpty());
    }

    /**
     * Test that the getting the removed source(s) works properly
     */
    @Test
    public void testGetRemovedSource() {
        DataSource fakeRemove = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        assertTrue(new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(), Stream.empty()).getRemoved()
                .isEmpty());
        DataSourceChangeEvent event = new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(),
                Stream.of(fakeRemove));
        assertSame(event.getRemoved(), event.getRemoved());
        assertSame(fakeRemove, new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(), Stream.of(fakeRemove))
                .getRemoved().iterator().next());
    }

    /**
     * Check that the sources don't include removed data
     */
    @Test
    public void testGetDataSources() {
        DataSource fakeRemove = new DataSource(new Bounds(0, 0, 0, 0), "fake-source");
        DataSourceChangeEvent event = new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(),
                Stream.of(fakeRemove));
        assertSame(event.getDataSources(), event.getDataSources());
        assertFalse(event.getDataSources().contains(fakeRemove));
        assertTrue(new DataSourceRemovedEvent(new DataSet(), Collections.singleton(fakeRemove), Stream.of(fakeRemove))
                .getDataSources().isEmpty());
        assertSame(fakeRemove,
                new DataSourceRemovedEvent(new DataSet(), Collections.singleton(fakeRemove), Stream.empty())
                        .getDataSources().iterator().next());
    }

    /**
     * Check that a string is returned with added/current/deleted
     */
    @Test
    public void testToString() {
        String toString = new DataSourceRemovedEvent(new DataSet(), Collections.emptySet(), Stream.empty()).toString();
        assertTrue(toString.contains("added"));
        assertTrue(toString.contains("current"));
        assertTrue(toString.contains("removed"));
    }
}
