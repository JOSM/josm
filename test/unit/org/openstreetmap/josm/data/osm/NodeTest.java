// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of the {@code Node} class.
 */
public class NodeTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Non-regression test for ticket #12060.
     */
    @Test
    public void testTicket12060() {
        DataSet ds = new DataSet();
        ds.dataSources.add(new DataSource(new Bounds(LatLon.ZERO), null));
        Node n = new Node(1, 1);
        n.setCoor(LatLon.ZERO);
        ds.addPrimitive(n);
        n.setCoor(null);
        assertFalse(n.isNewOrUndeleted());
        assertNotNull(ds.getDataSourceArea());
        assertNull(n.getCoor());
        assertFalse(n.isOutsideDownloadArea());
    }
}
