// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Parameters for OAuth 2.0
 * @author Taylor Smock
 * @since 18650
 */
public final class OAuth20Parameters implements IOAuthParameters {
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String TOKEN_URL = "token_url";
    private static final String AUTHORIZE_URL = "authorize_url";
    private static final String API_URL = "api_url";
    private final String redirectUri;
    private final String clientSecret;
    private final String clientId;
    private final String tokenUrl;
    private final String authorizeUrl;
    private final String apiUrl;

    /**
     * Recreate a parameter object from a JSON string
     * @param jsonString The JSON string with the required data
     */
    public OAuth20Parameters(String jsonString) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
             JsonReader reader = Json.createReader(bais)) {
            JsonStructure structure = reader.read();
            if (structure.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("Invalid JSON object: " + jsonString);
            }
            JsonObject jsonObject = structure.asJsonObject();
            this.redirectUri = jsonObject.getString(REDIRECT_URI);
            this.clientId = jsonObject.getString(CLIENT_ID);
            this.clientSecret = jsonObject.getString(CLIENT_SECRET, null);
            this.tokenUrl = jsonObject.getString(TOKEN_URL);
            this.authorizeUrl = jsonObject.getString(AUTHORIZE_URL);
            this.apiUrl = jsonObject.getString(API_URL);
        } catch (IOException e) {
            // This should literally never happen -- ByteArrayInputStream does not do *anything* in the close method.
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a new OAuth parameter object
     * @param clientId The client id. May not be {@code null}.
     * @param clientSecret The client secret. May be {@code null}. Not currently used.
     * @param baseUrl The base url. This assumes that the endpoints are {@code /token} and {@code /authorize}.
     * @param apiUrl The API url
     * @param redirectUri The redirect URI for the client.
     */
    public OAuth20Parameters(String clientId, String clientSecret, String baseUrl, String apiUrl, String redirectUri) {
        this(clientId, clientSecret, baseUrl + "/token", baseUrl + "/authorize", apiUrl, redirectUri);
    }

    /**
     * Create a new OAuth parameter object
     * @param clientId The client id.
     * @param clientSecret The client secret. May be {@code null}. Not currently used.
     * @param tokenUrl The token request URL (RFC6749 4.4.2)
     * @param authorizeUrl The authorization request URL (RFC6749 4.1.1)
     * @param apiUrl The API url
     * @param redirectUri The redirect URI for the client.
     */
    public OAuth20Parameters(String clientId, String clientSecret, String tokenUrl, String authorizeUrl, String apiUrl, String redirectUri) {
        Objects.requireNonNull(authorizeUrl, "authorizeUrl");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(redirectUri, "redirectUri");
        Objects.requireNonNull(tokenUrl, "tokenUrl");
        Objects.requireNonNull(apiUrl, "apiUrl");
        // Alternatively, we could try using rfc8414 ( /.well-known/oauth-authorization-server ), but OSM (doorkeeper) doesn't support it.
        this.redirectUri = redirectUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.authorizeUrl = authorizeUrl;
        this.apiUrl = apiUrl;
    }

    @Override
    public String getAccessTokenUrl() {
        return this.tokenUrl;
    }

    @Override
    public String getAuthorizationUrl() {
        return this.authorizeUrl;
    }

    @Override
    public OAuthVersion getOAuthVersion() {
        return OAuthVersion.OAuth20;
    }

    @Override
    public String getClientId() {
        return this.clientId;
    }

    @Override
    public String getClientSecret() {
        return this.clientSecret;
    }

    @Override
    public String getRedirectUri() {
        return this.redirectUri;
    }

    @Override
    public String getApiUrl() {
        return this.apiUrl;
    }

    @Override
    public void rememberPreferences() {
        Config.getPref().put("oauth.access-token.parameters." + OAuthVersion.OAuth20 + "." + this.apiUrl,
                this.toPreferencesString());
    }

    @Override
    public String toPreferencesString() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(CLIENT_ID, this.clientId);
        builder.add(REDIRECT_URI, this.redirectUri);
        if (this.apiUrl != null) builder.add(API_URL, this.apiUrl);
        if (this.authorizeUrl != null) builder.add(AUTHORIZE_URL, this.authorizeUrl);
        if (this.clientSecret != null) builder.add(CLIENT_SECRET, this.clientSecret);
        if (this.tokenUrl != null) builder.add(TOKEN_URL, this.tokenUrl);
        return builder.build().toString();
    }
}
