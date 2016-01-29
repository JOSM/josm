// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.Bounds;

/**
 * Unit tests of {@link BoundingBoxSelection} class.
 */
public class BoundingBoxSelectionTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test for {@link BoundingBoxSelection#BoundingBoxSelection}.
     */
    @Test
    public void testBoundingBoxSelection() {
        BoundingBoxSelection sel = new BoundingBoxSelection();
        sel.addGui(null);
        sel.setDownloadArea(null);
        sel.setDownloadArea(new Bounds(0, 0, 1, 1));
    }
}
