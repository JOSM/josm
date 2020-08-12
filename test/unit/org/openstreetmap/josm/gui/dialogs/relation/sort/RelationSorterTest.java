// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationSorter} class.
 */
public class RelationSorterTest {

    private final RelationSorter sorter = new RelationSorter();
    private DataSet testDataset;

    /**
     * Use Mercator projection
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    /**
     * Load the test data set
     * @throws IllegalDataException if an error was found while parsing the data
     * @throws IOException in case of I/O error
     */
    @Before
    public void loadData() throws IllegalDataException, IOException {
        if (testDataset == null) {
            try (InputStream fis = Files.newInputStream(Paths.get("nodist/data/relation_sort.osm"))) {
                testDataset = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            }
        }
    }

    private Relation getRelation(String testType) {
        return testDataset.getRelations().stream().filter(r -> testType.equals(r.get("test"))).findFirst().orElse(null);
    }

    private String[] getNames(List<RelationMember> members) {
        return members.stream().map(member -> member.getMember().get("name")).toArray(String[]::new);
    }

    // This cluster of tests checks whether these relations are sorted
    // as expected, so these relations are of course not sorted in the
    // data file.

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

    // The following cluster of tests does the same, but with various
    // configurations involving split / dual carriageway routes (i.e.
    // containing members with role=forward or role=backward). Again,
    // these are intentionally not already sorted.

    @Test
    public void testThreeLoopsEndsLoop() {
        Relation relation = getRelation("three-loops-ends-loop");
        // Check the first way before sorting, otherwise the sorter
        // might pick a different loop starting point than expected below
        Assert.assertEquals("t5w1", relation.getMembers().get(0).getMember().get("name"));

        String[] actual = getNames(sorter.sortMembers(relation.getMembers()));
        Assert.assertArrayEquals(new String[]{
            "t5w1", "t5w2a", "t5w3a", "t5w4a", "t5w2b", "t5w3b", "t5w4b",
            "t5w5", "t5w6a", "t5w7a", "t5w8a", "t5w6b", "t5w7b", "t5w8b",
            "t5w9a", "t5w10a", "t5w11a", "t5w9b", "t5w10b", "t5w11b",
            "t5w12", "t5w13",
        }, actual);
    }

    @Test
    public void testThreeLoopsEndsWay() {
        Relation relation = getRelation("three-loops-ends-way");
        // Check the first way before sorting, otherwise the sorter
        // might sort in reverse compared to what is expected below
        Assert.assertEquals("t5w1", relation.getMembers().get(0).getMember().get("name"));

        String[] actual = getNames(sorter.sortMembers(relation.getMembers()));
        Assert.assertArrayEquals(new String[]{
            "t5w1", "t5w2a", "t5w3a", "t5w4a", "t5w2b", "t5w3b", "t5w4b",
            "t5w5", "t5w6a", "t5w7a", "t5w8a", "t5w6b", "t5w7b", "t5w8b",
            "t5w9a", "t5w10a", "t5w11a", "t5w9b", "t5w10b", "t5w11b",
            "t5w12",
        }, actual);
    }

    @Test
    public void testThreeLoopsEndsNode() {
        Relation relation = getRelation("three-loops-ends-node");
        String[] actual = getNames(sorter.sortMembers(relation.getMembers()));
        Assert.assertArrayEquals(new String[]{
            "t5w4a", "t5w3a", "t5w2a", "t5w2b", "t5w3b", "t5w4b",
            "t5w5", "t5w6a", "t5w7a", "t5w8a", "t5w6b", "t5w7b", "t5w8b",
            "t5w9a", "t5w10a", "t5w11a", "t5w11b", "t5w10b", "t5w9b",
        }, actual);
    }

    @Test
    public void testOneLoopEndsSplit() {
        Relation relation = getRelation("one-loop-ends-split");
        String[] actual = getNames(sorter.sortMembers(relation.getMembers()));
        Assert.assertArrayEquals(new String[]{
            "t5w3a", "t5w4a", "t5w3b", "t5w4b",
            "t5w5", "t5w6a", "t5w7a", "t5w8a", "t5w6b", "t5w7b", "t5w8b",
            "t5w9a", "t5w10a", "t5w9b", "t5w10b",
        }, actual);
    }

    @Test
    public void testNoLoopEndsSplit() {
        Relation relation = getRelation("no-loop-ends-split");
        // TODO: This is not yet sorted properly, so this route is
        // presorted in the data file, making this a bit of a dummy test
        // for now.
        String[] actual = getNames(relation.getMembers());
        Assert.assertArrayEquals(new String[]{
            "t5w7a", "t5w8a", "t5w7b", "t5w8b",
            "t5w9a", "t5w10a", "t5w9b", "t5w10b",
        }, actual);
    }

    @Test
    public void testIncompleteLoops() {
        Relation relation = getRelation("incomplete-loops");
        // TODO: This is not yet sorted perfectly (might not be possible)
        String[] actual = getNames(sorter.sortMembers(relation.getMembers()));
        Assert.assertArrayEquals(new String[]{
            "t5w1", "t5w2a", "t5w3a", "t5w4a", "t5w2b", "t5w3b",
            "t5w5", "t5w6a", "t5w7a", "t5w8a", "t5w9a", "t5w10a", "t5w11a", "t5w6b", "t5w7b",
            "t5w12", "t5w11b", "t5w10b", "t5w9b",
        }, actual);
    }

    @Test
    public void testParallelOneWay() {
        Relation relation = getRelation("parallel-oneway");
        // TODO: This is not always sorted properly, only when the right
        // way is already at the top, so check that
        Assert.assertEquals("t6w1a", relation.getMembers().get(0).getMember().get("name"));

        String[] actual = getNames(sorter.sortMembers(relation.getMembers()));
        Assert.assertArrayEquals(new String[]{
            "t6w1a", "t6w2a", "t6w3a",
            "t6w1b", "t6w2b", "t6w3b",
        }, actual);
    }
}
