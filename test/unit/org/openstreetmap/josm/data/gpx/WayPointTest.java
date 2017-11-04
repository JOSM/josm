// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.color.ColorSpace;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link WayPoint}.
 */
public class WayPointTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link WayPoint#equals} and {@link WayPoint#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(WayPoint.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withIgnoredFields("customColoring", "dir", "drawLine", "east", "north", "eastNorthCacheKey")
            .withPrefabValues(ColorSpace.class, ColorSpace.getInstance(ColorSpace.CS_sRGB), ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB))
            .verify();
    }
}
