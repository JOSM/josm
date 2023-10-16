// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests of {@link KeyCondition}.
 */
@Projection
class KeyConditionTest {

    private DataSet ds;

    /**
     * Setup test
     */
    @BeforeEach
    public void setUp() {
        ds = new DataSet();
    }

    Relation relation(int id) {
        Relation r = new Relation(id, 1);
        ds.addPrimitive(r);
        return r;
    }

    Node node(int id) {
        Node n = new Node(id, 1);
        n.setCoor(LatLon.ZERO);
        ds.addPrimitive(n);
        return n;
    }

    private static void shouldFail(Runnable r) {
        try {
            r.run();
            fail("should throw exception");
        } catch (MapCSSException e) {
            Logging.trace(e);
        }
    }

    /**
     * Test {@link ConditionFactory#createKeyCondition}.
     */
    @Test
    public void create() {

        // ["a label"]
        ConditionFactory.createKeyCondition("a key", false, KeyMatchType.FALSE, Context.PRIMITIVE);
        // ["a label"?]
        ConditionFactory.createKeyCondition("a key", false, KeyMatchType.TRUE, Context.PRIMITIVE);
        // [!"a label"]
        ConditionFactory.createKeyCondition("a key", true, KeyMatchType.FALSE, Context.PRIMITIVE);
        // [!"a label"?]
        ConditionFactory.createKeyCondition("a key", true, KeyMatchType.TRUE, Context.PRIMITIVE);

        // [/regex/]
        Condition c = ConditionFactory.createKeyCondition("foo|bar", false, KeyMatchType.REGEX, Context.PRIMITIVE);
        assertTrue(c.applies(new Environment(OsmUtils.createPrimitive("node BARfooBAZ=true"))));
        assertFalse(c.applies(new Environment(OsmUtils.createPrimitive("node BARBAZ=true"))));
        c = ConditionFactory.createKeyCondition("colour:", false, KeyMatchType.REGEX, Context.PRIMITIVE);
        assertEquals(KeyMatchType.ANY_CONTAINS, ((KeyCondition) c).matchType);
        assertEquals("colour:", ((KeyCondition) c).label);
        assertTrue(c.applies(new Environment(OsmUtils.createPrimitive("node colour:roof=ref"))));
        assertFalse(c.applies(new Environment(OsmUtils.createPrimitive("node foo=bar"))));
        c = ConditionFactory.createKeyCondition("^wikipedia:", false, KeyMatchType.REGEX, Context.PRIMITIVE);
        assertEquals(KeyMatchType.ANY_STARTS_WITH, ((KeyCondition) c).matchType);
        assertEquals("wikipedia:", ((KeyCondition) c).label);
        assertTrue(c.applies(new Environment(OsmUtils.createPrimitive("node wikipedia:en=a"))));
        assertFalse(c.applies(new Environment(OsmUtils.createPrimitive("node wikipedia=a"))));
        c = ConditionFactory.createKeyCondition("_name$", false, KeyMatchType.REGEX, Context.PRIMITIVE);
        assertEquals(KeyMatchType.ANY_ENDS_WITH, ((KeyCondition) c).matchType);
        assertEquals("_name", ((KeyCondition) c).label);
        assertTrue(c.applies(new Environment(OsmUtils.createPrimitive("node alt_name=a"))));
        assertFalse(c.applies(new Environment(OsmUtils.createPrimitive("node name=a"))));

        // ["a label"]
        ConditionFactory.createKeyCondition("a key", false, null, Context.LINK);
        // [!"a label"]
        ConditionFactory.createKeyCondition("a key", true, null, Context.LINK);

        // ["a label"?]
        shouldFail(() ->
           ConditionFactory.createKeyCondition("a key", false, KeyMatchType.TRUE, Context.LINK)
        );

        // [!"a label"?]
        shouldFail(() ->
            ConditionFactory.createKeyCondition("a key", true, KeyMatchType.TRUE, Context.LINK)
        );
    }

    @Test
    public void applies_1() {
        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));

        Environment e = new Environment(n).withParent(r).withIndex(0, r.getMembersCount()).withLinkContext();

        Condition cond = ConditionFactory.createKeyCondition("my_role", false, null, Context.LINK);
        assertTrue(cond.applies(e));

        cond = ConditionFactory.createKeyCondition("my_role", true, null, Context.LINK);
        assertFalse(cond.applies(e));
    }

    @Test
    public void applies_2() {
        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));

        Environment e = new Environment(n).withParent(r).withIndex(0, r.getMembersCount()).withLinkContext();

        Condition cond = ConditionFactory.createKeyCondition("another_role", false, null, Context.LINK);
        assertFalse(cond.applies(e));

        cond = ConditionFactory.createKeyCondition("another_role", true, null, Context.LINK);
        assertTrue(cond.applies(e));
    }

    /**
     * Ensure that we are accounting for all necessary {@link ConditionFactory.KeyMatchType} are accounted for.
     * If this fails, and the key should not be fully matched against (i.e., it is a regex), please modify
     * {@link MapCSSRuleIndex#findAnyRequiredKey}.
     * <p>
     * Non-regression test for JOSM #22073.
     */
    @ParameterizedTest
    @EnumSource(ConditionFactory.KeyMatchType.class)
    void testNonRegression22073(final KeyMatchType keyMatchType) {
        final EnumSet<ConditionFactory.KeyMatchType> current = EnumSet.of(KeyMatchType.EQ, KeyMatchType.FALSE, KeyMatchType.TRUE,
                KeyMatchType.REGEX, KeyMatchType.ANY_CONTAINS, KeyMatchType.ANY_ENDS_WITH, KeyMatchType.ANY_STARTS_WITH);
        assertTrue(current.contains(keyMatchType), "Is this type supposed to be matched against a whole key?");

        final boolean fullKey = EnumSet.of(KeyMatchType.EQ, KeyMatchType.TRUE, KeyMatchType.FALSE).contains(keyMatchType);
        final MapCSSRuleIndex index = new MapCSSRuleIndex();
        final Condition condition = keyMatchType != KeyMatchType.REGEX
                ? new KeyCondition("highway", false, keyMatchType)
                : new ConditionFactory.KeyRegexpCondition(Pattern.compile("highway"), false);
        index.add(new MapCSSRule(Collections.singletonList(new Selector.GeneralSelector("*", Range.ZERO_TO_INFINITY,
                Collections.singletonList(condition), null)), null));
        index.initIndex();
        final Node testNode = TestUtils.newNode("highway=traffic_calming");
        // First get all the "remaining" candidates by passing a non-tagged node
        final Collection<MapCSSRule> remaining = convertIterator(index.getRuleCandidates(new Node(LatLon.ZERO)));
        // Then get all the matches for the test node
        final Collection<MapCSSRule> matches = convertIterator(index.getRuleCandidates(testNode));
        // Finally, remove the remaining rules from the matches
        matches.removeIf(remaining::contains);
        assertEquals(fullKey, !matches.isEmpty());
    }

    private static <T> Collection<T> convertIterator(Iterator<T> iterator) {
        final List<T> rList = new ArrayList<>();
        iterator.forEachRemaining(rList::add);
        return rList;
    }
}
