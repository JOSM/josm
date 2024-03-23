// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;


import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.Authenticator;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Utils;

/**
 * Used for tests that require a test user to be defined via -Dosm.username and -Dosm.password.
 * This uses the {@link OsmApi.APIType#DEV} server.
 */
@BasicPreferences
@OsmApi(OsmApi.APIType.DEV)
@ExtendWith(TestUser.TestUserExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestUser {
    /**
     * Initialize a user for tests
     */
    class TestUserExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            UserIdentityManager.getInstance().setAnonymous();
            CredentialsManager.getInstance().purgeCredentialsCache(Authenticator.RequestorType.SERVER);
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            this.beforeEach(context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            assumeTrue(TestUtils.areCredentialsProvided(),
                    "OSM DEV API credentials not provided. Please define them with -Dosm.oauth2");
            final String oauth2 = Utils.getSystemProperty("osm.oauth2");
            Config.getPref().put("osm-server.auth-method", "oauth20");

            // don't use atomic upload, the test API server can't cope with large diff uploads
            Config.getPref().putBoolean("osm-server.atomic-upload", false);
            final String serverUrl = org.openstreetmap.josm.io.OsmApi.getOsmApi().getServerUrl();
            final OAuth20Token token = new OAuth20Token(OAuthParameters.createDefault(),
                    "{\"token_type\":\"bearer\", \"access_token\": \"" + oauth2 + "\"}");
            OAuthAccessTokenHolder.getInstance().setAccessToken(serverUrl, token);
            CredentialsManager.getInstance().storeOAuthAccessToken(serverUrl, token);
            if (!UserIdentityManager.getInstance().isFullyIdentified()) {
                UserIdentityManager.getInstance().initFromOAuth();
            }
        }
    }
}
