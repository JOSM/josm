// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore.TrustedCertificateEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.PlatformManager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests for Remote Control
 */
public class RemoteControlTest {

    private String httpBase;

    private static class PlatformHookWindowsMock extends MockUp<PlatformHookWindows> {
        @Mock
        public boolean setupHttpsCertificate(String entryAlias, TrustedCertificateEntry trustedCert) {
            return true;
        }
    }

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().https().assertionsInEDT();

    /**
     * Starts Remote control before testing requests.
     * @throws GeneralSecurityException if a security error occurs
     */
    @Before
    public void setUp() throws GeneralSecurityException {
        if (PlatformManager.isPlatformWindows() && "True".equals(System.getenv("APPVEYOR"))) {
            // appveyor doesn't like us tinkering with the root keystore, so mock this out
            TestUtils.assumeWorkingJMockit();
            new PlatformHookWindowsMock();
        }

        RemoteControl.start();
        httpBase = "http://127.0.0.1:"+Config.getPref().getInt("remote.control.port", 8111);
    }

    /**
     * Stops Remote control after testing requests.
     */
    @After
    public void tearDown() {
        RemoteControl.stop();
    }

    /**
     * Tests that sending an HTTP request without command results in HTTP 400, with all available commands in error message.
     * @throws Exception if an error occurs
     */
    @Test
    public void testHttpListOfCommands() throws Exception {
        testListOfCommands(httpBase);
    }

    private void testListOfCommands(String url) throws IOException, ReflectiveOperationException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        assertEquals(connection.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        try (InputStream is = connection.getErrorStream()) {
            // TODO this code should be refactored somewhere in Utils as it is used in several JOSM classes
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String s;
                while ((s = in.readLine()) != null) {
                    responseBody.append(s);
                    responseBody.append("\n");
                }
            }
            assert responseBody.toString().contains(RequestProcessor.getUsageAsHtml());
        }
    }
}
