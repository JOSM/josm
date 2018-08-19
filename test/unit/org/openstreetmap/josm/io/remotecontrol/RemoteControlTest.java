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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
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
    private String httpsBase;

    /**
     * Starts Remote control before testing requests.
     * @throws GeneralSecurityException if a security error occurs
     */
    @Before
    public void setUp() throws GeneralSecurityException {
        JOSMFixture.createUnitTestFixture().init();
        RemoteControl.PROP_REMOTECONTROL_HTTPS_ENABLED.put(true);
        deleteKeystore();

        if (PlatformManager.isPlatformWindows() && "True".equals(System.getenv("APPVEYOR"))) {
            // appveyor doesn't like us tinkering with the root keystore, so mock this out
            TestUtils.assumeWorkingJMockit();
            new MockUp<PlatformHookWindows>() {
                @Mock
                public boolean setupHttpsCertificate(String entryAlias, TrustedCertificateEntry trustedCert) {
                    return true;
                }
            };
        }

        RemoteControl.start();
        disableCertificateValidation();
        httpBase = "http://127.0.0.1:"+Config.getPref().getInt("remote.control.port", 8111);
        httpsBase = "https://127.0.0.1:"+Config.getPref().getInt("remote.control.https.port", 8112);
    }

    /**
     * Deletes JOSM keystore, if it exists.
     */
    public static void deleteKeystore() {
        try {
            Files.deleteIfExists(Paths.get(
                    RemoteControl.getRemoteControlDir()).resolve(RemoteControlHttpsServer.KEYSTORE_FILENAME));
        } catch (IOException e) {
            Logging.error(e);
        }
    }

    /**
     * Disable all HTTPS validation mechanisms as described
     * <a href="http://stackoverflow.com/a/2893932/2257172">here</a> and
     * <a href="http://stackoverflow.com/a/19542614/2257172">here</a>
     * @throws GeneralSecurityException if a security error occurs
     */
    public void disableCertificateValidation() throws GeneralSecurityException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                @SuppressFBWarnings(value = "WEAK_TRUST_MANAGER")
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                @SuppressFBWarnings(value = "WEAK_TRUST_MANAGER")
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                @SuppressFBWarnings(value = "WEAK_TRUST_MANAGER")
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;

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
     * @throws Exception if an error occurs
     */
    @Test
    public void testHttpListOfCommands() throws Exception {
        testListOfCommands(httpBase);
    }

    /**
     * Tests that sending an HTTPS request without command results in HTTP 400, with all available commands in error message.
     * @throws Exception if an error occurs
     */
    @Test
    public void testHttpsListOfCommands() throws Exception {
        testListOfCommands(httpsBase);
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
