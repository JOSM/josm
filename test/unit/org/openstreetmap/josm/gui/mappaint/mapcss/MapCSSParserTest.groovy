package org.openstreetmap.josm.gui.mappaint.mapcss

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

    @Test
    public void testEqualCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface=paved]")).condition(Condition.Context.PRIMITIVE)
        assert condition instanceof Condition.KeyValueCondition
        assert Condition.Op.EQ.equals(condition.op)
        assert "surface".equals(condition.k)
        assert "paved".equals(condition.v)
        Main.pref = new Preferences()
        assert condition.applies(new Environment().withPrimitive(getPrimitive("surface", "paved")))
        assert !condition.applies(new Environment().withPrimitive(getPrimitive("surface", "unpaved")))
    }

    @Test
    public void testNotEqualCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface!=paved]")).condition(Condition.Context.PRIMITIVE)
        assert condition instanceof Condition.KeyValueCondition
        assert Condition.Op.NEQ.equals(condition.op)
        Main.pref = new Preferences()
        assert !condition.applies(new Environment().withPrimitive(getPrimitive("surface", "paved")))
        assert condition.applies(new Environment().withPrimitive(getPrimitive("surface", "unpaved")))
    }

    @Test
    public void testRegexCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface=~/paved|unpaved/]")).condition(Condition.Context.PRIMITIVE)
        assert condition instanceof Condition.KeyValueCondition
        assert Condition.Op.REGEX.equals(condition.op)
        Main.pref = new Preferences()
        assert condition.applies(new Environment().withPrimitive(getPrimitive("surface", "unpaved")))
        assert !condition.applies(new Environment().withPrimitive(getPrimitive("surface", "grass")))
    }

    @Test
    public void testNegatedRegexCondition() throws Exception {
        def condition = (Condition.KeyValueCondition) new MapCSSParser(new StringReader("[surface!~/paved|unpaved/]")).condition(Condition.Context.PRIMITIVE)
        assert condition instanceof Condition.KeyValueCondition
        assert Condition.Op.NREGEX.equals(condition.op)
        Main.pref = new Preferences()
        assert !condition.applies(new Environment().withPrimitive(getPrimitive("surface", "unpaved")))
        assert condition.applies(new Environment().withPrimitive(getPrimitive("surface", "grass")))
    }
}
