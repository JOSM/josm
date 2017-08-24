// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator.RequestorType;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.FutureTask;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsAgentResponse;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthException;

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 *
 * @author imi
 */
public class OsmConnection {
    protected boolean cancel;
    protected HttpClient activeConnection;
    protected OAuthParameters oauthParameters;

    /**
     * Cancels the connection.
     */
    public void cancel() {
        cancel = true;
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
     * @throws OsmTransferException if something went wrong. Check for nested exceptions
     */
    protected void addBasicAuthorizationHeader(HttpClient con) throws OsmTransferException {
        CredentialsAgentResponse response;
        try {
            synchronized (CredentialsManager.getInstance()) {
                response = CredentialsManager.getInstance().getCredentials(RequestorType.SERVER,
                con.getURL().getHost(), false /* don't know yet whether the credentials will succeed */);
            }
        } catch (CredentialsAgentException e) {
            throw new OsmTransferException(e);
        }
        if (response != null) {
            if (response.isCanceled()) {
                cancel = true;
                return;
            } else {
                String username = response.getUsername() == null ? "" : response.getUsername();
                String password = response.getPassword() == null ? "" : String.valueOf(response.getPassword());
                String token = username + ':' + password;
                con.setHeader("Authorization", "Basic "+Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    /**
     * Signs the connection with an OAuth authentication header
     *
     * @param connection the connection
     *
     * @throws MissingOAuthAccessTokenException if there is currently no OAuth Access Token configured
     * @throws OsmTransferException if signing fails
     */
    protected void addOAuthAuthorizationHeader(HttpClient connection) throws OsmTransferException {
        if (oauthParameters == null) {
            oauthParameters = OAuthParameters.createFromPreferences(Main.pref);
        }
        OAuthConsumer consumer = oauthParameters.buildConsumer();
        OAuthAccessTokenHolder holder = OAuthAccessTokenHolder.getInstance();
        if (!holder.containsAccessToken()) {
            obtainAccessToken(connection);
        }
        if (!holder.containsAccessToken()) { // check if wizard completed
            throw new MissingOAuthAccessTokenException();
        }
        consumer.setTokenWithSecret(holder.getAccessTokenKey(), holder.getAccessTokenSecret());
        try {
            consumer.sign(connection);
        } catch (OAuthException e) {
            throw new OsmTransferException(tr("Failed to sign a HTTP connection with an OAuth Authentication header"), e);
        }
    }

    /**
     * Obtains an OAuth access token for the connection. Afterwards, the token is accessible via {@link OAuthAccessTokenHolder}.
     * @param connection connection for which the access token should be obtained
     * @throws MissingOAuthAccessTokenException if the process cannot be completec successfully
     */
    protected void obtainAccessToken(final HttpClient connection) throws MissingOAuthAccessTokenException {
        try {
            final URL apiUrl = new URL(OsmApi.getOsmApi().getServerUrl());
            if (!Objects.equals(apiUrl.getHost(), connection.getURL().getHost())) {
                throw new MissingOAuthAccessTokenException();
            }
            final Runnable authTask = new FutureTask<>(() -> {
                // Concerning Utils.newDirectExecutor: Main worker cannot be used since this connection is already
                // executed via main worker. The OAuth connections would block otherwise.
                final OAuthAuthorizationWizard wizard = new OAuthAuthorizationWizard(
                        Main.parent, apiUrl.toExternalForm(), Utils.newDirectExecutor());
                wizard.showDialog();
                OAuthAccessTokenHolder.getInstance().setSaveToPreferences(true);
                OAuthAccessTokenHolder.getInstance().save(Main.pref, CredentialsManager.getInstance());
                return wizard;
            });
            // exception handling differs from implementation at GuiHelper.runInEDTAndWait()
            if (SwingUtilities.isEventDispatchThread()) {
                authTask.run();
            } else {
                SwingUtilities.invokeAndWait(authTask);
            }
        } catch (MalformedURLException | InterruptedException | InvocationTargetException e) {
            throw new MissingOAuthAccessTokenException(e);
        }
    }

    protected void addAuth(HttpClient connection) throws OsmTransferException {
        final String authMethod = OsmApi.getAuthMethod();
        if ("basic".equals(authMethod)) {
            addBasicAuthorizationHeader(connection);
        } else if ("oauth".equals(authMethod)) {
            addOAuthAuthorizationHeader(connection);
        } else {
            String msg = tr("Unexpected value for preference ''{0}''. Got ''{1}''.", "osm-server.auth-method", authMethod);
            Logging.warn(msg);
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
