// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.Authenticator.RequestorType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.oauth.IOAuthParameters;
import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuth20Authorization;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.data.oauth.osm.OsmScopes;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsAgentResponse;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 *
 * @author imi
 */
public class OsmConnection {

    private static final String BASIC_AUTH = "Basic ";

    protected boolean cancel;
    protected HttpClient activeConnection;
    protected IOAuthParameters oAuth20Parameters;

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
     * Retrieves login from basic authentication header, if set.
     *
     * @param con the connection
     * @return login from basic authentication header, or {@code null}
     * @throws OsmTransferException if something went wrong. Check for nested exceptions
     * @since 12992
     */
    protected String retrieveBasicAuthorizationLogin(HttpClient con) throws OsmTransferException {
        String auth = con.getRequestHeader("Authorization");
        if (auth != null && auth.startsWith(BASIC_AUTH)) {
            try {
                String[] token = new String(Base64.getDecoder().decode(auth.substring(BASIC_AUTH.length())),
                        StandardCharsets.UTF_8).split(":", -1);
                if (token.length == 2) {
                    return token[0];
                }
            } catch (IllegalArgumentException e) {
                Logging.error(e);
            }
        }
        return null;
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
            } else {
                String username = response.getUsername() == null ? "" : response.getUsername();
                String password = response.getPassword() == null ? "" : String.valueOf(response.getPassword());
                String token = username + ':' + password;
                con.setHeader("Authorization", BASIC_AUTH + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    /**
     * Obtains an OAuth access token for the connection.
     * Afterwards, the token is accessible via {@link OAuthAccessTokenHolder} / {@link CredentialsManager}.
     * @throws MissingOAuthAccessTokenException if the process cannot be completed successfully
     */
    private void obtainOAuth20Token() throws MissingOAuthAccessTokenException {
        if (Utils.isEmpty(this.oAuth20Parameters.getClientId())) {
            // Without a client id the authorization request is rejected by the server with "Missing required
            // parameter: client_id", so do not send the user to the browser at all, but tell them what is missing.
            throw MissingOAuthAccessTokenException.clientIdMissing(OsmApi.getOsmApi().getServerUrl());
        }
        if (!Boolean.TRUE.equals(GuiHelper.runInEDTAndWaitAndReturn(() ->
                ConditionalOptionPaneUtil.showConfirmationDialog("oauth.oauth20.obtain.automatically",
                    MainApplication.getMainFrame(),
                    tr("Obtain OAuth 2.0 token for authentication?"),
                    tr("Obtain authentication to OSM servers"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_OPTION)))) {
            return; // User doesn't want to perform auth
        }
        final boolean remoteControlIsRunning = Boolean.TRUE.equals(RemoteControl.PROP_REMOTECONTROL_ENABLED.get());
        if (!remoteControlIsRunning) {
            RemoteControl.start();
        }
        CountDownLatch done = new CountDownLatch(1);
        Consumer<Optional<IOAuthToken>> consumer = authToken -> {
                    if (!remoteControlIsRunning) {
                        RemoteControl.stop();
                    }
                    // Clean up old token/password
                    OAuthAccessTokenHolder.getInstance().setAccessToken(OsmApi.getOsmApi().getServerUrl(), authToken.orElse(null));
                    OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
                    done.countDown();
                };
        new OAuth20Authorization().authorize(oAuth20Parameters,
                consumer, OsmScopes.getJosmScopes());
        // Only wait at most 5 minutes for the user to authorize JOSM in the web browser
        try {
            if (!done.await(5, TimeUnit.MINUTES)) {
                Logging.warn("No OAuth access token received from {0} within 5 minutes", OsmApi.getOsmApi().getServerUrl());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logging.trace(e);
            consumer.accept(Optional.empty());
            throw new MissingOAuthAccessTokenException(e);
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
    protected void addOAuth20AuthorizationHeader(HttpClient connection) throws OsmTransferException {
        if (this.oAuth20Parameters == null) {
            // Look them up with the API URL and not with the host of the request: OAuthParameters.createFromApiUrl()
            // reduces it to the host itself for the stored parameters, and needs the URL for the default ones.
            this.oAuth20Parameters = OAuthParameters.createFromApiUrl(OsmApi.getOsmApi().getServerUrl(), OAuthVersion.OAuth20);
        }
        OAuthAccessTokenHolder holder = OAuthAccessTokenHolder.getInstance();
        IOAuthToken token = holder.getAccessToken(connection.getURL().toExternalForm(), OAuthVersion.OAuth20);
        // A token is obtained for the API, and so is only found for requests to the host of the API
        if (token == null && Objects.equals(connection.getURL().getHost(), OsmApi.getOsmApi().getHost())) {
            obtainOAuth20Token();
            token = holder.getAccessToken(connection.getURL().toExternalForm(), OAuthVersion.OAuth20);
        }
        if (token == null) { // check if authorization completed
            throw new MissingOAuthAccessTokenException();
        }
        try {
            token.sign(connection);
        } catch (org.openstreetmap.josm.data.oauth.OAuthException e) {
            throw new OsmTransferException(tr("Failed to sign a HTTP connection with an OAuth Authentication header"), e);
        }
    }

    protected void addAuth(HttpClient connection) throws OsmTransferException {
        final String authMethod = OsmApi.getAuthMethod();
        switch (authMethod) {
            case "basic":
                addBasicAuthorizationHeader(connection);
                return;
            case "oauth20":
                addOAuth20AuthorizationHeader(connection);
                return;
            default:
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
