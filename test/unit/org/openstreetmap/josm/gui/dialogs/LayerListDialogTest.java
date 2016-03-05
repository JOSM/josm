// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerGammaAction;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerOpacityAction;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.TMSLayerTest;

/**
 * Unit tests of {@link LayerListDialog} class.
 */
public class LayerListDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link LayerGammaAction} class.
     */
    @Test
    public void testLayerGammaAction() {
        TMSLayer layer = TMSLayerTest.createTmsLayer();
        try {
            Main.map.mapView.addLayer(layer);
            LayerListModel model = LayerListDialog.getInstance().getModel();
            LayerGammaAction action = new LayerGammaAction(model);
            action.updateEnabledState();
            assertTrue(action.isEnabled());
            assertTrue(action.supportLayers(model.getSelectedLayers()));
            assertEquals(1.0, action.getValue(), 1e-15);
            action.setValue(0.5);
            assertEquals(0.5, action.getValue(), 1e-15);
        } finally {
            Main.map.mapView.removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link LayerOpacityAction} class.
     */
    @Test
    public void testLayerOpacityAction() {
        TMSLayer layer = TMSLayerTest.createTmsLayer();
        try {
            Main.map.mapView.addLayer(layer);
            LayerListModel model = LayerListDialog.getInstance().getModel();
            LayerOpacityAction action = new LayerOpacityAction(model);
            action.updateEnabledState();
            assertTrue(action.isEnabled());
            assertTrue(action.supportLayers(model.getSelectedLayers()));
            assertEquals(1.0, action.getValue(), 1e-15);
            action.setValue(0.5);
            assertEquals(0.5, action.getValue(), 1e-15);
        } finally {
            Main.map.mapView.removeLayer(layer);
        }
    }
}
