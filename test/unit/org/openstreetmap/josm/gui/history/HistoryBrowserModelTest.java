// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link HistoryBrowserModel} class.
 */
public class HistoryBrowserModelTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().devAPI().timeout(20000);

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

    /**
     * Unit test of {@link HistoryBrowserModel#setCurrentPointInTime} and {@link HistoryBrowserModel#setReferencePointInTime} - null history.
     */
    @Test
    public void testSetPointsInTimeNullHistory() {
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
    public void testSetPointsInTimeNodeHistory() {
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
    public void testSetPointsInTimeWayHistory() {
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
    public void testSetPointsInTimeRelationHistory() {
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
    }
}
