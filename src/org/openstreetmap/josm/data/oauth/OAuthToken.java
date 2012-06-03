// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import oauth.signpost.OAuthConsumer;

import org.openstreetmap.josm.tools.CheckParameterUtil;

public class OAuthToken {

    /**
     * Creates an OAuthToken from the token currently managed by the {@link OAuthConsumer}.
     *
     * @param consumer the consumer
     * @return the token
     */
    static public OAuthToken createToken(OAuthConsumer consumer) {
        return new OAuthToken(consumer.getToken(), consumer.getTokenSecret());
    }

    private String key;
    private String secret;

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
     * @throws IllegalArgumentException thrown if other is null
     */
    public OAuthToken(OAuthToken other) throws IllegalArgumentException {
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((secret == null) ? 0 : secret.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OAuthToken other = (OAuthToken) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (secret == null) {
            if (other.secret != null)
                return false;
        } else if (!secret.equals(other.secret))
            return false;
        return true;
    }
}
