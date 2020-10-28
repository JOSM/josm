// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DownloadReferrersTask}.
 */
class DownloadReferrersTaskTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
        assertNotNull(new DownloadReferrersTask(layer, null));
        assertNotNull(new DownloadReferrersTask(layer, ds.allPrimitives()));
        try {
            new DownloadReferrersTask(layer, n1.getPrimitiveId(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot download referrers for new primitives (ID -1)", e.getMessage());
        }
        assertNotNull(new DownloadReferrersTask(layer, n2.getPrimitiveId(), null));
    }
}
