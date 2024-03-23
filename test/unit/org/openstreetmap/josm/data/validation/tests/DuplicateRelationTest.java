// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * JUnit Test of "Duplicate relation" validation test.
 */
@BasicPreferences
class DuplicateRelationTest {
    static class ExpectedResult {
        final int code;
        final boolean fixable;
        ExpectedResult(int code, boolean fixable) {
            this.code = code;
            this.fixable = fixable;
        }
    }

    private void doTest(String tags1, String tags2, ExpectedResult... expectations) {
        performTest(buildDataSet(tags1, tags2), expectations);
    }

    private void performTest(DataSet ds, ExpectedResult... expectations) {
        final DuplicateRelation TEST = new DuplicateRelation();
        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(expectations.length, TEST.getErrors().size());
        int i = 0;
        for (TestError error : TEST.getErrors()) {
            ExpectedResult expected = expectations[i++];
            assertEquals(expected.code, error.getCode());
            assertEquals(expected.fixable, error.isFixable());
            if (error.isFixable())
                error.getFix();
        }
    }

    private static DataSet buildDataSet(String tags1, String tags2) {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        ds.addPrimitive(a);
        ds.addPrimitive(TestUtils.newRelation(tags1, new RelationMember(null, a)));
        ds.addPrimitive(TestUtils.newRelation(tags2, new RelationMember(null, a)));
        return ds;
    }

    /**
     * Test of "Duplicate relation" validation test - no tags.
     */
    @Test
    void testDuplicateRelationNoTags() {
        doTest("", "",
                new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true)
                );
    }

    /**
     * Test of "Duplicate relation" validation test - same tags.
     */
    @Test
    void testDuplicateRelationSameTags() {
        doTest("type=boundary", "type=boundary",
                new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true)
                );
    }

    /**
     * Test of "Duplicate relation" validation test - different tags.
     */
    @Test
    void testDuplicateRelationDifferentTags() {
        doTest("type=boundary", "type=multipolygon",
                new ExpectedResult(DuplicateRelation.IDENTICAL_MEMBERLIST, false));
    }

    /**
     * Test of duplicate "tmc" relation, should not be ignored
     */
    @Test
    void testTMCRelation1() {
        doTest("type=tmc t1=v1", "type=tmc t1=v1",
                new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true));
    }

    /**
     * Test of "tmc" relation with equal members but different tags, should be ignored
     */
    @Test
    void testTMCRelation2() {
        doTest("type=tmc t1=v1", "type=tmc t1=v2");
    }

    /**
     * Test with incomplete members
     */
    @Test
    void testIncomplete() {
        DataSet ds = new DataSet();

        Node a = new Node(1234);
        ds.addPrimitive(a);
        ds.addPrimitive(TestUtils.newRelation("type=multipolygon", new RelationMember(null, a)));
        ds.addPrimitive(TestUtils.newRelation("type=multipolygon", new RelationMember(null, a)));
        performTest(ds, new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true));
    }

    /**
     * Test with different order of members, order doesn't count
     */
    @Test
    void testMemberOrder1() {
        DataSet ds = new DataSet();

        Node a = new Node(1);
        Node b = new Node(2);
        ds.addPrimitive(a);
        ds.addPrimitive(b);
        ds.addPrimitive(TestUtils.newRelation("type=multipolygon", new RelationMember(null, a), new RelationMember(null, b)));
        ds.addPrimitive(TestUtils.newRelation("type=multipolygon", new RelationMember(null, b), new RelationMember(null, a)));
        performTest(ds, new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true));
    }

    /**
     * Test with different order of members, order counts
     */
    @Test
    void testMemberOrder2() {
        DataSet ds = new DataSet();

        Node a = new Node(1);
        a.setCoor(new LatLon(10.0, 5.0));
        Node b = new Node(2);
        b.setCoor(new LatLon(10.0, 6.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);
        ds.addPrimitive(TestUtils.newRelation("type=route", new RelationMember(null, a), new RelationMember(null, b)));
        ds.addPrimitive(TestUtils.newRelation("type=route", new RelationMember(null, b), new RelationMember(null, a)));
        performTest(ds, new ExpectedResult(DuplicateRelation.SAME_RELATION, false));
    }

    /**
     * Test with different order of members, one is duplicated, order doesn't matter
     */
    @Test
    void testMemberOrder3() {
        DataSet ds = new DataSet();

        Node a = new Node(1);
        Node b = new Node(2);
        ds.addPrimitive(a);
        ds.addPrimitive(b);
        ds.addPrimitive(TestUtils.newRelation("type=restriction", new RelationMember(null, a),
                new RelationMember(null, b), new RelationMember(null, a)));
        ds.addPrimitive(TestUtils.newRelation("type=restriction", new RelationMember(null, b),
                new RelationMember(null, a), new RelationMember(null, a)));
        performTest(ds, new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true));
    }

    /**
     * Test with different order of members, one is duplicated in one of the relations
     */
    @Test
    void testMemberOrder4() {
        DataSet ds = new DataSet();
        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 6.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);
        ds.addPrimitive(TestUtils.newRelation("", new RelationMember(null, a), new RelationMember(null, b)));
        ds.addPrimitive(TestUtils.newRelation("", new RelationMember(null, b), new RelationMember(null, a), new RelationMember(null, b)));
        performTest(ds);
    }

    /**
     * Test with two relations where members are different but geometry is equal.
     */
    @Test
    void testImport() {
        DataSet ds = new DataSet();
        Node a = new Node(1234, 1);
        a.setCoor(new LatLon(10.0, 5.0));

        Node b = new Node(new LatLon(10.0, 5.0));

        ds.addPrimitive(a);
        ds.addPrimitive(b);
        ds.addPrimitive(TestUtils.newRelation("type=multipolygon", new RelationMember(null, a)));
        ds.addPrimitive(TestUtils.newRelation("type=multipolygon", new RelationMember(null, b)));
        performTest(ds, new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true));
    }
}
