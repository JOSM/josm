// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test the {@link LayerPositionStrategy} class.
 */
public class LayerPositionStrategyTest {

    /**
     * Test of robustness against null manager.
     */
    @Test
    public void testNullManager() {
        assertEquals(0, LayerPositionStrategy.inFrontOfFirst(l -> true).getPosition(null));
        assertEquals(0, LayerPositionStrategy.afterLast(l -> true).getPosition(null));
    }
}
