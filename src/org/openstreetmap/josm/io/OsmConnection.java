// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.Authenticator.RequestorType;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsAgentResponse;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.Base64;
import org.openstreetmap.josm.tools.Utils;

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 *
 * @author imi
 */
public class OsmConnection {
    protected boolean cancel = false;
    protected HttpURLConnection activeConnection;
    protected OAuthParameters oauthParameters;

    /**
     * Initialize the http defaults and the authenticator.
     */
    static {
        try {
            HttpURLConnection.setFollowRedirects(true);
        } catch (SecurityException e) {
            Main.error(e);
        }
    }

    /**
     * Cancels the connection.
     */
    public void cancel() {
        cancel = true;
        synchronized (this) {
            if (activeConnection != null) {
                activeConnection.setConnectTimeout(100);
                activeConnection.setReadTimeout(100);
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Main.warn("InterruptedException in "+getClass().getSimpleName()+" during cancel");
        }

        synchronized (this) {
            if (activeConnection != null) {
                activeConnection.disconnect();
            }
        }
    }

    /**
     * Adds an authentication header for basic authentication
     *
     * @param con the connection
     * @throws OsmTransferException thrown if something went wrong. Check for nested exceptions
     */
    protected void addBasicAuthorizationHeader(HttpURLConnection con) throws OsmTransferException {
        CharsetEncoder encoder = Utils.UTF_8.newEncoder();
        CredentialsAgentResponse response;
        String token;
        try {
            synchronized (CredentialsManager.getInstance()) {
                response = CredentialsManager.getInstance().getCredentials(RequestorType.SERVER,
                con.getURL().getHost(), false /* don't know yet whether the credentials will succeed */);
            }
        } catch (CredentialsAgentException e) {
            throw new OsmTransferException(e);
        }
        if (response == null) {
            token = ":";
        } else if (response.isCanceled()) {
            cancel = true;
            return;
        } else {
            String username= response.getUsername() == null ? "" : response.getUsername();
            String password = response.getPassword() == null ? "" : String.valueOf(response.getPassword());
            token = username + ":" + password;
            try {
                ByteBuffer bytes = encoder.encode(CharBuffer.wrap(token));
                con.addRequestProperty("Authorization", "Basic "+Base64.encode(bytes));
            } catch(CharacterCodingException e) {
                throw new OsmTransferException(e);
            }
        }
    }

    /**
     * Signs the connection with an OAuth authentication header
     *
     * @param connection the connection
     *
     * @throws OsmTransferException thrown if there is currently no OAuth Access Token configured
     * @throws OsmTransferException thrown if signing fails
     */
    protected void addOAuthAuthorizationHeader(HttpURLConnection connection) throws OsmTransferException {
        if (oauthParameters == null) {
            oauthParameters = OAuthParameters.createFromPreferences(Main.pref);
        }
        OAuthConsumer consumer = oauthParameters.buildConsumer();
        OAuthAccessTokenHolder holder = OAuthAccessTokenHolder.getInstance();
        if (! holder.containsAccessToken())
            throw new MissingOAuthAccessTokenException();
        consumer.setTokenWithSecret(holder.getAccessTokenKey(), holder.getAccessTokenSecret());
        try {
            consumer.sign(connection);
        } catch(OAuthException e) {
            throw new OsmTransferException(tr("Failed to sign a HTTP connection with an OAuth Authentication header"), e);
        }
    }

    protected void addAuth(HttpURLConnection connection) throws OsmTransferException {
        String authMethod = Main.pref.get("osm-server.auth-method", "basic");
        if (authMethod.equals("basic")) {
            addBasicAuthorizationHeader(connection);
        } else if (authMethod.equals("oauth")) {
            addOAuthAuthorizationHeader(connection);
        } else {
            String msg = tr("Unexpected value for preference ''{0}''. Got ''{1}''.", "osm-server.auth-method", authMethod);
            Main.warn(msg);
            throw new OsmTransferException(msg);
        }
    }

    /**
     * Replies true if this connection is canceled
     *
     * @return true if this connection is canceled
     */
    public boolean isCanceled() {
        return cancel;
    }
}
