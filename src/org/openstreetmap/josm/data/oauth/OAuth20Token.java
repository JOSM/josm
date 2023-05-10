// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Token holder for OAuth 2.0
 * @author Taylor Smock
 * @since 18650
 */
public final class OAuth20Token implements IOAuthToken {
    private static final String ACCESS_TOKEN = "access_token";
    private static final String CREATED_AT = "created_at";
    private static final String EXPIRES_IN = "expires_in";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String SCOPE = "scope";
    private static final String TOKEN_TYPE = "token_type";
    private final String accessToken;
    private final String tokenType;
    private final int expiresIn;
    private final String refreshToken;
    private final String[] scopes;
    private final Instant createdAt;
    private final IOAuthParameters oauthParameters;

    /**
     * Create a new OAuth token
     * @param oauthParameters The parameters for the OAuth token
     * @param json The stored JSON for the token
     * @throws OAuth20Exception If the JSON creates an invalid token
     */
    public OAuth20Token(IOAuthParameters oauthParameters, String json) throws OAuth20Exception {
        this(oauthParameters, new InputStreamReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    OAuth20Token(IOAuthParameters oauthParameters, Reader bufferedReader) throws OAuth20Exception {
        this.oauthParameters = oauthParameters;
        try (JsonReader reader = Json.createReader(bufferedReader)) {
            JsonStructure structure = reader.read();
            if (structure.getValueType() != JsonValue.ValueType.OBJECT
            || !structure.asJsonObject().containsKey(ACCESS_TOKEN)
            || !structure.asJsonObject().containsKey(TOKEN_TYPE)) {
                if (structure.getValueType() == JsonValue.ValueType.OBJECT
                && structure.asJsonObject().containsKey("error")) {
                    throw new OAuth20Exception(structure.asJsonObject());
                } else {
                    throw new OAuth20Exception("Either " + ACCESS_TOKEN + " or " + TOKEN_TYPE + " is not present: " + structure);
                }
            }
            JsonObject object = structure.asJsonObject();
            this.accessToken = object.getString(ACCESS_TOKEN);
            this.tokenType = object.getString(TOKEN_TYPE);
            this.expiresIn = object.getInt(EXPIRES_IN, Integer.MAX_VALUE);
            this.refreshToken = object.getString(REFRESH_TOKEN, null);
            this.scopes = object.getString(SCOPE, "").split(" ");
            if (object.containsKey(CREATED_AT)) {
                this.createdAt = Instant.ofEpochSecond(object.getJsonNumber(CREATED_AT).longValue());
            } else {
                this.createdAt = Instant.now();
            }
        }
    }

    @Override
    public void sign(HttpClient client) throws OAuthException {
        if (!Utils.isBlank(this.oauthParameters.getApiUrl())
                && !this.oauthParameters.getApiUrl().contains(client.getURL().getHost())) {
            String host = URI.create(this.oauthParameters.getAccessTokenUrl()).getHost();
            throw new IllegalArgumentException("Cannot sign URL with token for different host: Expected " + host
                + " but got " + client.getURL().getHost());
        }
        if (this.getBearerToken() != null) {
            client.setHeader("Authorization", "Bearer " + this.getBearerToken());
            return;
        }
        throw new OAuth20Exception("Unknown token type: " + this.tokenType);
    }

    /**
     * Get the OAuth 2.0 bearer token
     * @return The bearer token. May return {@code null} if the token type is not a bearer type.
     */
    public String getBearerToken() {
        if ("bearer".equalsIgnoreCase(this.tokenType)) {
            return this.accessToken;
        }
        return null;
    }

    @Override
    public String toPreferencesString() {
        final OAuth20Token tokenToSave;
        if (shouldRefresh()) {
            tokenToSave = refresh();
        } else {
            tokenToSave = this;
        }
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add(ACCESS_TOKEN, tokenToSave.accessToken);
        jsonObjectBuilder.add(TOKEN_TYPE, tokenToSave.tokenType);
        if (tokenToSave.createdAt != null) {
            jsonObjectBuilder.add(CREATED_AT, tokenToSave.createdAt.getEpochSecond());
        }
        if (tokenToSave.expiresIn != Integer.MAX_VALUE) {
            jsonObjectBuilder.add(EXPIRES_IN, tokenToSave.expiresIn);
        }
        if (tokenToSave.refreshToken != null) {
            jsonObjectBuilder.add(REFRESH_TOKEN, tokenToSave.refreshToken);
        }
        if (tokenToSave.scopes.length > 0) {
            jsonObjectBuilder.add(SCOPE, String.join(" ", tokenToSave.scopes));
        }
        return jsonObjectBuilder.build().toString();
    }

    @Override
    public OAuthVersion getOAuthType() {
        return OAuthVersion.OAuth20;
    }

    @Override
    public IOAuthParameters getParameters() {
        return this.oauthParameters;
    }

    /**
     * Check if the token should be refreshed
     * @return {@code true} if the token should be refreshed
     */
    boolean shouldRefresh() {
        return this.refreshToken != null && this.expiresIn != Integer.MAX_VALUE
                // We should refresh the token when 10% of its lifespan has been spent.
                // We aren't an application that will be used every day by every user.
                && this.createdAt.getEpochSecond() + this.expiresIn < Instant.now().getEpochSecond() - this.expiresIn * 9L / 10;
    }

    /**
     * Refresh the OAuth 2.0 token
     * @return The new token to use
     */
    OAuth20Token refresh() {
        // This bit isn't necessarily OAuth 2.1 compliant. Spec isn't finished yet, but
        // refresh tokens will either be sender constrained or some kind of rotation or both.
        // This refresh code handles rotation, mostly by creating a new OAuth20Token. :)
        // For sender constrained, it will likely allow self-signed certificates (RFC8705).
        // Note: OSM doesn't have age limits on their tokens, at time of writing.
        String refresh = "grant_type=refresh_token&refresh_token=" + this.refreshToken;
        if (this.scopes.length > 0) {
            refresh += "&scope=" + String.join(" ", this.scopes);
        }
        HttpClient client = null;
        try {
            client = HttpClient.create(new URL(this.oauthParameters.getAccessTokenUrl()), "POST");
            client.setRequestBody(refresh.getBytes(StandardCharsets.UTF_8));
            client.connect();
            HttpClient.Response response = client.getResponse();
            return new OAuth20Token(this.oauthParameters, response.getContentReader());
        } catch (IOException | OAuth20Exception e) {
            throw new JosmRuntimeException(e);
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
    }
}
