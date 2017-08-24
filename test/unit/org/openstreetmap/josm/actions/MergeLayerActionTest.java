// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
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
        for (TestLayer testLayer : MainApplication.getLayerManager().getLayersOfType(TestLayer.class)) {
            MainApplication.getLayerManager().removeLayer(testLayer);
        }
    }

    /**
     * Tests that no error occurs when no source layer exists.
     */
    @Test
    public void testMergeNoSourceLayer() {
        assertNull(MainApplication.getLayerManager().getActiveLayer());
        action.actionPerformed(null);
        assertEquals(0, MainApplication.getLayerManager().getLayers().size());
    }

    /**
     * Tests that no error occurs when no target layer exists.
     */
    @Test
    public void testMergeNoTargetLayer() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertEquals(1, MainApplication.getLayerManager().getLayers().size());
        assertNull(action.merge(layer));
        assertEquals(1, MainApplication.getLayerManager().getLayers().size());
    }

    /**
     * Tests that the merge is done with two empty layers.
     * @throws Exception if any error occurs
     */
    @Test
    public void testMergeTwoEmptyLayers() throws Exception {
        OsmDataLayer layer1 = new OsmDataLayer(new DataSet(), "1", null);
        OsmDataLayer layer2 = new OsmDataLayer(new DataSet(), "2", null);
        MainApplication.getLayerManager().addLayer(layer1);
        MainApplication.getLayerManager().addLayer(layer2);
        assertEquals(2, MainApplication.getLayerManager().getLayers().size());
        action.merge(layer2).get();
        assertEquals(1, MainApplication.getLayerManager().getLayers().size());
    }
}
