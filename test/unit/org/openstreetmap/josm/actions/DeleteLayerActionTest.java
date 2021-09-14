// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link DeleteLayerAction}.
 */
@Main
@Projection
final class DeleteLayerActionTest {
    /**
     * Unit test of {@link DeleteLayerAction#actionPerformed}
     */
    @Test
    void testActionPerformed() {
        DeleteLayerAction action = new DeleteLayerAction();
        // No layer
        action.actionPerformed(null);
        // OsmDataLayer
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(MainApplication.getLayerManager().getActiveLayer());
        action.actionPerformed(null);
        assertNull(MainApplication.getLayerManager().getActiveLayer());
    }
}
