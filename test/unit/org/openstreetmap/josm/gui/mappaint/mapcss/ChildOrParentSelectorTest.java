// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link ChildOrParentSelector}.
 */
@Projection
class ChildOrParentSelectorTest {

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

    Way way(int id) {
        Way w = new Way(id, 1);
        ds.addPrimitive(w);
        return w;
    }

    ChildOrParentSelector parse(String css) {
         MapCSSStyleSource source = new MapCSSStyleSource(css);
         source.loadStyleSource();
         assertEquals(1, source.rules.size());
         return (ChildOrParentSelector) source.rules.get(0).selectors.get(0);
    }

    @Test
    @Disabled
    public void matches_1() {
        String css = "relation >[role=\"my_role\"] node {}";
        ChildOrParentSelector selector = parse(css);

        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));
        Environment e = new Environment().withChild(n);

        assertTrue(selector.matches(e));
    }

    @Test
    @Disabled
    public void matches_2() {
        String css = "relation >[\"my_role\"] node {}";
        ChildOrParentSelector selector = parse(css);

        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));
        Environment e = new Environment().withChild(n);

        assertTrue(selector.matches(e));
    }

    @Test
    @Disabled
    public void matches_3() {
        String css = "relation >[!\"my_role\"] node {}";
        ChildOrParentSelector selector = parse(css);

        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));
        Environment e = new Environment().withChild(n);

        assertFalse(selector.matches(e));
    }

    @Test
    @Disabled
    public void matches_4() {
        String css = "way < relation {}";
        ChildOrParentSelector selector = parse(css);
        assertEquals(Selector.ChildOrParentSelectorType.PARENT, selector.type);

    }

    @Test
    public void matches_5() {
        String css = "way <[role != \"my_role\"] relation {text: index();}";
        ChildOrParentSelector selector = parse(css);
        assertEquals(Selector.ChildOrParentSelectorType.PARENT, selector.type);

        Relation r = relation(1);
        Way w1 = way(1);
        w1.setNodes(Arrays.asList(node(11), node(12)));

        Way w2 = way(2);
        w2.setNodes(Arrays.asList(node(21), node(22)));

        Way w3 = way(3);
        w3.setNodes(Arrays.asList(node(31), node(32)));

        r.addMember(new RelationMember("my_role", w1));
        r.addMember(new RelationMember("my_role", w2));
        r.addMember(new RelationMember("another role", w3));
        r.addMember(new RelationMember("yet another role", w3));

        Environment e = new Environment(r, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertTrue(selector.matches(e));

        MapCSSStyleSource source = new MapCSSStyleSource(css);
        source.loadStyleSource();
        source.rules.get(0).declaration.execute(e);
        assertEquals(Float.valueOf(3f), e.getCascade(Environment.DEFAULT_LAYER).get("text", null, Float.class));
    }

    @Test
    public void matches_6() {
        String css = "relation >[role != \"my_role\"] way {}";
        ChildOrParentSelector selector = parse(css);

        Relation r = relation(1);
        Way w1 = way(1);
        w1.setNodes(Arrays.asList(node(11), node(12)));

        Way w2 = way(2);
        w2.setNodes(Arrays.asList(node(21), node(22)));

        Way w3 = way(3);
        w3.setNodes(Arrays.asList(node(31), node(32)));

        r.addMember(new RelationMember("my_role", w1));
        r.addMember(new RelationMember("my_role", w2));
        r.addMember(new RelationMember("another role", w3));

        Environment e = new Environment(w1);
        assertFalse(selector.matches(e));

        e = new Environment(w2);
        assertFalse(selector.matches(e));

        e = new Environment(w3);
        assertTrue(selector.matches(e));
    }

    /**
     * Test inside/contains selectors (spatial test)
     * @throws Exception in case of any error
     */
    @Test
    void testContains() throws Exception {
        ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get("nodist/data/amenity-in-amenity.osm")), null);
        ChildOrParentSelector css = parse("node[tag(\"amenity\") = parent_tag(\"amenity\")] ∈ *[amenity] {}");
        assertFalse(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.NODE))));
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.WAY))));
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.RELATION))));
        css = parse("node[tag(\"amenity\") = parent_tag(\"amenity\")] ⊆  *[amenity] {}");
        assertFalse(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.NODE))));
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.WAY))));
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.RELATION))));
        css = parse("node[tag(\"amenity\") = parent_tag(\"amenity\")] ⊈  *[amenity] {}");
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.NODE))));
        assertFalse(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.WAY))));
        assertFalse(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.RELATION))));
        css = parse("*[tag(\"amenity\") = parent_tag(\"amenity\")] ⊇  *[amenity] {}");
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.NODE))));
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.WAY))));
        assertFalse(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.RELATION))));
        css = parse("*[tag(\"amenity\") = parent_tag(\"amenity\")] ⊉  *[amenity] {}");
        assertFalse(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.NODE))));
        assertFalse(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.WAY))));
        assertTrue(css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.RELATION))));
    }
}
