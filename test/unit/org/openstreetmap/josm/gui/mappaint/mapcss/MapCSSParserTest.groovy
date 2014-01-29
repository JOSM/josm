package org.openstreetmap.josm.gui.mappaint.mapcss

import org.junit.Before
import org.junit.Test
import org.openstreetmap.TestUtils
import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.Preferences
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.MultiCascade
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser
import org.openstreetmap.josm.tools.ColorHelper
import org.openstreetmap.josm.tools.Utils

import java.awt.Color

class MapCSSParserTest {

    protected static OsmPrimitive getPrimitive(String key, String value) {
        def w = new Way()
        w.put(key, value)
        return w
    }

    protected static Environment getEnvironment(String key, String value) {
        return new Environment().withPrimitive(getPrimitive(key, value))
    }

    protected static MapCSSParser getParser(String stringToParse) {
        return new MapCSSParser(new StringReader(stringToParse));
    }

    @Before
    public void setUp() throws Exception {
        Main.pref = new Preferences()
    }

    @Test
    public void testKothicStylesheets() throws Exception {
        new MapCSSParser(new URL("http://kothic.googlecode.com/hg/src/styles/default.mapcss").openStream(), "UTF-8")
        new MapCSSParser(new URL("http://kothic.googlecode.com/hg/src/styles/mapink.mapcss").openStream(), "UTF-8")
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
        assert conditions.get(0) instanceof Condition.KeyValueCondition
        assert conditions.get(0).applies(getEnvironment("name", "X"))
        assert conditions.get(1) instanceof Condition.ClassCondition
        assert conditions.get(2) instanceof Condition.PseudoClassCondition
    }

    @Test
    public void testClassMatching() throws Exception {
        def css = new MapCSSStyleSource("")
        getParser("" +
                "way[highway=footway] { set .path; color: #FF6644; width: 2; }\n" +
                "way[highway=path]    { set path; color: brown; width: 2; }\n" +
                "way[\"set\"=escape]  {  }\n" +
                "way.path             { text:auto; text-color: green; text-position: line; text-offset: 5; }\n" +
                "way!.path            { color: orange; }\n"
        ).sheet(css)
        assert css.getErrors().isEmpty()
        def mc1 = new MultiCascade()
        css.apply(mc1, getPrimitive("highway", "path"), 1, null, false);
        assert "green".equals(mc1.getCascade("default").get("text-color", null, String.class))
        assert "brown".equals(mc1.getCascade("default").get("color", null, String.class))
        def mc2 = new MultiCascade()
        css.apply(mc2, getPrimitive("highway", "residential"), 1, null, false);
        assert "orange".equals(mc2.getCascade("default").get("color", null, String.class))
        assert mc2.getCascade("default").get("text-color", null, String.class) == null
        def mc3 = new MultiCascade()
        css.apply(mc3, getPrimitive("highway", "footway"), 1, null, false);
        assert ColorHelper.html2color("#FF6644").equals(mc3.getCascade("default").get("color", null, Color.class))
    }

    @Test
    public void testEqualCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) getParser("[surface=paved]").condition(Condition.Context.PRIMITIVE)
        assert condition instanceof Condition.KeyValueCondition
        assert Condition.Op.EQ.equals(condition.op)
        assert "surface".equals(condition.k)
        assert "paved".equals(condition.v)
        assert condition.applies(getEnvironment("surface", "paved"))
        assert !condition.applies(getEnvironment("surface", "unpaved"))
    }

    @Test
    public void testNotEqualCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) getParser("[surface!=paved]").condition(Condition.Context.PRIMITIVE)
        assert Condition.Op.NEQ.equals(condition.op)
        assert !condition.applies(getEnvironment("surface", "paved"))
        assert condition.applies(getEnvironment("surface", "unpaved"))
    }

    @Test
    public void testRegexCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) getParser("[surface=~/paved|unpaved/]").condition(Condition.Context.PRIMITIVE)
        assert Condition.Op.REGEX.equals(condition.op)
        assert condition.applies(getEnvironment("surface", "unpaved"))
        assert !condition.applies(getEnvironment("surface", "grass"))
    }

    @Test
    public void testNegatedRegexCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) getParser("[surface!~/paved|unpaved/]").condition(Condition.Context.PRIMITIVE)
        assert Condition.Op.NREGEX.equals(condition.op)
        assert !condition.applies(getEnvironment("surface", "unpaved"))
        assert condition.applies(getEnvironment("surface", "grass"))
    }

    @Test
    public void testStandardKeyCondition() throws Exception {
        def c1 = (Condition.KeyCondition) getParser("[ highway ]").condition(Condition.Context.PRIMITIVE)
        assert c1.matchType == null
        assert c1.applies(getEnvironment("highway", "unclassified"))
        assert !c1.applies(getEnvironment("railway", "rail"))
        def c2 = (Condition.KeyCondition) getParser("[\"/slash/\"]").condition(Condition.Context.PRIMITIVE)
        assert c2.matchType == null
        assert c2.applies(getEnvironment("/slash/", "yes"))
        assert !c2.applies(getEnvironment("\"slash\"", "no"))
    }

    @Test
    public void testYesNoKeyCondition() throws Exception {
        def c1 = (Condition.KeyCondition) getParser("[oneway?]").condition(Condition.Context.PRIMITIVE)
        def c2 = (Condition.KeyCondition) getParser("[oneway?!]").condition(Condition.Context.PRIMITIVE)
        def c3 = (Condition.KeyCondition) getParser("[!oneway?]").condition(Condition.Context.PRIMITIVE)
        def c4 = (Condition.KeyCondition) getParser("[!oneway?!]").condition(Condition.Context.PRIMITIVE)
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
        def c1 = (Condition.KeyCondition) getParser("[/.*:(backward|forward)\$/]").condition(Condition.Context.PRIMITIVE)
        assert Condition.KeyMatchType.REGEX.equals(c1.matchType)
        assert !c1.applies(getEnvironment("lanes", "3"))
        assert c1.applies(getEnvironment("lanes:forward", "3"))
        assert c1.applies(getEnvironment("lanes:backward", "3"))
        assert !c1.applies(getEnvironment("lanes:foobar", "3"))
    }

    @Test
    public void testKeyKeyCondition() throws Exception {
        def c1 = (Condition.KeyValueCondition) getParser("[foo = *bar]").condition(Condition.Context.PRIMITIVE)
        def w1 = new Way()
        w1.put("foo", "123")
        w1.put("bar", "456")
        assert !c1.applies(new Environment().withPrimitive(w1))
        w1.put("bar", "123")
        assert c1.applies(new Environment().withPrimitive(w1))
        def c2 = (Condition.KeyValueCondition) getParser("[foo =~ */bar/]").condition(Condition.Context.PRIMITIVE)
        def w2 = new Way(w1)
        w2.put("bar", "[0-9]{3}")
        assert c2.applies(new Environment().withPrimitive(w2))
        w2.put("bar", "[0-9]")
        assert c2.applies(new Environment().withPrimitive(w2))
        w2.put("bar", "^[0-9]\$")
        assert !c2.applies(new Environment().withPrimitive(w2))
    }

    @Test
    public void testTicket8568() throws Exception {
        def sheet = new MapCSSStyleSource("")
        getParser("" +
                "way { width: 5; }\n" +
                "way[keyA], way[keyB] { width: eval(prop(width)+10); }").sheet(sheet)
        def mc = new MultiCascade()
        sheet.apply(mc, TestUtils.createPrimitive("way foo=bar"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 5
        sheet.apply(mc, TestUtils.createPrimitive("way keyA=true"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 15
        sheet.apply(mc, TestUtils.createPrimitive("way keyB=true"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 15
        sheet.apply(mc, TestUtils.createPrimitive("way keyA=true keyB=true"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("width") == 15
    }

    @Test
    public void testTicket80711() throws Exception {
        def sheet = new MapCSSStyleSource("")
        getParser("*[rcn_ref], *[name] {text: concat(tag(rcn_ref), \" \", tag(name)); }").sheet(sheet)
        def mc = new MultiCascade()
        sheet.apply(mc, TestUtils.createPrimitive("way name=Foo"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == " Foo"
        sheet.apply(mc, TestUtils.createPrimitive("way rcn_ref=15"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == "15 "
        sheet.apply(mc, TestUtils.createPrimitive("way rcn_ref=15 name=Foo"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == "15 Foo"

        sheet = new MapCSSStyleSource("")
        getParser("*[rcn_ref], *[name] {text: join(\" - \", tag(rcn_ref), tag(ref), tag(name)); }").sheet(sheet)
        sheet.apply(mc, TestUtils.createPrimitive("way rcn_ref=15 ref=1.5 name=Foo"), 20, null, false)
        assert mc.getCascade(Environment.DEFAULT_LAYER).get("text") == "15 - 1.5 - Foo"
    }

    @Test
    public void testColorNameTicket9191() throws Exception {
        def e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null)
        getParser("{color: testcolour1#88DD22}").declaration().get(0).execute(e)
        def expected = new Color(0x88DD22)
        assert e.getCascade(Environment.DEFAULT_LAYER).get("color") == expected
        assert Main.pref.getDefaultColor("mappaint.mapcss.testcolour1") == expected
    }

    @Test
    public void testColorNameTicket9191Alpha() throws Exception {
        def e = new Environment(null, new MultiCascade(), Environment.DEFAULT_LAYER, null)
        getParser("{color: testcolour2#12345678}").declaration().get(0).execute(e)
        def expected = new Color(0x12, 0x34, 0x56, 0x78)
        assert e.getCascade(Environment.DEFAULT_LAYER).get("color") == expected
        assert Main.pref.getDefaultColor("mappaint.mapcss.testcolour2") == expected
    }

    @Test
    public void testColorParsing() throws Exception {
        assert ColorHelper.html2color("#12345678") == new Color(0x12, 0x34, 0x56, 0x78)
    }
}
