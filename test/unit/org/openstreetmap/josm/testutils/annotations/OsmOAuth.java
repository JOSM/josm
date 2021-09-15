// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Enable OAuth in tests
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(OsmOAuth.OAuthExtension.class)
public @interface OsmOAuth {
    /**
     * The class to use for getting tokens
     * @return The class that will be instantiated. Must have a no-args constructor.
     */
    Class<? extends OsmConnection.OAuthAccessTokenFetcher> value() default DefaultOAuthAccessTokenFetcher.class;

    /**
     * Set up the token fetcher for OAuth in OSM
     */
    class OAuthExtension implements AfterAllCallback, BeforeAllCallback {
        static final OsmConnection.OAuthAccessTokenFetcher defaultFetcher = new DefaultOAuthAccessTokenFetcher();

        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            OsmConnection.setOAuthAccessTokenFetcher(u -> {
                throw new JosmRuntimeException("OsmConnection.setOAuthAccessTokenFetcher() has not been called");
            });
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Class<? extends OsmConnection.OAuthAccessTokenFetcher> clazz =
                    AnnotationUtils.findFirstParentAnnotation(context, OsmOAuth.class).map(OsmOAuth::value)
                            .orElseThrow(() -> (AssertionError) fail("No @OAuth annotation found"));
            // Special case this (expected) common case -- actually instantiating it will cause mocks to fail.
            if (DefaultOAuthAccessTokenFetcher.class.equals(clazz)) {
                OsmConnection.setOAuthAccessTokenFetcher(OAuthAuthorizationWizard::obtainAccessToken);
            } else {
                OsmConnection.setOAuthAccessTokenFetcher(clazz.getConstructor().newInstance());
            }
        }
    }

    class DefaultOAuthAccessTokenFetcher implements OsmConnection.OAuthAccessTokenFetcher {
        @Override
        public void obtainAccessToken(URL serverUrl) throws InvocationTargetException, InterruptedException {
            OsmConnection.setOAuthAccessTokenFetcher(OAuthAuthorizationWizard::obtainAccessToken);
        }
    }
}
