// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.stream.JsonParsingException;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IUrls;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

/**
 * This class manages an immutable set of OAuth parameters.
 * @since 2747 (static factory class since 18991)
 */
public final class OAuthParameters {
    private static final Map<String, JsonObject> RFC8414_RESPONSES = new HashMap<>(1);
    private static final String OSM_API_DEFAULT = "https://api.openstreetmap.org/api";
    private static final String OSM_API_DEV = "https://api06.dev.openstreetmap.org/api";
    private static final String OSM_API_MASTER = "https://master.apis.dev.openstreetmap.org/api";
    /** Whether the default parameters are used; one setting for each host, with the setting for all hosts as fallback */
    private static final String USE_DEFAULT = "oauth.settings.use-default";

    private OAuthParameters() {
        // Hide constructor
    }

    /**
     * Replies a set of default parameters for a consumer accessing the standard OSM server
     * at {@link IUrls#getDefaultOsmApiUrl}.
     * <p>
     * Note that this may make network requests for RFC 8414 compliant endpoints.
     * @return a set of default parameters
     */
    public static IOAuthParameters createDefault() {
        return createDefault(Config.getUrls().getDefaultOsmApiUrl(), OAuthVersion.OAuth20);
    }

    /**
     * Replies a set of default parameters for a consumer accessing an OSM server
     * at the given API url. URL parameters are only set if the URL equals {@link IUrls#getDefaultOsmApiUrl}
     * or references the domain "dev.openstreetmap.org", otherwise they may be <code>null</code>.
     * <p>
     * Note that this may make network requests for RFC 8414 compliant endpoints.
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
            case OAuth20:
            case OAuth21: // For now, OAuth 2.1 (draft) is just OAuth 2.0 with mandatory extensions, which we implement.
                return getDefaultOAuth20Parameters(apiUrl);
            default:
                throw new IllegalArgumentException("Unknown OAuth version: " + oAuthVersion);
        }
    }

    /**
     * Get the OAuth 2.0 parameters the user entered in the advanced OAuth settings and saved with
     * {@link IOAuthParameters#rememberPreferences()}, or the parameters of the access token of the server. Until a
     * token exists, this is the only place a client id for a server not in
     * {@link #createDefault(String, OAuthVersion)} can come from.
     * {@link OAuth20Parameters#rememberPreferences()} keys by the host of the API, as the access token is; earlier
     * versions of JOSM keyed them by the API URL, so callers pass that too.
     * @param keys The preference key suffixes to try, in order
     * @return The remembered parameters, or {@code null} if none are saved or they cannot be parsed
     * @since 19627
     */
    private static IOAuthParameters createFromPreferences(String... keys) {
        for (String key : keys) {
            if (Utils.isEmpty(key)) {
                continue;
            }
            final String json = Config.getPref().get("oauth.access-token.parameters." + OAuthVersion.OAuth20 + "." + key, null);
            if (Utils.isEmpty(json)) {
                continue;
            }
            try {
                OAuth20Parameters parameters = new OAuth20Parameters(json);
                if (!Utils.isEmpty(parameters.getClientId())) {
                    return parameters;
                }
            } catch (IllegalArgumentException | NullPointerException | JsonParsingException e) {
                // A preference written by hand, or by an older JOSM with different fields
                Logging.trace(e);
            }
        }
        return null;
    }

    /**
     * Get the preference key under which the OAuth 2.0 parameters for the server of an API are stored. This is the
     * key of the access token of that server too, see {@link org.openstreetmap.josm.io.auth.CredentialsAgent}.
     * @param apiUrl The API URL, or the host of the API
     * @return The preference key
     * @since 19707
     */
    static String getPreferenceKey(String apiUrl) {
        return "oauth.access-token.parameters." + OAuthVersion.OAuth20 + "." + getHost(apiUrl);
    }

    /**
     * Get the host of an API, which is what the OAuth settings and the access token of a server are stored under.
     * @param apiUrl The API URL, or the host of the API
     * @return The host, or {@code apiUrl} itself if it is not a URL (including {@code null})
     * @since 19707
     */
    public static String getHost(String apiUrl) {
        if (Utils.isEmpty(apiUrl)) {
            return apiUrl;
        }
        try {
            final String host = new URI(apiUrl).getHost();
            return host == null ? apiUrl : host;
        } catch (URISyntaxException e) {
            Logging.trace(e);
            return apiUrl;
        }
    }

    /**
     * Determines whether the default OAuth 2.0 parameters are used for the server of an API, that is the ones built
     * into JOSM or published by the server, rather than the ones the user entered in the advanced OAuth settings.
     * <p>
     * This is a setting of each server. Until it is set for a server, the setting earlier versions of JOSM had
     * for all servers is used.
     * @param apiUrl The API URL, or the host of the API
     * @return {@code true} if the default parameters are used
     * @since 19707
     */
    public static boolean isUseDefault(String apiUrl) {
        final boolean useDefaultForAll = Config.getPref().getBoolean(USE_DEFAULT, true);
        if (Utils.isEmpty(apiUrl)) {
            return useDefaultForAll;
        }
        return Config.getPref().getBoolean(USE_DEFAULT + "." + getHost(apiUrl), useDefaultForAll);
    }

    /**
     * Sets whether the default OAuth 2.0 parameters are used for the server of an API.
     * @param apiUrl The API URL, or the host of the API
     * @param useDefault {@code true} to use the default parameters, {@code false} to use the ones the user entered
     * @see #isUseDefault(String)
     * @since 19707
     */
    public static void setUseDefault(String apiUrl, boolean useDefault) {
        if (!Utils.isEmpty(apiUrl)) {
            Config.getPref().putBoolean(USE_DEFAULT + "." + getHost(apiUrl), useDefault);
        }
    }

    private static JsonObject getRFC8414Parameters(String apiUrl) {
        HttpClient client = null;
        try {
            final URI apiURI = new URI(apiUrl);
            // keep the port, as for a development server on http://localhost:3000
            final URL rfc8414URL = new URI(apiURI.getScheme(), null, apiURI.getHost(), apiURI.getPort(),
                    "/.well-known/oauth-authorization-server", null, null).toURL();
            client = HttpClient.create(rfc8414URL);
            HttpClient.Response response = client.connect();
            if (response.getResponseCode() == 200) {
                try (BufferedReader reader = response.getContentReader();
                     JsonReader jsonReader = Json.createReader(reader)) {
                    JsonStructure structure = jsonReader.read();
                    if (structure.getValueType() == JsonValue.ValueType.OBJECT) {
                        return structure.asJsonObject();
                    }
                }
            }
        } catch (JsonParsingException | URISyntaxException | IOException e) {
            throw new JosmRuntimeException(e);
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
        return Json.createObjectBuilder().build();
    }

    /**
     * Get the default OAuth 2.0 parameters
     * @param apiUrl The API url
     * @return The default parameters
     */
    private static OAuth20Parameters getDefaultOAuth20Parameters(String apiUrl) {
        final String clientId;
        final String clientSecret;
        final String redirectUri = "http://127.0.0.1:8111/oauth_authorization";
        apiUrl = OsmApi.normalizeApiUrl(apiUrl == null ? OsmApi.getOsmApi().getServerUrl() : apiUrl);
        switch (apiUrl) {
            case OSM_API_DEV:
            case OSM_API_MASTER:
                // This clientId/clientSecret are provided by taylor.smock. Feel free to change if needed, but
                // do let one of the maintainers with server access know so that they can update the test OAuth
                // token.
                clientId = "-QZt6n1btDfqrfJNGUIMZjzcyqTgIV6sy79_W4kmQLM";
                // Keep secret for dev apis, just in case we want to test something that needs it.
                clientSecret = "SWnmRD4AdLO-2-ttHE5TR3eLF2McNf7dh0_Z2WNzJdI";
                break;
            case OSM_API_DEFAULT:
                clientId = "edPII614Lm0_0zEpc_QzEltA9BUll93-Y-ugRQUoHMI";
                // We don't actually use the client secret in our authorization flow.
                clientSecret = null;
                break;
            case "https://www.openhistoricalmap.org/api":
            case "https://api.openhistoricalmap.org/api":
                // clientId provided by 1ec5 (Minh Nguyễn)
                clientId = "Hl5yIhFS-Egj6aY7A35ouLOuZl0EHjj8JJQQ46IO96E";
                clientSecret = null;
                break;
            case "https://opengeofiction.net/api":
            case "https://lugus.opengeofiction.net/api":
                // clientId provided by wangi (Lee Kindness), OpenGeofiction administrator.
                // lugus is the OpenGeofiction test server.
                clientId = "dKB9Fb5H4MxrM3I23UFxNCh5VGiAEQ_20gC7WmXnaaY";
                clientSecret = null;
                break;
            default:
                clientId = "";
                clientSecret = null;
        }
        // Check if the server is RFC 8414 compliant
        try {
            synchronized (RFC8414_RESPONSES) {
                final JsonObject data;
                if (NetworkManager.isOffline(apiUrl)) {
                    data = null;
                } else {
                    data = RFC8414_RESPONSES.computeIfAbsent(apiUrl, OAuthParameters::getRFC8414Parameters);
                }
                if (data == null || data.isEmpty()) {
                    RFC8414_RESPONSES.remove(apiUrl);
                } else {
                    return parseAuthorizationServerMetadataResponse(clientId, clientSecret, apiUrl,
                            redirectUri, data);
                }
            }
        } catch (JosmRuntimeException e) {
            if (e.getCause() instanceof URISyntaxException || e.getCause() instanceof IOException
                    || e.getCause() instanceof JsonParsingException) {
                Logging.trace(e);
            } else {
                throw e;
            }
        } catch (OAuthException e) {
            Logging.trace(e);
        }
        // Fall back to the endpoints of openstreetmap-website, which is what OSM API servers run
        return new OAuth20Parameters(clientId, clientSecret, getWebsite(apiUrl) + "/oauth2", apiUrl, redirectUri);
    }

    /**
     * Get the website of an OSM API server, where openstreetmap-website has its OAuth 2.0 endpoints: the API URL
     * without its {@code /api}, except for the default OSM API, which is served from a host of its own.
     * @param apiUrl The normalized API URL
     * @return The website URL
     */
    private static String getWebsite(String apiUrl) {
        if (OSM_API_DEFAULT.equals(apiUrl)) {
            return Config.getUrls().getOSMWebsite();
        }
        return apiUrl.replaceAll("/api$", "");
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
     * Replies a set of parameters as defined in the preferences: the parameters of the access token of the server,
     * otherwise the parameters the user entered, unless the server is set to {@link #isUseDefault(String) use the
     * default parameters}, otherwise the {@link #createDefault(String, OAuthVersion) default parameters}.
     *
     * @param oAuthVersion The OAuth version to use.
     * @param apiUrl the API URL. Must not be {@code null}.
     * @return the parameters
     * @since 18650
     */
    public static IOAuthParameters createFromApiUrl(String apiUrl, OAuthVersion oAuthVersion) {
        apiUrl = OsmApi.normalizeApiUrl(apiUrl);
        // We actually need the host
        final String host = getHost(apiUrl);
        switch (oAuthVersion) {
            case OAuth20:
            case OAuth21: // Right now, OAuth 2.1 will work with our OAuth 2.0 implementation
                try {
                    IOAuthToken storedToken = CredentialsManager.getInstance().lookupOAuthAccessToken(host);
                    if (storedToken != null) {
                        return storedToken.getParameters();
                    }
                } catch (CredentialsAgentException e) {
                    Logging.trace(e);
                }
                if (!isUseDefault(apiUrl)) {
                    // the API URL is where earlier versions of JOSM stored them
                    IOAuthParameters remembered = createFromPreferences(host, apiUrl);
                    if (remembered != null) {
                        return remembered;
                    }
                }
                // Use the URL as it was given: the host is never a valid URL, so createDefault() would silently
                // fall back to the API currently configured in the preferences.
                return createDefault(apiUrl, oAuthVersion);
            default:
                throw new IllegalArgumentException("Unknown OAuth version: " + oAuthVersion);
        }
    }
}
