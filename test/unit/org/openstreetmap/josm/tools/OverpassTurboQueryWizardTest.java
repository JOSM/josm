// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link OverpassTurboQueryWizard} class.
 */
public class OverpassTurboQueryWizardTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
        OverpassTurboQueryWizard.getInstance();
    }

    /**
     * Test key=value.
     */
    @Test
    public void testKeyValue() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("amenity=drinking_water");
        assertEquals("" +
                "[timeout:25];\n" +
                "// gather results\n" +
                "(\n" +
                "  // query part for: “amenity=drinking_water”\n" +
                "  node[\"amenity\"=\"drinking_water\"];\n" +
                "  way[\"amenity\"=\"drinking_water\"];\n" +
                "  relation[\"amenity\"=\"drinking_water\"];\n" +
                ");\n" +
                "// print results\n" +
                "out meta;\n" +
                ">;\n" +
                "out meta;", query);
    }

    /**
     * Test erroneous value.
     */
    @Test(expected = OverpassTurboQueryWizard.ParseException.class)
    public void testErroneous() {
        OverpassTurboQueryWizard.getInstance().constructQuery("foo");
    }
}
