// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.LayerManagerTest.TestLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link MergeLayerAction}.
 */
public class MergeLayerActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().main();

    private MergeLayerAction action;

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        if (action == null) {
            action = new MergeLayerAction();
        }
        for (TestLayer testLayer : Main.getLayerManager().getLayersOfType(TestLayer.class)) {
            Main.getLayerManager().removeLayer(testLayer);
        }
    }

    /**
     * Tests that no error occurs when no source layer exists.
     */
    @Test
    public void testMergeNoSourceLayer() {
        assertNull(Main.getLayerManager().getActiveLayer());
        action.actionPerformed(null);
        assertEquals(0, Main.getLayerManager().getLayers().size());
    }

    /**
     * Tests that no error occurs when no target layer exists.
     */
    @Test
    public void testMergeNoTargetLayer() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        Main.getLayerManager().addLayer(layer);
        assertEquals(1, Main.getLayerManager().getLayers().size());
        assertNull(action.merge(layer));
        assertEquals(1, Main.getLayerManager().getLayers().size());
    }

    /**
     * Tests that the merge is done with two empty layers.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMergeTwoEmptyLayers() throws Exception {
        OsmDataLayer layer1 = new OsmDataLayer(new DataSet(), "1", null);
        OsmDataLayer layer2 = new OsmDataLayer(new DataSet(), "2", null);
        Main.getLayerManager().addLayer(layer1);
        Main.getLayerManager().addLayer(layer2);
        assertEquals(2, Main.getLayerManager().getLayers().size());
        action.merge(layer2).get();
        assertEquals(1, Main.getLayerManager().getLayers().size());
    }
}
