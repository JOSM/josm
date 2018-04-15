// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link TMSCachedTileLoaderJob}.
 */
public class TMSCachedTileLoaderJobTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests that {@code TMSCachedTileLoaderJob#SERVICE_EXCEPTION_PATTERN} is correct.
     */
    @Test
    public void testServiceExceptionPattern() {
        test("missing parameters ['version', 'format']",
                "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE ServiceExceptionReport SYSTEM \"http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd\">\n" +
                "<ServiceExceptionReport version=\"1.1.1\">\n" +
                "    <ServiceException>missing parameters ['version', 'format']</ServiceException>\n" +
                "</ServiceExceptionReport>");
        test("Parameter 'layers' contains unacceptable layer names.",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\r\n" +
                "<!DOCTYPE ServiceExceptionReport SYSTEM \"http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd\">\r\n" +
                "<ServiceExceptionReport version=\"1.1.1\">\r\n" +
                "  <ServiceException code=\"LayerNotDefined\">\r\n" +
                "Parameter 'layers' contains unacceptable layer names.\r\n" +
                "  </ServiceException>\r\n" +
                "</ServiceExceptionReport>\r\n" +
                "");
    }

    private static void test(String expected, String xml) {
        Matcher m = TMSCachedTileLoaderJob.SERVICE_EXCEPTION_PATTERN.matcher(xml);
        assertTrue(xml, m.matches());
        assertEquals(expected, Utils.strip(m.group(1)));
    }
}
