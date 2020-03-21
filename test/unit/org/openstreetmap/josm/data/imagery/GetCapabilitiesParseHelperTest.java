// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link GetCapabilitiesParseHelper}.
 */
public class GetCapabilitiesParseHelperTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests that {@code GetCapabilitiesParseHelper} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(GetCapabilitiesParseHelper.class);
    }

    /**
     * Unit test of {@code GetCapabilitiesParseHelper#crsToCode} method.
     */
    @Test
    public void testCrsToCode() {
        assertEquals("EPSG:3127", GetCapabilitiesParseHelper.crsToCode("urn:ogc:def:crs:epsg:3127"));
        assertEquals("EPSG:3127", GetCapabilitiesParseHelper.crsToCode("urn:ogc:def:crs:epsg::3127"));
        assertEquals("EPSG:3127", GetCapabilitiesParseHelper.crsToCode("urn:ogc:def:crs:epsg:6.6:3127"));
    }
}
