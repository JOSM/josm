// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MarkerLayer} class.
 */
public class MarkerLayerTest {

    /**
     * For creating layers
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().mainMenu().platform().projection();

    /**
     * Unit test of {@code Main.map.mapView.playHeadMarker}.
     */
    @Test
    public void testPlayHeadMarker() {
        try {
            Main.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "", null));
            MarkerLayer layer = new MarkerLayer(new GpxData(), null, null, null);
            assertNull(Main.map.mapView.playHeadMarker);
            Main.getLayerManager().addLayer(layer);
            assertNotNull(Main.map.mapView.playHeadMarker);
            Main.getLayerManager().removeLayer(layer);
        } finally {
            Main.map.mapView.playHeadMarker = null;
        }
    }
}
