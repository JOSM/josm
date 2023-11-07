// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link GeoImageLayer} class.
 */
// Basic preferences are needed for OSM primitives
@BasicPreferences
@Main
class GeoImageLayerTest {
    @AfterEach
    void tearDown() {
        if (ImageViewerDialog.hasInstance()) {
            ImageViewerDialog.getInstance().destroy();
        }
    }

    /**
     * Test that {@link GeoImageLayer#mergeFrom} throws IAE for invalid arguments
     */
    @Test
    void testMergeFromIAE() {
        GeoImageLayer geoImageLayer = new GeoImageLayer(Collections.emptyList(), null);
        OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "", null);
        assertThrows(IllegalArgumentException.class, () -> geoImageLayer.mergeFrom(osmDataLayer));
    }
}
