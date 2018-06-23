// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of {@link SimplePrimitiveId} class.
 */
public class SimplePrimitiveIdTest {

    /**
     * Unit test of {@link SimplePrimitiveId#fromString} for nodes.
     */
    @Test
    public void testFromStringNode() {
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.NODE), SimplePrimitiveId.fromString("node/123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.NODE), SimplePrimitiveId.fromString("n123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.NODE), SimplePrimitiveId.fromString("node123"));
        assertEquals(new SimplePrimitiveId(123456789123456789L, OsmPrimitiveType.NODE), SimplePrimitiveId.fromString("n123456789123456789"));
    }

    /**
     * Unit test of {@link SimplePrimitiveId#fromString} for ways.
     */
    @Test
    public void testFromStringWay() {
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.WAY), SimplePrimitiveId.fromString("way/123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.WAY), SimplePrimitiveId.fromString("w123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.WAY), SimplePrimitiveId.fromString("way123"));
        assertEquals(new SimplePrimitiveId(123456789123456789L, OsmPrimitiveType.WAY), SimplePrimitiveId.fromString("w123456789123456789"));
    }

    /**
     * Unit test of {@link SimplePrimitiveId#fromString} for relations.
     */
    @Test
    public void testFromStringRelation() {
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.RELATION), SimplePrimitiveId.fromString("relation/123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.RELATION), SimplePrimitiveId.fromString("r123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.RELATION), SimplePrimitiveId.fromString("rel123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.RELATION), SimplePrimitiveId.fromString("relation123"));
    }

    /**
     * Unit test of {@link SimplePrimitiveId#fromString} for invalid input.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromStringBad() {
        SimplePrimitiveId.fromString("foobar");
    }

    /**
     * Unit test of {@link SimplePrimitiveId#fuzzyParse}.
     */
    @Test
    public void testFuzzyParse() {
        assertEquals("[relation 123]",
                SimplePrimitiveId.fuzzyParse("foo relation/123 bar").toString());
        assertEquals("[relation 123, way 345, node 789]",
                SimplePrimitiveId.fuzzyParse("foo relation/123 and way/345 but also node/789").toString());
        assertEquals("[relation 123, relation 124, way 345, way 346, node 789]",
                SimplePrimitiveId.fuzzyParse("foo relation/123-24 and way/345-346 but also node/789").toString());
        assertEquals("[]",
                SimplePrimitiveId.fuzzyParse("foo relation/0 bar").toString());
    }

    @Test
    public void testFromCopyAction() {
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.NODE), SimplePrimitiveId.fromString("node 123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.WAY), SimplePrimitiveId.fromString("way 123"));
        assertEquals(new SimplePrimitiveId(123, OsmPrimitiveType.RELATION), SimplePrimitiveId.fromString("relation 123"));
    }

    /**
     * Unit test of {@link SimplePrimitiveId#multipleFromString}.
     */
    @Test
    public void testMultipleFromString() {
        assertEquals("[node 234]", SimplePrimitiveId.multipleFromString("node/234").toString());
        assertEquals("[node 234]", SimplePrimitiveId.multipleFromString("node/234-234").toString());
        assertEquals("[]", SimplePrimitiveId.multipleFromString("node/2-1").toString());
        assertEquals("[node 123, node 124]", SimplePrimitiveId.multipleFromString("node/123-124").toString());
        assertEquals("[node 123, node 124]", SimplePrimitiveId.multipleFromString("n/123-124").toString());
        assertEquals("[node 123, node 124, node 125, node 126]", SimplePrimitiveId.multipleFromString("node123-126").toString());
        assertEquals("[way 123]", SimplePrimitiveId.multipleFromString("way/123-123").toString());
        assertEquals("[way 123, way 124, way 125, way 126, way 127]", SimplePrimitiveId.multipleFromString("w/123-127").toString());
        assertEquals("[way 123, way 124, way 125]", SimplePrimitiveId.multipleFromString("way123-125").toString());
        assertEquals("[relation 123, relation 124, relation 125]", SimplePrimitiveId.multipleFromString("relation/123-125").toString());
        assertEquals("[relation 123, relation 124, relation 125]", SimplePrimitiveId.multipleFromString("r/123-125").toString());
        assertEquals("[relation 123, relation 124, relation 125]", SimplePrimitiveId.multipleFromString("relation123-125").toString());
        assertEquals("[node 234, node 235]", SimplePrimitiveId.multipleFromString("node/234-5").toString());
        assertEquals("[node 234, node 235]", SimplePrimitiveId.multipleFromString("node/234-35").toString());
        assertEquals("[node 234, node 235]", SimplePrimitiveId.multipleFromString("node/234-235").toString());
        assertEquals("[node 998, node 999, node 1000, node 1001]", SimplePrimitiveId.multipleFromString("node/998-1001").toString());
        assertEquals("[]", SimplePrimitiveId.multipleFromString("node/0").toString());
    }

    /**
     * Unit test of {@link SimplePrimitiveId#multipleFromString} for invalid data.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMultipleFromStringBad() {
        SimplePrimitiveId.multipleFromString("foo node123 bar");
    }
}
