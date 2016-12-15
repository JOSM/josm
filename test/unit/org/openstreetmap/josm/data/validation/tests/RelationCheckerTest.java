// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.data.osm.OsmUtils.createPrimitive;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;

public class RelationCheckerTest {

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        getRelationChecker();
    }

    private static RelationChecker getRelationChecker() {
        RelationChecker checker = new RelationChecker();
        TaggingPresets.readFromPreferences();
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
        List<TestError> errors = testRelation(createRelation("type=foobar"));
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
        assertEquals("Role outer2 unknown in templates outer/inner", errors.get(0).getDescription());
    }

    @Test
    public void testRestrictionViaMissing() {
        Relation r = createRelation("type=restriction");
        r.addMember(new RelationMember("from", new Way()));
        r.addMember(new RelationMember("to", new Way()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Role via missing", errors.get(0).getDescription());
    }

    @Test
    public void testRestrictionViaRelation() {
        Relation r = createRelation("type=restriction");
        r.addMember(new RelationMember("from", new Way()));
        r.addMember(new RelationMember("to", new Way()));
        r.addMember(new RelationMember("via", new Relation()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Role member type relation does not match accepted list of node/way in template Turn Restriction",
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
        assertEquals("Number of from roles too high (2)", errors.get(0).getDescription());
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
        assertTrue(errors.get(0).getDescription().startsWith("Empty role type found when expecting one of"));
    }

    @Test
    public void testPowerMemberExpression() {
        Relation r = createRelation("type=route route=power");
        r.addMember(new RelationMember("", new Way()));

        List<TestError> errors = testRelation(r);
        assertEquals(1, errors.size());
        assertEquals("Role member does not match expression power in template Power Route", errors.get(0).getDescription());
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
        assertEquals("Role member does not match expression railway in template Public Transport Route (Rail)",
                testRelation(r).get(0).getDescription());

        r.removeMember(3);
        r.addMember(new RelationMember("stop", createPrimitive("way no-rail-way=yes")));
        assertEquals(1, testRelation(r).size());
        assertEquals("Role member type way does not match accepted list of node in template Public Transport Route (Rail)",
                testRelation(r).get(0).getDescription());

        r.removeMember(3);
        r.addMember(new RelationMember("stop", createPrimitive("node public_transport=stop_position bus=yes")));
        assertEquals(1, testRelation(r).size());
        assertEquals("Role member does not match expression public_transport=stop_position && "+
                "(train=yes || subway=yes || monorail=yes || tram=yes || light_rail=yes) in template Public Transport Route (Rail)",
                testRelation(r).get(0).getDescription());
    }
}
