// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.mapmode.ImproveWayAccuracyAction.State;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ImproveWayAccuracyAction}.
 */
public class ImproveWayAccuracyActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().platform().mainMenu().projection();

    /**
     * Unit test of {@link ImproveWayAccuracyAction#enterMode} and {@link ImproveWayAccuracyAction#exitMode}.
     */
    @Test
    public void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            Main.getLayerManager().addLayer(layer);
            ImproveWayAccuracyAction mapMode = new ImproveWayAccuracyAction();
            MapFrame map = MainApplication.getMap();
            MapMode oldMapMode = map.mapMode;
            assertTrue(map.selectMapMode(mapMode));
            assertEquals(mapMode, map.mapMode);
            assertTrue(map.selectMapMode(oldMapMode));
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link State} enum.
     */
    @Test
    public void testEnumState() {
        TestUtils.superficialEnumCodeCoverage(State.class);
    }
}
