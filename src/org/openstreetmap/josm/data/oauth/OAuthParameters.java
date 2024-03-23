// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

    private static JsonObject getRFC8414Parameters(String apiUrl) {
        HttpClient client = null;
        try {
            final URI apiURI = new URI(apiUrl);
            final URL rfc8414URL = new URI(apiURI.getScheme(), apiURI.getHost(),
                    "/.well-known/oauth-authorization-server", null).toURL();
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
        } catch (URISyntaxException | IOException e) {
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
        final String baseUrl;
        apiUrl = apiUrl == null ? OsmApi.getOsmApi().getServerUrl() : apiUrl;
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
                // clientId provided by 1ec5 (Minh Nguyễn)
                clientId = "Hl5yIhFS-Egj6aY7A35ouLOuZl0EHjj8JJQQ46IO96E";
                clientSecret = null;
                break;
            default:
                clientId = "";
                clientSecret = null;
        }
        baseUrl = apiUrl;
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
            if (e.getCause() instanceof URISyntaxException || e.getCause() instanceof IOException) {
                Logging.trace(e);
            } else {
                throw e;
            }
        } catch (OAuthException e) {
            Logging.trace(e);
        }
        // Fall back to guessing the parameters.
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
     * Replies a set of parameters as defined in the preferences.
     *
     * @param oAuthVersion The OAuth version to use.
     * @param apiUrl the API URL. Must not be {@code null}.
     * @return the parameters
     * @since 18650
     */
    public static IOAuthParameters createFromApiUrl(String apiUrl, OAuthVersion oAuthVersion) {
        // We actually need the host
        if (apiUrl.startsWith("https://") || apiUrl.startsWith("http://")) {
            try {
                apiUrl = new URI(apiUrl).getHost();
            } catch (URISyntaxException syntaxException) {
                Logging.trace(apiUrl);
            }
        }
        switch (oAuthVersion) {
            case OAuth20:
            case OAuth21: // Right now, OAuth 2.1 will work with our OAuth 2.0 implementation
                try {
                    IOAuthToken storedToken = CredentialsManager.getInstance().lookupOAuthAccessToken(apiUrl);
                    if (storedToken != null) {
                        return storedToken.getParameters();
                    }
                } catch (CredentialsAgentException e) {
                    Logging.trace(e);
                }
                return createDefault(apiUrl, oAuthVersion);
            default:
                throw new IllegalArgumentException("Unknown OAuth version: " + oAuthVersion);
        }
    }
}
