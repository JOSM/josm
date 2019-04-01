// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetDataSetEntry;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

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
     * Unit test of method {@link ChangesetDataSet#iterator}.
     */
    @Test
    public void testIterator() {
        final ChangesetDataSet cds = new ChangesetDataSet();
        HistoryNode prim1 = new HistoryNode(1, 1, true, User.getAnonymous(), 1, new Date(), LatLon.ZERO);
        cds.put(prim1, ChangesetModificationType.CREATED);
        Iterator<ChangesetDataSetEntry> it = cds.iterator();
        assertTrue(it.hasNext());
        ChangesetDataSetEntry cdse = it.next();
        assertEquals(ChangesetModificationType.CREATED, cdse.getModificationType());
        assertEquals(prim1, cdse.getPrimitive());
        assertFalse(it.hasNext());
        try {
            it.remove();
            fail("remove should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            Logging.trace(e.getMessage());
        }
    }

    /**
     * Unit test of {@link ChangesetModificationType} enum.
     */
    @Test
    public void testEnumChangesetModificationType() {
        TestUtils.superficialEnumCodeCoverage(ChangesetModificationType.class);
    }
}
