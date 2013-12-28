package org.openstreetmap.josm.gui.mappaint.mapcss

import org.junit.Before
import org.junit.Test
import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.Preferences
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser

class MapCSSParserTest {

    protected static OsmPrimitive getPrimitive(String key, String value) {
        def w = new Way()
        w.put(key, value)
        return w
    }

    protected static Environment getEnvironment(String key, String value) {
        return new Environment().withPrimitive(getPrimitive(key, value))
    }

    @Before
    public void setUp() throws Exception {
        Main.pref = new Preferences()
    }

    @Test
    public void testEqualCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface=paved]")).condition(Condition.Context.PRIMITIVE)
        assert condition instanceof Condition.KeyValueCondition
        assert Condition.Op.EQ.equals(condition.op)
        assert "surface".equals(condition.k)
        assert "paved".equals(condition.v)
        assert condition.applies(getEnvironment("surface", "paved"))
        assert !condition.applies(getEnvironment("surface", "unpaved"))
    }

    @Test
    public void testNotEqualCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface!=paved]")).condition(Condition.Context.PRIMITIVE)
        assert Condition.Op.NEQ.equals(condition.op)
        assert !condition.applies(getEnvironment("surface", "paved"))
        assert condition.applies(getEnvironment("surface", "unpaved"))
    }

    @Test
    public void testRegexCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface=~/paved|unpaved/]")).condition(Condition.Context.PRIMITIVE)
        assert Condition.Op.REGEX.equals(condition.op)
        assert condition.applies(getEnvironment("surface", "unpaved"))
        assert !condition.applies(getEnvironment("surface", "grass"))
    }

    @Test
    public void testNegatedRegexCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface!~/paved|unpaved/]")).condition(Condition.Context.PRIMITIVE)
        assert Condition.Op.NREGEX.equals(condition.op)
        assert !condition.applies(getEnvironment("surface", "unpaved"))
        assert condition.applies(getEnvironment("surface", "grass"))
    }

    @Test
    public void testStandardKeyCondition() throws Exception {
        def c1 = (Condition.KeyCondition) new MapCSSParser(new StringReader("[ highway ]")).condition(Condition.Context.PRIMITIVE)
        assert c1.matchType == null
        assert c1.applies(getEnvironment("highway", "unclassified"))
        assert !c1.applies(getEnvironment("railway", "rail"))
        def c2 = (Condition.KeyCondition) new MapCSSParser(new StringReader("[\"/slash/\"]")).condition(Condition.Context.PRIMITIVE)
        assert c2.matchType == null
        assert c2.applies(getEnvironment("/slash/", "yes"))
        assert !c2.applies(getEnvironment("\"slash\"", "no"))
    }

    @Test
    public void testYesNoKeyCondition() throws Exception {
        def c1 = (Condition.KeyCondition) new MapCSSParser(new StringReader("[oneway?]")).condition(Condition.Context.PRIMITIVE)
        def c2 = (Condition.KeyCondition) new MapCSSParser(new StringReader("[oneway?!]")).condition(Condition.Context.PRIMITIVE)
        def c3 = (Condition.KeyCondition) new MapCSSParser(new StringReader("[!oneway?]")).condition(Condition.Context.PRIMITIVE)
        def c4 = (Condition.KeyCondition) new MapCSSParser(new StringReader("[!oneway?!]")).condition(Condition.Context.PRIMITIVE)
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
        def c1 = (Condition.KeyCondition) new MapCSSParser(new StringReader("[/.*:(backward|forward)\$/]")).condition(Condition.Context.PRIMITIVE)
        assert Condition.KeyMatchType.REGEX.equals(c1.matchType)
        assert !c1.applies(getEnvironment("lanes", "3"))
        assert c1.applies(getEnvironment("lanes:forward", "3"))
        assert c1.applies(getEnvironment("lanes:backward", "3"))
        assert !c1.applies(getEnvironment("lanes:foobar", "3"))
    }

    @Test
    public void testKeyKeyCondition() throws Exception {
        def c1 = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[foo = *bar]")).condition(Condition.Context.PRIMITIVE)
        def w1 = new Way()
        w1.put("foo", "123")
        w1.put("bar", "456")
        assert !c1.applies(new Environment().withPrimitive(w1))
        w1.put("bar", "123")
        assert c1.applies(new Environment().withPrimitive(w1))
        def c2 = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[foo =~ */bar/]")).condition(Condition.Context.PRIMITIVE)
        def w2 = new Way(w1)
        w2.put("bar", "[0-9]{3}")
        assert c2.applies(new Environment().withPrimitive(w2))
        w2.put("bar", "[0-9]")
        assert c2.applies(new Environment().withPrimitive(w2))
        w2.put("bar", "^[0-9]\$")
        assert !c2.applies(new Environment().withPrimitive(w2))
    }
}
