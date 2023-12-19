// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.HTTPS;

/**
 * Integration tests of {@link CertificateAmendment} class.
 */
@HTTPS
@Timeout(20)
class CertificateAmendmentTestIT {
    private static final List<String> errorsToIgnore = new ArrayList<>();

    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @BeforeAll
    public static void beforeClass() throws IOException {
        errorsToIgnore.addAll(TestUtils.getIgnoredErrorMessages(CertificateAmendmentTestIT.class));
    }

    /**
     * Test a well-known certificate.
     * @throws IOException in case of I/O error
     */
    @Test
    void testDefault() throws IOException {
        // something that is not embedded
        connect("https://www.bing.com", true);
    }

    /**
     * Test <a href="https://letsencrypt.org">Let's Encrypt</a>.
     * @throws IOException in case of I/O error
     */
    @Test
    void testLetsEncrypt() throws IOException {
        // signed by letsencrypt's own ISRG root
        connect("https://valid-isrgrootx1.letsencrypt.org", true);
        // signed by letsencrypt's cross-sign CA
        connect("https://letsencrypt.org", true);
        // signed by letsencrypt's cross-sign CA, requires SNI
        connect("https://acme-v02.api.letsencrypt.org", true);
    }

    /**
     * Test overpass API.
     * @throws IOException in case of I/O error
     */
    @Test
    void testOverpass() throws IOException {
        connect("https://overpass-api.de", true);
    }

    /**
     * Test Dutch government.
     * @throws IOException in case of I/O error
     */
    @Test
    void testDutchGovernment() throws IOException {
        connect("https://geodata.nationaalgeoregister.nl", true);
    }

    /**
     * Test Taiwan government.
     * @throws IOException in case of I/O error
     */
    @Test
    void testTaiwanGovernment() throws IOException {
        connect("https://grca.nat.gov.tw", true);
    }

    private static void connect(String url, boolean shouldWork) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        try {
            connection.connect();
        } catch (SSLHandshakeException e) {
            String error = "Untrusted: " + url;
            assumeFalse(errorsToIgnore.contains(error));
            if (shouldWork) {
                throw new IOException(error, e);
            } else {
                return;
            }
        }
        String error = "Expected error: " + url;
        assumeFalse(errorsToIgnore.contains(error));
        if (!shouldWork) {
            fail(error);
        }
    }
}
