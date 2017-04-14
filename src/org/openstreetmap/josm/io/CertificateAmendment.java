// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class to add missing root certificates to the list of trusted certificates
 * for TLS connections.
 *
 * The added certificates are deemed trustworthy by the main web browsers and
 * operating systems, but not included in some distributions of Java.
 *
 * The certificates are added in-memory at each start, nothing is written to disk.
 * @since 9995
 */
public final class CertificateAmendment {

    private static final String[] CERT_AMEND = {
        "resource://data/security/DST_Root_CA_X3.pem"
    };

    private static final String[] SHA_HASHES = {
        "0687260331a72403d909f105e69bcf0d32e1bd2493ffc6d9206d11bcd6770739"
    };

    private CertificateAmendment() {
        // Hide default constructor for utility classes
    }

    /**
     * Add missing root certificates to the list of trusted certificates for TLS connections.
     * @throws IOException if an I/O error occurs
     * @throws GeneralSecurityException if a security error occurs
     */
    public static void addMissingCertificates() throws IOException, GeneralSecurityException {
        if (!Main.pref.getBoolean("tls.add-missing-certificates", true))
            return;
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Path cacertsPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        try (InputStream is = Files.newInputStream(cacertsPath)) {
            keyStore.load(is, "changeit".toCharArray());
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        boolean certificateAdded = false;
        for (int i = 0; i < CERT_AMEND.length; i++) {
            try (CachedFile certCF = new CachedFile(CERT_AMEND[i])) {
                byte[] certBytes = certCF.getByteContent();
                ByteArrayInputStream certIS = new ByteArrayInputStream(certBytes);
                X509Certificate cert = (X509Certificate) cf.generateCertificate(certIS);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                String sha1 = Utils.toHexString(md.digest(cert.getEncoded()));
                if (!SHA_HASHES[i].equals(sha1)) {
                    throw new IllegalStateException(
                            tr("Error adding certificate {0} - certificate fingerprint mismatch. Expected {1}, was {2}",
                            CERT_AMEND[i],
                            SHA_HASHES[i],
                            sha1
                            ));
                }
                if (certificateIsMissing(keyStore, cert)) {
                    if (Main.isDebugEnabled()) {
                        Main.debug(tr("Adding certificate for TLS connections: {0}", cert.getSubjectX500Principal().getName()));
                    }
                    String alias = "josm:" + new File(CERT_AMEND[i]).getName();
                    keyStore.setCertificateEntry(alias, cert);
                    certificateAdded = true;
                }
            }
        }

        if (certificateAdded) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            SSLContext.setDefault(sslContext);
        }
    }

    /**
     * Check if the certificate is missing and needs to be added to the keystore.
     * @param keyStore the keystore
     * @param crt the certificate
     * @return true, if the certificate is not contained in the keystore
     * @throws InvalidAlgorithmParameterException if the keystore does not contain at least one trusted certificate entry
     * @throws KeyStoreException if the keystore has not been initialized
     */
    private static boolean certificateIsMissing(KeyStore keyStore, X509Certificate crt)
            throws KeyStoreException, InvalidAlgorithmParameterException {
        PKIXParameters params = new PKIXParameters(keyStore);
        String id = crt.getSubjectX500Principal().getName();
        for (TrustAnchor ta : params.getTrustAnchors()) {
            X509Certificate cert = ta.getTrustedCert();
            if (Objects.equals(id, cert.getSubjectX500Principal().getName()))
                return false;
        }
        return true;
    }
}
