// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link OAuthParameters}.
 */
@BasicPreferences
class OAuthParametersTest {

    @AfterEach
    void tearDown() {
        NetworkManager.setOnline(OnlineResource.ALL);
        // the API URL is static state shared with the other tests of this class
        Config.getPref().put("osm-server.url", null);
    }

    /**
     * {@link OAuthParameters#createFromApiUrl} answers for the server it is asked about, whichever server is
     * configured in the preferences, and finds the parameters the user entered in the advanced OAuth settings.
     * <p>
     * Non-regression test for #24925: the URL was reduced to the host before the fallback to
     * {@link OAuthParameters#createDefault}, and a host is never a valid URL, so the defaults of the configured
     * API were returned instead of those of the server which was asked about.
     */
    @Test
    void testCreateFromApiUrlUsesTheRequestedServer() {
        // the RFC 8414 lookup must not make a network request
        NetworkManager.setOffline(OnlineResource.ALL);
        final String thirdParty = "https://nonprod-mapops.example.org/api";
        Config.getPref().put("osm-server.url", thirdParty);

        // a server JOSM has a client id for is answered with that client id, not with the configured server's
        assertEquals("Hl5yIhFS-Egj6aY7A35ouLOuZl0EHjj8JJQQ46IO96E",
                OAuthParameters.createFromApiUrl("https://api.openhistoricalmap.org/api", OAuthVersion.OAuth20).getClientId());
        // a server JOSM knows nothing about has no client id until the user enters one
        assertEquals("", OAuthParameters.createFromApiUrl(thirdParty, OAuthVersion.OAuth20).getClientId());

        // once the parameters are remembered - as the authentication preferences do, under the API URL - they
        // are found both by the API URL and by the host, which is what a connection has at hand
        new OAuth20Parameters("entered-client-id", null, thirdParty, thirdParty, "http://127.0.0.1:8111/oauth_authorization")
                .rememberPreferences();
        assertEquals("entered-client-id",
                OAuthParameters.createFromApiUrl(thirdParty, OAuthVersion.OAuth20).getClientId());
        assertEquals("https://api.openhistoricalmap.org/api",
                OAuthParameters.createFromApiUrl("https://api.openhistoricalmap.org/api", OAuthVersion.OAuth20).getApiUrl());
    }

    /**
     * Unit test of method {@link OAuthParameters#createDefault}.
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    void testCreateDefault() {
        IOAuthParameters def = OAuthParameters.createDefault();
        assertNotNull(def);
        assertEquals(def, OAuthParameters.createDefault(Config.getUrls().getDefaultOsmApiUrl(), OAuthVersion.OAuth20));
        IOAuthParameters dev = OAuthParameters.createDefault("https://api06.dev.openstreetmap.org/api", OAuthVersion.OAuth20);
        assertNotNull(dev);
        assertNotEquals(def, dev);
        Logging.setLogLevel(Logging.LEVEL_TRACE); // enable trace for line coverage
        assertEquals(def, OAuthParameters.createDefault("wrong_url", OAuthVersion.OAuth20));
    }

    /**
     * Unit test of methods {@link OAuthParameters#equals} and {@link OAuthParameters#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(OAuth20Parameters.class).usingGetClass().verify();
    }
}
