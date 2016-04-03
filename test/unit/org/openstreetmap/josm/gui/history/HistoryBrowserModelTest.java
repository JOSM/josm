// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.gui.history.HistoryBrowserModel.TagTableModel;

/**
 * Unit tests of {@link HistoryBrowserModel} class.
 */
public class HistoryBrowserModelTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Test for {@link HistoryBrowserModel#HistoryBrowserModel}.
     */
    @Test
    public void testHistoryBrowserModel() {
        HistoryBrowserModel model = new HistoryBrowserModel();
        assertNotNull(model.getVersionTableModel());
        assertNull(model.getHistory());
        Node n = new Node(1, 1);
        n.setUser(User.getAnonymous());
        n.setChangesetId(1);
        HistoryDataSet.getInstance().put(new HistoryNode(n));
        History history = HistoryDataSet.getInstance().getHistory(1, OsmPrimitiveType.NODE);
        assertNotNull(history);
        model.setHistory(history);
        assertEquals(history, model.getHistory());
        model = new HistoryBrowserModel(history);
        assertEquals(history, model.getHistory());
    }

    /**
     * Unit test of {@link HistoryBrowserModel#getTagTableModel}.
     */
    @Test
    public void testGetTagTableModel() {
        HistoryBrowserModel model = new HistoryBrowserModel();
        TagTableModel t1 = model.getTagTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        TagTableModel t2 = model.getTagTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotEquals(t1, t2);
    }

    /**
     * Unit test of {@link HistoryBrowserModel#getNodeListTableModel}.
     */
    @Test
    public void testGetNodeListTableModel() {
        HistoryBrowserModel model = new HistoryBrowserModel();
        DiffTableModel t1 = model.getNodeListTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        DiffTableModel t2 = model.getNodeListTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotEquals(t1, t2);
    }

    /**
     * Unit test of {@link HistoryBrowserModel#getRelationMemberTableModel}.
     */
    @Test
    public void testGetRelationMemberTableModel() {
        HistoryBrowserModel model = new HistoryBrowserModel();
        DiffTableModel t1 = model.getRelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        DiffTableModel t2 = model.getRelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotEquals(t1, t2);
    }
}
