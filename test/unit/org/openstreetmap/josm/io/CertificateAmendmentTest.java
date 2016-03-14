// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.SSLHandshakeException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link CertificateAmendment} class.
 */
public class CertificateAmendmentTest {

    /**
     * Setup test.
     * @throws IOException in case of I/O error
     */
    @BeforeClass
    public static void setUp() throws IOException {
        JOSMFixture.createUnitTestFixture().init();
        CertificateAmendment.addMissingCertificates();
    }

    /**
     * Test a well-known certificate.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testDefault() throws IOException {
        // something that is neither DST nor StartSSL
        connect("https://google.com", true);
    }

    /**
     * Test <a href="https://letsencrypt.org">Let's Encrypt</a>.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testLetsEncrypt() throws IOException {
        // signed by letsencrypt
        connect("https://helloworld.letsencrypt.org", true);
        // signed by LE's cross-sign CA
        connect("https://letsencrypt.org", true);
    }

    /**
     * Test <a href="https://www.startssl.com">StartSSL</a>.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testStartSSL() throws IOException {
        connect("https://map.dgpsonline.eu", true);
        connect("https://www.startssl.com", true);
    }

    /**
     * Test a broken certificate.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testBrokenCert() throws IOException {
        // broken at the moment (may get fixed some day)
        connect("https://www.pcwebshop.co.uk", false);
    }

    private void connect(String url, boolean shouldWork) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        try {
            connection.connect();
        } catch (SSLHandshakeException e) {
            if (shouldWork) {
                Assert.fail("Untrusted: " + url);
            } else {
                return;
            }
        }
        if (!shouldWork) {
            Assert.fail("Expected error: " + url);
        }
    }
}
