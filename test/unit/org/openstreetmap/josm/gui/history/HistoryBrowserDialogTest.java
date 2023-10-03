// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;

/**
 * Unit tests of {@link HistoryBrowserDialog} class.
 */
class HistoryBrowserDialogTest {
    /**
     * Test for {@link HistoryBrowserDialog#buildTitle}.
     */
    @Test
    void testBuildTitle() {
        HistoryDataSet hds = new HistoryDataSet();
        User user = User.createOsmUser(1, "");
        Instant date = Instant.parse("2016-01-01T00:00:00Z");
        hds.put(new HistoryNode(1, 1, true, user, 1, date, null));
        assertEquals("History for node 1", HistoryBrowserDialog.buildTitle(hds.getHistory(1, OsmPrimitiveType.NODE)));
        hds.put(new HistoryWay(1, 1, true, user, 1, date));
        assertEquals("History for way 1", HistoryBrowserDialog.buildTitle(hds.getHistory(1, OsmPrimitiveType.WAY)));
        hds.put(new HistoryRelation(1, 1, true, user, 1, date));
        assertEquals("History for relation 1", HistoryBrowserDialog.buildTitle(hds.getHistory(1, OsmPrimitiveType.RELATION)));
    }
}
