// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link CoordinateInfoViewer} class.
 */
public class CoordinateInfoViewerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test for {@link CoordinateInfoViewer#CoordinateInfoViewer} - {@code null} handling.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCoordinateInfoViewerNull() {
        new CoordinateInfoViewer(null);
    }

    /**
     * Test for {@link CoordinateInfoViewer#CoordinateInfoViewer} - nominal case.
     */
    @Test
    public void testCoordinateInfoViewerNominal() {
        assertNotNull(new CoordinateInfoViewer(new HistoryBrowserModel()));
    }
}
