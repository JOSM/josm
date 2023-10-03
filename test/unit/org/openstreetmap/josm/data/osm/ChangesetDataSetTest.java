// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetDataSetEntry;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests for class {@link ChangesetDataSet}.
 */
class ChangesetDataSetTest {
    /**
     * Unit test of method {@link ChangesetDataSet#iterator}.
     */
    @Test
    void testIterator() {
        final ChangesetDataSet cds = new ChangesetDataSet();
        HistoryNode prim1 = new HistoryNode(1, 1, true, User.getAnonymous(), 1, Instant.now(), LatLon.ZERO);
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
     * Unit test of method {@link ChangesetDataSet#getFirstEntry(PrimitiveId)} and {@link ChangesetDataSet#getLastEntry(PrimitiveId)}.
     */
    @Test
    void testGetEntry() {
        final ChangesetDataSet cds = new ChangesetDataSet();
        HistoryNode prim1 = new HistoryNode(1, 1, true, User.getAnonymous(), 1, Instant.now(), LatLon.ZERO);
        cds.put(prim1, ChangesetModificationType.CREATED);
        HistoryNode prim2 = new HistoryNode(1, 2, true, User.getAnonymous(), 1, Instant.now(), LatLon.ZERO);
        prim2.put("highway", "stop");
        cds.put(prim2, ChangesetModificationType.UPDATED);
        assertEquals(prim1, cds.getFirstEntry(prim1.getPrimitiveId()).getPrimitive());
        assertEquals(prim2, cds.getLastEntry(prim1.getPrimitiveId()).getPrimitive());
        HistoryNode prim3 = new HistoryNode(1, 3, false, User.getAnonymous(), 1, Instant.now(), null);

        cds.put(prim3, ChangesetModificationType.DELETED);
        assertEquals(prim1, cds.getFirstEntry(prim1.getPrimitiveId()).getPrimitive());
        assertEquals(prim3, cds.getLastEntry(prim1.getPrimitiveId()).getPrimitive());
    }

    /**
     * Unit test of {@link ChangesetModificationType} enum.
     */
    @Test
    void testEnumChangesetModificationType() {
        TestUtils.superficialEnumCodeCoverage(ChangesetModificationType.class);
    }
}
