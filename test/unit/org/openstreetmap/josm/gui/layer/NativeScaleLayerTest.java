// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link NativeScaleLayer} class.
 */
class NativeScaleLayerTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12255">#12255</a>.
     */
    @Test
    void testTicket12255() {
        assertNull(new NativeScaleLayer.ScaleList(Collections.<Double>emptyList()).getSnapScale(10, 2, false));
    }
}
