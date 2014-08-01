// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests

import static org.openstreetmap.josm.data.osm.OsmUtils.createPrimitive

import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.RelationMember
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.validation.TestError
import org.openstreetmap.josm.gui.tagging.TaggingPresets

class RelationCheckerTest extends GroovyTestCase {

    @Override
    void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        getRelationChecker()
    }

    static def RelationChecker getRelationChecker() {
        def checker = new RelationChecker()
        TaggingPresets.readFromPreferences()
        checker.initialize()
        return checker
    }

    static def List<TestError> testRelation(Relation r) {
        def checker = getRelationChecker()
        checker.visit(r)
        return checker.getErrors()
    }

    static def Relation createRelation(String tags) {
        return (Relation) createPrimitive("relation "+ tags)
    }

    void testUnknownType() {
        def errors = testRelation(createRelation("type=foobar"))
        assert errors.size() == 1
        assert errors.get(0).getMessage() == "Relation type is unknown"
    }

    void testEmpty() {
        def errors = testRelation(createRelation("type=multipolygon"))
        assert errors.size() == 1
        assert errors.get(0).getMessage() == "Relation is empty"
    }

    void testOuter2() {
        def r = createRelation("type=multipolygon")
        r.addMember(new RelationMember("outer", new Way()))
        r.addMember(new RelationMember("outer2", new Way()))

        def errors = testRelation(r)
        assert errors.size() == 1
        assert errors.get(0).getDescription() == "Role outer2 unknown"
    }

    void testRestrictionViaMissing() {
        def r = createRelation("type=restriction")
        r.addMember(new RelationMember("from", new Way()))
        r.addMember(new RelationMember("to", new Way()))

        def errors = testRelation(r)
        assert errors.size() == 1
        assert errors.get(0).getDescription() == "Role via missing"
    }

    void testRestrictionViaRelation() {
        def r = createRelation("type=restriction")
        r.addMember(new RelationMember("from", new Way()))
        r.addMember(new RelationMember("to", new Way()))
        r.addMember(new RelationMember("via", new Relation()))

        def errors = testRelation(r)
        assert errors.size() == 1
        assert errors.get(0).getDescription() == "Member for role via of wrong type"
    }

    void testRestrictionTwoFrom() {
        def r = createRelation("type=restriction")
        r.addMember(new RelationMember("from", new Way()))
        r.addMember(new RelationMember("from", new Way()))
        r.addMember(new RelationMember("to", new Way()))
        r.addMember(new RelationMember("via", new Way()))

        def errors = testRelation(r)
        assert errors.size() == 1
        assert errors.get(0).getDescription() == "Number of from roles too high (2)"
    }

    void testRestrictionEmpty() {
        def r = createRelation("type=restriction")
        r.addMember(new RelationMember("from", new Way()))
        r.addMember(new RelationMember("to", new Way()))
        r.addMember(new RelationMember("via", new Way()))
        r.addMember(new RelationMember("", new Way()))

        def errors = testRelation(r)
        assert errors.size() == 1
        assert errors.get(0).getDescription() == "Empty role found"
    }

    void testPowerMemberExpression() {
        def r = createRelation("type=route route=power")
        r.addMember(new RelationMember("", new Way()))

        def errors = testRelation(r)
        assert errors.size() == 1
        assert errors.get(0).getDescription() == "Member for role '<empty>' does not match 'power'"
    }
}
