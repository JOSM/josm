// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link HistoryBrowserDialog} class.
 */
class HistoryBrowserDialogTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test for {@link HistoryBrowserDialog#buildTitle}.
     */
    @Test
    void testBuildTitle() {
        HistoryDataSet hds = new HistoryDataSet();
        User user = User.createOsmUser(1, "");
        Date date = DateUtils.fromString("2016-01-01");
        hds.put(new HistoryNode(1, 1, true, user, 1, date, null));
        assertEquals("History for node 1", HistoryBrowserDialog.buildTitle(hds.getHistory(1, OsmPrimitiveType.NODE)));
        hds.put(new HistoryWay(1, 1, true, user, 1, date));
        assertEquals("History for way 1", HistoryBrowserDialog.buildTitle(hds.getHistory(1, OsmPrimitiveType.WAY)));
        hds.put(new HistoryRelation(1, 1, true, user, 1, date));
        assertEquals("History for relation 1", HistoryBrowserDialog.buildTitle(hds.getHistory(1, OsmPrimitiveType.RELATION)));
    }
}
