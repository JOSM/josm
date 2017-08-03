// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.TMSLayerTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link LayerVisibilityAction} class.
 */
public class LayerVisibilityActionTest {
    /**
     * TMS layer needs prefs. Platform for LayerListDialog shortcuts.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection().platform().mainMenu();

    /**
     * Unit test of {@link LayerVisibilityAction} class.
     */
    @Test
    public void testLayerVisibilityAction() {
        TMSLayer layer = TMSLayerTest.createTmsLayer();
        LayerListModel model = new LayerListDialog(Main.getLayerManager()) {
            @Override
            protected void registerInWindowMenu() {
                // ignore
            }
        }.getModel();
        LayerVisibilityAction action = new LayerVisibilityAction(model);
        action.updateEnabledState();
        assertFalse(action.isEnabled());

        Main.getLayerManager().addLayer(layer);
        model.setSelectedLayer(layer);
        action.updateEnabledState();
        assertTrue(action.isEnabled());
        assertTrue(action.supportLayers(model.getSelectedLayers()));

        // now check values
        action.updateValues();
        assertEquals(1.0, action.opacitySlider.getRealValue(), 1e-15);
        assertEquals("OpacitySlider [getRealValue()=1.0]", action.opacitySlider.toString());

        action.opacitySlider.setRealValue(.5);
        action.updateValues();

        assertEquals(0.5, action.opacitySlider.getRealValue(), 1e-15);
        assertEquals("OpacitySlider [getRealValue()=0.5]", action.opacitySlider.toString());

        action.setVisibleFlag(false);
        action.updateValues();
        assertFalse(layer.isVisible());

        action.setVisibleFlag(true);
        action.updateValues();
        assertTrue(layer.isVisible());

        // layer stays visible during adjust
        action.opacitySlider.slider.setValueIsAdjusting(true);
        action.opacitySlider.setRealValue(0);
        assertEquals(0, layer.getOpacity(), 1e-15);
        layer.setOpacity(.1); // to make layer.isVisible work
        assertTrue(layer.isVisible());
        layer.setOpacity(0);

        action.opacitySlider.slider.setValueIsAdjusting(false);
        action.opacitySlider.setRealValue(0);
        assertEquals(0, layer.getOpacity(), 1e-15);
        layer.setOpacity(.1); // to make layer.isVisible work
        assertFalse(layer.isVisible());
        layer.setOpacity(0);
        action.updateValues();

        // Opacity reset when it was 0 and user set layer to visible.
        action.setVisibleFlag(true);
        action.updateValues();
        assertEquals(1.0, action.opacitySlider.getRealValue(), 1e-15);
        assertEquals(1.0, layer.getOpacity(), 1e-15);
    }
}
