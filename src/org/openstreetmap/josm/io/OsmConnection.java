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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsAgentResponse;
import org.openstreetmap.josm.io.auth.CredentialsManager;
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
    protected boolean cancel;
    protected HttpClient activeConnection;
    protected OAuthParameters oauthParameters;

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
