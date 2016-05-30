// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link NativeScaleLayer} class.
 */
public class NativeScaleLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12255">#12255</a>.
     */
    @Test
    public void testTicket12255() {
        assertNull(new NativeScaleLayer.ScaleList(Collections.<Double>emptyList()).getSnapScale(10, 2, false));
    }
}
