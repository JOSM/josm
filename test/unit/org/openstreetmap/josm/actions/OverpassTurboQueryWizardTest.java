// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

public class OverpassTurboQueryWizardTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
        OverpassTurboQueryWizard.getInstance();
    }

    @Test
    public void testKeyValue() throws Exception {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("amenity=drinking_water");
        assertThat(query, is("" +
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
                "out meta;"));
    }

    @Test(expected = OverpassTurboQueryWizard.ParseException.class)
    public void testErroneous() throws Exception {
        OverpassTurboQueryWizard.getInstance().constructQuery("foo");
    }
}
