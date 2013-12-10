// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.fest.reflect.core.Reflection;
import org.fest.reflect.reference.TypeRef;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;

public class QuadBucketsTest {

    @BeforeClass
    public static void init() {
        Main.initApplicationPreferences();
    }

    private void removeAllTest(DataSet ds) {
        List<Node> allNodes = new ArrayList<Node>(ds.getNodes());
        List<Way> allWays = new ArrayList<Way>(ds.getWays());
        List<Relation> allRelations = new ArrayList<Relation>(ds.getRelations());

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
        DataSet ds = OsmReader.parseDataSet(new FileInputStream("data_nodist/restriction.osm"), NullProgressMonitor.INSTANCE);
        removeAllTest(ds);
    }

    @Test
    public void testMove() throws Exception {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        DataSet ds = OsmReader.parseDataSet(new FileInputStream("data_nodist/restriction.osm"), NullProgressMonitor.INSTANCE);

        for (Node n: ds.getNodes()) {
            n.setCoor(new LatLon(10, 10));
        }

        removeAllTest(ds);
    }

}
