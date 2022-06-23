// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link UploadedObjectsSummaryPanel} class.
 */
class UploadedObjectsSummaryPanelTest {
    /**
     * Test of {@link UploadedObjectsSummaryPanel#UploadedObjectsSummaryPanel}.
     */
    @Test
    void testUploadedObjectsSummaryPanel() {
        assertNotNull(new UploadedObjectsSummaryPanel());
    }
}
