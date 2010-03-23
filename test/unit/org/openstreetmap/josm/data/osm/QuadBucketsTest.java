// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;

public class QuadBucketsTest {

    @Test
    public void testRemove() throws Exception {
        Main.proj = new Mercator();
        DataSet ds = OsmReader.parseDataSet(new FileInputStream("data_nodist/restriction.osm"), NullProgressMonitor.INSTANCE);
        List<Node> allNodes = new ArrayList<Node>(ds.getNodes());
        List<Way> allWays = new ArrayList<Way>(ds.getWays());
        List<Relation> allRelations = new ArrayList<Relation>(ds.getRelations());
        for (OsmPrimitive o: allNodes) {
            ds.removePrimitive(o);
        }
        for (OsmPrimitive o: allWays) {
            ds.removePrimitive(o);
        }
        for (OsmPrimitive o: allRelations) {
            ds.removePrimitive(o);
        }

        Assert.assertTrue(ds.getNodes().isEmpty());
        Assert.assertTrue(ds.getWays().isEmpty());
        Assert.assertTrue(ds.getRelations().isEmpty());
    }

}
