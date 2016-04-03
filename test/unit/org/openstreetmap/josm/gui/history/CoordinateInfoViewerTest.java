// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link CoordinateInfoViewer} class.
 */
public class CoordinateInfoViewerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

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
