// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationListener;

/**
 * Test of the base {@link AbstractMapViewPaintable} class
 * @author Michael Zangl
 */
class AbstractMapViewPaintableTest {
    private Layer testLayer;

    /**
     * Create test layer
     */
    @BeforeEach
    public void setUp() {
        testLayer = new LayerManagerTest.TestLayer();
    }

    /**
     * Test {@link Layer#invalidate()}
     */
    @Test
    void testInvalidate() {
        AtomicBoolean fired = new AtomicBoolean();
        PaintableInvalidationListener listener = l -> fired.set(true);
        testLayer.addInvalidationListener(listener);
        assertFalse(fired.get());
        testLayer.invalidate();
        assertTrue(fired.get());

        fired.set(false);
        testLayer.removeInvalidationListener(listener);
        testLayer.invalidate();
        assertFalse(fired.get());
    }
}
