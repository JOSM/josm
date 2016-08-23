// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationListener;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test of the base {@link AbstractMapViewPaintable} class
 * @author Michael Zangl
 */
public class AbstractMapViewPaintableTest {
    /**
     * No special test rules
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private Layer testLayer;

    /**
     * Create test layer
     */
    @Before
    public void setUp() {
        testLayer = new LayerManagerTest.TestLayer();
    }

    /**
     * Test {@link Layer#invalidate()}
     */
    @Test
    public void testInvalidate() {
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
