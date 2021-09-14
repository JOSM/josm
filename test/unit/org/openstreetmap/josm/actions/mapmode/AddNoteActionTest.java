// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link AddNoteAction}.
 */
@Main
@Projection
class AddNoteActionTest {
    /**
     * Unit test of {@link AddNoteAction#enterMode} and {@link AddNoteAction#exitMode}.
     */
    @Test
    void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        AddNoteAction mapMode = new AddNoteAction(new NoteData());
        MapFrame map = MainApplication.getMap();
        MapMode oldMapMode = map.mapMode;
        assertTrue(map.selectMapMode(mapMode));
        assertEquals(mapMode, map.mapMode);
        assertTrue(map.selectMapMode(oldMapMode));
    }
}

