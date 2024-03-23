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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
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
        private final Collection<String> aliases;
        private final String httpsWebSite;

        NativeCertAmend(Collection<String> aliases, String filename, String sha256, String httpsWebSite) {
            super(filename, sha256);
            this.aliases = Objects.requireNonNull(aliases);
            this.httpsWebSite = Objects.requireNonNull(httpsWebSite);
        }

        /**
         * Returns the native aliases in System Root Certificates keystore/keychain.
         * @return the native aliases in System Root Certificates keystore/keychain
         * @since 15006
         */
        public final Collection<String> getNativeAliases() {
            return aliases;
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
            return String.join(" / ", aliases);
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
     * To find correct values, see:<ul>
     * <li><a href="https://ccadb-public.secure.force.com/mozilla/IncludedCACertificateReport">Mozilla List</a></li>
     * <li><a href="https://ccadb-public.secure.force.com/microsoft/IncludedCACertificateReportForMSFT">Microsoft List</a></li>
     * <li><a href="https://support.apple.com/en-us/HT210770">Apple List</a></li>
     * </ul>
     */
    private static final NativeCertAmend[] PLATFORM_CERT_AMEND = {
        // #14649 - Government of Netherlands - for PDOK aerial imagery at ​https://geodata.nationaalgeoregister.nl (expires 2028)
        new NativeCertAmend(Arrays.asList("Government of Netherlands G3", "Staat der Nederlanden Root CA - G3"),
                "Staat_der_Nederlanden_Root_CA_-_G3.crt",
                "3c4fb0b95ab8b30032f432b86f535fe172c185d0fd39865837cf36187fa6f428",
                "https://roottest-g3.pkioverheid.nl"),
        // #15178 - Trusted and used by French Government - for cadastre - https://www.certigna.fr/autorites/index.xhtml?ac=Racine#lracine
        // (expires 2027, should be in Java 21)
        new NativeCertAmend(Collections.singleton("Certigna"),
                "Certigna.crt",
                "e3b6a2db2ed7ce48842f7ac53241c7b71d54144bfb40c11f3f1d0b42f5eea12d",
                "https://www.certigna.fr"),
        // #16307 - Trusted and used by Slovakian Government - https://eidas.disig.sk/en/cacert/ (expires 2042)
        new NativeCertAmend(Collections.singleton("CA Disig Root R2"),
                "CA_Disig_Root_R2.pem",
                "e23d4a036d7b70e9f595b1422079d2b91edfbb1fb651a0633eaa8a9dc5f80703",
                "https://eidas.disig.sk"),
        // #17668 - used by city of Budapest - for https://terinfo.ujbuda.hu - https://e-szigno.hu/ (expires 2029)
        new NativeCertAmend(Collections.singleton("MicroSec e-Szigno Root CA 2009"),
                "Microsec_e-Szigno_Root_CA_2009.pem",
                "3c5f81fea5fab82c64bfa2eaecafcde8e077fc8620a7cae537163df36edbf378",
                "https://e-szigno.hu"),
        // #18920 - Spanish Government - https://www.sede.fnmt.gob.es/descargas/certificados-raiz-de-la-fnmt (expires 2030)
        new NativeCertAmend(Collections.singleton("AC RAIZ FNMT-RCM"),
                "AC_RAIZ_FNMT-RCM.pem",
                "ebc5570c29018c4d67b1aa127baf12f703b4611ebc17b7dab5573894179b93fa",
                "https://www.sede.fnmt.gob.es"),
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
                X509Certificate cert = PlatformManager.getPlatform().getX509Certificate(certAmend);
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
                    Logging.debug("Adding certificate for TLS connections: " + cert.getSubjectX500Principal().getName());
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
        return params.getTrustAnchors().stream()
                .map(TrustAnchor::getTrustedCert)
                .noneMatch(c -> Objects.equals(crt.getSubjectX500Principal().getName(), c.getSubjectX500Principal().getName()));
    }
}
