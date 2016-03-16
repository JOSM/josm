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
        "resource://data/security/StartCom_Certification_Authority.pem"
    };

    private static final String[] SHA_HASHES = {
        "139a5e4a4e0fa505378c72c5f700934ce8333f4e6b1b508886c4b0eb14f4be99",
        "916a8f9232328192968c81c8edb672fa539f726861dfe379ca722050e19962cd"
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
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
            byte[] certBytes = certCF.getByteContent();
            byte[] sha = md.digest(certBytes);
            if (!SHA_HASHES[i].equals(Utils.toHexString(sha)))
                throw new RuntimeException(tr("certificate hash mismatch"));

            ByteArrayInputStream certIS = new ByteArrayInputStream(certBytes);
            X509Certificate cert;
            try {
                cert = (X509Certificate) cf.generateCertificate(certIS);
            } catch (CertificateException ex) {
                throw new IOException(ex);
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
