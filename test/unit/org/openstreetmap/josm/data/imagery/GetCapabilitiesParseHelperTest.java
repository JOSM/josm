// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link GetCapabilitiesParseHelper}.
 */
class GetCapabilitiesParseHelperTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests that {@code GetCapabilitiesParseHelper} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(GetCapabilitiesParseHelper.class);
    }

    /**
     * Unit test of {@code GetCapabilitiesParseHelper#crsToCode} method.
     */
    @Test
    void testCrsToCode() {
        assertEquals("EPSG:3127", GetCapabilitiesParseHelper.crsToCode("urn:ogc:def:crs:epsg:3127"));
        assertEquals("EPSG:3127", GetCapabilitiesParseHelper.crsToCode("urn:ogc:def:crs:epsg::3127"));
        assertEquals("EPSG:3127", GetCapabilitiesParseHelper.crsToCode("urn:ogc:def:crs:epsg:6.6:3127"));
    }
}
