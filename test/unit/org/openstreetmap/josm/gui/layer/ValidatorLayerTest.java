// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * Unit tests of {@link ValidatorLayer} class.
 */
public class ValidatorLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link ValidatorLayer#ValidatorLayer}.
     */
    @Test
    public void testValidatorLayer() {
        ValidatorLayer layer = null;
        try {
            layer = new ValidatorLayer();
            MainApplication.getLayerManager().addLayer(layer);
            assertFalse(layer.isMergable(null));
            assertNotNull(layer.getIcon());
            assertEquals("<html>No validation errors</html>", layer.getToolTipText());
            assertEquals("<html>No validation errors</html>", layer.getInfoComponent());
            assertTrue(layer.getMenuEntries().length > 0);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            if (layer != null) {
                MainApplication.getLayerManager().removeLayer(layer);
            }
        }
    }
}
