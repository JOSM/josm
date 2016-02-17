// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests of {@link NodeListViewer} class.
 */
public class NodeListViewerTest {

    /**
     * Test for {@link NodeListViewer#NodeListViewer} - {@code null} handling.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNodeListViewerNull() {
        new NodeListViewer(null);
    }

    /**
     * Test for {@link NodeListViewer#NodeListViewer} - nominal case.
     */
    @Test
    public void testNodeListViewerNominal() {
        assertNotNull(new NodeListViewer(new HistoryBrowserModel()));
    }
}
