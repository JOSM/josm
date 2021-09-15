// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link MapPaintDialog} class.
 */
@Main
@Projection
class MapPaintDialogTest {
    /**
     * Unit test of {@link MapPaintDialog.InfoAction} class.
     */
    @Test
    void testInfoAction() {
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "", null));
        MainApplication.getMap().mapPaintDialog.new InfoAction().actionPerformed(null);
    }
}

