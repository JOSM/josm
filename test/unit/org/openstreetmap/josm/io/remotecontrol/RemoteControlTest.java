// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;

/**
 * Unit tests for Remote Control
 */
public class RemoteControlTest {

    private String httpBase;
    private String httpsBase;

    /**
     * Starts Remote control before testing requests.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        RemoteControl.PROP_REMOTECONTROL_HTTPS_ENABLED.put(true);
        try {
            Files.deleteIfExists(Paths.get(
                    RemoteControl.getRemoteControlDir()).resolve(RemoteControlHttpsServer.KEYSTORE_FILENAME));
        } catch (IOException e) {
            Main.error(e);
        }

        RemoteControl.start();
        disableCertificateValidation();
        httpBase = "http://127.0.0.1:"+Main.pref.getInteger("remote.control.port", 8111);
        httpsBase = "https://127.0.0.1:"+Main.pref.getInteger("remote.control.https.port", 8112);
    }

    /**
     * Disable all HTTPS validation mechanisms as described
     * <a href="http://stackoverflow.com/a/2893932/2257172">here</a> and
     * <a href="http://stackoverflow.com/a/19542614/2257172">here</a>
     */
    public void disableCertificateValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
            fail(e.getMessage());
        }

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
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
     */
    @Test
    public void testHttpListOfCommands() {
        testListOfCommands(httpBase);
    }

    /**
     * Tests that sending an HTTPS request without command results in HTTP 400, with all available commands in error message.
     */
    @Test
    public void testHttpsListOfCommands() {
        testListOfCommands(httpsBase);
    }

    private void testListOfCommands(String url) {
        try {
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
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
