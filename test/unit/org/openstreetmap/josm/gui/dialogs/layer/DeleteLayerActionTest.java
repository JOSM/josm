// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link DeleteLayerAction}
 */
@Main
@Projection
class DeleteLayerActionTest {
    private LayerListDialog.LayerListModel model;
    private DeleteLayerAction deleteLayerAction;
    private ShowHideLayerAction showHideLayerAction;

    @BeforeEach
    void setup() {
        final AtomicInteger counter = new AtomicInteger();
        final Supplier<OsmDataLayer> layerSupplier = () -> new OsmDataLayer(new DataSet(), "testActiveLayer" + counter.getAndIncrement(), null);
        for (int i = 0; i < 10; i++) {
            MainApplication.getLayerManager().addLayer(layerSupplier.get());
        }
        final LayerListDialog layerListDialog = LayerListDialog.getInstance();
        this.model = layerListDialog.getModel();
        this.deleteLayerAction = layerListDialog.createDeleteLayerAction();
        this.showHideLayerAction = layerListDialog.createShowHideLayerAction();
    }

    @Test
    void testSetActiveLayerOnlyOneVisible0to8() {
        hideRange(0, 8);
        assertEquals(9, model.getSelectedLayers().size());
        assertEquals(1, model.getLayers().stream().filter(Layer::isVisible).count());
        deleteLayerAction.actionPerformed(null);
        assertEquals(1, model.getSelectedLayers().size());
        final Layer layer = assertInstanceOf(OsmDataLayer.class, model.getLayer(0));
        assertNotNull(layer);
        assertTrue(layer.isVisible());
        assertSame(layer, model.getLayerManager().getActiveLayer());
        assertEquals("testActiveLayer0", layer.getName());
    }

    @Test
    void testSetActiveLayerOnlyOneVisible1to9() {
        hideRange(1, 9);
        assertEquals(9, model.getSelectedLayers().size());
        assertEquals(1, model.getLayers().stream().filter(Layer::isVisible).count());
        deleteLayerAction.actionPerformed(null);
        assertEquals(1, model.getSelectedLayers().size());
        final Layer layer = assertInstanceOf(OsmDataLayer.class, model.getLayer(0));
        assertNotNull(layer);
        assertTrue(layer.isVisible());
        assertSame(layer, model.getLayerManager().getActiveLayer());
        assertEquals("testActiveLayer9", layer.getName());
    }

    @Test
    void testRemoveMiddleActiveWithSurroundingHiddenLayers() {
        hideRange(3, 3);
        hideRange(5, 5);
        final Layer toRemove = model.getLayer(4);
        assertNotNull(toRemove);
        assertTrue(toRemove.isVisible());
        assertFalse(Objects.requireNonNull(model.getLayer(3)).isVisible());
        assertFalse(Objects.requireNonNull(model.getLayer(5)).isVisible());
        model.getLayerManager().setActiveLayer(toRemove);
        model.setSelectedLayer(toRemove);
        deleteLayerAction.actionPerformed(null);
        assertSame(model.getLayerManager().getActiveLayer(), model.getLayer(5));
        assertEquals("testActiveLayer3", Objects.requireNonNull(model.getLayer(5)).getName());
        assertAll(model.getLayers().stream().map(layer -> () -> assertNotSame(toRemove, layer)));
    }

    @Test
    void testRemoveTopActiveWithSurroundingHiddenLayers() {
        hideRange(1, 1);
        final Layer toRemove = model.getLayer(0);
        assertNotNull(toRemove);
        assertTrue(toRemove.isVisible());
        assertFalse(Objects.requireNonNull(model.getLayer(1)).isVisible());
        model.getLayerManager().setActiveLayer(toRemove);
        model.setSelectedLayer(toRemove);
        deleteLayerAction.actionPerformed(null);
        assertSame(model.getLayerManager().getActiveLayer(), model.getLayer(1));
        assertEquals("testActiveLayer7", Objects.requireNonNull(model.getLayer(1)).getName());
        assertAll(model.getLayers().stream().map(layer -> () -> assertNotSame(toRemove, layer)));
    }

    @Test
    void testRemoveBottomActiveWithSurroundingHiddenLayers() {
        hideRange(8, 8);
        final Layer toRemove = model.getLayer(9);
        assertNotNull(toRemove);
        assertTrue(toRemove.isVisible());
        assertFalse(Objects.requireNonNull(model.getLayer(8)).isVisible());
        model.getLayerManager().setActiveLayer(toRemove);
        model.setSelectedLayer(toRemove);
        deleteLayerAction.actionPerformed(null);
        assertSame(model.getLayerManager().getActiveLayer(), model.getLayer(7));
        assertEquals("testActiveLayer2", Objects.requireNonNull(model.getLayer(7)).getName());
        assertAll(model.getLayers().stream().map(layer -> () -> assertNotSame(toRemove, layer)));
    }

    @Test
    void testRemoveBottomActiveWithBackgroundLayer() {
        GeoImageLayer geoImageLayer = new GeoImageLayer(Collections.emptyList(), null, "imageLayer");
        OsmDataLayer osmDataLayer1 = new OsmDataLayer(new DataSet(), "dataLayer1", null);
        OsmDataLayer osmDataLayer2 = new OsmDataLayer(new DataSet(), "dataLayer2", null);

        // remove all the layers added in BeforeEach()
        for (Layer l : MainApplication.getLayerManager().getLayers()) {
            MainApplication.getLayerManager().removeLayer(l);
        }
        MainApplication.getLayerManager().addLayer(geoImageLayer);
        MainApplication.getLayerManager().addLayer(osmDataLayer1);
        MainApplication.getLayerManager().addLayer(osmDataLayer2);

        model.getLayerManager().setActiveLayer(osmDataLayer1);
        model.setSelectedLayer(osmDataLayer1);

        deleteLayerAction.actionPerformed(null);

        assertSame(model.getLayerManager().getActiveLayer(), model.getLayer(0));
        assertEquals("dataLayer2", Objects.requireNonNull(model.getLayerManager().getActiveLayer().getName()));
        assertAll(model.getLayers().stream().map(layer -> () -> assertNotSame(osmDataLayer1, layer)));
    }

    @Test
    void testRemoveBottomActiveAllHidden() {
        hideRange(0, 9);
        final Layer toRemove = model.getLayer(9);
        assertNotNull(toRemove);
        assertFalse(toRemove.isVisible());
        assertEquals(0, model.getLayers().stream().filter(Layer::isVisible).count());

        model.getLayerManager().setActiveLayer(toRemove);
        model.setSelectedLayer(toRemove);
        deleteLayerAction.actionPerformed(null);

        assertSame(model.getLayerManager().getActiveLayer(), model.getLayer(8));
        assertEquals("testActiveLayer1", Objects.requireNonNull(model.getLayer(8)).getName());
        assertAll(model.getLayers().stream().map(layer -> () -> assertNotSame(toRemove, layer)));
    }

    private void hideRange(int start, int end) {
        model.getSelectionModel().setSelectionInterval(start, end);
        showHideLayerAction.actionPerformed(null);
    }
}
