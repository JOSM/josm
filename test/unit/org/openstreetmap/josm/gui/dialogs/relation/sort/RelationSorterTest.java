// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

public class RelationSorterTest {

    private RelationSorter sorter = new RelationSorter();
    private static DataSet testDataset;

    @BeforeClass
    public static void loadData() throws FileNotFoundException, IllegalDataException {
        Main.initApplicationPreferences();
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        testDataset = OsmReader.parseDataSet(new FileInputStream("data_nodist/relation_sort.osm"), NullProgressMonitor.INSTANCE);
    }

    private Relation getRelation(String testType) {
        for (Relation r: testDataset.getRelations()) {
            if (testType.equals(r.get("test")))
                return r;
        }
        return null;
    }

    private String[] getNames(List<RelationMember> members) {
        String[] result = new String[members.size()];
        for (int i=0; i<result.length; i++) {
            result[i] = members.get(i).getMember().get("name");
        }
        return result;
    }

    @Test
    public void testGeneric() {
        String[] actual = getNames(sorter.sortMembers(getRelation("generic").getMembers()));
        final String[] expected = {"t1w4", "t1w3", "t1w2", "t1w1", "t1w7", "t1w6", "t1w5", "t1n1", "t1n2"};
        // expect nodes to be sorted correctly
        Assert.assertEquals(expected[7], actual[7]);
        Assert.assertEquals(expected[8], actual[8]);
    }

    @Test
    public void testAssociatedStreet() {
        String[] actual = getNames(sorter.sortMembers(getRelation("associatedStreet").getMembers()));
        Assert.assertArrayEquals(new String[] {"t2w1", "t2w2", "t2n1", "t2n2", "t2n3", "t2n4"}, actual);
    }

    @Test
    public void testStreet() {
        String[] actual = getNames(sorter.sortMembers(getRelation("street").getMembers()));
        Assert.assertArrayEquals(new String[]{"t2w1", "t2w2", "t2n1", "t2n2", "t2n3", "t2n4", "playground", "tree"}, actual);
    }

}
