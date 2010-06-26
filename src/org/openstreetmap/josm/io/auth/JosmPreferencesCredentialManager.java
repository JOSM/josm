// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.PasswordAuthentication;
import java.net.Authenticator.RequestorType;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.io.CredentialDialog;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferencesPanel;

/**
 * This is the default credential manager in JOSM. It keeps username and password for both
 * the OSM API and an optional HTTP proxy in the JOSM preferences file.
 *
 */
public class JosmPreferencesCredentialManager implements CredentialsManager {

    Map<RequestorType, PasswordAuthentication> memoryCredentialsCache = new HashMap<RequestorType, PasswordAuthentication>();
    /**
     * @see CredentialsManager#lookup(RequestorType)
     */
    public PasswordAuthentication lookup(RequestorType requestorType) throws CredentialsManagerException{
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
            return new PasswordAuthentication(user, password == null ? null : password.toCharArray());
        }
        return null;
    }

    /**
     * @see CredentialsManager#store(RequestorType, PasswordAuthentication)
     */
    public void store(RequestorType requestorType, PasswordAuthentication credentials) throws CredentialsManagerException {
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
     * @see CredentialsManager#getCredentials(RequestorType, boolean)
     */
    public CredentialsManagerResponse getCredentials(RequestorType requestorType, boolean noSuccessWithLastResponse) throws CredentialsManagerException{
        if (requestorType == null)
            return null;
        PasswordAuthentication credentials =  lookup(requestorType);
        String username = (credentials == null || credentials.getUserName() == null) ? "" : credentials.getUserName();
        String password = (credentials == null || credentials.getPassword() == null) ? "" : String.valueOf(credentials.getPassword());

        CredentialsManagerResponse response = new CredentialsManagerResponse();

        /*
         * Last request was successful and there was no credentials stored
         * in file. -> Try to recall credentials that have been entered
         * manually in this session.
         */
        if (!noSuccessWithLastResponse && credentials == null && memoryCredentialsCache.containsKey(requestorType)) {
            PasswordAuthentication pa = memoryCredentialsCache.get(requestorType);
            response.setUsername(pa.getUserName());
            response.setPassword(pa.getPassword());
            response.setCanceled(false);
        /*
         * Prompt the user for credentials. This happens the first time each
         * josm start if the user does not save the credentials to preference
         * file (username=="") and each time after authentication failed
         * (noSuccessWithLastResponse == true).
         */
        } else if (noSuccessWithLastResponse || username.equals("") || password.equals("")) {
            CredentialDialog dialog = null;
            switch(requestorType) {
            case SERVER: dialog = CredentialDialog.getOsmApiCredentialDialog(username, password); break;
            case PROXY: dialog = CredentialDialog.getHttpProxyCredentialDialog(username, password); break;
            }
            dialog.setVisible(true);
            response.setCanceled(dialog.isCanceled());
            if (dialog.isCanceled())
                return response;
            response.setUsername(dialog.getUsername());
            response.setPassword(dialog.getPassword());
            if (dialog.isSaveCredentials()) {
                store(requestorType, new PasswordAuthentication(
                        response.getUsername(),
                        response.getPassword()
                ));
            /*
             * User decides not to save credentials to file. Keep it
             * in memory so we don't have to ask over and over again.
             */
            } else {
                PasswordAuthentication pa = new PasswordAuthentication(dialog.getUsername(), dialog.getPassword());
                memoryCredentialsCache.put(requestorType, pa);
            }
        /*
         * We got it from file.
         */
        } else {
            response.setUsername(username);
            response.setPassword(password.toCharArray());
            response.setCanceled(false);
        }
        return response;
    }

    /**
     * Lookup the current OAuth Access Token to access the OSM server. Replies null, if no
     * Access Token is currently managed by this CredentialManager.
     *
     * @return the current OAuth Access Token to access the OSM server.
     * @throws CredentialsManagerException thrown if something goes wrong
     */
    public OAuthToken lookupOAuthAccessToken() throws CredentialsManagerException {
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
     * @throws CredentialsManagerException thrown if something goes wrong
     */
    public void storeOAuthAccessToken(OAuthToken accessToken) throws CredentialsManagerException {
        if (accessToken == null) {
            Main.pref.put("oauth.access-token.key", null);
            Main.pref.put("oauth.access-token.secret", null);
        } else {
            Main.pref.put("oauth.access-token.key", accessToken.getKey());
            Main.pref.put("oauth.access-token.secret", accessToken.getSecret());
        }
    }
}
