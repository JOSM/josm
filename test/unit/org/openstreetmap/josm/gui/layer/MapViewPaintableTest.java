// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationEvent;

/**
 * Unit tests of {@link MapViewPaintable} class.
 */
class MapViewPaintableTest {

    /**
     * Setup tests
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link MapViewPaintable.PaintableInvalidationEvent#toString}
     */
    @Test
    void testToString() {
        assertEquals("LayerInvalidationEvent [layer=null]", new PaintableInvalidationEvent(null).toString());
    }
}
