// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OsmDataLayer} class.
 */
public class OsmDataLayerTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().projection().main();

    private DataSet ds;
    private OsmDataLayer layer;

    /**
     * Setup tests
     */
    @Before
    public void setUp() {
        ds = new DataSet();
        layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
    }

    /**
     * Unit test of {@link OsmDataLayer#setRecentRelation} and {@link OsmDataLayer#getRecentRelations}.
     */
    @Test
    public void testRecentRelation() {
        int n = OsmDataLayer.PROPERTY_RECENT_RELATIONS_NUMBER.get();
        assertTrue(n > 0);
        for (int i = 0; i < 2*n; i++) {
            Relation r = new Relation(i, 1);
            ds.addPrimitive(r);
            layer.setRecentRelation(r);
        }
        assertEquals(n, layer.getRecentRelations().size());
        for (OsmPrimitive r : ds.allPrimitives()) {
            if (r instanceof Relation) {
                layer.removeRecentRelation((Relation) r);
            }
        }
        assertTrue(layer.getRecentRelations().isEmpty());
    }

    /**
     * Unit test of {@link OsmDataLayer#getInfoComponent}.
     */
    @Test
    public void testGetInfoComponent() {
        assertNotNull(layer.getInfoComponent());

        layer.setUploadDiscouraged(true);

        fillDataSet(ds);

        assertNotNull(layer.getInfoComponent());
    }

    private void fillDataSet(DataSet ds) {
        Node n = new Node(1, 2);
        n.setCoor(LatLon.ZERO);
        n.setDeleted(true);
        n.setVisible(false);
        ds.addPrimitive(n);
        n = new Node(2, 2);
        n.setCoor(LatLon.ZERO);
        ds.addPrimitive(n);

        Way w = new Way(1, 2);
        w.setDeleted(true);
        w.setVisible(false);
        ds.addPrimitive(w);
        ds.addPrimitive(new Way(2, 2));

        Relation r = new Relation(1, 2);
        r.setDeleted(true);
        r.setVisible(false);
        ds.addPrimitive(r);
        ds.addPrimitive(new Relation(2, 2));
    }

    /**
     * Unit test of {@link OsmDataLayer#addLayerStateChangeListener}.
     */
    @Test
    public void testLayerStateChangeListenerNull() {
        layer.addLayerStateChangeListener(null);
    }

    /**
     * Unit test of {@link OsmDataLayer#getIcon}.
     */
    @Test
    public void testGetIcon() {
        assertNotNull(layer.getIcon());
        layer.setUploadDiscouraged(true);
        assertNotNull(layer.getIcon());
    }

    /**
     * Unit test of {@link OsmDataLayer#paint}.
     */
    @Test
    public void testPaint() {
        fillDataSet(ds);
        assertNotNull(MainApplication.getMap());
        layer.paint(TestUtils.newGraphics(), MainApplication.getMap().mapView, new Bounds(LatLon.ZERO));
    }

    /**
     * Unit test of {@link OsmDataLayer#getToolTipText}.
     */
    @Test
    public void testGetToolTipText() {
        assertEquals("<html>0 nodes<br>0 ways<br>0 relations</html>", new OsmDataLayer(ds, "", null).getToolTipText());
        fillDataSet(ds);
        assertEquals("<html>1 node<br>1 way<br>1 relation</html>", new OsmDataLayer(ds, "", null).getToolTipText());
        assertEquals("<html>1 node<br>1 way<br>1 relation<br>data.osm</html>", new OsmDataLayer(ds, "", new File("data.osm")).getToolTipText());
    }

    /**
     * Unit test of {@link OsmDataLayer#mergeFrom}.
     */
    @Test
    public void testMergeFrom() {
        fillDataSet(ds);
        OsmDataLayer layer2 = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer2);
        assertTrue(layer2.data.allPrimitives().isEmpty());
        assertTrue(layer2.isMergable(layer));
        layer2.mergeFrom(layer);
        assertEquals(6, layer2.data.allPrimitives().size());
        layer.setUploadDiscouraged(true);
        layer2.mergeFrom(layer);
        assertTrue(layer2.isUploadDiscouraged());
    }

    /**
     * Unit test of {@link OsmDataLayer#cleanupAfterUpload}.
     */
    @Test
    public void testCleanupAfterUpload() {
        fillDataSet(ds);
        assertEquals(6, layer.data.allPrimitives().size());
        layer.cleanupAfterUpload(ds.allPrimitives());
        assertEquals(3, layer.data.allPrimitives().size());
    }

    /**
     * Unit test of {@link OsmDataLayer#getMenuEntries}.
     */
    @Test
    public void testGetMenuEntries() {
        ExpertToggleAction.getInstance().setExpert(true);
        assertEquals(16, layer.getMenuEntries().length);

        ExpertToggleAction.getInstance().setExpert(false);
        assertEquals(13, layer.getMenuEntries().length);
    }

    /**
     * Unit test of {@link OsmDataLayer#toGpxData}.
     */
    @Test
    public void testToGpxData() {
        fillDataSet(ds);
        assertNotNull(layer.toGpxData());
    }

    /**
     * Unit test of {@link OsmDataLayer#containsPoint}.
     */
    @Test
    public void testContainsPoint() {
        fillDataSet(ds);
        assertTrue(layer.containsPoint(LatLon.ZERO));
    }

    /**
     * Unit test of {@link OsmDataLayer#isModified}.
     */
    @Test
    public void testIsModified() {
        assertFalse(layer.isModified());
        fillDataSet(ds);
        assertTrue(layer.isModified());
    }

    /**
     * Unit test of {@link OsmDataLayer#projectionChanged}.
     */
    @Test
    public void testProjectionChanged() {
        layer.projectionChanged(null, null);
    }

    /**
     * Unit test of {@link OsmDataLayer#checkSaveConditions}.
     */
    @Test
    public void testCheckSaveConditions() {
        assertFalse(layer.checkSaveConditions());
        fillDataSet(ds);
        assertTrue(layer.checkSaveConditions());
    }

    @Test
    public void testLayerNameIncreases() throws Exception {
        final OsmDataLayer layer1 = new OsmDataLayer(new DataSet(), OsmDataLayer.createLayerName(147), null);
        final OsmDataLayer layer2 = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
        assertEquals("Data Layer 147", layer1.getName());
        assertEquals("Data Layer 148", layer2.getName());
    }

    @Test
    public void testLayerUnnumberedName() {
        final OsmDataLayer layer = new OsmDataLayer(new DataSet(), "Data Layer ", null);
        assertEquals("Data Layer ", layer.getName());
    }

    /**
     * Non-regression test for ticket #13985
     */
    @Test
    public void testLayerNameDoesFinish() {
        final OsmDataLayer layer = new OsmDataLayer(new DataSet(), "Data Layer from GeoJSON: foo.geojson", null);
        assertEquals("Data Layer from GeoJSON: foo.geojson", layer.getName());
    }
}
