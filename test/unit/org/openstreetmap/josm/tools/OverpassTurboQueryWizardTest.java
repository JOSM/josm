// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

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
     * Test key=value.
     */
    @Test
    public void testKeyValue() {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery("amenity=drinking_water");
        assertEquals("" +
                "[out:xml][timeout:90][bbox:{{bbox}}];\n" +
                "(\n" +
                "  node[\"amenity\"=\"drinking_water\"];\n" +
                "  way[\"amenity\"=\"drinking_water\"];\n" +
                "  relation[\"amenity\"=\"drinking_water\"];\n" +
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
