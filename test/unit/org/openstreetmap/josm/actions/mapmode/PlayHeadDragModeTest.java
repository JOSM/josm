// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link PlayHeadDragMode}.
 */
@Main
@Projection
class PlayHeadDragModeTest {
    /**
     * Unit test of {@link PlayHeadDragMode#enterMode} and {@link PlayHeadDragMode#exitMode}.
     */
    @Test
    void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            PlayHeadDragMode mapMode = new PlayHeadDragMode(PlayHeadMarker.create());
            MapFrame map = MainApplication.getMap();
            MapMode oldMapMode = map.mapMode;
            assertTrue(map.selectMapMode(mapMode));
            assertEquals(mapMode, map.mapMode);
            assertTrue(map.selectMapMode(oldMapMode));
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
