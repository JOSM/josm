// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
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
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Unit test of {@code Main.map.mapView.playHeadMarker}.
     */
    @Test
    public void testPlayHeadMarker() {
        try {
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "", null));
            MapFrame map = MainApplication.getMap();
            MarkerLayer layer = new MarkerLayer(new GpxData(), null, null, null);
            assertNull(map.mapView.playHeadMarker);
            MainApplication.getLayerManager().addLayer(layer);
            assertNotNull(map.mapView.playHeadMarker);
            MainApplication.getLayerManager().removeLayer(layer);
        } finally {
            if (MainApplication.isDisplayingMapView()) {
                MainApplication.getMap().mapView.playHeadMarker = null;
            }
        }
    }
}
