// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.junit.Assert.assertEquals;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test class for {@link CycleLayerDownAction}
 *
 * @author Taylor Smock
 */
public class CycleLayerActionTest {
    /** Layers need a projection */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().preferences().projection().fakeImagery();

    private CycleLayerDownAction cycleDown;
    private CycleLayerUpAction cycleUp;
    private MainLayerManager manager;

    /**
     * Set up common items (make layers, etc.)
     */
    @Before
    public void setUp() {
        cycleDown = new CycleLayerDownAction();
        cycleUp = new CycleLayerUpAction();
        manager = MainApplication.getLayerManager();
        for (int i = 0; i < 10; i++) {
            manager.addLayer(new OsmDataLayer(new DataSet(), tr("Layer {0}", i), null));
        }
    }

    /**
     * Test going down from the bottom
     */
    @Test
    public void testDownBottom() {
        manager.setActiveLayer(manager.getLayers().get(0));
        cycleDown.actionPerformed(null);
        assertEquals(manager.getLayers().size() - 1, manager.getLayers().indexOf(manager.getActiveLayer()));
    }

    /**
     * Check going up from the top
     */
    @Test
    public void testUpTop() {
        manager.setActiveLayer(manager.getLayers().get(manager.getLayers().size() - 1));
        cycleUp.actionPerformed(null);
        assertEquals(0, manager.getLayers().indexOf(manager.getActiveLayer()));
    }

    /**
     * Check going down
     */
    @Test
    public void testDown() {
        manager.setActiveLayer(manager.getLayers().get(3));
        cycleDown.actionPerformed(null);
        assertEquals(2, manager.getLayers().indexOf(manager.getActiveLayer()));
    }

    /**
     * Check going up
     */
    @Test
    public void testUp() {
        manager.setActiveLayer(manager.getLayers().get(3));
        cycleUp.actionPerformed(null);
        assertEquals(4, manager.getLayers().indexOf(manager.getActiveLayer()));
    }

    /**
     * Test no layers
     */
    @Test
    public void testNoLayers() {
        manager.getLayers().forEach(manager::removeLayer);
        cycleUp.actionPerformed(null);
        cycleDown.actionPerformed(null);
        assertEquals(0, manager.getLayers().size());
    }

    /**
     * Test with an aerial imagery layer
     */
    @Test
    public void testWithAerialImagery() {
        final ImageryInfo magentaTilesInfo = ImageryLayerInfo.instance.getLayers().stream()
                .filter(i -> i.getName().equals("Magenta Tiles")).findAny().get();
        ImageryLayer imageryLayer = ImageryLayer.create(magentaTilesInfo);
        manager.addLayer(imageryLayer);
        manager.moveLayer(imageryLayer, 5);
        manager.setActiveLayer(manager.getLayers().get(4));
        cycleUp.actionPerformed(null);
        assertEquals(6, manager.getLayers().indexOf(manager.getActiveLayer()));
        cycleDown.actionPerformed(null);
        assertEquals(4, manager.getLayers().indexOf(manager.getActiveLayer()));
    }
}
