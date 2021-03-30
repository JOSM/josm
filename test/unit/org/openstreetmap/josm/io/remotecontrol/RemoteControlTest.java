// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for Remote Control
 */
class RemoteControlTest {

    private String httpBase;

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().https().assertionsInEDT();

    /**
     * Starts Remote control before testing requests.
     * @throws GeneralSecurityException if a security error occurs
     */
    @BeforeEach
    public void setUp() throws GeneralSecurityException {
        RemoteControl.start();
        httpBase = "http://127.0.0.1:"+Config.getPref().getInt("remote.control.port", 8111);
    }

    /**
     * Stops Remote control after testing requests.
     */
    @AfterEach
    public void tearDown() {
        RemoteControl.stop();
    }

    /**
     * Tests that sending an HTTP request without command results in HTTP 400, with all available commands in error message.
     * @throws Exception if an error occurs
     */
    @Test
    void testHttpListOfCommands() throws Exception {
        testListOfCommands(httpBase);
    }

    private void testListOfCommands(String url) throws IOException, ReflectiveOperationException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        assertEquals(connection.getResponseCode(), HttpURLConnection.HTTP_BAD_REQUEST);
        try (InputStream is = connection.getErrorStream()) {
            // TODO this code should be refactored somewhere in Utils as it is used in several JOSM classes
            String responseBody;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                responseBody = in.lines().collect(Collectors.joining("\n"));
            }
            assert responseBody.contains(RequestProcessor.getUsageAsHtml());
        }
    }
}
