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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthException;

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
    protected OAuthParameters oauthParameters;
    protected IOAuthParameters oAuth20Parameters;

    /**
     * Retrieves OAuth access token.
     * @since 12803
     */
    public interface OAuthAccessTokenFetcher {
        /**
         * Obtains an OAuth access token for the connection. Afterwards, the token is accessible via {@link OAuthAccessTokenHolder}.
         * @param serverUrl the URL to OSM server
         * @throws InterruptedException if we're interrupted while waiting for the event dispatching thread to finish OAuth authorization task
         * @throws InvocationTargetException if an exception is thrown while running OAuth authorization task
         */
        void obtainAccessToken(URL serverUrl) throws InvocationTargetException, InterruptedException;
    }

    static volatile OAuthAccessTokenFetcher fetcher = u -> {
        throw new JosmRuntimeException("OsmConnection.setOAuthAccessTokenFetcher() has not been called");
    };

    /**
     * Sets the OAuth access token fetcher.
     * @param tokenFetcher new OAuth access token fetcher. Cannot be null
     * @since 12803
     */
    public static void setOAuthAccessTokenFetcher(OAuthAccessTokenFetcher tokenFetcher) {
        fetcher = Objects.requireNonNull(tokenFetcher, "tokenFetcher");
    }

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
     * Signs the connection with an OAuth authentication header
     *
     * @param connection the connection
     *
     * @throws MissingOAuthAccessTokenException if there is currently no OAuth Access Token configured
     * @throws OsmTransferException if signing fails
     */
    protected void addOAuthAuthorizationHeader(HttpClient connection) throws OsmTransferException {
        if (oauthParameters == null) {
            oauthParameters = OAuthParameters.createFromApiUrl(OsmApi.getOsmApi().getServerUrl());
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
     * Obtains an OAuth access token for the connection.
     * Afterwards, the token is accessible via {@link OAuthAccessTokenHolder} / {@link CredentialsManager}.
     * @param connection connection for which the access token should be obtained
     * @throws MissingOAuthAccessTokenException if the process cannot be completed successfully
     */
    protected void obtainAccessToken(final HttpClient connection) throws MissingOAuthAccessTokenException {
        try {
            final URL apiUrl = new URL(OsmApi.getOsmApi().getServerUrl());
            if (!Objects.equals(apiUrl.getHost(), connection.getURL().getHost())) {
                throw new MissingOAuthAccessTokenException();
            }
            fetcher.obtainAccessToken(apiUrl);
            OAuthAccessTokenHolder.getInstance().setSaveToPreferences(true);
            OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
        } catch (MalformedURLException | InvocationTargetException e) {
            throw new MissingOAuthAccessTokenException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MissingOAuthAccessTokenException(e);
        }
    }

    /**
     * Obtains an OAuth access token for the connection.
     * Afterwards, the token is accessible via {@link OAuthAccessTokenHolder} / {@link CredentialsManager}.
     * @throws MissingOAuthAccessTokenException if the process cannot be completed successfully
     */
    private void obtainOAuth20Token() throws MissingOAuthAccessTokenException {
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
        AtomicBoolean done = new AtomicBoolean();
        Consumer<IOAuthToken> consumer = authToken -> {
                    if (!remoteControlIsRunning) {
                        RemoteControl.stop();
                    }
                    // Clean up old token/password
                    OAuthAccessTokenHolder.getInstance().setAccessToken(null);
                    OAuthAccessTokenHolder.getInstance().setAccessToken(OsmApi.getOsmApi().getServerUrl(), authToken);
                    OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
                    synchronized (done) {
                        done.set(true);
                        done.notifyAll();
                    }
                };
        new OAuth20Authorization().authorize(oAuth20Parameters,
                consumer, OsmScopes.read_gpx, OsmScopes.write_gpx,
                OsmScopes.read_prefs, OsmScopes.write_prefs,
                OsmScopes.write_api, OsmScopes.write_notes);
        synchronized (done) {
            // Only wait at most 5 minutes
            int counter = 0;
            while (!done.get() && counter < 5) {
                try {
                    done.wait(TimeUnit.MINUTES.toMillis(1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Logging.trace(e);
                    consumer.accept(null);
                    throw new MissingOAuthAccessTokenException(e);
                }
                counter++;
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
    protected void addOAuth20AuthorizationHeader(HttpClient connection) throws OsmTransferException {
        if (this.oAuth20Parameters == null) {
            this.oAuth20Parameters = OAuthParameters.createFromApiUrl(connection.getURL().getHost(), OAuthVersion.OAuth20);
        }
        OAuthAccessTokenHolder holder = OAuthAccessTokenHolder.getInstance();
        IOAuthToken token = holder.getAccessToken(connection.getURL().toExternalForm(), OAuthVersion.OAuth20);
        if (token == null) {
            obtainOAuth20Token();
            token = holder.getAccessToken(connection.getURL().toExternalForm(), OAuthVersion.OAuth20);
        }
        if (token == null) { // check if wizard completed
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
            case "oauth":
                addOAuthAuthorizationHeader(connection);
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
