// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.PasswordAuthentication;
import java.net.Authenticator.RequestorType;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferencesPanel;

/**
 * This is the default credentials agent in JOSM. It keeps username and password for both
 * the OSM API and an optional HTTP proxy in the JOSM preferences file.
 *
 */
public class JosmPreferencesCredentialAgent extends AbstractCredentialsAgent {

    /**
     * @see CredentialsAgent#lookup(RequestorType)
     */
    @Override
    public PasswordAuthentication lookup(RequestorType requestorType) throws CredentialsAgentException{
        if (requestorType == null)
            return null;
        String user;
        String password;
        switch(requestorType) {
        case SERVER:
            user = Main.pref.get("osm-server.username", null);
            password = Main.pref.get("osm-server.password", null);
            if (user == null)
                return null;
            return new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
        case PROXY:
            user = Main.pref.get(ProxyPreferencesPanel.PROXY_USER, null);
            password = Main.pref.get(ProxyPreferencesPanel.PROXY_PASS, null);
            if (user == null)
                return null;
            return new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
        }
        return null;
    }

    /**
     * @see CredentialsAgent#store(RequestorType, PasswordAuthentication)
     */
    @Override
    public void store(RequestorType requestorType, PasswordAuthentication credentials) throws CredentialsAgentException {
        if (requestorType == null)
            return;
        switch(requestorType) {
        case SERVER:
            Main.pref.put("osm-server.username", credentials.getUserName());
            if (credentials.getPassword() == null) {
                Main.pref.put("osm-server.password", null);
            } else {
                Main.pref.put("osm-server.password", String.valueOf(credentials.getPassword()));
            }
            break;
        case PROXY:
            Main.pref.put(ProxyPreferencesPanel.PROXY_USER, credentials.getUserName());
            if (credentials.getPassword() == null) {
                Main.pref.put(ProxyPreferencesPanel.PROXY_PASS, null);
            } else {
                Main.pref.put(ProxyPreferencesPanel.PROXY_PASS, String.valueOf(credentials.getPassword()));
            }
            break;
        }
    }

    /**
     * Lookup the current OAuth Access Token to access the OSM server. Replies null, if no
     * Access Token is currently managed by this CredentialManager.
     *
     * @return the current OAuth Access Token to access the OSM server.
     * @throws CredentialsAgentException thrown if something goes wrong
     */
    @Override
    public OAuthToken lookupOAuthAccessToken() throws CredentialsAgentException {
        String accessTokenKey = Main.pref.get("oauth.access-token.key", null);
        String accessTokenSecret = Main.pref.get("oauth.access-token.secret", null);
        if (accessTokenKey == null && accessTokenSecret == null)
            return null;
        return new OAuthToken(accessTokenKey, accessTokenSecret);
    }

    /**
     * Stores the OAuth Access Token <code>accessToken</code>.
     *
     * @param accessToken the access Token. null, to remove the Access Token.
     * @throws CredentialsAgentException thrown if something goes wrong
     */
    @Override
    public void storeOAuthAccessToken(OAuthToken accessToken) throws CredentialsAgentException {
        if (accessToken == null) {
            Main.pref.put("oauth.access-token.key", null);
            Main.pref.put("oauth.access-token.secret", null);
        } else {
            Main.pref.put("oauth.access-token.key", accessToken.getKey());
            Main.pref.put("oauth.access-token.secret", accessToken.getSecret());
        }
    }
}
