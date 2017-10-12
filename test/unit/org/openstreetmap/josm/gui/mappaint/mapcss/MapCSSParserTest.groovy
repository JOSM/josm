package org.openstreetmap.josm.gui.mappaint.mapcss

import java.awt.Color

import org.junit.Before
import org.junit.Test
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmUtils
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.MultiCascade
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.ClassCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyValueCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.PseudoClassCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.SimpleKeyValueCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser
import org.openstreetmap.josm.tools.ColorHelper

class MapCSSParserTest {

    protected static Environment getEnvironment(String key, String value) {
        return new Environment(OsmUtils.createPrimitive("way " + key + "=" + value))
    }

    protected static MapCSSParser getParser(String stringToParse) {
        return new MapCSSParser(new StringReader(stringToParse));
    }

    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testKothicStylesheets() throws Exception {
        new MapCSSParser(new URL("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/default.mapcss").openStream(), "UTF-8")
        new MapCSSParser(new URL("https://raw.githubusercontent.com/kothic/kothic/master/src/styles/mapink.mapcss").openStream(), "UTF-8")
    }

    @Test
    public void testDeclarations() {
        getParser("{ opacity: 0.5; color: rgb(1.0, 0.0, 0.0); }").declaration()
        getParser("{ set tag=value; }").declaration() //set a tag
        getParser("{ set tag; }").declaration() // set a tag to 'yes'
        getParser("{ opacity: eval(\"tag('population')/100000\"); }").declaration()
        getParser("{ set width_in_metres=eval(\"tag('lanes')*3\"); }").declaration()
    }

    @Test
    public void testClassCondition() throws Exception {
        def conditions = ((Selector.GeneralSelector) getParser("way[name=X].highway:closed").selector()).conds
        assert conditions.get(0) instanceof SimpleKeyValueCondition
        assert conditions.get(0).applies(getEnvironment("name", "X"))
        assert conditions.get(1) instanceof ClassCondition
        assert conditions.get(2) instanceof PseudoClassCondition
        assert !conditions.get(2).applies(getEnvironment("name", "X"))
    }

    @Test
    public void testPseudoClassCondition() throws Exception {
        def c1 = ((Selector.GeneralSelector) getParser("way!:area-style").selector()).conds.get(0)
        def c2 = ((Selector.GeneralSelector) getParser("way!:areaStyle").selector()).conds.get(0)
        def c3 = ((Selector.GeneralSelector) getParser("way!:area_style").selector()).conds.get(0)
        assert c1.toString() == "!:areaStyle"
        assert c2.toString() == "!:areaStyle"
        assert c3.toString() == "!:areaStyle"
    }

    @Test
    public void testClassMatching() throws Exception {
        def css = new MapCSSStyleSource("" +
                "way[highway=footway] { set .path; color: #FF6644; width: 2; }\n" +
                "way[highway=path]    { set path; color: brown; width: 2; }\n" +
                "way[\"set\"=escape]  {  }\n" +
                "way.path             { text:auto; text-color: green; text-position: line; text-offset: 5; }\n" +
                "way!.path            { color: orange; }\n"
        )
        css.loadStyleSource()
        assert css.getErrors().isEmpty()
        def mc1 = new MultiCascade()
        css.apply(mc1, OsmUtils.createPrimitive("way highway=path"), 1, false);
        assert "green".equals(mc1.getCascade("default").get("text-color", null, String.class))
        assert "brown".equals(mc1.getCascade("default").get("color", null, String.class))
        def mc2 = new MultiCascade()
        css.apply(mc2, OsmUtils.createPrimitive("way highway=residential"), 1, false);
        assert "orange".equals(mc2.getCascade("default").get("color", null, String.class))
        assert mc2.getCascade("default").get("text-color", null, String.class) == null
        def mc3 = new MultiCascade()
        css.apply(mc3, OsmUtils.createPrimitive("way highway=footway"), 1, false);
        assert ColorHelper.html2color("#FF6644").equals(mc3.getCascade("default").get("color", null, Color.class))
    }

    @Test
    public void testEqualCondition() throws Exception {
        def condition = (SimpleKeyValueCondition) getParser("[surface=paved]").condition(Condition.Context.PRIMITIVE)
        assert condition instanceof SimpleKeyValueCondition
        assert "surface".equals(condition.k)
        assert "paved".equals(condition.v)
        assert condition.applies(getEnvironment("surface", "paved"))
        assert !condition.applies(getEnvironment("surface", "unpaved"))
    }

    @Test
    public void testNotEqualCondition() throws Exception {
        def condition = (KeyValueCondition) getParser("[surface!=paved]").condition(Condition.Context.PRIMITIVE)
        assert Op.NEQ.equals(condition.op)
        assert !condition.applies(getEnvironment("surface", "paved"))
        assert condition.applies(getEnvironment("surface", "unpaved"))
    }

    @Test
    public void testRegexCondition() throws Exception {
        def condition = (KeyValueCondition) getParser("[surface=~/paved|unpaved/]").condition(Condition.Context.PRIMITIVE)
        assert Op.REGEX.equals(condition.op)
        assert condition.applies(getEnvironment("surface", "unpaved"))
        assert !condition.applies(getEnvironment("surface", "grass"))
    }

    @Test
    public void testRegexConditionParenthesis() throws Exception {
        def condition = (KeyValueCondition) getParser("[name =~ /^\\(foo\\)/]").condition(Condition.Context.PRIMITIVE)
        assert condition.applies(getEnvironment("name", "(foo)"))
        assert !condition.applies(getEnvironment("name", "foo"))
        assert !condition.applies(getEnvironment("name", "((foo))"))
    }

    @Test
    public void testNegatedRegexCondition() throws Exception {
        def condition = (KeyValueCondition) getParser("[surface!~/paved|unpaved/]").condition(Condition.Context.PRIMITIVE)
        assert Op.NREGEX.equals(condition.op)
        assert !condition.applies(getEnvironment("surface", "unpaved"))
        assert condition.applies(getEnvironment("surface", "grass"))
    }

    @Test
    public void testBeginsEndsWithCondition() throws Exception {
        def condition = (KeyValueCondition) getParser('[foo ^= bar]').condition(Condition.Context.PRIMITIVE)
        assert Op.BEGINS_WITH.equals(condition.op)
        assert condition.applies(getEnvironment("foo", "bar123"))
        assert !condition.applies(getEnvironment("foo", "123bar"))
        assert !condition.applies(getEnvironment("foo", "123bar123"))
        condition = (KeyValueCondition) getParser('[foo $= bar]').condition(Condition.Context.PRIMITIVE)
        assert Op.ENDS_WITH.equals(condition.op)
        assert !condition.applies(getEnvironment("foo", "bar123"))
        assert condition.applies(getEnvironment("foo", "123bar"))
        assert !condition.applies(getEnvironment("foo", "123bar123"))
    }

    @Test
    public void testOneOfCondition() throws Exception {
        def condition = getParser('[vending~=stamps]').condition(Condition.Context.PRIMITIVE)
        assert condition.applies(getEnvironment("vending", "stamps"))
        assert condition.applies(getEnvironment("vending", "bar;stamps;foo"))
        assert !condition.applies(getEnvironment("vending", "every;thing;else"))
        assert !condition.applies(getEnvironment("vending", "or_nothing"))
    }

    @Test
    public void testStandardKeyCondition() throws Exception {
        def c1 = (KeyCondition) getParser("[ highway ]").condition(Condition.Context.PRIMITIVE)
        assert KeyMatchType.EQ.equals(c1.matchType)
        assert c1.applies(getEnvironment("highway", "unclassified"))
        assert !c1.applies(getEnvironment("railway", "rail"))
        def c2 = (KeyCondition) getParser("[\"/slash/\"]").condition(Condition.Context.PRIMITIVE)
        assert KeyMatchType.EQ.equals(c2.matchType)
        assert c2.applies(getEnvironment("/slash/", "yes"))
        assert !c2.applies(getEnvironment("\"slash\"", "no"))
    }

    @Test
    public void testYesNoKeyCondition() throws Exception {
        def c1 = (KeyCondition) getParser("[oneway?]").condition(Condition.Context.PRIMITIVE)
        def c2 = (KeyCondition) getParser("[oneway?!]").condition(Condition.Context.PRIMITIVE)
        def c3 = (KeyCondition) getParser("[!oneway?]").condition(Condition.Context.PRIMITIVE)
        def c4 = (KeyCondition) getParser("[!oneway?!]").condition(Condition.Context.PRIMITIVE)
        def yes = getEnvironment("oneway", "yes")
        def no = getEnvironment("oneway", "no")
        def none = getEnvironment("no-oneway", "foo")
        assert c1.applies(yes)
        assert !c1.applies(no)
        assert !c1.applies(none)
        assert !c2.applies(yes)
        assert c2.applies(no)
        assert !c2.applies(none)
        assert !c3.applies(yes)
        assert c3.applies(no)
        assert c3.applies(none)
        assert c4.applies(yes)
        assert !c4.applies(no)
        assert c4.applies(none)
    }

    @Test
    public void testRegexKeyCondition() throws Exception {
        def c1 = (KeyCondition) getParser("[/.*:(backward|forward)\$/]").condition(Condition.Context.PRIMITIVE)
        assert KeyMatchType.REGEX.equals(c1.matchType)
        assert !c1.applies(getEnvironment("lanes", "3"))
        assert c1.applies(getEnvironment("lanes:forward", "3"))
        assert c1.applies(getEnvironment("lanes:backward", "3"))
        assert !c1.applies(getEnvironment("lanes:foobar", "3"))
    }

    @Test
    public void testNRegexKeyConditionSelector() throws Exception {
        def s1 = getParser("*[sport][tourism != hotel]").selector()
        assert s1.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar")))
        assert !s1.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar tourism=hotel")))
        def s2 = getParser("*[sport][tourism != hotel][leisure !~ /^(sports_centre|stadium|)\$/]").selector()
        assert s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar")))
        assert !s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar tourism=hotel")))
        assert !s2.matches(new Environment(OsmUtils.createPrimitive("node sport=foobar leisure=stadium")))
    }

    @Test
    public void testKeyKeyCondition() throws Exception {
        def c1 = (KeyValueCondition) getParser("[foo = *bar]").condition(Condition.Context.PRIMITIVE)
        def w1 = new Way()
        w1.put("foo", "123")
        w1.put("bar", "456")
        assert !c1.applies(new Environment(w1))
        w1.put("bar", "123")
        assert c1.applies(new Environment(w1))
        def c2 = (KeyValueCondition) getParser("[foo =~ */bar/]").condition(Condition.Context.PRIMITIVE)
        def w2 = new Way(w1)
        w2.put("bar", "[0-9]{3}")
        assert c2.applies(new Environment(w2))
        w2.put("bar", "[0-9]")
        assert c2.applies(new Environment(w2))
        w2.put("bar", "^[0-9]\$")
        assert !c2.applies(new Environment(w2))
    }

    @Test
    public void testParentTag() throws Exception {
        def c1 = getParser("way[foo] > node[tag(\"foo\")=parent_tag(\"foo\")] {}").child_selector()
        def ds = new DataSet()
        def w1 = new Way()
        def w2 = new Way()
        def n1 = new Node(new LatLon(1, 1))
        def n2 = new Node(new LatLon(2, 2))
        w1.put("foo", "123")
        w2.put("foo", "123")
        n1.put("foo", "123")
        n2.put("foo", "0")
        ds.addPrimitive(w1)
        ds.addPrimitive(n1)
        ds.addPrimitive(n2)
        w1.addNode(n1)
        w2.addNode(n2)
        assert c1.matches(new Environment(n1))
        assert !c1.matches(new Environment(n2))
        assert !c1.matches(new Environment(w1))
        assert !c1.matches(new Environment(w2))
        n1.put("foo", "0")
        assert !c1.matches(new Environment(n1))
        n1.put("foo", "123")
        assert c1.matches(new Environment(n1))
    }

    @Test
    public void testTicket8568() throws Exception {
        def sheet = new MapCSSStyleSource("" +
                "way { width: 5; }\n" +
                "way[keyA], way[keyB] { width: eval(prop(width)+10); }")
        sheet.loadStyleSource()
        def mc = new MultiCascade()
        sheet.apply(mc, OsmUtils.createPrimitive("way foo=bar"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 5
        sheet.apply(mc, OsmUtils.createPrimitive("way keyA=true"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 15
        sheet.apply(mc, OsmUtils.createPrimitive("way keyB=true"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 15
        sheet.apply(mc, OsmUtils.createPrimitive("way keyA=true keyB=true"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 15
    }

    @Test
    public void testTicket8071() throws Exception {
        def sheet = new MapCSSStyleSource("" +
                "*[rcn_ref], *[name] {text: concat(tag(rcn_ref), \" \", tag(name)); }")
        sheet.loadStyleSource()
        def mc = new MultiCascade()
        sheet.apply(mc, OsmUtils.createPrimitive("way name=Foo"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == " Foo"
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == "15 "
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15 name=Foo"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == "15 Foo"

        sheet = new MapCSSStyleSource("" +
                "*[rcn_ref], *[name] {text: join(\" - \", tag(rcn_ref), tag(ref), tag(name)); }")
        sheet.loadStyleSource()
        sheet.apply(mc, OsmUtils.createPrimitive("way rcn_ref=15 ref=1.5 name=Foo"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == "15 - 1.5 - Foo"
    }

    @Test
    public void testColorNameTicket9191() throws Exception {
        def e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null)
        getParser("{color: testcolour1#88DD22}").declaration().instructions.get(0).execute(e)
        def expected = new Color(0x88DD22)
        assert e.getCascade(Environment.DEFAULT_LAYER).get("color") == expected
    }

    @Test
    public void testColorNameTicket9191Alpha() throws Exception {
        def e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null)
        getParser("{color: testcolour2#12345678}").declaration().instructions.get(0).execute(e)
        def expected = new Color(0x12, 0x34, 0x56, 0x78)
        assert e.getCascade(Environment.DEFAULT_LAYER).get("color") == expected
    }

    @Test
    public void testColorParsing() throws Exception {
        assert ColorHelper.html2color("#12345678") == new Color(0x12, 0x34, 0x56, 0x78)
    }

    @Test
    public void testChildSelectorGreaterThanSignIsOptional() throws Exception {
        assert getParser("relation[type=route] way[highway]").child_selector().toString() ==
                getParser("relation[type=route] > way[highway]").child_selector().toString()
    }

    @Test
    public void testSiblingSelector() throws Exception {
        def s1 = (Selector.ChildOrParentSelector) getParser("*[a?][parent_tag(\"highway\")=\"unclassified\"] + *[b?]").child_selector()
        def ds = new DataSet()
        def n1 = new org.openstreetmap.josm.data.osm.Node(new LatLon(1, 2))
        n1.put("a", "true")
        def n2 = new org.openstreetmap.josm.data.osm.Node(new LatLon(1.1, 2.2))
        n2.put("b", "true")
        def w = new Way()
        w.put("highway", "unclassified")
        ds.addPrimitive(n1)
        ds.addPrimitive(n2)
        ds.addPrimitive(w)
        w.addNode(n1)
        w.addNode(n2)

        def e = new Environment(n2)
        assert s1.matches(e)
        assert e.osm == n2
        assert e.child == n1
        assert e.parent == w
        assert !s1.matches(new Environment(n1))
        assert !s1.matches(new Environment(w))
    }

    @Test
    public void testParentTags() throws Exception {
        def ds = new DataSet()
        def n = new org.openstreetmap.josm.data.osm.Node(new LatLon(1, 2))
        n.put("foo", "bar")
        def w1 = new Way()
        w1.put("ref", "x10")
        def w2 = new Way()
        w2.put("ref", "x2")
        def w3 = new Way()
        ds.addPrimitive(n)
        ds.addPrimitive(w1)
        ds.addPrimitive(w2)
        ds.addPrimitive(w3)
        w1.addNode(n)
        w2.addNode(n)
        w3.addNode(n)

        MapCSSStyleSource source = new MapCSSStyleSource("node[foo=bar] {refs: join_list(\";\", parent_tags(\"ref\"));}")
        source.loadStyleSource()
        assert source.rules.size() == 1
        def e = new Environment(n, new MultiCascade(), Environment.DEFAULT_LAYER, null)
        assert source.rules.get(0).selector.matches(e)
        source.rules.get(0).declaration.execute(e)
        assert e.getCascade(Environment.DEFAULT_LAYER).get("refs", null, String.class) == "x2;x10"
    }

    @Test
    public void testSiblingSelectorInterpolation() throws Exception {
        def s1 = (Selector.ChildOrParentSelector) getParser(
                "*[tag(\"addr:housenumber\") > child_tag(\"addr:housenumber\")][regexp_test(\"even|odd\", parent_tag(\"addr:interpolation\"))]" +
                        " + *[addr:housenumber]").child_selector()
        def ds = new DataSet()
        def n1 = new org.openstreetmap.josm.data.osm.Node(new LatLon(1, 2))
        n1.put("addr:housenumber", "10")
        def n2 = new org.openstreetmap.josm.data.osm.Node(new LatLon(1.1, 2.2))
        n2.put("addr:housenumber", "100")
        def n3 = new org.openstreetmap.josm.data.osm.Node(new LatLon(1.2, 2.3))
        n3.put("addr:housenumber", "20")
        def w = new Way()
        w.put("addr:interpolation", "even")
        ds.addPrimitive(n1)
        ds.addPrimitive(n2)
        ds.addPrimitive(n3)
        ds.addPrimitive(w)
        w.addNode(n1)
        w.addNode(n2)
        w.addNode(n3)

        assert s1.right.matches(new Environment(n3))
        assert s1.left.matches(new Environment(n2).withChild(n3).withParent(w))
        assert s1.matches(new Environment(n3))
        assert !s1.matches(new Environment(n1))
        assert !s1.matches(new Environment(n2))
        assert !s1.matches(new Environment(w))
    }

    @Test
    public void testInvalidBaseSelector() throws Exception {
        def css = new MapCSSStyleSource("invalid_base[key=value] {}")
        css.loadStyleSource()
        assert !css.getErrors().isEmpty()
        assert css.getErrors().iterator().next().toString().contains("Unknown MapCSS base selector invalid_base")
    }

    @Test
    public void testMinMaxFunctions() throws Exception {
        def sheet = new MapCSSStyleSource("* {" +
                "min_value: min(tag(x), tag(y), tag(z)); " +
                "max_value: max(tag(x), tag(y), tag(z)); " +
                "max_split: max(split(\";\", tag(widths))); " +
                "}")
        sheet.loadStyleSource()
        def mc = new MultiCascade()

        sheet.apply(mc, OsmUtils.createPrimitive("way x=4 y=6 z=8 u=100"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("min_value", Float.NaN, Float.class) == 4.0f
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("max_value", Float.NaN, Float.class) == 8.0f

        sheet.apply(mc, OsmUtils.createPrimitive("way x=4 y=6 widths=1;2;8;56;3;a"), 20, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("min_value", -777f, Float.class) == 4
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("max_value", -777f, Float.class) == 6
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("max_split", -777f, Float.class) == 56
    }

    @Test
    public void testTicket12549() throws Exception {
        def condition = getParser("[name =~ /^(?i)(?u)fóo\$/]").condition(Condition.Context.PRIMITIVE)
        assert condition.applies(new Environment(OsmUtils.createPrimitive("way name=fóo")))
        assert condition.applies(new Environment(OsmUtils.createPrimitive("way name=fÓo")))
        condition = getParser("[name =~ /^(\\p{Lower})+\$/]").condition(Condition.Context.PRIMITIVE)
        assert !condition.applies(new Environment(OsmUtils.createPrimitive("way name=fóo")))
        condition = getParser("[name =~ /^(?U)(\\p{Lower})+\$/]").condition(Condition.Context.PRIMITIVE)
        assert condition.applies(new Environment(OsmUtils.createPrimitive("way name=fóo")))
        assert !condition.applies(new Environment(OsmUtils.createPrimitive("way name=fÓo")))
    }
}
