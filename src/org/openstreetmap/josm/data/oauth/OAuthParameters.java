// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IUrls;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;

/**
 * This class manages an immutable set of OAuth parameters.
 * @since 2747
 */
public class OAuthParameters implements IOAuthParameters {

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
     * at {@link IUrls#getDefaultOsmApiUrl}.
     *
     * @return a set of default parameters
     */
    public static OAuthParameters createDefault() {
        return createDefault(null);
    }

    /**
     * Replies a set of default parameters for a consumer accessing an OSM server
     * at the given API url. URL parameters are only set if the URL equals {@link IUrls#getDefaultOsmApiUrl}
     * or references the domain "dev.openstreetmap.org", otherwise they may be <code>null</code>.
     *
     * @param apiUrl The API URL for which the OAuth default parameters are created. If null or empty, the default OSM API url is used.
     * @return a set of default parameters for the given {@code apiUrl}
     * @since 5422
     */
    public static OAuthParameters createDefault(String apiUrl) {
        return (OAuthParameters) createDefault(apiUrl, OAuthVersion.OAuth10a);
    }

    /**
     * Replies a set of default parameters for a consumer accessing an OSM server
     * at the given API url. URL parameters are only set if the URL equals {@link IUrls#getDefaultOsmApiUrl}
     * or references the domain "dev.openstreetmap.org", otherwise they may be <code>null</code>.
     *
     * @param apiUrl The API URL for which the OAuth default parameters are created. If null or empty, the default OSM API url is used.
     * @param oAuthVersion The OAuth version to create default parameters for
     * @return a set of default parameters for the given {@code apiUrl}
     * @since 18650
     */
    public static IOAuthParameters createDefault(String apiUrl, OAuthVersion oAuthVersion) {
        if (!Utils.isValidUrl(apiUrl)) {
            apiUrl = null;
        }

        switch (oAuthVersion) {
            case OAuth10a:
                return getDefaultOAuth10Parameters(apiUrl);
            case OAuth20:
            case OAuth21: // For now, OAuth 2.1 (draft) is just OAuth 2.0 with mandatory extensions, which we implement.
                return getDefaultOAuth20Parameters(apiUrl);
            default:
                throw new IllegalArgumentException("Unknown OAuth version: " + oAuthVersion);
        }
    }

    /**
     * Get the default OAuth 2.0 parameters
     * @param apiUrl The API url
     * @return The default parameters
     */
    private static OAuth20Parameters getDefaultOAuth20Parameters(String apiUrl) {
        final String clientId;
        final String clientSecret;
        final String redirectUri;
        final String baseUrl;
        if (apiUrl != null && !Config.getUrls().getDefaultOsmApiUrl().equals(apiUrl) && !"http://invalid".equals(apiUrl)) {
            clientId = "";
            clientSecret = "";
            baseUrl = apiUrl;
            HttpClient client = null;
            redirectUri = "";
            // Check if the server is RFC 8414 compliant
            try {
                client = HttpClient.create(new URL(apiUrl + (apiUrl.endsWith("/") ? "" : "/") + ".well-known/oauth-authorization-server"));
                HttpClient.Response response = client.connect();
                if (response.getResponseCode() == 200) {
                    try (BufferedReader reader = response.getContentReader();
                         JsonReader jsonReader = Json.createReader(reader)) {
                        JsonStructure structure = jsonReader.read();
                        if (structure.getValueType() == JsonValue.ValueType.OBJECT) {
                            return parseAuthorizationServerMetadataResponse(clientId, clientSecret, apiUrl,
                                    redirectUri, structure.asJsonObject());
                        }
                    }
                }
            } catch (IOException | OAuthException e) {
                Logging.trace(e);
            } finally {
                if (client != null) client.disconnect();
            }
        } else {
            clientId = "edPII614Lm0_0zEpc_QzEltA9BUll93-Y-ugRQUoHMI";
            // We don't actually use the client secret in our authorization flow.
            clientSecret = null;
            baseUrl = "https://www.openstreetmap.org/oauth2";
            redirectUri = "http://127.0.0.1:8111/oauth_authorization";
            apiUrl = OsmApi.getOsmApi().getBaseUrl();
        }
        return new OAuth20Parameters(clientId, clientSecret, baseUrl, apiUrl, redirectUri);
    }

    /**
     * Parse the response from <a href="https://www.rfc-editor.org/rfc/rfc8414.html">RFC 8414</a>
     * (OAuth 2.0 Authorization Server Metadata)
     * @return The parameters for the server metadata
     */
    private static OAuth20Parameters parseAuthorizationServerMetadataResponse(String clientId, String clientSecret,
                                                                              String apiUrl, String redirectUri,
                                                                              JsonObject serverMetadata)
            throws OAuthException {
        final String authorizationEndpoint = serverMetadata.getString("authorization_endpoint", null);
        final String tokenEndpoint = serverMetadata.getString("token_endpoint", null);
        // This may also have additional documentation like what the endpoints allow (e.g. scopes, algorithms, etc.)
        if (authorizationEndpoint == null || tokenEndpoint == null) {
            throw new OAuth20Exception("Either token endpoint or authorization endpoints are missing");
        }
        return new OAuth20Parameters(clientId, clientSecret, tokenEndpoint, authorizationEndpoint, apiUrl, redirectUri);
    }

    /**
     * Get the default OAuth 1.0a parameters
     * @param apiUrl The api url
     * @return The default parameters
     */
    private static OAuthParameters getDefaultOAuth10Parameters(String apiUrl) {
        final String consumerKey;
        final String consumerSecret;
        final String serverUrl;

        if (apiUrl != null && !Config.getUrls().getDefaultOsmApiUrl().equals(apiUrl)) {
            consumerKey = ""; // a custom consumer key is required
            consumerSecret = ""; // a custom consumer secret is requireds
            serverUrl = apiUrl.replaceAll("/api$", "");
        } else {
            consumerKey = DEFAULT_JOSM_CONSUMER_KEY;
            consumerSecret = DEFAULT_JOSM_CONSUMER_SECRET;
            serverUrl = Config.getUrls().getOSMWebsite();
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
        return (OAuthParameters) createFromApiUrl(apiUrl, OAuthVersion.OAuth10a);
    }

    /**
     * Replies a set of parameters as defined in the preferences.
     *
     * @param oAuthVersion The OAuth version to use.
     * @param apiUrl the API URL. Must not be {@code null}.
     * @return the parameters
     * @since 18650
     */
    public static IOAuthParameters createFromApiUrl(String apiUrl, OAuthVersion oAuthVersion) {
        IOAuthParameters parameters = createDefault(apiUrl, oAuthVersion);
        switch (oAuthVersion) {
            case OAuth10a:
                OAuthParameters oauth10aParameters = (OAuthParameters) parameters;
                return new OAuthParameters(
                    Config.getPref().get("oauth.settings.consumer-key", oauth10aParameters.getConsumerKey()),
                    Config.getPref().get("oauth.settings.consumer-secret", oauth10aParameters.getConsumerSecret()),
                    Config.getPref().get("oauth.settings.request-token-url", oauth10aParameters.getRequestTokenUrl()),
                    Config.getPref().get("oauth.settings.access-token-url", oauth10aParameters.getAccessTokenUrl()),
                    Config.getPref().get("oauth.settings.authorise-url", oauth10aParameters.getAuthoriseUrl()),
                    Config.getPref().get("oauth.settings.osm-login-url", oauth10aParameters.getOsmLoginUrl()),
                    Config.getPref().get("oauth.settings.osm-logout-url", oauth10aParameters.getOsmLogoutUrl()));
            case OAuth20:
            case OAuth21: // Right now, OAuth 2.1 will work with our OAuth 2.0 implementation
                OAuth20Parameters oAuth20Parameters = (OAuth20Parameters) parameters;
                try {
                    IOAuthToken storedToken = CredentialsManager.getInstance().lookupOAuthAccessToken(apiUrl);
                    return storedToken != null ? storedToken.getParameters() : oAuth20Parameters;
                } catch (CredentialsAgentException e) {
                    Logging.trace(e);
                }
                return oAuth20Parameters;
            default:
                throw new IllegalArgumentException("Unknown OAuth version: " + oAuthVersion);
        }
    }

    /**
     * Remembers the current values in the preferences.
     */
    @Override
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
    @Override
    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }

    @Override
    public String getAuthorizationUrl() {
        return this.authoriseUrl;
    }

    @Override
    public OAuthVersion getOAuthVersion() {
        return OAuthVersion.OAuth10a;
    }

    @Override
    public String getClientId() {
        return this.consumerKey;
    }

    @Override
    public String getClientSecret() {
        return this.consumerSecret;
    }

    /**
     * Gets the authorise URL.
     * @return The authorise URL
     */
    public String getAuthoriseUrl() {
        return this.getAuthorizationUrl();
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
