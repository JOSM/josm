// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.data.osm.OsmUtils.createPrimitive;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationChecker} class.
 */
public class RelationCheckerTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rule = new JOSMTestRules().presets();

    private static RelationChecker getRelationChecker() {
        RelationChecker checker = new RelationChecker();
        checker.initialize();
        return checker;
    }

    private static List<TestError> testRelation(Relation r) {
        RelationChecker checker = getRelationChecker();
        checker.visit(r);
        return checker.getErrors();
    }

    private static Relation createRelation(String tags) {
        return (Relation) createPrimitive("relation "+ tags);
    }

    @Test
    public void testUnknownType() {
        Relation r = createRelation("type=foobar");
        r.addMember(new RelationMember("", new Way()));
        List<TestError> errors = testRelation(r);

        assertTrue(errors.size() >= 1);
        assertEquals("Relation type is unknown", errors.get(0).getMessage());
    }

    @Test
    public void testEmpty() {
        List<TestError> errors = testRelation(createRelation("type=multipolygon"));
        assertEquals(1, errors.size());
        assertEquals("Relation is empty", errors.get(0).getMessage());
    }

    @Test
    public void testNormal() {
        Relation r = createRelation("type=multipolygon");
        r.addMember(new RelationMember("outer", new Way()));
        r.addMember(new RelationMember("inner", new Way()));
        assertTrue(testRelation(r).isEmpty());
    }

    @Test
    public void testOuter2() {
        Relation r = createRelation("type=multipolygon");
        r.addMember(new RelationMember("outer", new Way()));
        r.addMember(new RelationMember("outer2", new Way()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Role 'outer2' is not among expected values 'outer/inner'", errors.get(0).getDescription());
    }

    @Test
    public void testRestrictionViaMissing() {
        Relation r = createRelation("type=restriction");
        r.addMember(new RelationMember("from", new Way()));
        r.addMember(new RelationMember("to", new Way()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Role 'via' missing", errors.get(0).getDescription());
    }

    @Test
    public void testRestrictionViaRelation() {
        Relation r = createRelation("type=restriction");
        r.addMember(new RelationMember("from", new Way()));
        r.addMember(new RelationMember("to", new Way()));
        r.addMember(new RelationMember("via", new Relation()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Type 'relation' of relation member with role 'via' does not match accepted types 'node/way' in preset Turn Restriction",
                errors.get(0).getDescription());
    }

    @Test
    public void testRestrictionTwoFrom() {
        Relation r = createRelation("type=restriction");
        r.addMember(new RelationMember("from", new Way()));
        r.addMember(new RelationMember("from", new Way()));
        r.addMember(new RelationMember("to", new Way()));
        r.addMember(new RelationMember("via", new Way()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Number of 'from' roles too high (2)", errors.get(0).getDescription());
    }

    @Test
    public void testRestrictionEmpty() {
        Relation r = createRelation("type=restriction");
        r.addMember(new RelationMember("from", new Way()));
        r.addMember(new RelationMember("to", new Way()));
        r.addMember(new RelationMember("via", new Way()));
        r.addMember(new RelationMember("", new Way()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getDescription().startsWith("Empty role found when expecting one of"));
    }

    @Test
    public void testPowerMemberExpression() {
        Relation r = createRelation("type=route route=power");
        r.addMember(new RelationMember("", new Way()));

        List<TestError> errors = testRelation(r);
        assertEquals(2, errors.size());
        assertEquals("Role 'line' missing", errors.get(0).getDescription());
        assertEquals("Empty role found when expecting one of 'line/substation'", errors.get(1).getDescription());
    }

    @Test
    public void testBuildingMemberExpression() {
        Relation r = createRelation("type=building");
        r.addMember(new RelationMember("outline", new Way()));
        r.addMember(new RelationMember("part", new Way()));
        r.addMember(new RelationMember("level_-12", new Relation()));
        r.addMember(new RelationMember("level_0", new Relation()));
        r.addMember(new RelationMember("level_12", new Relation()));
        r.addMember(new RelationMember("level_x", new Relation())); // fails

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Role 'level_x' is not among expected values 'outline/part/ridge/edge/entrance/level_-?\\d+'",
                errors.get(0).getDescription());
    }

    @Test
    public void testHikingRouteMembers() {
        Relation r = createRelation("type=route route=hiking");
        r.addMember(new RelationMember("", OsmUtils.createPrimitive("way highway=path")));
        r.addMember(new RelationMember("route", OsmUtils.createPrimitive("way highway=path"))); // fails
        r.addMember(new RelationMember("guidepost", new Node())); // fails

        List<TestError> errors = testRelation(r);
        assertEquals(2, errors.size());
        assertEquals("Role of relation member does not match template expression 'information=guidepost' in preset Hiking Route",
                errors.get(0).getDescription());
        assertEquals("Role 'route' is not among expected values '<empty>/guidepost'", errors.get(1).getDescription());
    }

    @Test
    public void testRouteMemberExpression() {
        Relation r = createRelation("type=route route=tram public_transport:version=2");
        r.addMember(new RelationMember("", createPrimitive("way railway=tram")));
        r.addMember(new RelationMember("stop", createPrimitive("node public_transport=stop_position tram=yes")));
        r.addMember(new RelationMember("platform", createPrimitive("node public_transport=platform tram=yes")));
        assertTrue(testRelation(r).isEmpty());

        r.addMember(new RelationMember("", createPrimitive("way no-rail-way=yes")));
        assertEquals(1, testRelation(r).size());
        assertEquals("Role of relation member does not match template expression 'railway' in preset Public Transport Route (Rail)",
                testRelation(r).get(0).getDescription());

        r.removeMember(3);
        r.addMember(new RelationMember("stop", createPrimitive("way no-rail-way=yes")));
        assertEquals(1, testRelation(r).size());
        assertEquals(
                "Type 'way' of relation member with role 'stop' does not match accepted types 'node' in preset Public Transport Route (Rail)",
                testRelation(r).get(0).getDescription());

        r.removeMember(3);
        r.addMember(new RelationMember("stop", createPrimitive("node public_transport=stop_position bus=yes")));
        assertEquals(1, testRelation(r).size());
        assertEquals("Role of relation member does not match template expression 'public_transport=stop_position && "+
                "(train=yes || subway=yes || monorail=yes || tram=yes || light_rail=yes)' in preset Public Transport Route (Rail)",
                testRelation(r).get(0).getDescription());
    }
}
