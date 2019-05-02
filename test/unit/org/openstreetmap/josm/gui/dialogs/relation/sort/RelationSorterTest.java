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
            try (InputStream fis = Files.newInputStream(Paths.get("data_nodist/relation_sort.osm"))) {
                testDataset = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            }
        }
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
        for (int i = 0; i < result.length; i++) {
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
