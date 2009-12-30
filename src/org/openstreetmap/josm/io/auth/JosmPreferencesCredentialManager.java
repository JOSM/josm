// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.PasswordAuthentication;
import java.net.Authenticator.RequestorType;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.io.CredentialDialog;
import org.openstreetmap.josm.gui.preferences.ProxyPreferences;

/**
 * This is the default credential manager in JOSM. It keeps username and password for both
 * the OSM API and an optional HTTP proxy in the JOSM preferences file.
 *
 */
public class JosmPreferencesCredentialManager implements CredentialsManager {

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
            user = Main.pref.get(ProxyPreferences.PROXY_USER, null);
            password = Main.pref.get(ProxyPreferences.PROXY_PASS, null);
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
            Main.pref.put("proxy.username", credentials.getUserName());
            if (credentials.getPassword() == null) {
                Main.pref.put("proxy.password", null);
            } else {
                Main.pref.put("proxy.password", String.valueOf(credentials.getPassword()));
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

        if (noSuccessWithLastResponse|| username.equals("") || password.equals("")) {
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
            }
        } else {
            response.setUsername(username);
            response.setPassword(password.toCharArray());
            response.setCanceled(false);
        }
        return response;
    }
}
