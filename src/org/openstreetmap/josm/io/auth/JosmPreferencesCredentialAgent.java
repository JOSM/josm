// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.util.Objects;

import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;

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
                user = Config.getPref().get("osm-server.username", null);
                password = Config.getPref().get("osm-server.password", null);
            } else if (host != null) {
                user = Config.getPref().get("server.username."+host, null);
                password = Config.getPref().get("server.password."+host, null);
            } else {
                user = null;
                password = null;
            }
            if (user == null)
                return null;
            return new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
        case PROXY:
            user = Config.getPref().get(DefaultProxySelector.PROXY_USER, null);
            password = Config.getPref().get(DefaultProxySelector.PROXY_PASS, null);
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
                Config.getPref().put("osm-server.username", credentials.getUserName());
                if (credentials.getPassword() == null) {
                    Config.getPref().put("osm-server.password", null);
                } else {
                    Config.getPref().put("osm-server.password", String.valueOf(credentials.getPassword()));
                }
            } else if (host != null) {
                Config.getPref().put("server.username."+host, credentials.getUserName());
                if (credentials.getPassword() == null) {
                    Config.getPref().put("server.password."+host, null);
                } else {
                    Config.getPref().put("server.password."+host, String.valueOf(credentials.getPassword()));
                }
            }
            break;
        case PROXY:
            Config.getPref().put(DefaultProxySelector.PROXY_USER, credentials.getUserName());
            if (credentials.getPassword() == null) {
                Config.getPref().put(DefaultProxySelector.PROXY_PASS, null);
            } else {
                Config.getPref().put(DefaultProxySelector.PROXY_PASS, String.valueOf(credentials.getPassword()));
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
        String accessTokenKey = Config.getPref().get("oauth.access-token.key", null);
        String accessTokenSecret = Config.getPref().get("oauth.access-token.secret", null);
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
            Config.getPref().put("oauth.access-token.key", null);
            Config.getPref().put("oauth.access-token.secret", null);
        } else {
            Config.getPref().put("oauth.access-token.key", accessToken.getKey());
            Config.getPref().put("oauth.access-token.secret", accessToken.getSecret());
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
                        + "<strong>Note:</strong> The password is stored in plain text in the JOSM preferences file on your computer. "
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
