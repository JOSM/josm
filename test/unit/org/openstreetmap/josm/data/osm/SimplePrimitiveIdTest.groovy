package org.openstreetmap.josm.data.osm

class SimplePrimitiveIdTest extends GroovyTestCase {

    void testNode() {
        assert SimplePrimitiveId.fromString("node/123") == new SimplePrimitiveId(123, OsmPrimitiveType.NODE)
        assert SimplePrimitiveId.fromString("n123") == new SimplePrimitiveId(123, OsmPrimitiveType.NODE)
        assert SimplePrimitiveId.fromString("node123") == new SimplePrimitiveId(123, OsmPrimitiveType.NODE)
        assert SimplePrimitiveId.fromString("n123456789123456789") == new SimplePrimitiveId(123456789123456789, OsmPrimitiveType.NODE)
    }

    void testWay() {
        assert SimplePrimitiveId.fromString("way/123") == new SimplePrimitiveId(123, OsmPrimitiveType.WAY)
        assert SimplePrimitiveId.fromString("w123") == new SimplePrimitiveId(123, OsmPrimitiveType.WAY)
        assert SimplePrimitiveId.fromString("way123") == new SimplePrimitiveId(123, OsmPrimitiveType.WAY)
        assert SimplePrimitiveId.fromString("w123456789123456789") == new SimplePrimitiveId(123456789123456789, OsmPrimitiveType.WAY)
    }

    void testRelation() {
        assert SimplePrimitiveId.fromString("relation/123") == new SimplePrimitiveId(123, OsmPrimitiveType.RELATION)
        assert SimplePrimitiveId.fromString("r123") == new SimplePrimitiveId(123, OsmPrimitiveType.RELATION)
        assert SimplePrimitiveId.fromString("rel123") == new SimplePrimitiveId(123, OsmPrimitiveType.RELATION)
        assert SimplePrimitiveId.fromString("relation123") == new SimplePrimitiveId(123, OsmPrimitiveType.RELATION)
    }

    void testBad() {
        shouldFail { SimplePrimitiveId.fromString("foobar") }
    }

    void testFuzzy() {
        assert SimplePrimitiveId.fuzzyParse("foo relation/123 bar").toString() == "[relation 123]"
        assert SimplePrimitiveId.fuzzyParse("foo relation/123 and way/345 but also node/789").toString() == "[relation 123, way 345, node 789]"
        assert SimplePrimitiveId.fuzzyParse("foo relation/123-24 and way/345-346 but also node/789").toString() == "[relation 123, relation 124, way 345, way 346, node 789]"
    }

    void testFromCopyAction() {
        assert SimplePrimitiveId.fromString("node 123") == new SimplePrimitiveId(123, OsmPrimitiveType.NODE)
        assert SimplePrimitiveId.fromString("way 123") == new SimplePrimitiveId(123, OsmPrimitiveType.WAY)
        assert SimplePrimitiveId.fromString("relation 123") == new SimplePrimitiveId(123, OsmPrimitiveType.RELATION)
    }

    void testMultipleIDs() {
        assert SimplePrimitiveId.multipleFromString("node/234").toString() == "[node 234]"
        assert SimplePrimitiveId.multipleFromString("node/234-234").toString() == "[node 234]"
        assert SimplePrimitiveId.multipleFromString("node/2-1").toString() == "[]"
        assert SimplePrimitiveId.multipleFromString("node/123-124").toString() == "[node 123, node 124]"
        assert SimplePrimitiveId.multipleFromString("n/123-124").toString() == "[node 123, node 124]"
        assert SimplePrimitiveId.multipleFromString("node123-126").toString() == "[node 123, node 124, node 125, node 126]"
        assert SimplePrimitiveId.multipleFromString("way/123-123").toString() == "[way 123]"
        assert SimplePrimitiveId.multipleFromString("w/123-127").toString() == "[way 123, way 124, way 125, way 126, way 127]"
        assert SimplePrimitiveId.multipleFromString("way123-125").toString() == "[way 123, way 124, way 125]"
        assert SimplePrimitiveId.multipleFromString("relation/123-125").toString() == "[relation 123, relation 124, relation 125]"
        assert SimplePrimitiveId.multipleFromString("r/123-125").toString() == "[relation 123, relation 124, relation 125]"
        assert SimplePrimitiveId.multipleFromString("relation123-125").toString() == "[relation 123, relation 124, relation 125]"
        assert SimplePrimitiveId.multipleFromString("node/234-5").toString() == "[node 234, node 235]"
        assert SimplePrimitiveId.multipleFromString("node/234-35").toString() == "[node 234, node 235]"
        assert SimplePrimitiveId.multipleFromString("node/234-235").toString() == "[node 234, node 235]"
        assert SimplePrimitiveId.multipleFromString("node/998-1001").toString() == "[node 998, node 999, node 1000, node 1001]"
        shouldFail { SimplePrimitiveId.multipleFromString("foo node123 bar") }
    }
}
