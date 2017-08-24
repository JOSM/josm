// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link SelectByInternalPointAction}.
 */
public final class SelectByInternalPointActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().preferences().projection().main();

    /**
     * Tests that {@code SelectByInternalPointAction} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(SelectByInternalPointAction.class);
    }

    /**
     * Unit test - no dataset.
     */
    @Test
    public void testNoDataSet() {
        assertNull(MainApplication.getLayerManager().getEditDataSet());
        assertEquals(0, SelectByInternalPointAction.getSurroundingObjects(null).size());
        assertNull(SelectByInternalPointAction.getSmallestSurroundingObject(null));
        SelectByInternalPointAction.performSelection(null, false, false);
    }

    private static Layer initDataSet() {
        DataSet ds = new DataSet();
        Node n1 = new Node(new EastNorth(1, 1));
        Node n2 = new Node(new EastNorth(1, 2));
        Node n3 = new Node(new EastNorth(2, 2));
        Node n4 = new Node(new EastNorth(2, 1));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(n4);
        Way w = new Way();
        w.addNode(n1);
        w.addNode(n2);
        w.addNode(n3);
        w.addNode(n4);
        w.addNode(n1);
        assertTrue(w.isClosed());
        ds.addPrimitive(w);
        Relation r = new Relation();
        r.addMember(new RelationMember("outer", w));
        ds.addPrimitive(r);
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        return layer;
    }

    /**
     * Unit test of {@link SelectByInternalPointAction#getSurroundingObjects} method.
     */
    @Test
    public void testGetSurroundingObjects() {
        initDataSet();
        assertEquals(0, SelectByInternalPointAction.getSurroundingObjects(null).size());
        assertEquals(0, SelectByInternalPointAction.getSurroundingObjects(new EastNorth(0, 0)).size());
        assertEquals(1, SelectByInternalPointAction.getSurroundingObjects(new EastNorth(1.5, 1.5)).size());
        assertEquals(0, SelectByInternalPointAction.getSurroundingObjects(new EastNorth(3, 3)).size());
    }

    /**
     * Unit test of {@link SelectByInternalPointAction#getSmallestSurroundingObject} method.
     */
    @Test
    public void testGetSmallestSurroundingObject() {
        initDataSet();
        assertNull(SelectByInternalPointAction.getSmallestSurroundingObject(null));
        assertNotNull(SelectByInternalPointAction.getSmallestSurroundingObject(new EastNorth(1.5, 1.5)));
    }

    /**
     * Unit test of {@link SelectByInternalPointAction#performSelection} method.
     */
    @Test
    public void testPerformSelection() {
        initDataSet();
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();

        assertEquals(0, ds.getSelected().size());
        SelectByInternalPointAction.performSelection(null, false, false);
        assertEquals(0, ds.getSelected().size());
        SelectByInternalPointAction.performSelection(new EastNorth(0, 0), false, false);
        assertEquals(0, ds.getSelected().size());
        SelectByInternalPointAction.performSelection(new EastNorth(1.5, 1.5), false, false);
        assertEquals(1, ds.getSelected().size());
        ds.clearSelection();
        ds.addSelected(ds.getNodes());
        assertEquals(4, ds.getSelected().size());
        SelectByInternalPointAction.performSelection(new EastNorth(1.5, 1.5), true, false);
        assertEquals(5, ds.getSelected().size());
        SelectByInternalPointAction.performSelection(new EastNorth(1.5, 1.5), false, true);
        assertEquals(4, ds.getSelected().size());
    }
}
