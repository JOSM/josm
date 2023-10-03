// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link DownloadReferrersTask}.
 */
class DownloadReferrersTaskTest {
    /**
     * Unit test of {@code DownloadReferrersTask#DownloadReferrersTask}.
     */
    @Test
    void testDownloadReferrersTask() {
        DataSet ds = new DataSet();
        Node n1 = (Node) OsmPrimitiveType.NODE.newInstance(-1, true);
        n1.setCoor(LatLon.ZERO);
        Node n2 = new Node(1);
        n2.setCoor(LatLon.ZERO);
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        assertDoesNotThrow(() -> new DownloadReferrersTask(layer, null));
        assertDoesNotThrow(() -> new DownloadReferrersTask(layer, ds.allPrimitives()));
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> new DownloadReferrersTask(layer, n1, null));
        assertEquals("Cannot download referrers for new primitives (ID -1)", iae.getMessage());
        assertDoesNotThrow(() -> new DownloadReferrersTask(layer, n2.getPrimitiveId(), null));
    }
}
