// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.mapmode.ParallelWayAction.Mode;
import org.openstreetmap.josm.actions.mapmode.ParallelWayAction.Modifier;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.MapModeUtils;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests for class {@link ParallelWayAction}.
 */
@Main
@Projection
class ParallelWayActionTest {
    private MapFrame map;
    private ParallelWayAction mapMode;
    private DataSet dataSet;

    @BeforeEach
    void setup() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "ParallelWayActionTest", null);
        MainApplication.getLayerManager().addLayer(layer);
        this.map = MainApplication.getMap();
        this.mapMode = new ParallelWayAction(this.map);
        this.dataSet = layer.getDataSet();
    }

    /**
     * Unit test of {@link ParallelWayAction#enterMode} and {@link ParallelWayAction#exitMode}.
     */
    @Test
    void testMode() {
        final MapMode oldMapMode = this.map.mapMode;
        assertTrue(this.map.selectMapMode(mapMode));
        assertEquals(this.mapMode, this.map.mapMode);
        assertTrue(this.map.selectMapMode(oldMapMode));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20908">#20908</a>
     */
    @Test
    void testNonRegression20908() {
        Logging.clearLastErrorAndWarnings();
        this.map.selectMapMode(this.map.mapModeDraw);
        MapModeUtils.clickAt(LatLon.ZERO);
        MapModeUtils.clickAt(2, new LatLon(0.0001, 0));
        assertEquals(3, this.dataSet.allPrimitives().size());
        this.map.selectMapMode(mapMode);
        MapModeUtils.dragFromTo(new LatLon(0.00005, 0), new LatLon(0.00005, 0.0001));
        assertEquals(6, this.dataSet.allPrimitives().size());
        UndoRedoHandler.getInstance().undo();
        assertEquals(3, this.dataSet.allPrimitives().size());
        this.map.mapMode.mousePressed(MapModeUtils.mouseClickAt(new LatLon(0.00005, 0.0001)));
        assertTrue(Logging.getLastErrorAndWarnings().isEmpty(), String.join("\n", Logging.getLastErrorAndWarnings()));
    }

    /**
     * Unit test of {@link Mode} enum.
     */
    @Test
    void testEnumMode() {
        TestUtils.superficialEnumCodeCoverage(Mode.class);
    }

    /**
     * Unit test of {@link Modifier} enum.
     */
    @Test
    void testEnumModifier() {
        TestUtils.superficialEnumCodeCoverage(Modifier.class);
    }
}
