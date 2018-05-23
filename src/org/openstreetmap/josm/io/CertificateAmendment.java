// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
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
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
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

    /**
     * A certificate amendment.
     * @since 11943
     */
    public static class CertAmend {
        private final String filename;
        private final String sha256;

        protected CertAmend(String filename, String sha256) {
            this.filename = Objects.requireNonNull(filename);
            this.sha256 = Objects.requireNonNull(sha256);
        }

        /**
         * Returns the certificate filename.
         * @return filename for both JOSM embedded certificate and Unix platform certificate
         * @since 12241
         */
        public final String getFilename() {
            return filename;
        }

        /**
         * Returns the SHA-256 hash.
         * @return the SHA-256 hash, in hexadecimal
         */
        public final String getSha256() {
            return sha256;
        }
    }

    /**
     * An embedded certificate amendment.
     * @since 13450
     */
    public static class EmbeddedCertAmend extends CertAmend {
        private final String url;

        EmbeddedCertAmend(String url, String filename, String sha256) {
            super(filename, sha256);
            this.url = Objects.requireNonNull(url);
        }

        /**
         * Returns the embedded URL in JOSM jar.
         * @return path for JOSM embedded certificate
         */
        public final String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return url;
        }
    }

    /**
     * A certificate amendment relying on native platform certificate store.
     * @since 13450
     */
    public static class NativeCertAmend extends CertAmend {
        private final String winAlias;
        private final String macAlias;
        private final String httpsWebSite;

        NativeCertAmend(String winAlias, String macAlias, String filename, String sha256, String httpsWebSite) {
            super(filename, sha256);
            this.winAlias = Objects.requireNonNull(winAlias);
            this.macAlias = Objects.requireNonNull(macAlias);
            this.httpsWebSite = Objects.requireNonNull(httpsWebSite);
        }

        /**
         * Returns the Windows alias in System Root Certificates keystore.
         * @return the Windows alias in System Root Certificates keystore
         */
        public final String getWinAlias() {
            return winAlias;
        }

        /**
         * Returns the macOS alias in System Root Certificates keychain.
         * @return the macOS alias in System Root Certificates keychain
         */
        public final String getMacAlias() {
            return macAlias;
        }

        /**
         * Returns the https website we need to call to notify Windows we need its root certificate.
         * @return the https website signed with this root CA
         * @since 13451
         */
        public String getWebSite() {
            return httpsWebSite;
        }

        @Override
        public String toString() {
            String result = winAlias;
            if (!winAlias.equals(macAlias)) {
                result += " / " + macAlias;
            }
            return result;
        }
    }

    /**
     * Certificates embedded in JOSM
     */
    private static final EmbeddedCertAmend[] CERT_AMEND = {
    };

    /**
     * Certificates looked into platform native keystore and not embedded in JOSM.
     * Identifiers must match Windows/macOS keystore aliases and Unix filenames for efficient search.
     * To find correct values, see https://ccadb-public.secure.force.com/mozilla/IncludedCACertificateReport
     */
    private static final NativeCertAmend[] PLATFORM_CERT_AMEND = {
        // Let's Encrypt - should be included in JDK, but problems with Ubuntu 18.04, see #15851
        new NativeCertAmend("DST Root CA X3", "DST Root CA X3",
                "DST_Root_CA_X3.pem",
                "0687260331a72403d909f105e69bcf0d32e1bd2493ffc6d9206d11bcd6770739",
                "https://acme-v02.api.letsencrypt.org"),
        // Government of Netherlands
        new NativeCertAmend("Staat der Nederlanden Root CA - G2", "Staat der Nederlanden Root CA - G2",
                "Staat_der_Nederlanden_Root_CA_-_G2.crt",
                "668c83947da63b724bece1743c31a0e6aed0db8ec5b31be377bb784f91b6716f",
                "https://roottest-g2.pkioverheid.nl"),
        // Government of Netherlands
        new NativeCertAmend("Government of Netherlands G3", "Staat der Nederlanden Root CA - G3",
                "Staat_der_Nederlanden_Root_CA_-_G3.crt",
                "3c4fb0b95ab8b30032f432b86f535fe172c185d0fd39865837cf36187fa6f428",
                "https://roottest-g3.pkioverheid.nl"),
        // Trusted and used by French Government - https://www.certigna.fr/autorites/index.xhtml?ac=Racine#lracine
        new NativeCertAmend("Certigna", "Certigna", "Certigna.crt",
                "e3b6a2db2ed7ce48842f7ac53241c7b71d54144bfb40c11f3f1d0b42f5eea12d",
                "https://www.certigna.fr"),
        // Trusted and used by Slovakian Government - https://eidas.disig.sk/en/cacert/
        new NativeCertAmend("CA Disig Root R2", "CA Disig Root R2", "CA_Disig_Root_R2.pem",
                "e23d4a036d7b70e9f595b1422079d2b91edfbb1fb651a0633eaa8a9dc5f80703",
                "https://eidas.disig.sk"),
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
        if (!Config.getPref().getBoolean("tls.add-missing-certificates", true))
            return;
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Path cacertsPath = Paths.get(Utils.getSystemProperty("java.home"), "lib", "security", "cacerts");
        try (InputStream is = Files.newInputStream(cacertsPath)) {
            keyStore.load(is, "changeit".toCharArray());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to load keystore", e);
            return;
        }

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        boolean certificateAdded = false;
        // Add embedded certificates. Exit in case of error
        for (EmbeddedCertAmend certAmend : CERT_AMEND) {
            try (CachedFile certCF = new CachedFile(certAmend.url)) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(certCF.getByteContent()));
                if (checkAndAddCertificate(md, cert, certAmend, keyStore)) {
                    certificateAdded = true;
                }
            }
        }

        try {
            // Try to add platform certificates. Do not exit in case of error (embedded certificates may be OK)
            for (NativeCertAmend certAmend : PLATFORM_CERT_AMEND) {
                X509Certificate cert = Main.platform.getX509Certificate(certAmend);
                if (checkAndAddCertificate(md, cert, certAmend, keyStore)) {
                    certificateAdded = true;
                }
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | IllegalStateException e) {
            Logging.error(e);
        }

        if (certificateAdded) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, tmf.getTrustManagers(), null);
            SSLContext.setDefault(sslContext);
        }
    }

    private static boolean checkAndAddCertificate(MessageDigest md, X509Certificate cert, CertAmend certAmend, KeyStore keyStore)
            throws CertificateEncodingException, KeyStoreException, InvalidAlgorithmParameterException {
        if (cert != null) {
            String sha256 = Utils.toHexString(md.digest(cert.getEncoded()));
            if (!certAmend.sha256.equals(sha256)) {
                throw new IllegalStateException(
                        tr("Error adding certificate {0} - certificate fingerprint mismatch. Expected {1}, was {2}",
                            certAmend, certAmend.sha256, sha256));
            }
            if (certificateIsMissing(keyStore, cert)) {
                if (Logging.isDebugEnabled()) {
                    Logging.debug(tr("Adding certificate for TLS connections: {0}", cert.getSubjectX500Principal().getName()));
                }
                String alias = "josm:" + certAmend.filename;
                keyStore.setCertificateEntry(alias, cert);
                return true;
            }
        }
        return false;
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
