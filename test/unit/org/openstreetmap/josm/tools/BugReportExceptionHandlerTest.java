// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.DatatypeConverter;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ShowStatusReportAction;

/**
 * Bug report unit tests.
 */
public class BugReportExceptionHandlerTest {

    /**
     * Setup tests.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test method for {@link org.openstreetmap.josm.tools.BugReportExceptionHandler#getBugReportUrl(java.lang.String)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testGetBugReportUrl() throws IOException {
        String report = ShowStatusReportAction.getReportHeader();
        String url = BugReportExceptionHandler.getBugReportUrl(report).toExternalForm();
        String prefix = Main.getJOSMWebsite()+"/josmticket?gdata=";
        assertTrue(url.startsWith(prefix));

        String gdata = url.substring(prefix.length());
        // JAXB only provides support for "base64" decoding while we encode url in "base64url", so switch encoding, only for test purpose
        byte[] data = DatatypeConverter.parseBase64Binary(gdata.replace('-', '+').replace('_', '/'));
        byte[] buff = new byte[8192];
        try (GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            for (int n = is.read(buff); n > 0; n = is.read(buff)) {
                sb.append(new String(buff, 0, n, StandardCharsets.UTF_8));
            }
            assertEquals(report, sb.toString());
        }
    }
}
