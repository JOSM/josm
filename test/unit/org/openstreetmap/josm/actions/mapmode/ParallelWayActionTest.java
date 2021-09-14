// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.mapmode.ParallelWayAction.Mode;
import org.openstreetmap.josm.actions.mapmode.ParallelWayAction.Modifier;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link ParallelWayAction}.
 */
@Main
@Projection
class ParallelWayActionTest {
    /**
     * Unit test of {@link ParallelWayAction#enterMode} and {@link ParallelWayAction#exitMode}.
     */
    @Test
    void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        MapFrame map = MainApplication.getMap();
        ParallelWayAction mapMode = new ParallelWayAction(map);
        MapMode oldMapMode = map.mapMode;
        assertTrue(map.selectMapMode(mapMode));
        assertEquals(mapMode, map.mapMode);
        assertTrue(map.selectMapMode(oldMapMode));
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

