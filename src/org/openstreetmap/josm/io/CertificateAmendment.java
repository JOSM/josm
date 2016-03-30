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
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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
        "resource://data/security/DST_Root_CA_X3.pem",
        "resource://data/security/StartCom_Certification_Authority.pem",
        "resource://data/security/Certrum_CA.pem"
    };

    private static final String[] SHA_HASHES = {
        "0687260331a72403d909f105e69bcf0d32e1bd2493ffc6d9206d11bcd6770739",
        "c766a9bef2d4071c863a31aa4920e813b2d198608cb7b7cfe21143b836df09ea",
        "fd02362244f31266caff005818d1004ec4eb08fb239aafaaafff47497d6005d6"
    };

    private CertificateAmendment() {
        // Hide default constructor for utility classes
    }

    /**
     * Add missing root certificates to the list of trusted certificates for TLS connections.
     * @throws IOException if an I/O error occurs
     */
    public static void addMissingCertificates() throws IOException {
        if (!Main.pref.getBoolean("tls.add-missing-certificates", true))
            return;
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException ex) {
            throw new IOException(ex);
        }
        Path cacertsPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        try (InputStream is = Files.newInputStream(cacertsPath)) {
            keyStore.load(is, "changeit".toCharArray());
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        } catch (CertificateException ex) {
            throw new IOException(ex);
        }

        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException ex) {
            throw new RuntimeException(ex);
        }
        boolean certificateAdded = false;
        for (int i = 0; i < CERT_AMEND.length; i++) {
            CachedFile certCF = new CachedFile(CERT_AMEND[i]);
            byte[] certBytes = certCF.getByteContent();
            ByteArrayInputStream certIS = new ByteArrayInputStream(certBytes);
            X509Certificate cert;

            try {
                cert = (X509Certificate) cf.generateCertificate(certIS);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                String sha1 = Utils.toHexString(md.digest(cert.getEncoded()));
                if (!SHA_HASHES[i].equals(sha1)) {
                    throw new RuntimeException(tr("Error adding certificate {0} - certificate fingerprint mismatch. Expected {1}, was {2}",
                            CERT_AMEND[i],
                            SHA_HASHES[i],
                            sha1
                            ));
                }
            } catch (CertificateException ex) {
                throw new IOException(ex);
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
            if (certificateIsMissing(keyStore, cert)) {
                if (Main.isDebugEnabled()) {
                    Main.debug(tr("Adding certificate for TLS connections: {0}", cert.getSubjectX500Principal().getName()));
                }
                String alias = "josm:" + new File(CERT_AMEND[i]).getName();
                try {
                    keyStore.setCertificateEntry(alias, cert);
                } catch (KeyStoreException ex) {
                    throw new AssertionError(ex);
                }
                certificateAdded = true;
            }
        }

        if (certificateAdded) {
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                SSLContext.setDefault(sslContext);
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Check if the certificate is missing and needs to be added to the keystore.
     * @param keyStore the keystore
     * @param crt the certificate
     * @return true, if the certificate is not contained in the keystore
     */
    private static boolean certificateIsMissing(KeyStore keyStore, X509Certificate crt) {
        PKIXParameters params;
        try {
            params = new PKIXParameters(keyStore);
        } catch (KeyStoreException ex) {
            throw new AssertionError(ex);
        } catch (InvalidAlgorithmParameterException ex) {
            throw new RuntimeException(ex);
        }
        String id = crt.getSubjectX500Principal().getName();
        for (TrustAnchor ta : params.getTrustAnchors()) {
            X509Certificate cert = ta.getTrustedCert();
            if (Objects.equals(id, cert.getSubjectX500Principal().getName()))
                return false;
        }
        return true;
    }
}
