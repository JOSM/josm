// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.SSLHandshakeException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link CertificateAmendment} class.
 */
public class CertificateAmendmentTestIT {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().timeout(20000);

    /**
     * Test a well-known certificate.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testDefault() throws IOException {
        // something that is not embedded
        connect("https://www.bing.com", true);
    }

    /**
     * Test <a href="https://letsencrypt.org">Let's Encrypt</a>.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testLetsEncrypt() throws IOException {
        // signed by letsencrypt's own ISRG root
        // (not included yet)
        // TODO: they switched to cross-sign CA, re-enable it if ISRG root is used again
        // connect("https://helloworld.letsencrypt.org", false);
        // signed by letsencrypt's cross-sign CA
        connect("https://letsencrypt.org", true);
        // signed by letsencrypt's cross-sign CA, requires SNI
        connect("https://acme-v01.api.letsencrypt.org", true);
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

    /**
     * Test overpass API.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testOverpass() throws IOException {
        connect("https://overpass-api.de", true);
    }

    /**
     * Test Dutch government.
     * @throws IOException in case of I/O error
     */
    @Test
    public void testDutchGovernment() throws IOException {
        connect("https://geodata.nationaalgeoregister.nl", true);
    }

    private static void connect(String url, boolean shouldWork) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        try {
            connection.connect();
        } catch (SSLHandshakeException e) {
            if (shouldWork) {
                throw new IOException("Untrusted: " + url, e);
            } else {
                return;
            }
        }
        if (!shouldWork) {
            Assert.fail("Expected error: " + url);
        }
    }
}
