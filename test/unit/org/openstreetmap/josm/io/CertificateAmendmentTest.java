// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of {@link CertificateAmendment} class.
 */
@BasicPreferences
class CertificateAmendmentTest {
    /**
     * Tests that {@code CertificateAmendment} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(CertificateAmendment.class);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/24962">Bug #24962</a>.
     * Adding missing certificates must not restrict HTTPS connections to TLS 1.2.
     * @throws IOException if an I/O error occurs
     * @throws GeneralSecurityException if a security error occurs
     */
    @Test
    void testTicket24962() throws IOException, GeneralSecurityException {
        CertificateAmendment.addMissingCertificates();
        List<String> protocols = Arrays.asList(SSLContext.getDefault().getDefaultSSLParameters().getProtocols());
        assertTrue(protocols.contains("TLSv1.3"), protocols::toString);
    }
}
