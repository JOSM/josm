// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link NodeListViewer} class.
 */
public class NodeListViewerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
