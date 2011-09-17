// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;


import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.Relation;
import org.unitils.reflectionassert.ReflectionAssert;

public class TemplateEngineTest {

    @BeforeClass
    public static void before() {
        Main.pref = new Preferences();
    }

    @Test
    public void testEmpty() throws ParseError {
        TemplateParser parser = new TemplateParser("");
        ReflectionAssert.assertReflectionEquals(new StaticText(""), parser.parse());
    }

    @Test
    public void testVariable() throws ParseError {
        TemplateParser parser = new TemplateParser("abc{var}\\{ef\\$\\{g");
        ReflectionAssert.assertReflectionEquals(CompoundTemplateEntry.fromArray(new StaticText("abc"), new Variable("var"), new StaticText("{ef${g")), parser.parse());
    }

    @Test
    public void testConditionWhitespace() throws ParseError {
        TemplateParser parser = new TemplateParser("?{ '{name} {desc}' | '{name}' | '{desc}'    }");
        Condition condition = new Condition();
        condition.getEntries().add(CompoundTemplateEntry.fromArray(new Variable("name"), new StaticText(" "), new Variable("desc")));
        condition.getEntries().add(new Variable("name"));
        condition.getEntries().add(new Variable("desc"));
        ReflectionAssert.assertReflectionEquals(condition, parser.parse());
    }

    @Test
    public void testConditionNoWhitespace() throws ParseError {
        TemplateParser parser = new TemplateParser("?{'{name} {desc}'|'{name}'|'{desc}'}");
        Condition condition = new Condition();
        condition.getEntries().add(CompoundTemplateEntry.fromArray(new Variable("name"), new StaticText(" "), new Variable("desc")));
        condition.getEntries().add(new Variable("name"));
        condition.getEntries().add(new Variable("desc"));
        ReflectionAssert.assertReflectionEquals(condition, parser.parse());
    }

    private static Match compile(String expression) throws org.openstreetmap.josm.actions.search.SearchCompiler.ParseError {
        return SearchCompiler.compile(expression, false, false);
    }

    @Test
    public void testConditionSearchExpression() throws Exception {
        TemplateParser parser = new TemplateParser("?{ admin_level = 2 'NUTS 1' | admin_level = 4 'NUTS 2' |  '{admin_level}'}");
        Condition condition = new Condition();
        condition.getEntries().add(new SearchExpressionCondition(compile("admin_level = 2"), new StaticText("NUTS 1")));
        condition.getEntries().add(new SearchExpressionCondition(compile("admin_level = 4"), new StaticText("NUTS 2")));
        condition.getEntries().add(new Variable("admin_level"));
        ReflectionAssert.assertReflectionEquals(condition, parser.parse());
    }

    TemplateEngineDataProvider dataProvider = new TemplateEngineDataProvider() {
        @Override
        public Object getTemplateValue(String name, boolean special) {
            if (special) {
                if ("localName".equals(name))
                    return "localName";
                else
                    return null;
            } else {
                if ("name".equals(name))
                    return "waypointName";
                else if ("number".equals(name))
                    return 10;
                else if ("special:key".equals(name))
                    return "specialKey";
                else
                    return null;
            }
        }
        @Override
        public boolean evaluateCondition(Match condition) {
            return true;
        }
        @Override
        public List<String> getTemplateKeys() {
            return Arrays.asList("name", "number");
        }
    };

    @Test
    public void testFilling() throws Exception {
        TemplateParser parser = new TemplateParser("{name} u{unknown}u i{number}i");
        TemplateEntry entry = parser.parse();
        StringBuilder sb = new StringBuilder();
        entry.appendText(sb, dataProvider);
        Assert.assertEquals("waypointName uu i10i", sb.toString());
    }

    @Test
    public void testFillingSearchExpression() throws Exception {
        TemplateParser parser = new TemplateParser("?{ admin_level = 2 'NUTS 1' | admin_level = 4 'NUTS 2' |  '{admin_level}'}");
        TemplateEntry templateEntry = parser.parse();

        StringBuilder sb = new StringBuilder();
        Relation r = new Relation();
        r.put("admin_level", "2");
        templateEntry.appendText(sb, r);
        Assert.assertEquals("NUTS 1", sb.toString());

        sb.setLength(0);
        r.put("admin_level", "5");
        templateEntry.appendText(sb, r);
        Assert.assertEquals("5", sb.toString());
    }

    @Test
    public void testPrintAll() throws Exception {
        TemplateParser parser = new TemplateParser("{special:everything}");
        TemplateEntry entry = parser.parse();
        StringBuilder sb = new StringBuilder();
        entry.appendText(sb, dataProvider);
        Assert.assertEquals("name=waypointName, number=10", sb.toString());
    }

    @Test
    public void testPrintMultiline() throws Exception {
        TemplateParser parser = new TemplateParser("{name}\\n{number}");
        TemplateEntry entry = parser.parse();
        StringBuilder sb = new StringBuilder();
        entry.appendText(sb, dataProvider);
        Assert.assertEquals("waypointName\n10", sb.toString());
    }

    @Test
    public void testSpecialVariable() throws Exception {
        TemplateParser parser = new TemplateParser("{name}u{special:localName}u{special:special:key}");
        TemplateEntry templateEntry = parser.parse();

        StringBuilder sb = new StringBuilder();
        templateEntry.appendText(sb, dataProvider);
        Assert.assertEquals("waypointNameulocalNameuspecialKey", sb.toString());

    }


}
