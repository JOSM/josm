// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This class manages a set of OAuth parameters.
 *
 */
public class OAuthParameters {

    static public final String DEFAULT_JOSM_CONSUMER_KEY = "AdCRxTpvnbmfV8aPqrTLyA";
    static public final String DEFAULT_JOSM_CONSUMER_SECRET = "XmYOiGY9hApytcBC3xCec3e28QBqOWz5g6DSb5UpE";
    static public final String DEFAULT_REQUEST_TOKEN_URL = "http://www.openstreetmap.org/oauth/request_token";
    static public final String DEFAULT_ACCESS_TOKEN_URL = "http://www.openstreetmap.org/oauth/access_token";
    static public final String DEFAULT_AUTHORISE_URL = "http://www.openstreetmap.org/oauth/authorize";


    /**
     * Replies a set of default parameters for a consumer accessing the standard OSM server
     * at http://api.openstreetmap.org/api
     *
     * @return a set of default parameters
     */
    static public OAuthParameters createDefault() {
        OAuthParameters parameters = new OAuthParameters();
        parameters.setConsumerKey(DEFAULT_JOSM_CONSUMER_KEY);
        parameters.setConsumerSecret(DEFAULT_JOSM_CONSUMER_SECRET);
        parameters.setRequestTokenUrl(DEFAULT_REQUEST_TOKEN_URL);
        parameters.setAccessTokenUrl(DEFAULT_ACCESS_TOKEN_URL);
        parameters.setAuthoriseUrl(DEFAULT_AUTHORISE_URL);
        return parameters;
    }

    /**
     * Replies a set of parameters as defined in the preferences.
     *
     * @param pref the preferences
     * @return the parameters
     */
    static public OAuthParameters createFromPreferences(Preferences pref) {
        boolean useDefault = pref.getBoolean("oauth.settings.use-default", true );
        if (useDefault)
            return createDefault();
        OAuthParameters parameters = new OAuthParameters();
        parameters.setConsumerKey(pref.get("oauth.settings.consumer-key", ""));
        parameters.setConsumerSecret(pref.get("oauth.settings.consumer-secret", ""));
        parameters.setRequestTokenUrl(pref.get("oauth.settings.request-token-url", ""));
        parameters.setAccessTokenUrl(pref.get("oauth.settings.access-token-url", ""));
        parameters.setAuthoriseUrl(pref.get("oauth.settings.authorise-url", ""));
        return parameters;
    }

    /**
     * Clears the preferences for OAuth parameters
     *
     * @param pref the preferences in which keys related to OAuth parameters are
     * removed
     */
    static public void clearPreferences(Preferences pref) {
        pref.put("oauth.settings.consumer-key", null);
        pref.put("oauth.settings.consumer-secret", null);
        pref.put("oauth.settings.request-token-url", null);
        pref.put("oauth.settings.access-token-url", null);
        pref.put("oauth.settings.authorise-url", null);
    }

    private String consumerKey;
    private String consumerSecret;
    private String requestTokenUrl;
    private String accessTokenUrl;
    private String authoriseUrl;

    public OAuthParameters() {
    }

    /**
     * Creates a clone of the parameters in <code>other</code>.
     *
     * @param other the other parameters. Must not be null.
     * @throws IllegalArgumentException thrown if other is null
     */
    public OAuthParameters(OAuthParameters other) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(other, "other");
        this.consumerKey = other.consumerKey;
        this.consumerSecret = other.consumerSecret;
        this.accessTokenUrl = other.accessTokenUrl;
        this.requestTokenUrl = other.requestTokenUrl;
        this.authoriseUrl = other.authoriseUrl;
    }

    public String getConsumerKey() {
        return consumerKey;
    }
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }
    public String getConsumerSecret() {
        return consumerSecret;
    }
    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }
    public String getRequestTokenUrl() {
        return requestTokenUrl;
    }
    public void setRequestTokenUrl(String requestTokenUrl) {
        this.requestTokenUrl = requestTokenUrl;
    }
    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }
    public void setAccessTokenUrl(String accessTokenUrl) {
        this.accessTokenUrl = accessTokenUrl;
    }
    public String getAuthoriseUrl() {
        return authoriseUrl;
    }
    public void setAuthoriseUrl(String authoriseUrl) {
        this.authoriseUrl = authoriseUrl;
    }

    /**
     * Builds an {@see OAuthConsumer} based on these parameters
     *
     * @return the consumer
     */
    public OAuthConsumer buildConsumer() {
        OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
        return consumer;
    }

    /**
     * Builds an {@see OAuthProvider} based on these parameters and a OAuth consumer <code>consumer</code>.
     *
     * @param consumer the consumer. Must not be null.
     * @return the provider
     * @throws IllegalArgumentException thrown if consumer is null
     */
    public OAuthProvider buildProvider(OAuthConsumer consumer) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(consumer, "consumer");
        return new DefaultOAuthProvider(
                requestTokenUrl,
                accessTokenUrl,
                authoriseUrl
        );
    }

    public void saveToPreferences(Preferences pref) {
        if (this.equals(createDefault())) {
            pref.put("oauth.settings.use-default", true );
            clearPreferences(pref);
            return;
        }
        pref.put("oauth.settings.use-default", false);
        pref.put("oauth.settings.consumer-key", consumerKey);
        pref.put("oauth.settings.consumer-secret", consumerSecret);
        pref.put("oauth.settings.request-token-url", requestTokenUrl);
        pref.put("oauth.settings.access-token-url", accessTokenUrl);
        pref.put("oauth.settings.authorise-url", authoriseUrl);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accessTokenUrl == null) ? 0 : accessTokenUrl.hashCode());
        result = prime * result + ((authoriseUrl == null) ? 0 : authoriseUrl.hashCode());
        result = prime * result + ((consumerKey == null) ? 0 : consumerKey.hashCode());
        result = prime * result + ((consumerSecret == null) ? 0 : consumerSecret.hashCode());
        result = prime * result + ((requestTokenUrl == null) ? 0 : requestTokenUrl.hashCode());
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
        OAuthParameters other = (OAuthParameters) obj;
        if (accessTokenUrl == null) {
            if (other.accessTokenUrl != null)
                return false;
        } else if (!accessTokenUrl.equals(other.accessTokenUrl))
            return false;
        if (authoriseUrl == null) {
            if (other.authoriseUrl != null)
                return false;
        } else if (!authoriseUrl.equals(other.authoriseUrl))
            return false;
        if (consumerKey == null) {
            if (other.consumerKey != null)
                return false;
        } else if (!consumerKey.equals(other.consumerKey))
            return false;
        if (consumerSecret == null) {
            if (other.consumerSecret != null)
                return false;
        } else if (!consumerSecret.equals(other.consumerSecret))
            return false;
        if (requestTokenUrl == null) {
            if (other.requestTokenUrl != null)
                return false;
        } else if (!requestTokenUrl.equals(other.requestTokenUrl))
            return false;
        return true;
    }
}
