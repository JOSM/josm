// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.Bounds;

/**
 * Unit tests of {@link BookmarkSelection} class.
 */
public class BookmarkSelectionTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test for {@link BookmarkSelection#BookmarkSelection}.
     */
    @Test
    public void testBookmarkSelection() {
        BookmarkSelection sel = new BookmarkSelection();
        sel.addGui(null);
        sel.setDownloadArea(null);
        sel.setDownloadArea(new Bounds(0, 0, 1, 1));
    }
}
