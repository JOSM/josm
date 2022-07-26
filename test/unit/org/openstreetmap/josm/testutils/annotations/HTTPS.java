// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Set up HTTPS certificates
 *
 * @author Taylor Smock
 * @see JOSMTestRules#https
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@BasicPreferences
@ExtendWith(HTTPS.HTTPSExtension.class)
public @interface HTTPS {
    /**
     * Set up HTTPS certificates.
     */
    class HTTPSExtension implements AfterAllCallback, BeforeAllCallback {
        @Override
        public void afterAll(final ExtensionContext context) throws Exception {
            /*
             * CertificateAmendment loads all certificates, then creates a new SSLContext for TLSv1.2
             * This pretty much undoes all that work (none of the `getInstance` calls returns a singular instance --
             * it is always a new instance).
             */
            SSLContext.setDefault(SSLContext.getInstance("Default"));
        }

        @Override
        public void beforeAll(final ExtensionContext context) throws Exception {
            CertificateAmendment.addMissingCertificates();
        }
    }
}
