// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TileSelection} class.
 */
public class TileSelectionTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test for {@link TileSelection#TileSelection}.
     */
    @Test
    public void testTileSelection() {
        TileSelection sel = new TileSelection();
        sel.addGui(null);
        sel.setDownloadArea(null);
        sel.setDownloadArea(new Bounds(0, 0, 1, 1));
    }
}
