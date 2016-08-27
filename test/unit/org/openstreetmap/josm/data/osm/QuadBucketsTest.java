// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.fest.reflect.core.Reflection;
import org.fest.reflect.reference.TypeRef;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Unit tests of {@link QuadBuckets}.
 */
public class QuadBucketsTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private void removeAllTest(DataSet ds) {
        List<Node> allNodes = new ArrayList<>(ds.getNodes());
        List<Way> allWays = new ArrayList<>(ds.getWays());
        List<Relation> allRelations = new ArrayList<>(ds.getRelations());

        QuadBuckets<Node> nodes = Reflection.field("nodes").ofType(new TypeRef<QuadBuckets<Node>>() {}).in(ds).get();
        QuadBuckets<Way> ways = Reflection.field("ways").ofType(new TypeRef<QuadBuckets<Way>>() {}).in(ds).get();
        Collection<Relation> relations = Reflection.field("relations").ofType(new TypeRef<Collection<Relation>>() {}).in(ds).get();

        int expectedCount = allNodes.size();
        for (OsmPrimitive o: allNodes) {
            ds.removePrimitive(o);
            checkIterator(nodes, --expectedCount);
        }
        expectedCount = allWays.size();
        for (OsmPrimitive o: allWays) {
            ds.removePrimitive(o);
            checkIterator(ways, --expectedCount);
        }
        for (OsmPrimitive o: allRelations) {
            ds.removePrimitive(o);
        }
        Assert.assertTrue(nodes.isEmpty());
        Assert.assertTrue(ways.isEmpty());
        Assert.assertTrue(relations.isEmpty());
    }

    private void checkIterator(Collection<? extends OsmPrimitive> col, int expectedCount) {
        int count = 0;
        Iterator<? extends OsmPrimitive> it = col.iterator();
        while (it.hasNext()) {
            count++;
            it.next();
        }
        Assert.assertEquals(expectedCount, count);
    }

    @Test
    public void testRemove() throws Exception {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        try (InputStream fis = new FileInputStream("data_nodist/restriction.osm")) {
            DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            removeAllTest(ds);
        }
    }

    @Test
    public void testMove() throws Exception {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        try (InputStream fis = new FileInputStream("data_nodist/restriction.osm")) {
            DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);

            for (Node n: ds.getNodes()) {
                n.setCoor(new LatLon(10, 10));
            }

            removeAllTest(ds);
        }
    }

    /**
     * Test handling of objects with invalid bbox
     */
    @Test
    public void testSpecialBBox() {
        QuadBuckets<Node> qbNodes = new QuadBuckets<>();
        QuadBuckets<Way> qbWays = new QuadBuckets<>();
        Way w1 = new Way(1);
        Way w2 = new Way(2);
        Way w3 = new Way(3);
        Node n1 = new Node(1);
        Node n2 = new Node(2); n2.setCoor(new LatLon(10, 20));
        Node n3 = new Node(3); n2.setCoor(new LatLon(20, 30));
        w2.setNodes(Arrays.asList(n1));
        w3.setNodes(Arrays.asList(n1, n2, n3));

        qbNodes.add(n1);
        qbNodes.add(n2);
        Assert.assertEquals(2, qbNodes.size());
        Assert.assertTrue(qbNodes.contains(n1));
        Assert.assertTrue(qbNodes.contains(n2));
        Assert.assertFalse(qbNodes.contains(n3));
        qbNodes.remove(n1);
        Assert.assertEquals(1, qbNodes.size());
        Assert.assertFalse(qbNodes.contains(n1));
        Assert.assertTrue(qbNodes.contains(n2));
        qbNodes.remove(n2);
        Assert.assertEquals(0, qbNodes.size());
        Assert.assertFalse(qbNodes.contains(n1));
        Assert.assertFalse(qbNodes.contains(n2));

        qbNodes.addAll(Arrays.asList(n1, n2, n3));
        qbNodes.removeAll(Arrays.asList(n1, n3));
        Assert.assertEquals(1, qbNodes.size());
        Assert.assertTrue(qbNodes.contains(n2));

        qbWays.add(w1);
        qbWays.add(w2);
        qbWays.add(w3);
        Assert.assertEquals(3, qbWays.size());
        Assert.assertTrue(qbWays.contains(w1));
        Assert.assertTrue(qbWays.contains(w2));
        Assert.assertTrue(qbWays.contains(w3));
        qbWays.remove(w1);
        Assert.assertEquals(2, qbWays.size());
        Assert.assertFalse(qbWays.contains(w1));
        Assert.assertTrue(qbWays.contains(w2));
        Assert.assertTrue(qbWays.contains(w3));
        qbWays.remove(w2);
        Assert.assertEquals(1, qbWays.size());
        Assert.assertFalse(qbWays.contains(w1));
        Assert.assertFalse(qbWays.contains(w2));
        Assert.assertTrue(qbWays.contains(w3));
        qbWays.remove(w3);
        Assert.assertEquals(0, qbWays.size());
        Assert.assertFalse(qbWays.contains(w1));
        Assert.assertFalse(qbWays.contains(w2));
        Assert.assertFalse(qbWays.contains(w3));

        qbWays.clear();
        Assert.assertEquals(0, qbWays.size());
        List<Way> allWays = new ArrayList<>(Arrays.asList(w1, w2, w3));
        qbWays.addAll(allWays);
        Assert.assertEquals(3, qbWays.size());
        int count = 0;
        for (Way w : qbWays) {
            Assert.assertTrue(allWays.contains(w));
            count++;
        }
        Assert.assertEquals(3, count);
        // test remove with iterator
        Iterator<Way> iter = qbWays.iterator();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
            count--;
            Assert.assertEquals(count, qbWays.size());
        }
        Assert.assertEquals(0, qbWays.size());
    }
}
