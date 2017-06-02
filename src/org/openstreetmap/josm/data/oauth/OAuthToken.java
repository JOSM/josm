// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.util.Objects;

import org.openstreetmap.josm.tools.CheckParameterUtil;

import oauth.signpost.OAuthConsumer;

/**
 * An oauth token that has been obtained by JOSM and can be used to authenticate the user on the server.
 */
public class OAuthToken {

    /**
     * Creates an OAuthToken from the token currently managed by the {@link OAuthConsumer}.
     *
     * @param consumer the consumer
     * @return the token
     */
    public static OAuthToken createToken(OAuthConsumer consumer) {
        return new OAuthToken(consumer.getToken(), consumer.getTokenSecret());
    }

    private final String key;
    private final String secret;

    /**
     * Creates a new token
     *
     * @param key the token key
     * @param secret the token secret
     */
    public OAuthToken(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }

    /**
     * Creates a clone of another token
     *
     * @param other the other token. Must not be null.
     * @throws IllegalArgumentException if other is null
     */
    public OAuthToken(OAuthToken other) {
        CheckParameterUtil.ensureParameterNotNull(other, "other");
        this.key = other.key;
        this.secret = other.secret;
    }

    /**
     * Replies the token key
     *
     * @return the token key
     */
    public String getKey() {
        return key;
    }

    /**
     * Replies the token secret
     *
     * @return the token secret
     */
    public String getSecret() {
        return secret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, secret);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OAuthToken that = (OAuthToken) obj;
        return Objects.equals(key, that.key) &&
                Objects.equals(secret, that.secret);
    }
}
