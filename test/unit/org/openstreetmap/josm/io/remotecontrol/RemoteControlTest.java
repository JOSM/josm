// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.AssertionsInEDT;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests for Remote Control
 */
@AssertionsInEDT
@HTTPS
class RemoteControlTest {

    private String httpBase;

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
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, connection.getResponseCode());
        try (InputStream is = connection.getErrorStream()) {
            String responseBody = new String(Utils.readBytesFromStream(is), StandardCharsets.UTF_8);
            assert responseBody.contains(RequestProcessor.getUsageAsHtml());
        }
    }
}
