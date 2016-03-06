// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link ImproveWayAccuracyAction}.
 */
public class ImproveWayAccuracyActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link ImproveWayAccuracyAction#enterMode} and {@link ImproveWayAccuracyAction#exitMode}.
     */
    @Test
    public void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            Main.main.addLayer(layer);
            ImproveWayAccuracyAction mapMode = new ImproveWayAccuracyAction(Main.map);
            MapMode oldMapMode = Main.map.mapMode;
            assertTrue(Main.map.selectMapMode(mapMode));
            assertEquals(mapMode, Main.map.mapMode);
            assertTrue(Main.map.selectMapMode(oldMapMode));
        } finally {
            Main.main.removeLayer(layer);
        }
    }
}
