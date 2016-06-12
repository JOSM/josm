// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;

/**
 * Unit tests for class {@link AddImageryLayerAction}.
 */
public final class AddImageryLayerActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link AddImageryLayerAction#updateEnabledState}.
     */
    @Test
    public void testEnabledState() {
        assertFalse(new AddImageryLayerAction(new ImageryInfo()).isEnabled());
        assertFalse(new AddImageryLayerAction(new ImageryInfo("google", "http://maps.google.com/api", "tms", null, null)).isEnabled());
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo_tms", "http://bar", "tms", null, null)).isEnabled());
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo_bing", "http://bar", "bing", null, null)).isEnabled());
        assertTrue(new AddImageryLayerAction(new ImageryInfo("foo_scanex", "http://bar", "scanex", null, null)).isEnabled());
        assertFalse(new AddImageryLayerAction(new ImageryInfo("foo_wms_endpoint", "http://bar", "wms_endpoint", null, null)).isEnabled());
    }

    /**
     * Unit test of {@link AddImageryLayerAction#actionPerformed} - Enabled cases.
     */
    @Test
    public void testActionPerformedEnabled() {
        assertTrue(Main.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
        new AddImageryLayerAction(new ImageryInfo("foo_tms", "http://bar", "tms", null, null)).actionPerformed(null);
        List<TMSLayer> tmsLayers = Main.getLayerManager().getLayersOfType(TMSLayer.class);
        assertEquals(1, tmsLayers.size());

        try {
            new AddImageryLayerAction(new ImageryInfo("wms.openstreetmap.fr", "http://wms.openstreetmap.fr/wms?",
                    "wms_endpoint", null, null)).actionPerformed(null);
            List<WMSLayer> wmsLayers = Main.getLayerManager().getLayersOfType(WMSLayer.class);
            assertEquals(1, wmsLayers.size());

            Main.map.mapView.removeLayer(wmsLayers.get(0));
        } finally {
            Main.map.mapView.removeLayer(tmsLayers.get(0));
        }
    }

    /**
     * Unit test of {@link AddImageryLayerAction#actionPerformed} - disabled case.
     */
    @Test
    public void testActionPerformedDisabled() {
        assertTrue(Main.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
        new AddImageryLayerAction(new ImageryInfo()).actionPerformed(null);
        assertTrue(Main.getLayerManager().getLayersOfType(TMSLayer.class).isEmpty());
    }
}
