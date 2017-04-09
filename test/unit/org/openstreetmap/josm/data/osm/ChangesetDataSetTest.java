// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Date;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ChangesetDataSet}.
 */
public class ChangesetDataSetTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of method {@link ChangesetDataSet#getPrimitivesByModificationType}.
     */
    @Test
    public void testGetPrimitivesByModificationType() {
        final ChangesetDataSet cds = new ChangesetDataSet();
        // empty object, null parameter => IllegalArgumentException
        try {
            cds.getPrimitivesByModificationType(null);
            Assert.fail("Should have thrown an IllegalArgumentException as we gave a null argument.");
        } catch (IllegalArgumentException e) {
            Main.trace(e);
            // Was expected
        }

        // empty object, a modification type => empty list
        Assert.assertTrue(
            "Empty data set should produce an empty list.",
            cds.getPrimitivesByModificationType(
                    ChangesetModificationType.CREATED).isEmpty()
        );

        // object with various items and modification types, fetch for CREATED
        // => list containing only the CREATED item
        HistoryNode prim1 = new HistoryNode(1, 1, true, User.getAnonymous(), 1, new Date(), LatLon.ZERO);
        HistoryNode prim2 = new HistoryNode(2, 1, true, User.createLocalUser("test"), 1, new Date(), LatLon.NORTH_POLE);
        HistoryNode prim3 = new HistoryNode(3, 1, true, User.getAnonymous(), 1, new Date(), LatLon.SOUTH_POLE);
        cds.put(prim1, ChangesetModificationType.CREATED);
        cds.put(prim2, ChangesetModificationType.DELETED);
        cds.put(prim3, ChangesetModificationType.UPDATED);
        Set<HistoryOsmPrimitive> result = cds.getPrimitivesByModificationType(
                    ChangesetModificationType.CREATED);
        Assert.assertEquals("We should have found only one item.", 1, result.size());
        Assert.assertTrue("The item found is prim1.", result.contains(prim1));
    }
}
