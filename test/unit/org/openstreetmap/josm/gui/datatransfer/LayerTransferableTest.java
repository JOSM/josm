// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.datatransfer.LayerTransferable.Data;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManagerTest;
import org.openstreetmap.josm.gui.layer.LayerManagerTest.TestLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;

/**
 * Tests for {@link LayerTransferable}
 * @author Michael Zangl
 * @since 10605
 */
class LayerTransferableTest {
    private TestLayer layer1;
    private TestLayer layer2;
    private MainLayerManager manager;

    /**
     * Set up test data
     */
    @BeforeEach
    public void createTestData() {
        layer1 = new LayerManagerTest.TestLayer();
        layer2 = new LayerManagerTest.TestLayer();
        manager = new MainLayerManager();
        manager.addLayer(layer1);
        manager.addLayer(layer2);
    }

    /**
     * Test {@link LayerTransferable.Data}
     */
    @Test
    void testLayerData() {
        Data data = new Data(manager, Arrays.<Layer>asList(layer1, layer2));

        // need to be identity
        assertSame(manager, data.getManager());
        assertSame(layer1, data.getLayers().get(0));
        assertSame(layer2, data.getLayers().get(1));
    }

    /**
     * Test {@link LayerTransferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)}
     * and {@link LayerTransferable#getTransferDataFlavors()}
     */
    @Test
    void testSupportedDataFlavor() {
        LayerTransferable transferable = new LayerTransferable(manager, Arrays.<Layer>asList(layer1, layer2));

        assertFalse(transferable.isDataFlavorSupported(DataFlavor.imageFlavor));
        assertTrue(transferable.isDataFlavorSupported(LayerTransferable.LAYER_DATA));

        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        assertEquals(1, flavors.length);
        assertEquals(LayerTransferable.LAYER_DATA, flavors[0]);
    }

    /**
     * Test {@link LayerTransferable#getTransferData(DataFlavor)}
     * @throws Exception if any error occurs
     */
    @Test
    void testTransferData() throws Exception {
        LayerTransferable transferable = new LayerTransferable(manager, Arrays.<Layer>asList(layer1, layer2));

        Object object = transferable.getTransferData(LayerTransferable.LAYER_DATA);
        assertTrue(object instanceof Data);
        Data data = (Data) object;
        assertSame(manager, data.getManager());
        assertSame(layer1, data.getLayers().get(0));
        assertSame(layer2, data.getLayers().get(1));
    }

    /**
     * Test {@link LayerTransferable#getTransferData(DataFlavor)} for unsupported {@link DataFlavor}
     */
    @Test
    void testTransferDataUnsupported() {
        LayerTransferable transferable = new LayerTransferable(manager, Arrays.<Layer>asList(layer1, layer2));

        assertThrows(UnsupportedFlavorException.class, () -> transferable.getTransferData(DataFlavor.imageFlavor));
    }
}
