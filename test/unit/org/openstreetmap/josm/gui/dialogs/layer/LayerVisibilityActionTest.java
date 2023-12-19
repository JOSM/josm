// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.dialogs.layer.LayerVisibilityAction.OpacitySlider;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.TMSLayerTest;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link LayerVisibilityAction} class.
 */
@BasicPreferences
@Main
@Projection
class LayerVisibilityActionTest {
    /**
     * Unit test of {@link LayerVisibilityAction} class.
     */
    @Test
    void testLayerVisibilityAction() {
        TMSLayer layer = TMSLayerTest.createTmsLayer();
        LayerListModel model = new LayerListDialog(MainApplication.getLayerManager()) {
            @Override
            protected void registerInWindowMenu(boolean isExpert) {
                // ignore
            }
        }.getModel();
        LayerVisibilityAction action = new LayerVisibilityAction(model);
        action.updateEnabledState();
        assertFalse(action.isEnabled());

        MainApplication.getLayerManager().addLayer(layer);
        model.setSelectedLayer(layer);
        action.updateEnabledState();
        assertTrue(action.isEnabled());
        assertTrue(action.supportLayers(model.getSelectedLayers()));

        // now check values
        action.updateValues();
        OpacitySlider opacitySlider = action.sliders.stream()
                .filter(x -> x instanceof OpacitySlider).map(x -> (OpacitySlider) x).findFirst().get();

        assertEquals(1.0, opacitySlider.getRealValue(), 1e-15);
        assertEquals("OpacitySlider [getRealValue()=1.0]", opacitySlider.toString());

        opacitySlider.setRealValue(.5);
        action.updateValues();

        assertEquals(0.5, opacitySlider.getRealValue(), 1e-15);
        assertEquals("OpacitySlider [getRealValue()=0.5]", opacitySlider.toString());

        action.setVisibleFlag(false);
        action.updateValues();
        assertFalse(layer.isVisible());

        action.setVisibleFlag(true);
        action.updateValues();
        assertTrue(layer.isVisible());

        // layer stays visible during adjust
        opacitySlider.slider.setValueIsAdjusting(true);
        opacitySlider.setRealValue(0);
        assertEquals(0, layer.getOpacity(), 1e-15);
        layer.setOpacity(.1); // to make layer.isVisible work
        assertTrue(layer.isVisible());
        layer.setOpacity(0);

        opacitySlider.slider.setValueIsAdjusting(false);
        opacitySlider.setRealValue(0);
        assertEquals(0, layer.getOpacity(), 1e-15);
        layer.setOpacity(.1); // to make layer.isVisible work
        assertFalse(layer.isVisible());
        layer.setOpacity(0);
        action.updateValues();

        // Opacity reset when it was 0 and user set layer to visible.
        action.setVisibleFlag(true);
        action.updateValues();
        assertEquals(1.0, opacitySlider.getRealValue(), 1e-15);
        assertEquals(1.0, layer.getOpacity(), 1e-15);
    }
}
