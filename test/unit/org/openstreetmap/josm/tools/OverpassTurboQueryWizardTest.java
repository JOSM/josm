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

    /**
     * Test {@code key=value}.
     */
    @Test
    public void testKeyValue() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("amenity=drinking_water");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  nwr[\"amenity\"=\"drinking_water\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test {@code key!=value}.
     */
    @Test
    public void testKeyNotValue() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("amenity!=drinking_water");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  nwr[\"amenity\"!=\"drinking_water\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test {@code foo=bar and baz=42}.
     */
    @Test
    public void testBooleanAnd() {
        final String expected = "" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"][\"baz\"=\"42\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;";
        assertEquals(expected, OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar and baz=42"));
        assertEquals(expected, OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar && baz=42"));
        assertEquals(expected, OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar & baz=42"));
    }

    /**
     * Test {@code foo=bar or baz=42}.
     */
    @Test
    public void testBooleanOr() {
        final String expected = "" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"];\n" +
                "  nwr[\"baz\"=\"42\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;";
        assertEquals(expected, OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar or baz=42"));
        assertEquals(expected, OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar || baz=42"));
        assertEquals(expected, OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar | baz=42"));
    }

    /**
     * Test {@code (foo=* or bar=*) and (asd=* or fasd=*)}.
     */
    @Test
    public void testBoolean() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("(foo=* or bar=*) and (asd=* or fasd=*)");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  nwr[\"foo\"][\"asd\"];\n" +
                "  nwr[\"foo\"][\"fasd\"];\n" +
                "  nwr[\"bar\"][\"asd\"];\n" +
                "  nwr[\"bar\"][\"fasd\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test {@code foo=bar and (type:node or type:way)}.
     */
    @Test
    public void testType() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar and (type:node or type:way)");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  node[\"foo\"=\"bar\"];\n" +
                "  way[\"foo\"=\"bar\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test {@code user:foo or uid:42}.
     */
    @Test
    public void testUser() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("user:foo or uid:42");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  nwr(user:\"foo\");\n" +
                "  nwr(uid:42);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test {@code foo=bar and (type:node or type:way)}.
     */
    @Test
    public void testEmpty() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("foo='' and type:way");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  way[\"foo\"~\"^$\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test geocodeArea.
     */
    @Test
    public void testInArea() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("foo=bar in Josmland");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "{{geocodeArea:Josmland}}->.searchArea;\n" +
                "(\n" +
                "  nwr[\"foo\"=\"bar\"](area.searchArea);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test building an Overpass query based on a preset name.
     */
    @Test
    @Ignore("preset handling not implemented")
    public void testPreset() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("Hospital");
        assertEquals("" +
                "[out:xml][timeout:90];\n" +
                "(\n" +
                "  nwr[\"amenity\"=\"hospital\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Test erroneous value.
     */
    @Test(expected = UncheckedParseException.class)
    public void testErroneous() {
        OverpassTurboQueryWizard.getInstance().constructQuery("foo");
    }
}
