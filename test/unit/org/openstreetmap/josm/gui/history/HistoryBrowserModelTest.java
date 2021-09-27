// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;
import org.openstreetmap.josm.testutils.annotations.Users;

/**
 * Unit tests of {@link HistoryBrowserModel} class.
 */
@BasicPreferences
@OsmApiType(OsmApiType.APIType.DEV)
@Timeout(30)
@Users
class HistoryBrowserModelTest {
    /**
     * Test for {@link HistoryBrowserModel#HistoryBrowserModel}.
     */
    @Test
    void testHistoryBrowserModel() {
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
    void testGetTagTableModel() {
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
    void testGetNodeListTableModel() {
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
    void testGetRelationMemberTableModel() {
        HistoryBrowserModel model = new HistoryBrowserModel();
        DiffTableModel t1 = model.getRelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        DiffTableModel t2 = model.getRelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotEquals(t1, t2);
    }

    /**
     * Unit test of {@link HistoryBrowserModel#setCurrentPointInTime} and {@link HistoryBrowserModel#setReferencePointInTime} - null history.
     */
    @Test
    void testSetPointsInTimeNullHistory() {
        HistoryBrowserModel model = new HistoryBrowserModel();
        VersionTableModel tableModel = model.getVersionTableModel();
        tableModel.setValueAt(false, 0, 0); // code coverage
        tableModel.setValueAt(true, 0, 1);  // reference point
        tableModel.setValueAt(true, 1, 2);  // current point
        tableModel.setValueAt(true, 3, 3);  // code coverage
    }

    /**
     * Unit test of {@link HistoryBrowserModel#setCurrentPointInTime} and {@link HistoryBrowserModel#setReferencePointInTime} - node history.
     */
    @Test
    void testSetPointsInTimeNodeHistory() {
        SimplePrimitiveId id = new SimplePrimitiveId(2, OsmPrimitiveType.NODE);
        new HistoryLoadTask().add(id).run();
        History history = HistoryDataSet.getInstance().getHistory(id);
        assertTrue(history.getNumVersions() >= 4);
        HistoryBrowserModel model = new HistoryBrowserModel(history);
        VersionTableModel tableModel = model.getVersionTableModel();
        tableModel.setValueAt(false, 0, 0); // code coverage
        tableModel.setValueAt(true, 0, 1);  // reference point
        tableModel.setValueAt(true, 3, 2);  // current point
        tableModel.setValueAt(true, 3, 3);  // code coverage
        // nodes only for ways
        assertEquals(0, model.getNodeListTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME).getRowCount());
        assertEquals(0, model.getNodeListTableModel(PointInTimeType.CURRENT_POINT_IN_TIME).getRowCount());
        // members only for relations
        assertEquals(0, model.getRelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME).getRowCount());
        assertEquals(0, model.getRelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME).getRowCount());
    }

    /**
     * Unit test of {@link HistoryBrowserModel#setCurrentPointInTime} and {@link HistoryBrowserModel#setReferencePointInTime} - way history.
     */
    @Test
    void testSetPointsInTimeWayHistory() {
        SimplePrimitiveId id = new SimplePrimitiveId(2, OsmPrimitiveType.WAY);
        new HistoryLoadTask().add(id).run();
        History history = HistoryDataSet.getInstance().getHistory(id);
        assertTrue(history.getNumVersions() >= 2);
        HistoryBrowserModel model = new HistoryBrowserModel(history);
        VersionTableModel tableModel = model.getVersionTableModel();
        tableModel.setValueAt(false, 0, 0); // code coverage
        tableModel.setValueAt(true, 0, 1);  // reference point
        tableModel.setValueAt(true, 3, 2);  // current point
        tableModel.setValueAt(true, 3, 3);  // code coverage
        // nodes only for ways
        assertEquals(2, model.getNodeListTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME).getRowCount());
        assertEquals(2, model.getNodeListTableModel(PointInTimeType.CURRENT_POINT_IN_TIME).getRowCount());
        // members only for relations
        assertEquals(0, model.getRelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME).getRowCount());
        assertEquals(0, model.getRelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME).getRowCount());
    }

    /**
     * Unit test of {@link HistoryBrowserModel#setCurrentPointInTime} and {@link HistoryBrowserModel#setReferencePointInTime} - relation history.
     */
    @Test
    void testSetPointsInTimeRelationHistory() {
        SimplePrimitiveId id = new SimplePrimitiveId(2, OsmPrimitiveType.RELATION);
        new HistoryLoadTask().add(id).run();
        History history = HistoryDataSet.getInstance().getHistory(id);
        assertTrue(history.getNumVersions() >= 2);
        HistoryBrowserModel model = new HistoryBrowserModel(history);
        VersionTableModel tableModel = model.getVersionTableModel();
        tableModel.setValueAt(false, 0, 0); // code coverage
        tableModel.setValueAt(true, 0, 1);  // reference point
        tableModel.setValueAt(true, 3, 2);  // current point
        tableModel.setValueAt(true, 3, 3);  // code coverage
        // nodes only for ways
        assertEquals(0, model.getNodeListTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME).getRowCount());
        assertEquals(0, model.getNodeListTableModel(PointInTimeType.CURRENT_POINT_IN_TIME).getRowCount());
        // members only for relations
        assertEquals(1, model.getRelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME).getRowCount());
        assertEquals(1, model.getRelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME).getRowCount());
        assertEquals(new Color(0xff0000), model.getVersionColor(0));
        assertEquals(new Color(0xff00e5), model.getVersionColor(1));
        assertNull(model.getVersionColor(2));
    }
}
