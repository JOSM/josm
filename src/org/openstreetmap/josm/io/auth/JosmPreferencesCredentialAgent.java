// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.util.Objects;

import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.OsmApi;

/**
 * This is the default credentials agent in JOSM. It keeps username and password for both
 * the OSM API and an optional HTTP proxy in the JOSM preferences file.
 * @since 2641
 */
public class JosmPreferencesCredentialAgent extends AbstractCredentialsAgent {

    /**
     * @see CredentialsAgent#lookup
     */
    @Override
    public PasswordAuthentication lookup(RequestorType requestorType, String host) throws CredentialsAgentException {
        if (requestorType == null)
            return null;
        String user;
        String password;
        switch(requestorType) {
        case SERVER:
            if (Objects.equals(OsmApi.getOsmApi().getHost(), host)) {
                user = Main.pref.get("osm-server.username", null);
                password = Main.pref.get("osm-server.password", null);
            } else if (host != null) {
                user = Main.pref.get("server.username."+host, null);
                password = Main.pref.get("server.password."+host, null);
            } else {
                user = null;
                password = null;
            }
            if (user == null)
                return null;
            return new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
        case PROXY:
            user = Main.pref.get(DefaultProxySelector.PROXY_USER, null);
            password = Main.pref.get(DefaultProxySelector.PROXY_PASS, null);
            if (user == null)
                return null;
            return new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
        }
        return null;
    }

    /**
     * @see CredentialsAgent#store
     */
    @Override
    public void store(RequestorType requestorType, String host, PasswordAuthentication credentials) throws CredentialsAgentException {
        if (requestorType == null)
            return;
        switch(requestorType) {
        case SERVER:
            if (Objects.equals(OsmApi.getOsmApi().getHost(), host)) {
                Main.pref.put("osm-server.username", credentials.getUserName());
                if (credentials.getPassword() == null) {
                    Main.pref.put("osm-server.password", null);
                } else {
                    Main.pref.put("osm-server.password", String.valueOf(credentials.getPassword()));
                }
            } else if (host != null) {
                Main.pref.put("server.username."+host, credentials.getUserName());
                if (credentials.getPassword() == null) {
                    Main.pref.put("server.password."+host, null);
                } else {
                    Main.pref.put("server.password."+host, String.valueOf(credentials.getPassword()));
                }
            }
            break;
        case PROXY:
            Main.pref.put(DefaultProxySelector.PROXY_USER, credentials.getUserName());
            if (credentials.getPassword() == null) {
                Main.pref.put(DefaultProxySelector.PROXY_PASS, null);
            } else {
                Main.pref.put(DefaultProxySelector.PROXY_PASS, String.valueOf(credentials.getPassword()));
            }
            break;
        }
    }

    /**
     * Lookup the current OAuth Access Token to access the OSM server. Replies null, if no
     * Access Token is currently managed by this CredentialManager.
     *
     * @return the current OAuth Access Token to access the OSM server.
     * @throws CredentialsAgentException if something goes wrong
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
     * @throws CredentialsAgentException if something goes wrong
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

    @Override
    public Component getPreferencesDecorationPanel() {
        HtmlPanel pnlMessage = new HtmlPanel();
        HTMLEditorKit kit = (HTMLEditorKit) pnlMessage.getEditorPane().getEditorKit();
        kit.getStyleSheet().addRule(
                ".warning-body {background-color:rgb(253,255,221);padding: 10pt; " +
                "border-color:rgb(128,128,128);border-style: solid;border-width: 1px;}");
        pnlMessage.setText(tr(
                        "<html><body>"
                        + "<p class=\"warning-body\">"
                        + "<strong>Warning:</strong> The password is stored in plain text in the JOSM preferences file. "
                        + "Furthermore, it is transferred <strong>unencrypted</strong> in every request sent to the OSM server. "
                        + "<strong>Do not use a valuable password.</strong>"
                        + "</p>"
                        + "</body></html>"
                )
        );
        return pnlMessage;
    }

    @Override
    public String getSaveUsernameAndPasswordCheckboxText() {
        return tr("Save user and password (unencrypted)");
    }
}
