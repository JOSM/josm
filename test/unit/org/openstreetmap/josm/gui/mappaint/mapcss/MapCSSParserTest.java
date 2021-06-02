// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context.PRIMITIVE;

import java.awt.Color;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.ClassCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyRegexpCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.PseudoClassCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.RegexpKeyValueRegexpCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.SimpleKeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ColorHelper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MapCSSParser}.
 */
class MapCSSParserTest {

    protected static Environment getEnvironment(String key, String value) {
        return new Environment(OsmUtils.createPrimitive("way " + key + "=" + value));
    }

    protected static MapCSSParser getParser(String stringToParse) {
        return new MapCSSParser(new StringReader(stringToParse));
    }

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    @Test
    void testDeclarations() throws Exception {
        getParser("{ opacity: 0.5; color: rgb(1.0, 0.0, 0.0); }").declaration();
        getParser("{ set tag=value; }").declaration(); //set a tag
        getParser("{ set tag; }").declaration(); // set a tag to 'yes'
        getParser("{ opacity: eval(\"tag('population')/100000\"); }").declaration();
        getParser("{ set width_in_metres=eval(\"tag('lanes')*3\"); }").declaration();
    }

    @Test
    void testClassCondition() throws Exception {
        List<Condition> conditions = getParser("way[name=X].highway:closed").selector().getConditions();
        assertTrue(conditions.get(0) instanceof SimpleKeyValueCondition);
        assertTrue(conditions.get(0).applies(getEnvironment("name", "X")));
        assertTrue(conditions.get(1) instanceof ClassCondition);
        assertTrue(conditions.get(2) instanceof PseudoClassCondition);
        assertFalse(conditions.get(2).applies(getEnvironment("name", "X")));
    }

    @Test
    void testPseudoClassCondition() throws Exception {
        Condition c0 = getParser("way:area-style").selector().getConditions().get(0);
        Condition c1 = getParser("way!:area-style").selector().getConditions().get(0);
        Condition c2 = getParser("way!:areaStyle").selector().getConditions().get(0);
        Condition c3 = getParser("way!:area_style").selector().getConditions().get(0);
        assertEquals(":areaStyle", c0.toString());
        assertEquals("!:areaStyle", c1.toString());
        assertEquals("!:areaStyle", c2.toString());
        assertEquals("!:areaStyle", c3.toString());
        Selector tagged = getParser("way:tagged").selector();
        Selector notTagged = getParser("way!:tagged").selector();
        assertFalse(tagged.matches((new Environment(OsmUtils.createPrimitive("way")))));
        assertTrue(tagged.matches((new Environment(OsmUtils.createPrimitive("way building=yes")))));
        assertTrue(notTagged.matches((new Environment(OsmUtils.createPrimitive("way")))));
        assertFalse(notTagged.matches((new Environment(OsmUtils.createPrimitive("way building=yes")))));
    }

    @Test
    void testClassMatching() throws Exception {
        MapCSSStyleSource css = new MapCSSStyleSource(
                "way[highway=footway] { set .path; color: #FF6644; width: 2; }\n" +
                "way[highway=path]    { set path; color: brown; width: 2; }\n" +
                "way[\"set\"=escape]  {  }\n" +
                "way.path             { text:auto; text-color: green; text-position: line; text-offset: 5; }\n" +
                "way!.path            { color: orange; }\n"
        );
        css.loadStyleSource();
        assertTrue(css.getErrors().isEmpty());
        MultiCascade mc1 = new MultiCascade();
        css.apply(mc1, OsmUtils.createPrimitive("way highway=path"), 1, false);
        assertEquals("green", mc1.getCascade(null).get("text-color", null, String.class));
        assertEquals("brown", mc1.getCascade(null).get("color", null, String.class));
        MultiCascade mc2 = new MultiCascade();
        css.apply(mc2, OsmUtils.createPrimitive("way highway=residential"), 1, false);
        assertEquals("orange", mc2.getCascade(null).get("color", null, String.class));
        assertNull(mc2.getCascade(null).get("text-color", null, String.class));
        MultiCascade mc3 = new MultiCascade();
        css.apply(mc3, OsmUtils.createPrimitive("way highway=footway"), 1, false);
        assertEquals(ColorHelper.html2color("#FF6644"), mc3.getCascade(null).get("color", null, Color.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "way[railway][bridge=yes]::bridges { z-index: 1; casting-width: 4; casing-color: #797979 }",
            "way[bridge=yes]::bridges { set .bridge }\nway[railway].bridge::bridges { z-index: 1; casting-width: 4; casing-color: #797979 }"
    })
    void testLayerMatching(String cssString) {
        MapCSSStyleSource css = new MapCSSStyleSource(cssString);
        css.loadStyleSource();
        assertTrue(css.getErrors().isEmpty());
        MultiCascade mc1 = new MultiCascade();
        css.apply(mc1, OsmUtils.createPrimitive("way railway=rail bridge=yes"), 1, false);
        assertNull(mc1.getCascade(null).get("casing-color", null, String.class));
        assertEquals("#797979", mc1.getCascade("bridges").get("casing-color", null, String.class));
    }

    @Test
    void testEqualCondition() throws Exception {
        Condition condition = getParser("[surface=paved]").condition(PRIMITIVE);
        assertTrue(condition instanceof SimpleKeyValueCondition);
        assertEquals("surface", ((SimpleKeyValueCondition) condition).k);
        assertEquals("paved", ((SimpleKeyValueCondition) condition).v);
        assertTrue(condition.applies(getEnvironment("surface", "paved")));
        assertFalse(condition.applies(getEnvironment("surface", "unpaved")));
    }

    @Test
    void testNotEqualCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[surface!=paved]").condition(PRIMITIVE);
        assertEquals(Op.NEQ, condition.op);
        assertFalse(condition.applies(getEnvironment("surface", "paved")));
        assertTrue(condition.applies(getEnvironment("surface", "unpaved")));
    }

    @Test
    void testRegexCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[surface=~/paved|unpaved/]").condition(PRIMITIVE);
        assertEquals(Op.REGEX, condition.op);
        assertTrue(condition.applies(getEnvironment("surface", "unpaved")));
        assertFalse(condition.applies(getEnvironment("surface", "grass")));
    }

    @Test
    void testRegexConditionParenthesis() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[name =~ /^\\(foo\\)/]").condition(PRIMITIVE);
        assertTrue(condition.applies(getEnvironment("name", "(foo)")));
        assertFalse(condition.applies(getEnvironment("name", "foo")));
        assertFalse(condition.applies(getEnvironment("name", "((foo))")));
    }

    @Test
    void testNegatedRegexCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[surface!~/paved|unpaved/]").condition(PRIMITIVE);
        assertEquals(Op.NREGEX, condition.op);
        assertFalse(condition.applies(getEnvironment("surface", "unpaved")));
        assertTrue(condition.applies(getEnvironment("surface", "grass")));
    }

    @Test
    void testBeginsEndsWithCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[foo ^= bar]").condition(PRIMITIVE);
        assertEquals(Op.BEGINS_WITH, condition.op);
        assertTrue(condition.applies(getEnvironment("foo", "bar123")));
        assertFalse(condition.applies(getEnvironment("foo", "123bar")));
        assertFalse(condition.applies(getEnvironment("foo", "123bar123")));
        condition = (KeyValueCondition) getParser("[foo $= bar]").condition(PRIMITIVE);
        assertEquals(Op.ENDS_WITH, condition.op);
        assertFalse(condition.applies(getEnvironment("foo", "bar123")));
        assertTrue(condition.applies(getEnvironment("foo", "123bar")));
        assertFalse(condition.applies(getEnvironment("foo", "123bar123")));
    }

    @Test
    void testOneOfCondition() throws Exception {
        Condition condition = getParser("[vending~=stamps]").condition(PRIMITIVE);
        assertTrue(condition.applies(getEnvironment("vending", "stamps")));
        assertTrue(condition.applies(getEnvironment("vending", "bar;stamps;foo")));
        assertFalse(condition.applies(getEnvironment("vending", "every;thing;else")));
        assertFalse(condition.applies(getEnvironment("vending", "or_nothing")));
    }

    @Test
    void testStandardKeyCondition() throws Exception {
        KeyCondition c1 = (KeyCondition) getParser("[ highway ]").condition(PRIMITIVE);
        assertEquals(KeyMatchType.EQ, c1.matchType);
        assertTrue(c1.applies(getEnvironment("highway", "unclassified")));
        assertFalse(c1.applies(getEnvironment("railway", "rail")));
        KeyCondition c2 = (KeyCondition) getParser("[\"/slash/\"]").condition(PRIMITIVE);
        assertEquals(KeyMatchType.EQ, c2.matchType);
        assertTrue(c2.applies(getEnvironment("/slash/", "yes")));
        assertFalse(c2.applies(getEnvironment("\"slash\"", "no")));
    }

    @Test
    void testYesNoKeyCondition() throws Exception {
        KeyCondition c1 = (KeyCondition) getParser("[oneway?]").condition(PRIMITIVE);
        KeyCondition c2 = (KeyCondition) getParser("[oneway?!]").condition(PRIMITIVE);
        KeyCondition c3 = (KeyCondition) getParser("[!oneway?]").condition(PRIMITIVE);
        KeyCondition c4 = (KeyCondition) getParser("[!oneway?!]").condition(PRIMITIVE);
        Environment yes = getEnvironment("oneway", "yes");
        Environment no = getEnvironment("oneway", "no");
        Environment none = getEnvironment("no-oneway", "foo");
        assertTrue(c1.applies(yes));
        assertFalse(c1.applies(no));
        assertFalse(c1.applies(none));
        assertFalse(c2.applies(yes));
        assertTrue(c2.applies(no));
        assertFalse(c2.applies(none));
        assertFalse(c3.applies(yes));
        assertTrue(c3.applies(no));
        assertTrue(c3.applies(none));
        assertTrue(c4.applies(yes));
        assertFalse(c4.applies(no));
        assertTrue(c4.applies(none));
    }

    @Test
    void testRegexKeyCondition() throws Exception {
        KeyRegexpCondition c1 = (KeyRegexpCondition) getParser("[/.*:(backward|forward)$/]").condition(PRIMITIVE);
        assertEquals(Pattern.compile(".*:(backward|forward)$").pattern(), c1.pattern.pattern());
        assertFalse(c1.applies(getEnvironment("lanes", "3")));
        assertTrue(c1.applies(getEnvironment("lanes:forward", "3")));
        assertTrue(c1.applies(getEnvironment("lanes:backward", "3")));
        assertFalse(c1.applies(getEnvironment("lanes:foobar", "3")));
    }

    @Test
    void testRegexKeyValueRegexpCondition() throws Exception {
        RegexpKeyValueRegexpCondition c1 = (RegexpKeyValueRegexpCondition) getParser("[/^name/=~/Test/]").condition(PRIMITIVE);
        assertEquals("^name", c1.keyPattern.pattern());
        assertEquals("Test", c1.pattern.pattern());
        assertTrue(c1.applies(getEnvironment("name", "Test St")));
        assertFalse(c1.applies(getEnvironment("alt_name", "Test St")));
    }

    @Test
    void testNRegexKeyConditionSelector() throws Exception {
        Selector s1 = getParser("*[sport][tourism != hotel]").selector();
        assertTrue(s1.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar"))));
        assertFalse(s1.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar tourism=hotel"))));
        Selector s2 = getParser("*[sport][tourism != hotel][leisure !~ /^(sports_centre|stadium|)$/]").selector();
        assertTrue(s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar"))));
        assertFalse(s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar tourism=hotel"))));
        assertFalse(s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar leisure=stadium"))));
    }

    @Test
    void testKeyKeyCondition() throws Exception {
        KeyValueCondition c1 = (KeyValueCondition) getParser("[foo = *bar]").condition(PRIMITIVE);
        Way w1 = new Way();
        w1.put("foo", "123");
        w1.put("bar", "456");
        assertFalse(c1.applies(new Environment(w1)));
        w1.put("bar", "123");
        assertTrue(c1.applies(new Environment(w1)));
        KeyValueCondition c2 = (KeyValueCondition) getParser("[foo =~ */bar/]").condition(PRIMITIVE);
        Way w2 = new Way(w1);
        w2.put("bar", "[0-9]{3}");
        assertTrue(c2.applies(new Environment(w2)));
        w2.put("bar", "[0-9]");
        assertTrue(c2.applies(new Environment(w2)));
        w2.put("bar", "^[0-9]$");
        assertFalse(c2.applies(new Environment(w2)));
    }

    /**
     * Make certain that getting tags by regex works
     * @throws Exception if there is an assert error (or another error)
     */
    @Test
    void testTagRegex() throws Exception {
        DataSet ds = new DataSet();
        Way way1 = TestUtils.newWay("old_ref=A1 ref=A2", new Node(new LatLon(1, 1)), new Node(new LatLon(2, 2)));
        for (Node node : way1.getNodes()) {
            ds.addPrimitive(node);
        }
        ds.addPrimitive(way1);

        tagRegex(way1, "way[ref][count(tag_regex(\"ref\")) > 1] {}", new Boolean[] {true, false, false, true, false});
        way1.visitKeys((primitive, key, value) -> way1.put(key, null));
        way1.put("old_ref", "A1");
        way1.put("ref", "A2");
        tagRegex(way1, "way[ref][count(tag_regex(\"ref\", \"i\")) > 1] {}", new Boolean[] {true, false, false, true, true});
    }

    private void tagRegex(Way way, String parserString, Boolean[] expected) throws Exception {
        Selector selector = getParser(parserString).selector();
        Assert.assertEquals(expected[0], selector.matches(new Environment(way)));
        way.put("old_ref", null);
        Assert.assertEquals(expected[1], selector.matches(new Environment(way)));
        way.put("no_match_tag", "false");
        Assert.assertEquals(expected[2], selector.matches(new Environment(way)));
        way.put("old_ref", "A22");
        Assert.assertEquals(expected[3], selector.matches(new Environment(way)));
        way.put("old_ref", null);
        way.put("OLD_REF", "A23");
        Assert.assertEquals(expected[4], selector.matches(new Environment(way)));
    }

    @Test
    void testParentTag() throws Exception {
        Selector c1 = getParser("way[foo] > node[tag(\"foo\")=parent_tag(\"foo\")] {}").child_selector();
        DataSet ds = new DataSet();
        Way w1 = new Way();
        Way w2 = new Way();
        Node n1 = new Node(new LatLon(1, 1));
        Node n2 = new Node(new LatLon(2, 2));
        w1.put("foo", "123");
        w2.put("foo", "123");
        n1.put("foo", "123");
        n2.put("foo", "0");
        ds.addPrimitive(w1);
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        w1.addNode(n1);
        w2.addNode(n2);
        assertTrue(c1.matches(new Environment(n1)));
        assertFalse(c1.matches(new Environment(n2)));
        assertFalse(c1.matches(new Environment(w1)));
        assertFalse(c1.matches(new Environment(w2)));
        n1.put("foo", "0");
        assertFalse(c1.matches(new Environment(n1)));
        n1.put("foo", "123");
        assertTrue(c1.matches(new Environment(n1)));
    }

    /**
     * Test case for {@link Functions#trim_list}
     */
    @Test
    void testTrimList() {
        List<String> trimmed = Functions.trim_list(Arrays.asList(" A1 ", "A2", " A3", "A4 ", ""));
        assertEquals(4, trimmed.size());
        assertEquals("A1", trimmed.get(0));
        assertEquals("A2", trimmed.get(1));
        assertEquals("A3", trimmed.get(2));
        assertEquals("A4", trimmed.get(3));
    }

    @Test
    void testTicket8568() throws Exception {
        MapCSSStyleSource sheet = new MapCSSStyleSource(
                "way { width: 5; }\n" +
                "way[keyA], way[keyB] { width: eval(prop(width)+10); }");
        sheet.loadStyleSource();
        MultiCascade mc = new MultiCascade();
        sheet.apply(mc, OsmUtils.createPrimitive("way foo=bar"), 20, false);
        assertEquals(5.0f, mc.getCascade(null).get("width"));
        sheet.apply(mc, OsmUtils.createPrimitive("way keyA=true"), 20, false);
        assertEquals(15.0, mc.getCascade(null).get("width"));
        sheet.apply(mc, OsmUtils.createPrimitive("way keyB=true"), 20, false);
        assertEquals(15.0, mc.getCascade(null).get("width"));
        sheet.apply(mc, OsmUtils.createPrimitive("way keyA=true keyB=true"), 20, false);
        assertEquals(15.0, mc.getCascade(null).get("width"));
    }

    @Test
    void testTicket8071() throws Exception {
        MapCSSStyleSource sheet = new MapCSSStyleSource(
                "*[rcn_ref], *[name] {text: concat(tag(rcn_ref), \" \", tag(name)); }");
        sheet.loadStyleSource();
        MultiCascade mc = new MultiCascade();
        sheet.apply(mc, OsmUtils.createPrimitive("way name=Foo"), 20, false);
        assertEquals(" Foo", mc.getCascade(null).get("text"));
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15"), 20, false);
        assertEquals("15 ", mc.getCascade(null).get("text"));
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15 name=Foo"), 20, false);
        assertEquals("15 Foo", mc.getCascade(null).get("text"));

        sheet = new MapCSSStyleSource("" +
                "*[rcn_ref], *[name] {text: join(\" - \", tag(rcn_ref), tag(ref), tag(name)); }");
        sheet.loadStyleSource();
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15 ref=1.5 name=Foo"), 20, false);
        assertEquals("15 - 1.5 - Foo", mc.getCascade(null).get("text"));
    }

    @Test
    void testColorNameTicket9191() throws Exception {
        Environment e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        getParser("{color: testcolour1#88DD22}").declaration().instructions.get(0).execute(e);
        Color expected = new Color(0x88DD22);
        assertEquals(expected, e.getCascade(null).get("color"));
    }

    @Test
    void testColorNameTicket9191Alpha() throws Exception {
        Environment e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        getParser("{color: testcolour2#12345678}").declaration().instructions.get(0).execute(e);
        Color expected = new Color(0x12, 0x34, 0x56, 0x78);
        assertEquals(expected, e.getCascade(null).get("color"));
    }

    @Test
    void testColorParsing() throws Exception {
        assertEquals(new Color(0x12, 0x34, 0x56, 0x78), ColorHelper.html2color("#12345678"));
    }

    @Test
    void testChildSelectorGreaterThanSignIsOptional() throws Exception {
        assertEquals(
                getParser("relation[type=route] way[highway]").child_selector().toString(),
                getParser("relation[type=route] > way[highway]").child_selector().toString());
    }

    @Test
    void testSiblingSelector() throws Exception {
        ChildOrParentSelector s1 = (Selector.ChildOrParentSelector) getParser(
                "*[a?][parent_tag(\"highway\")=\"unclassified\"] + *[b?]").child_selector();
        DataSet ds = new DataSet();
        Node n1 = new Node(new LatLon(1, 2));
        n1.put("a", "true");
        Node n2 = new Node(new LatLon(1.1, 2.2));
        n2.put("b", "true");
        Way w = new Way();
        w.put("highway", "unclassified");
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(w);
        w.addNode(n1);
        w.addNode(n2);

        Environment e = new Environment(n2);
        assertTrue(s1.matches(e));
        assertEquals(n2, e.osm);
        assertEquals(n1, e.child);
        assertEquals(w, e.parent);
        assertFalse(s1.matches(new Environment(n1)));
        assertFalse(s1.matches(new Environment(w)));
    }

    @Test
    void testParentTags() throws Exception {
        DataSet ds = new DataSet();
        Node n = new Node(new LatLon(1, 2));
        n.put("foo", "bar");
        Way w1 = new Way();
        w1.put("ref", "x10");
        Way w2 = new Way();
        w2.put("ref", "x2");
        Way w3 = new Way();
        ds.addPrimitive(n);
        ds.addPrimitive(w1);
        ds.addPrimitive(w2);
        ds.addPrimitive(w3);
        w1.addNode(n);
        w2.addNode(n);
        w3.addNode(n);

        MapCSSStyleSource source = new MapCSSStyleSource("node[foo=bar] {refs: join_list(\";\", parent_tags(\"ref\"));}");
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
        Environment e = new Environment(n, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertTrue(source.rules.get(0).matches(e));
        source.rules.get(0).declaration.execute(e);
        assertEquals("x2;x10", e.getCascade(null).get("refs", null, String.class));
    }

    @Test
    void testSort() throws Exception {
        assertEquals(Arrays.asList(new String[] {"alpha", "beta"}), Functions.sort(null, "beta", "alpha"));
        Way way1 = TestUtils.newWay("highway=residential name=Alpha alt_name=Beta ref=\"A9;A8\"", new Node(new LatLon(0.001, 0.001)),
                new Node(new LatLon(0.002, 0.002)));

        MapCSSStyleSource source = new MapCSSStyleSource("way[highway] {sorted: join_list(\",\", sort(tag(\"alt_name\"), tag(\"name\")));}");
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
        Environment e = new Environment(way1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertTrue(source.rules.get(0).matches(e));
        source.rules.get(0).declaration.execute(e);
        assertEquals(Functions.join(null, ",", "Alpha", "Beta"), e.getCascade(null).get("sorted", null, String.class));

        source = new MapCSSStyleSource("way[ref] {sorted: join_list(\",\", sort_list(split(\";\", tag(\"ref\"))));}");
        source.loadStyleSource();
        e = new Environment(way1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertTrue(source.rules.get(0).matches(e));
        source.rules.get(0).declaration.execute(e);
        assertEquals(Functions.join(null, ",", "A8", "A9"), e.getCascade(null).get("sorted", null, String.class));
    }

    @Test
    void testUniqueValues() throws Exception {
        assertEquals(Arrays.asList(new String[] {"alpha", "beta"}),
                Functions.uniq(null, "alpha", "alpha", "alpha", "beta"));
        assertEquals(Arrays.asList(new String[] {"one", "two", "three"}),
                Functions.uniq_list(Arrays.asList(new String[] {"one", "one", "two", "two", "two", "three"})));
    }

    @Test
    void testCountRoles() throws Exception {
        DataSet ds = new DataSet();
        Way way1 = TestUtils.newWay("highway=residential name=1",
                new Node(new LatLon(0, 0)), new Node((new LatLon(0.001, 0.001))));
        for (Node node : way1.getNodes()) {
            ds.addPrimitive(node);
        }
        ds.addPrimitive(way1);

        Relation rel1 = TestUtils.newRelation("type=destination_sign", new RelationMember("", way1));
        ds.addPrimitive(rel1);

        /* Check with empty role and one object */
        Environment e = new Environment(rel1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertEquals(1, Functions.count_roles(e, ""));

        /* Check with non-empty role and one object */
        e = new Environment(rel1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertEquals(0, Functions.count_roles(e, "from"));

        /* Check with empty role and two objects */
        Way way2 = TestUtils.newWay("highway=residential name=2", way1.firstNode(), way1.lastNode());
        ds.addPrimitive(way2);
        rel1.addMember(new RelationMember("", way2));
        e = new Environment(rel1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertEquals(2, Functions.count_roles(e, ""));

        /* Check with non-empty role and two objects */
        rel1.setMember(0, new RelationMember("from", way1));
        e = new Environment(rel1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertEquals(1, Functions.count_roles(e, "from"));

        /* Check with multiple roles */
        assertEquals(1, Functions.count_roles(e, "from", "to"));

        /* Check with non-relation */
        e = new Environment(way1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertEquals(0, Functions.count_roles(e, "from", "to"));

        /* Check with actual call to mapcss functions */
        MapCSSStyleSource source = new MapCSSStyleSource("relation[type=destination_sign] {roles: count_roles(\"from\");}");
        source.loadStyleSource();
        assertEquals(1, source.rules.size());
        e = new Environment(rel1, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        assertTrue(source.rules.get(0).matches(e));
        source.rules.get(0).declaration.execute(e);
        assertEquals((Integer) 1, e.getCascade(null).get("roles", null, Integer.class));
    }

    @Test
    void testSiblingSelectorInterpolation() throws Exception {
        ChildOrParentSelector s1 = (Selector.ChildOrParentSelector) getParser(
                "*[tag(\"addr:housenumber\") > child_tag(\"addr:housenumber\")][regexp_test(\"even|odd\", parent_tag(\"addr:interpolation\"))]" +
                        " + *[addr:housenumber]").child_selector();
        DataSet ds = new DataSet();
        Node n1 = new Node(new LatLon(1, 2));
        n1.put("addr:housenumber", "10");
        Node n2 = new Node(new LatLon(1.1, 2.2));
        n2.put("addr:housenumber", "100");
        Node n3 = new Node(new LatLon(1.2, 2.3));
        n3.put("addr:housenumber", "20");
        Way w = new Way();
        w.put("addr:interpolation", "even");
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(w);
        w.addNode(n1);
        w.addNode(n2);
        w.addNode(n3);

        assertTrue(s1.right.matches(new Environment(n3)));
        assertTrue(s1.left.matches(new Environment(n2).withChild(n3).withParent(w)));
        assertTrue(s1.matches(new Environment(n3)));
        assertFalse(s1.matches(new Environment(n1)));
        assertFalse(s1.matches(new Environment(n2)));
        assertFalse(s1.matches(new Environment(w)));
    }

    @Test
    void testInvalidBaseSelector() throws Exception {
        MapCSSStyleSource css = new MapCSSStyleSource("invalid_base[key=value] {}");
        css.loadStyleSource();
        assertFalse(css.getErrors().isEmpty());
        assertTrue(css.getErrors().iterator().next().toString().contains("Unknown MapCSS base selector invalid_base"));
    }

    @Test
    void testMath() {
        MapCSSStyleSource source = new MapCSSStyleSource("node {" +
                "add: 1 + 2 + 3 + 4;" +
                "mul: 2 * 3 * 5 * 7;" +
                "sub: 0 - 1 - 2 - 3;" +
                "div: 360 / 15;" +
                "neg: -13;" +
                "not: !0;" +
                "null0: -tag(does_not_exist);" +
                "null1: tag(x1) + tag(x2);" +
                "null2: 3 + tag(does_not_exist) + 5;" +
                "rounding: concat(\"ele=\", round(tag(\"ele\")*100)/100);" +
                "}");
        source.loadStyleSource();
        MultiCascade mc = new MultiCascade();
        source.apply(mc, OsmUtils.createPrimitive("node"), 20, false);
        assertEquals(10.0, mc.getCascade(null).get("add"));
        assertEquals(210.0, mc.getCascade(null).get("mul"));
        assertEquals(-6.0, mc.getCascade(null).get("sub"));
        assertEquals(24.0, mc.getCascade(null).get("div"));
        assertEquals(-13.0, mc.getCascade(null).get("neg"));
        assertEquals(true, mc.getCascade(null).get("not"));
        assertNull(mc.getCascade(null).get("null0"));
        assertNull(mc.getCascade(null).get("null1"));
        assertEquals(8.0, mc.getCascade(null).get("null2"));
        assertEquals(8.0, mc.getCascade(null).get("rounding"));
    }

    @Test
    void testMinMaxFunctions() throws Exception {
        MapCSSStyleSource sheet = new MapCSSStyleSource("* {" +
                "min_value: min(tag(x), tag(y), tag(z)); " +
                "max_value: max(tag(x), tag(y), tag(z)); " +
                "max_split: max(split(\";\", tag(widths))); " +
                "}");
        sheet.loadStyleSource();
        MultiCascade mc = new MultiCascade();

        sheet.apply(mc, OsmUtils.createPrimitive("way x=4 y=6 z=8 u=100"), 20, false);
        assertEquals(Float.valueOf(4.0f), mc.getCascade(null).get("min_value", Float.NaN, Float.class));
        assertEquals(Float.valueOf(8.0f), mc.getCascade(null).get("max_value", Float.NaN, Float.class));

        sheet.apply(mc, OsmUtils.createPrimitive("way x=4 y=6 widths=1;2;8;56;3;a"), 20, false);
        assertEquals(Float.valueOf(4f), mc.getCascade(null).get("min_value", -777f, Float.class));
        assertEquals(Float.valueOf(6f), mc.getCascade(null).get("max_value", -777f, Float.class));
        assertEquals(Float.valueOf(56f), mc.getCascade(null).get("max_split", -777f, Float.class));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12549">Bug #12549</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket12549() throws ParseException {
        Condition condition = getParser("[name =~ /^(?i)(?u)fóo$/]").condition(PRIMITIVE);
        assertTrue(condition.applies(getEnvironment("name", "fóo")));
        assertTrue(condition.applies(getEnvironment("name", "fÓo")));
        condition = getParser("[name =~ /^(\\p{Lower})+$/]").condition(PRIMITIVE);
        assertFalse(condition.applies(getEnvironment("name", "fóo")));
        condition = getParser("[name =~ /^(?U)(\\p{Lower})+$/]").condition(PRIMITIVE);
        assertTrue(condition.applies(getEnvironment("name", "fóo")));
        assertFalse(condition.applies(getEnvironment("name", "fÓo")));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17053">Bug #17053</a>.
     */
    @Test
    void testTicket17053() {
        MapCSSStyleSource sheet = new MapCSSStyleSource(
            "way {\n" +
            "    placement_offset: eval(\n" +
            "        cond(prop(\"placement_value\")=\"right_of:1\",eval((prop(lane_width_forward_1)/2)-prop(lane_offset_forward_1)),\n" +
            "        0\n" +
            "        )\n" +
            "       );\n" +
            "}");
        sheet.loadStyleSource();
        assertTrue(sheet.getErrors().isEmpty(), sheet.getErrors()::toString);
    }

    /**
     * Test parsing zoom expressions
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testZoom() throws ParseException {
        assertNotNull(getParser("|z12").zoom());
        assertNotNull(getParser("|z12-").zoom());
        assertNotNull(getParser("|z-12").zoom());
        assertNotNull(getParser("|z12-16").zoom());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18759">Bug #18759</a>.
     */
    @Test
    void testZoomIAE() {
        assertThrows(IllegalArgumentException.class, () -> getParser("|z16-15").zoom());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/16183">Bug #16183</a>.
     */
    @Test
    void testTicket16183() {
        MapCSSStyleSource sheet = new MapCSSStyleSource(
                "area:closed:areaStyle ⧉ area:closed:areaStyle {throwOther: \"xxx\";}");
        sheet.loadStyleSource();
        final String rule = sheet.rules.get(0).toString();
        assertTrue(rule.contains("closed"));
        assertFalse(rule.contains("areaStyle"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19685">Bug #19685</a>.
     */
    @Test
    void testTicket19685() {
        MapCSSStyleSource sheet = new MapCSSStyleSource("node:connection:foobar {}");
        sheet.loadStyleSource();
        assertEquals(1, sheet.getErrors().size());
        assertEquals("Error at line 1, column 17: Invalid pseudo class specified: foobar", sheet.getErrors().iterator().next().getMessage());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20757">Bug #20757</a>.
     */
    @Test
    void testTicket20757() {
        MapCSSStyleSource source = new MapCSSStyleSource("node {name: osm_user_name()}");
        source.loadStyleSource();
        MultiCascade mc = new MultiCascade();
        source.apply(mc, OsmUtils.createPrimitive("node"), 20, false);
        assertNull(mc.getCascade(null).get("name"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20957">Bug #20957</a>.
     */
    @Test
    void testTicket20957() {
        MapCSSStyleSource source = new MapCSSStyleSource("node {fixAdd: concat(\"ele=\", round(tag(\"ele\")*100)/100)}");
        source.loadStyleSource();
        MultiCascade mc = new MultiCascade();
        source.apply(mc, OsmUtils.createPrimitive("node ele=12.123456"), 20, false);
        assertEquals("ele=12.12", mc.getCascade(null).get("fixAdd"));
    }
}
