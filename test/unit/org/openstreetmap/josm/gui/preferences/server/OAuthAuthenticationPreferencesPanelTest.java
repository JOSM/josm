// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuth20Exception;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Test class for {@link OAuthAuthenticationPreferencesPanel}
 */
@BasicPreferences
class OAuthAuthenticationPreferencesPanelTest {
    @AfterEach
    void tearDown() {
        OAuthAccessTokenHolder.getInstance().clear();
        for (Authenticator.RequestorType type : Authenticator.RequestorType.values()) {
            CredentialsManager.getInstance().purgeCredentialsCache(type);
        }
        List<Exception> exceptionList = new ArrayList<>();
        try {
            CredentialsManager.getInstance().storeOAuthAccessToken(null);
        } catch (CredentialsAgentException exception) {
            exceptionList.add(exception);
        }
        try {
            CredentialsManager.getInstance().storeOAuthAccessToken(OsmApi.getOsmApi().getHost(), null);
        } catch (CredentialsAgentException exception) {
            exceptionList.add(exception);
        }

        assertAll(exceptionList.stream().map(e -> () -> fail(e)));
    }

    @ParameterizedTest
    @EnumSource(value = OAuthVersion.class, names = {"OAuth10a", "OAuth20"})
    void testRemoveToken(OAuthVersion oAuthVersion) throws ReflectiveOperationException, CredentialsAgentException, OAuth20Exception {
        final OAuthAuthenticationPreferencesPanel panel = new OAuthAuthenticationPreferencesPanel(oAuthVersion);
        final Field pnlNotYetAuthorised = OAuthAuthenticationPreferencesPanel.class.getDeclaredField("pnlNotYetAuthorised");
        final Field pnlAlreadyAuthorised = OAuthAuthenticationPreferencesPanel.class.getDeclaredField("pnlAlreadyAuthorised");
        final JPanel holder = (JPanel) panel.getComponent(0);
        ReflectionUtils.setObjectsAccessible(pnlNotYetAuthorised, pnlAlreadyAuthorised);
        panel.initFromPreferences();
        assertNull(getAuthorization(oAuthVersion));
        assertSame(pnlNotYetAuthorised.get(panel), holder.getComponent(0), "No authentication should be set yet");
        addAuthorization(oAuthVersion);
        assertNotNull(getAuthorization(oAuthVersion));
        panel.initFromPreferences();
        assertSame(pnlAlreadyAuthorised.get(panel), holder.getComponent(0), "Authentication should now be set");
        assertNotNull(getAuthorization(oAuthVersion));
        final JPanel buttons = (JPanel) ((JPanel) pnlAlreadyAuthorised.get(panel)).getComponent(6);
        final JButton action = (JButton) buttons.getComponent(oAuthVersion == OAuthVersion.OAuth10a ? 2 : 0);
        action.getAction().actionPerformed(null);
        panel.saveToPreferences(); // Save to preferences should make the removal permanent
        // Ensure that the token holder has been reset for OAuth 1.0a
        OAuthAccessTokenHolder.getInstance().clear();
        OAuthAccessTokenHolder.getInstance().init(CredentialsManager.getInstance());
        assertNull(getAuthorization(oAuthVersion), "No authentication data should be stored");
        panel.initFromPreferences();
        assertSame(pnlNotYetAuthorised.get(panel), holder.getComponent(0), "No authentication should be set");
    }

    /**
     * Add authorization preferences for the specified oauth version
     * @param oAuthVersion The oauth version to use
     * @throws CredentialsAgentException If something goes wrong
     * @throws OAuth20Exception If something goes wrong
     */
    private static void addAuthorization(OAuthVersion oAuthVersion) throws CredentialsAgentException, OAuth20Exception {
        switch (oAuthVersion) {
            case OAuth10a:
                CredentialsManager.getInstance().storeOAuthAccessToken(new OAuthToken("fake_key", "fake_secret"));
                break;
            case OAuth20:
            case OAuth21:
                CredentialsManager.getInstance().storeOAuthAccessToken(OsmApi.getOsmApi().getHost(),
                        new OAuth20Token(new OAuth20Parameters("fake_id", "fake_secret", "https://fake.url/token",
                                "https://fake.url/authorize", OsmApi.getOsmApi().getBaseUrl(), "http://127.0.0.1:8111/oauth_authorization"),
                                "{\"access_token\": \"fake_token\", \"token_type\": \"bearer\"}"));
        }
        assertNotNull(getAuthorization(oAuthVersion));
    }

    /**
     * Get the authorization token
     * @param oAuthVersion The oauth version
     * @return The token
     */
    private static Object getAuthorization(OAuthVersion oAuthVersion) {
        OAuthAccessTokenHolder.getInstance().clear();
        OAuthAccessTokenHolder.getInstance().setAccessToken(OsmApi.getOsmApi().getHost(), (IOAuthToken) null);
        OAuthAccessTokenHolder.getInstance().init(CredentialsManager.getInstance());
        // Ensure that we are not saving authorization data
        switch (oAuthVersion) {
            case OAuth10a:
                return OAuthAccessTokenHolder.getInstance().getAccessToken();
            case OAuth20:
            case OAuth21:
                return OAuthAccessTokenHolder.getInstance().getAccessToken(OsmApi.getOsmApi().getHost(), oAuthVersion);
            default:
                throw new AssertionError("OAuth version not understood");
        }
    }
}
