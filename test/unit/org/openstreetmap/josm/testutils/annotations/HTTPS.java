// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Set up the HTTPS certificates
 * @author Taylor Smock
 * @since 18893
 * @see JOSMTestRules#https()
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@BasicPreferences
@ExtendWith(HTTPS.HTTPSExtension.class)
public @interface HTTPS {
    class HTTPSExtension implements BeforeEachCallback {
        private static boolean initialized;
        @Override
        public void beforeEach(ExtensionContext extensionContext) throws IOException, GeneralSecurityException {
            if (!initialized) {
                CertificateAmendment.addMissingCertificates();
                initialized = true;
            }
        }
    }
}
