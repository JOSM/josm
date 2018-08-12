// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.StringReader;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.ClassCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.PseudoClassCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.SimpleKeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ColorHelper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MapCSSParser}.
 */
public class MapCSSParserTest {

    protected static Environment getEnvironment(String key, String value) {
        return new Environment(OsmUtils.createPrimitive("way " + key + "=" + value));
    }

    protected static MapCSSParser getParser(String stringToParse) {
        return new MapCSSParser(new StringReader(stringToParse));
    }

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    @Test
    public void testDeclarations() throws Exception {
        getParser("{ opacity: 0.5; color: rgb(1.0, 0.0, 0.0); }").declaration();
        getParser("{ set tag=value; }").declaration(); //set a tag
        getParser("{ set tag; }").declaration(); // set a tag to 'yes'
        getParser("{ opacity: eval(\"tag('population')/100000\"); }").declaration();
        getParser("{ set width_in_metres=eval(\"tag('lanes')*3\"); }").declaration();
    }

    @Test
    public void testClassCondition() throws Exception {
        List<Condition> conditions = ((Selector.GeneralSelector) getParser("way[name=X].highway:closed").selector()).conds;
        assertTrue(conditions.get(0) instanceof SimpleKeyValueCondition);
        assertTrue(conditions.get(0).applies(getEnvironment("name", "X")));
        assertTrue(conditions.get(1) instanceof ClassCondition);
        assertTrue(conditions.get(2) instanceof PseudoClassCondition);
        assertFalse(conditions.get(2).applies(getEnvironment("name", "X")));
    }

    @Test
    public void testPseudoClassCondition() throws Exception {
        Condition c1 = ((Selector.GeneralSelector) getParser("way!:area-style").selector()).conds.get(0);
        Condition c2 = ((Selector.GeneralSelector) getParser("way!:areaStyle").selector()).conds.get(0);
        Condition c3 = ((Selector.GeneralSelector) getParser("way!:area_style").selector()).conds.get(0);
        assertEquals("!:areaStyle", c1.toString());
        assertEquals("!:areaStyle", c2.toString());
        assertEquals("!:areaStyle", c3.toString());
    }

    @Test
    public void testClassMatching() throws Exception {
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
        assertEquals("green", mc1.getCascade("default").get("text-color", null, String.class));
        assertEquals("brown", mc1.getCascade("default").get("color", null, String.class));
        MultiCascade mc2 = new MultiCascade();
        css.apply(mc2, OsmUtils.createPrimitive("way highway=residential"), 1, false);
        assertEquals("orange", mc2.getCascade("default").get("color", null, String.class));
        assertNull(mc2.getCascade("default").get("text-color", null, String.class));
        MultiCascade mc3 = new MultiCascade();
        css.apply(mc3, OsmUtils.createPrimitive("way highway=footway"), 1, false);
        assertEquals(ColorHelper.html2color("#FF6644"), mc3.getCascade("default").get("color", null, Color.class));
    }

    @Test
    public void testEqualCondition() throws Exception {
        Condition condition = getParser("[surface=paved]").condition(Condition.Context.PRIMITIVE);
        assertTrue(condition instanceof SimpleKeyValueCondition);
        assertEquals("surface", ((SimpleKeyValueCondition) condition).k);
        assertEquals("paved", ((SimpleKeyValueCondition) condition).v);
        assertTrue(condition.applies(getEnvironment("surface", "paved")));
        assertFalse(condition.applies(getEnvironment("surface", "unpaved")));
    }

    @Test
    public void testNotEqualCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[surface!=paved]").condition(Condition.Context.PRIMITIVE);
        assertEquals(Op.NEQ, condition.op);
        assertFalse(condition.applies(getEnvironment("surface", "paved")));
        assertTrue(condition.applies(getEnvironment("surface", "unpaved")));
    }

    @Test
    public void testRegexCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[surface=~/paved|unpaved/]").condition(Condition.Context.PRIMITIVE);
        assertEquals(Op.REGEX, condition.op);
        assertTrue(condition.applies(getEnvironment("surface", "unpaved")));
        assertFalse(condition.applies(getEnvironment("surface", "grass")));
    }

    @Test
    public void testRegexConditionParenthesis() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[name =~ /^\\(foo\\)/]").condition(Condition.Context.PRIMITIVE);
        assertTrue(condition.applies(getEnvironment("name", "(foo)")));
        assertFalse(condition.applies(getEnvironment("name", "foo")));
        assertFalse(condition.applies(getEnvironment("name", "((foo))")));
    }

    @Test
    public void testNegatedRegexCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[surface!~/paved|unpaved/]").condition(Condition.Context.PRIMITIVE);
        assertEquals(Op.NREGEX, condition.op);
        assertFalse(condition.applies(getEnvironment("surface", "unpaved")));
        assertTrue(condition.applies(getEnvironment("surface", "grass")));
    }

    @Test
    public void testBeginsEndsWithCondition() throws Exception {
        KeyValueCondition condition = (KeyValueCondition) getParser("[foo ^= bar]").condition(Condition.Context.PRIMITIVE);
        assertEquals(Op.BEGINS_WITH, condition.op);
        assertTrue(condition.applies(getEnvironment("foo", "bar123")));
        assertFalse(condition.applies(getEnvironment("foo", "123bar")));
        assertFalse(condition.applies(getEnvironment("foo", "123bar123")));
        condition = (KeyValueCondition) getParser("[foo $= bar]").condition(Condition.Context.PRIMITIVE);
        assertEquals(Op.ENDS_WITH, condition.op);
        assertFalse(condition.applies(getEnvironment("foo", "bar123")));
        assertTrue(condition.applies(getEnvironment("foo", "123bar")));
        assertFalse(condition.applies(getEnvironment("foo", "123bar123")));
    }

    @Test
    public void testOneOfCondition() throws Exception {
        Condition condition = getParser("[vending~=stamps]").condition(Condition.Context.PRIMITIVE);
        assertTrue(condition.applies(getEnvironment("vending", "stamps")));
        assertTrue(condition.applies(getEnvironment("vending", "bar;stamps;foo")));
        assertFalse(condition.applies(getEnvironment("vending", "every;thing;else")));
        assertFalse(condition.applies(getEnvironment("vending", "or_nothing")));
    }

    @Test
    public void testStandardKeyCondition() throws Exception {
        KeyCondition c1 = (KeyCondition) getParser("[ highway ]").condition(Condition.Context.PRIMITIVE);
        assertEquals(KeyMatchType.EQ, c1.matchType);
        assertTrue(c1.applies(getEnvironment("highway", "unclassified")));
        assertFalse(c1.applies(getEnvironment("railway", "rail")));
        KeyCondition c2 = (KeyCondition) getParser("[\"/slash/\"]").condition(Condition.Context.PRIMITIVE);
        assertEquals(KeyMatchType.EQ, c2.matchType);
        assertTrue(c2.applies(getEnvironment("/slash/", "yes")));
        assertFalse(c2.applies(getEnvironment("\"slash\"", "no")));
    }

    @Test
    public void testYesNoKeyCondition() throws Exception {
        KeyCondition c1 = (KeyCondition) getParser("[oneway?]").condition(Condition.Context.PRIMITIVE);
        KeyCondition c2 = (KeyCondition) getParser("[oneway?!]").condition(Condition.Context.PRIMITIVE);
        KeyCondition c3 = (KeyCondition) getParser("[!oneway?]").condition(Condition.Context.PRIMITIVE);
        KeyCondition c4 = (KeyCondition) getParser("[!oneway?!]").condition(Condition.Context.PRIMITIVE);
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
    public void testRegexKeyCondition() throws Exception {
        KeyCondition c1 = (KeyCondition) getParser("[/.*:(backward|forward)$/]").condition(Condition.Context.PRIMITIVE);
        assertEquals(KeyMatchType.REGEX, c1.matchType);
        assertFalse(c1.applies(getEnvironment("lanes", "3")));
        assertTrue(c1.applies(getEnvironment("lanes:forward", "3")));
        assertTrue(c1.applies(getEnvironment("lanes:backward", "3")));
        assertFalse(c1.applies(getEnvironment("lanes:foobar", "3")));
    }

    @Test
    public void testNRegexKeyConditionSelector() throws Exception {
        Selector s1 = getParser("*[sport][tourism != hotel]").selector();
        assertTrue(s1.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar"))));
        assertFalse(s1.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar tourism=hotel"))));
        Selector s2 = getParser("*[sport][tourism != hotel][leisure !~ /^(sports_centre|stadium|)$/]").selector();
        assertTrue(s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar"))));
        assertFalse(s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar tourism=hotel"))));
        assertFalse(s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar leisure=stadium"))));
    }

    @Test
    public void testKeyKeyCondition() throws Exception {
        KeyValueCondition c1 = (KeyValueCondition) getParser("[foo = *bar]").condition(Condition.Context.PRIMITIVE);
        Way w1 = new Way();
        w1.put("foo", "123");
        w1.put("bar", "456");
        assertFalse(c1.applies(new Environment(w1)));
        w1.put("bar", "123");
        assertTrue(c1.applies(new Environment(w1)));
        KeyValueCondition c2 = (KeyValueCondition) getParser("[foo =~ */bar/]").condition(Condition.Context.PRIMITIVE);
        Way w2 = new Way(w1);
        w2.put("bar", "[0-9]{3}");
        assertTrue(c2.applies(new Environment(w2)));
        w2.put("bar", "[0-9]");
        assertTrue(c2.applies(new Environment(w2)));
        w2.put("bar", "^[0-9]$");
        assertFalse(c2.applies(new Environment(w2)));
    }

    @Test
    public void testParentTag() throws Exception {
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

    @Test
    public void testTicket8568() throws Exception {
        MapCSSStyleSource sheet = new MapCSSStyleSource(
                "way { width: 5; }\n" +
                "way[keyA], way[keyB] { width: eval(prop(width)+10); }");
        sheet.loadStyleSource();
        MultiCascade mc = new MultiCascade();
        sheet.apply(mc, OsmUtils.createPrimitive("way foo=bar"), 20, false);
        assertEquals(Float.valueOf(5f), mc.getCascade(Environment.DEFAULT_LAYER).get("width"));
        sheet.apply(mc, OsmUtils.createPrimitive("way keyA=true"), 20, false);
        assertEquals(Float.valueOf(15f), mc.getCascade(Environment.DEFAULT_LAYER).get("width"));
        sheet.apply(mc, OsmUtils.createPrimitive("way keyB=true"), 20, false);
        assertEquals(Float.valueOf(15f), mc.getCascade(Environment.DEFAULT_LAYER).get("width"));
        sheet.apply(mc, OsmUtils.createPrimitive("way keyA=true keyB=true"), 20, false);
        assertEquals(Float.valueOf(15f), mc.getCascade(Environment.DEFAULT_LAYER).get("width"));
    }

    @Test
    public void testTicket8071() throws Exception {
        MapCSSStyleSource sheet = new MapCSSStyleSource(
                "*[rcn_ref], *[name] {text: concat(tag(rcn_ref), \" \", tag(name)); }");
        sheet.loadStyleSource();
        MultiCascade mc = new MultiCascade();
        sheet.apply(mc, OsmUtils.createPrimitive("way name=Foo"), 20, false);
        assertEquals(" Foo", mc.getCascade(Environment.DEFAULT_LAYER).get("text"));
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15"), 20, false);
        assertEquals("15 ", mc.getCascade(Environment.DEFAULT_LAYER).get("text"));
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15 name=Foo"), 20, false);
        assertEquals("15 Foo", mc.getCascade(Environment.DEFAULT_LAYER).get("text"));

        sheet = new MapCSSStyleSource("" +
                "*[rcn_ref], *[name] {text: join(\" - \", tag(rcn_ref), tag(ref), tag(name)); }");
        sheet.loadStyleSource();
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15 ref=1.5 name=Foo"), 20, false);
        assertEquals("15 - 1.5 - Foo", mc.getCascade(Environment.DEFAULT_LAYER).get("text"));
    }

    @Test
    public void testColorNameTicket9191() throws Exception {
        Environment e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        getParser("{color: testcolour1#88DD22}").declaration().instructions.get(0).execute(e);
        Color expected = new Color(0x88DD22);
        assertEquals(expected, e.getCascade(Environment.DEFAULT_LAYER).get("color"));
    }

    @Test
    public void testColorNameTicket9191Alpha() throws Exception {
        Environment e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        getParser("{color: testcolour2#12345678}").declaration().instructions.get(0).execute(e);
        Color expected = new Color(0x12, 0x34, 0x56, 0x78);
        assertEquals(expected, e.getCascade(Environment.DEFAULT_LAYER).get("color"));
    }

    @Test
    public void testColorParsing() throws Exception {
        assertEquals(new Color(0x12, 0x34, 0x56, 0x78), ColorHelper.html2color("#12345678"));
    }

    @Test
    public void testChildSelectorGreaterThanSignIsOptional() throws Exception {
        assertEquals(
                getParser("relation[type=route] way[highway]").child_selector().toString(),
                getParser("relation[type=route] > way[highway]").child_selector().toString());
    }

    @Test
    public void testSiblingSelector() throws Exception {
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
    public void testParentTags() throws Exception {
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
        assertTrue(source.rules.get(0).selector.matches(e));
        source.rules.get(0).declaration.execute(e);
        assertEquals("x2;x10", e.getCascade(Environment.DEFAULT_LAYER).get("refs", null, String.class));
    }

    @Test
    public void testSiblingSelectorInterpolation() throws Exception {
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
    public void testInvalidBaseSelector() throws Exception {
        MapCSSStyleSource css = new MapCSSStyleSource("invalid_base[key=value] {}");
        css.loadStyleSource();
        assertFalse(css.getErrors().isEmpty());
        assertTrue(css.getErrors().iterator().next().toString().contains("Unknown MapCSS base selector invalid_base"));
    }

    @Test
    public void testMinMaxFunctions() throws Exception {
        MapCSSStyleSource sheet = new MapCSSStyleSource("* {" +
                "min_value: min(tag(x), tag(y), tag(z)); " +
                "max_value: max(tag(x), tag(y), tag(z)); " +
                "max_split: max(split(\";\", tag(widths))); " +
                "}");
        sheet.loadStyleSource();
        MultiCascade mc = new MultiCascade();

        sheet.apply(mc, OsmUtils.createPrimitive("way x=4 y=6 z=8 u=100"), 20, false);
        assertEquals(Float.valueOf(4.0f), mc.getCascade(Environment.DEFAULT_LAYER).get("min_value", Float.NaN, Float.class));
        assertEquals(Float.valueOf(8.0f), mc.getCascade(Environment.DEFAULT_LAYER).get("max_value", Float.NaN, Float.class));

        sheet.apply(mc, OsmUtils.createPrimitive("way x=4 y=6 widths=1;2;8;56;3;a"), 20, false);
        assertEquals(Float.valueOf(4f), mc.getCascade(Environment.DEFAULT_LAYER).get("min_value", -777f, Float.class));
        assertEquals(Float.valueOf(6f), mc.getCascade(Environment.DEFAULT_LAYER).get("max_value", -777f, Float.class));
        assertEquals(Float.valueOf(56f), mc.getCascade(Environment.DEFAULT_LAYER).get("max_split", -777f, Float.class));
    }

    @Test
    public void testTicket12549() throws Exception {
        Condition condition = getParser("[name =~ /^(?i)(?u)fóo$/]").condition(Condition.Context.PRIMITIVE);
        assertTrue(condition.applies(new Environment(OsmUtils.createPrimitive("way name=fóo"))));
        assertTrue(condition.applies(new Environment(OsmUtils.createPrimitive("way name=fÓo"))));
        condition = getParser("[name =~ /^(\\p{Lower})+$/]").condition(Condition.Context.PRIMITIVE);
        assertFalse(condition.applies(new Environment(OsmUtils.createPrimitive("way name=fóo"))));
        condition = getParser("[name =~ /^(?U)(\\p{Lower})+$/]").condition(Condition.Context.PRIMITIVE);
        assertTrue(condition.applies(new Environment(OsmUtils.createPrimitive("way name=fóo"))));
        assertFalse(condition.applies(new Environment(OsmUtils.createPrimitive("way name=fÓo"))));
    }
}
