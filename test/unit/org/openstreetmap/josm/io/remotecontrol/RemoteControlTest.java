// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.AssertionsInEDT;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.tools.Logging;

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

    /**
     * The OAuth 2.0 authorization code of the redirect to Remote Control is not written to the log.
     * @throws Exception if an error occurs
     */
    @Test
    void testAuthorizationCodeIsNotLogged() throws Exception {
        final List<String> logged = Collections.synchronizedList(new ArrayList<>());
        final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                logged.add(String.valueOf(logRecord.getMessage()));
            }

            @Override
            public void flush() {
                // nothing to flush
            }

            @Override
            public void close() {
                // nothing to close
            }
        };
        Logging.getLogger().addHandler(handler);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(httpBase
                    + "/oauth_authorization?code=a-secret-code&state=a-state").openConnection();
            connection.connect();
            connection.getResponseCode();
            connection.disconnect();
        } finally {
            Logging.getLogger().removeHandler(handler);
        }
        final String log = String.join("\n", logged);
        assertTrue(log.contains("RemoteControl received: GET /oauth_authorization?code=...stripped...&state=a-state"), log);
        assertFalse(log.contains("a-secret-code"), log);
    }

    private void testListOfCommands(String url) throws IOException, ReflectiveOperationException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, connection.getResponseCode());
        try (InputStream is = connection.getErrorStream()) {
            String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assert responseBody.contains(RequestProcessor.getUsageAsHtml());
        }
    }
}
