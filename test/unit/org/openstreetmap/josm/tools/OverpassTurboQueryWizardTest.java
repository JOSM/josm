// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OverpassTurboQueryWizard} class.
 */
public class OverpassTurboQueryWizardTest {
    /**
     * Base test environment is enough
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().timeout(15000);

    private void assertQueryEquals(String expectedQueryPart, String input) {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery(input);
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                expectedQueryPart +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test {@code key=value}.
     */
    @Test
    public void testKeyValue() {
        assertQueryEquals("  nwr[\"amenity\"=\"drinking_water\"];\n", "amenity=drinking_water");
        assertQueryEquals("  nwr[\"amenity\"];\n", "amenity=*");
    }

    /**
     * Test {@code key!=value} and {@code key<>value}.
     */
    @Test
    public void testKeyNotValue() {
        assertQueryEquals("  nwr[\"amenity\"!=\"drinking_water\"];\n", "-amenity=drinking_water");
        assertQueryEquals("  nwr[!\"amenity\"];\n", "-amenity=*");
    }

    /**
     * Test {@code key~value} and similar.
     */
    @Test
    public void testKeyLikeValue() {
        assertQueryEquals("  nwr[\"foo\"~\"bar\"];\n", "foo~bar");
        assertQueryEquals("  nwr[\"foo\"~\"bar\"];\n", "foo~/bar/");
        // case insensitive
        assertQueryEquals("  nwr[\"foo\"~\"bar\",i];\n", "foo~/bar/i");
        // negated
        assertQueryEquals("  nwr[\"foo\"!~\"bar\"];\n", "-foo~bar");
        assertQueryEquals("  nwr[\"foo\"!~\"bar\",i];\n", "-foo~/bar/i");
    }

    /**
     * Test OSM boolean true/false.
     */
    @Test
    public void testOsmBoolean() {
        assertQueryEquals("  nwr[\"highway\"][\"oneway\"~\"true|yes|1|on\"];\n", "highway=* AND oneway?");
        assertQueryEquals("  nwr[\"highway\"][\"oneway\"~\"false|no|0|off\"];\n", "highway=* AND -oneway?");
    }

    /**
     * Test {@code foo=bar and baz=42}.
     */
    @Test
    public void testBooleanAnd() {
        assertQueryEquals("  nwr[\"foo\"=\"bar\"][\"baz\"=\"42\"];\n", "foo=bar and baz=42");
        assertQueryEquals("  nwr[\"foo\"=\"bar\"][\"baz\"=\"42\"];\n", "foo=bar && baz=42");
        assertQueryEquals("  nwr[\"foo\"=\"bar\"][\"baz\"=\"42\"];\n", "foo=bar & baz=42");
    }

    /**
     * Test {@code foo=bar or baz=42}.
     */
    @Test
    public void testBooleanOr() {
        assertQueryEquals("  nwr[\"foo\"=\"bar\"];\n  nwr[\"baz\"=\"42\"];\n", "foo=bar or baz=42");
        assertQueryEquals("  nwr[\"foo\"=\"bar\"];\n  nwr[\"baz\"=\"42\"];\n", "foo=bar | baz=42");
    }

    /**
     * Test {@code (foo=* or bar=*) and (asd=* or fasd=*)}.
     */
    @Test
    public void testBoolean() {
        assertQueryEquals("" +
                "  nwr[\"foo\"][\"baz1\"];\n" +
                "  nwr[\"foo\"][\"baz2\"];\n" +
                "  nwr[\"foo\"][\"baz3\"][\"baz4\"];\n" +
                "  nwr[\"foo\"][\"baz3\"][\"baz5\"];\n" +
                "  nwr[\"bar\"][\"baz1\"];\n" +
                "  nwr[\"bar\"][\"baz2\"];\n" +
                "  nwr[\"bar\"][\"baz3\"][\"baz4\"];\n" +
                "  nwr[\"bar\"][\"baz3\"][\"baz5\"];\n",
                "(foo=* or bar=*) and (baz1=* or baz2=* or (baz3=* and (baz4=* or baz5=*)))");
    }

    /**
     * Test {@code foo=bar and (type:node or type:way)}.
     */
    @Test
    public void testType() {
        assertQueryEquals("  node[\"foo\"=\"bar\"];\n  way[\"foo\"=\"bar\"];\n", "foo=bar and (type:node or type:way)");
    }

    /**
     * Test {@code user:foo or uid:42}.
     */
    @Test
    public void testUser() {
        assertQueryEquals("  nwr(user:\"foo\");\n  nwr(uid:42);\n", "user:foo or user:42");
    }

    /**
     * Test {@code foo=bar and (type:node or type:way)}.
     */
    @Test
    public void testEmpty() {
        assertQueryEquals("  way[\"foo\"~\"^$\"];\n", "foo=\"\" and type:way");
    }

    /**
     * Test geocodeArea.
     */
    @Test
    public void testInArea() {
        String query = OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar | foo=baz in Innsbruck");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "{{geocodeArea:Innsbruck}}->.searchArea;\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"](area.searchArea);\n" +
                "  nwr[\"foo\"=\"baz\"](area.searchArea);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
        query = OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar | foo=baz in \"Sankt Sigmund im Sellrain\"");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "{{geocodeArea:Sankt Sigmund im Sellrain}}->.searchArea;\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"](area.searchArea);\n" +
                "  nwr[\"foo\"=\"baz\"](area.searchArea);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
        query = OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar | foo=baz in \"Новосибирск\"");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "{{geocodeArea:Новосибирск}}->.searchArea;\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"](area.searchArea);\n" +
                "  nwr[\"foo\"=\"baz\"](area.searchArea);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test geocodeArea.
     */
    @Test
    public void testAroundArea() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar | foo=baz around \"Sankt Sigmund im Sellrain\"");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "{{radius=1000}}\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"](around:{{radius}},{{geocodeCoords:Sankt Sigmund im Sellrain}});\n" +
                "  nwr[\"foo\"=\"baz\"](around:{{radius}},{{geocodeCoords:Sankt Sigmund im Sellrain}});\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test global query.
     */
    @Test
    public void testGlobal() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar global");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test "in bbox" query.
     */
    @Test
    public void testInBbox() {
        assertQueryEquals("  nwr[\"foo\"=\"bar\"];\n", "foo=bar IN BBOX");
    }

    /**
     * Test building an Overpass query based on a preset name.
     */
    @Test
    @Ignore("preset handling not implemented")
    public void testPreset() {
        assertQueryEquals("  nwr[\"amenity\"=\"hospital\"];\n", "Hospital");
    }

    /**
     * Test erroneous value.
     */
    @Test(expected = UncheckedParseException.class)
    public void testErroneous() {
        OverpassTurboQueryWizard.getInstance().constructQuery("-(foo or bar)");
    }
}
