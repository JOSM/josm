// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test the {@link LayerPositionStrategy} class.
 */
class LayerPositionStrategyTest {

    /**
     * Test of robustness against null manager.
     */
    @Test
    void testNullManager() {
        assertEquals(0, LayerPositionStrategy.inFrontOfFirst(l -> true).getPosition(null));
        assertEquals(0, LayerPositionStrategy.afterLast(l -> true).getPosition(null));
    }
}
