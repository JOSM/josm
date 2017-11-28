// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.util.Objects;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;

/**
 * This class manages an immutable set of OAuth parameters.
 * @since 2747
 */
public class OAuthParameters {

    /**
     * The default JOSM OAuth consumer key (created by user josmeditor).
     */
    public static final String DEFAULT_JOSM_CONSUMER_KEY = "F7zPYlVCqE2BUH9Hr4SsWZSOnrKjpug1EgqkbsSb";
    /**
     * The default JOSM OAuth consumer secret (created by user josmeditor).
     */
    public static final String DEFAULT_JOSM_CONSUMER_SECRET = "rIkjpPcBNkMQxrqzcOvOC4RRuYupYr7k8mfP13H5";

    /**
     * Replies a set of default parameters for a consumer accessing the standard OSM server
     * at {@link OsmApi#DEFAULT_API_URL}.
     *
     * @return a set of default parameters
     */
    public static OAuthParameters createDefault() {
        return createDefault(null);
    }

    /**
     * Replies a set of default parameters for a consumer accessing an OSM server
     * at the given API url. URL parameters are only set if the URL equals {@link OsmApi#DEFAULT_API_URL}
     * or references the domain "dev.openstreetmap.org", otherwise they may be <code>null</code>.
     *
     * @param apiUrl The API URL for which the OAuth default parameters are created. If null or empty, the default OSM API url is used.
     * @return a set of default parameters for the given {@code apiUrl}
     * @since 5422
     */
    public static OAuthParameters createDefault(String apiUrl) {
        final String consumerKey;
        final String consumerSecret;
        final String serverUrl;

        if (!Utils.isValidUrl(apiUrl)) {
            apiUrl = null;
        }

        if (apiUrl != null && !OsmApi.DEFAULT_API_URL.equals(apiUrl)) {
            consumerKey = ""; // a custom consumer key is required
            consumerSecret = ""; // a custom consumer secret is requireds
            serverUrl = apiUrl.replaceAll("/api$", "");
        } else {
            consumerKey = DEFAULT_JOSM_CONSUMER_KEY;
            consumerSecret = DEFAULT_JOSM_CONSUMER_SECRET;
            serverUrl = Main.getOSMWebsite();
        }

        return new OAuthParameters(
                consumerKey,
                consumerSecret,
                serverUrl + "/oauth/request_token",
                serverUrl + "/oauth/access_token",
                serverUrl + "/oauth/authorize",
                serverUrl + "/login",
                serverUrl + "/logout");
    }

    /**
     * Replies a set of parameters as defined in the preferences.
     *
     * @param apiUrl the API URL. Must not be null.
     * @return the parameters
     */
    public static OAuthParameters createFromApiUrl(String apiUrl) {
        OAuthParameters parameters = createDefault(apiUrl);
        return new OAuthParameters(
                Config.getPref().get("oauth.settings.consumer-key", parameters.getConsumerKey()),
                Config.getPref().get("oauth.settings.consumer-secret", parameters.getConsumerSecret()),
                Config.getPref().get("oauth.settings.request-token-url", parameters.getRequestTokenUrl()),
                Config.getPref().get("oauth.settings.access-token-url", parameters.getAccessTokenUrl()),
                Config.getPref().get("oauth.settings.authorise-url", parameters.getAuthoriseUrl()),
                Config.getPref().get("oauth.settings.osm-login-url", parameters.getOsmLoginUrl()),
                Config.getPref().get("oauth.settings.osm-logout-url", parameters.getOsmLogoutUrl()));
    }

    /**
     * Remembers the current values in the preferences.
     */
    public void rememberPreferences() {
        Config.getPref().put("oauth.settings.consumer-key", getConsumerKey());
        Config.getPref().put("oauth.settings.consumer-secret", getConsumerSecret());
        Config.getPref().put("oauth.settings.request-token-url", getRequestTokenUrl());
        Config.getPref().put("oauth.settings.access-token-url", getAccessTokenUrl());
        Config.getPref().put("oauth.settings.authorise-url", getAuthoriseUrl());
        Config.getPref().put("oauth.settings.osm-login-url", getOsmLoginUrl());
        Config.getPref().put("oauth.settings.osm-logout-url", getOsmLogoutUrl());
    }

    private final String consumerKey;
    private final String consumerSecret;
    private final String requestTokenUrl;
    private final String accessTokenUrl;
    private final String authoriseUrl;
    private final String osmLoginUrl;
    private final String osmLogoutUrl;

    /**
     * Constructs a new {@code OAuthParameters}.
     * @param consumerKey consumer key
     * @param consumerSecret consumer secret
     * @param requestTokenUrl request token URL
     * @param accessTokenUrl access token URL
     * @param authoriseUrl authorise URL
     * @param osmLoginUrl the OSM login URL (for automatic mode)
     * @param osmLogoutUrl the OSM logout URL (for automatic mode)
     * @see #createDefault
     * @see #createFromApiUrl
     * @since 9220
     */
    public OAuthParameters(String consumerKey, String consumerSecret,
                           String requestTokenUrl, String accessTokenUrl, String authoriseUrl, String osmLoginUrl, String osmLogoutUrl) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.requestTokenUrl = requestTokenUrl;
        this.accessTokenUrl = accessTokenUrl;
        this.authoriseUrl = authoriseUrl;
        this.osmLoginUrl = osmLoginUrl;
        this.osmLogoutUrl = osmLogoutUrl;
    }

    /**
     * Creates a clone of the parameters in <code>other</code>.
     *
     * @param other the other parameters. Must not be null.
     * @throws IllegalArgumentException if other is null
     */
    public OAuthParameters(OAuthParameters other) {
        CheckParameterUtil.ensureParameterNotNull(other, "other");
        this.consumerKey = other.consumerKey;
        this.consumerSecret = other.consumerSecret;
        this.accessTokenUrl = other.accessTokenUrl;
        this.requestTokenUrl = other.requestTokenUrl;
        this.authoriseUrl = other.authoriseUrl;
        this.osmLoginUrl = other.osmLoginUrl;
        this.osmLogoutUrl = other.osmLogoutUrl;
    }

    /**
     * Gets the consumer key.
     * @return The consumer key
     */
    public String getConsumerKey() {
        return consumerKey;
    }

    /**
     * Gets the consumer secret.
     * @return The consumer secret
     */
    public String getConsumerSecret() {
        return consumerSecret;
    }

    /**
     * Gets the request token URL.
     * @return The request token URL
     */
    public String getRequestTokenUrl() {
        return requestTokenUrl;
    }

    /**
     * Gets the access token URL.
     * @return The access token URL
     */
    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }

    /**
     * Gets the authorise URL.
     * @return The authorise URL
     */
    public String getAuthoriseUrl() {
        return authoriseUrl;
    }

    /**
     * Gets the URL used to login users on the website (for automatic mode).
     * @return The URL used to login users
     */
    public String getOsmLoginUrl() {
        return osmLoginUrl;
    }

    /**
     * Gets the URL used to logout users on the website (for automatic mode).
     * @return The URL used to logout users
     */
    public String getOsmLogoutUrl() {
        return osmLogoutUrl;
    }

    /**
     * Builds an {@link OAuthConsumer} based on these parameters.
     *
     * @return the consumer
     */
    public OAuthConsumer buildConsumer() {
        return new SignpostAdapters.OAuthConsumer(consumerKey, consumerSecret);
    }

    /**
     * Builds an {@link OAuthProvider} based on these parameters and a OAuth consumer <code>consumer</code>.
     *
     * @param consumer the consumer. Must not be null.
     * @return the provider
     * @throws IllegalArgumentException if consumer is null
     */
    public OAuthProvider buildProvider(OAuthConsumer consumer) {
        CheckParameterUtil.ensureParameterNotNull(consumer, "consumer");
        return new SignpostAdapters.OAuthProvider(
                requestTokenUrl,
                accessTokenUrl,
                authoriseUrl
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OAuthParameters that = (OAuthParameters) o;
        return Objects.equals(consumerKey, that.consumerKey) &&
                Objects.equals(consumerSecret, that.consumerSecret) &&
                Objects.equals(requestTokenUrl, that.requestTokenUrl) &&
                Objects.equals(accessTokenUrl, that.accessTokenUrl) &&
                Objects.equals(authoriseUrl, that.authoriseUrl) &&
                Objects.equals(osmLoginUrl, that.osmLoginUrl) &&
                Objects.equals(osmLogoutUrl, that.osmLogoutUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerKey, consumerSecret, requestTokenUrl, accessTokenUrl, authoriseUrl, osmLoginUrl, osmLogoutUrl);
    }
}
