// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationEvent;

/**
 * Unit tests of {@link MapViewPaintable} class.
 */
public class MapViewPaintableTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link MapViewPaintable.PaintableInvalidationEvent#toString}
     */
    @Test
    public void testToString() {
        assertEquals("LayerInvalidationEvent [layer=null]", new PaintableInvalidationEvent(null).toString());
    }
}
