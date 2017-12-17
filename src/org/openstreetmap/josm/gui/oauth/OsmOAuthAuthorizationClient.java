// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.data.oauth.OsmPrivileges;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthException;

/**
 * An OAuth 1.0 authorization client.
 * @since 2746
 */
public class OsmOAuthAuthorizationClient {
    private final OAuthParameters oauthProviderParameters;
    private final OAuthConsumer consumer;
    private final OAuthProvider provider;
    private boolean canceled;
    private HttpClient connection;

    private static class SessionId {
        private String id;
        private String token;
        private String userName;
    }

    /**
     * Creates a new authorisation client with the parameters <code>parameters</code>.
     *
     * @param parameters the OAuth parameters. Must not be null.
     * @throws IllegalArgumentException if parameters is null
     */
    public OsmOAuthAuthorizationClient(OAuthParameters parameters) {
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        oauthProviderParameters = new OAuthParameters(parameters);
        consumer = oauthProviderParameters.buildConsumer();
        provider = oauthProviderParameters.buildProvider(consumer);
    }

    /**
     * Creates a new authorisation client with the parameters <code>parameters</code>
     * and an already known Request Token.
     *
     * @param parameters the OAuth parameters. Must not be null.
     * @param requestToken the request token. Must not be null.
     * @throws IllegalArgumentException if parameters is null
     * @throws IllegalArgumentException if requestToken is null
     */
    public OsmOAuthAuthorizationClient(OAuthParameters parameters, OAuthToken requestToken) {
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        oauthProviderParameters = new OAuthParameters(parameters);
        consumer = oauthProviderParameters.buildConsumer();
        provider = oauthProviderParameters.buildProvider(consumer);
        consumer.setTokenWithSecret(requestToken.getKey(), requestToken.getSecret());
    }

    /**
     * Cancels the current OAuth operation.
     */
    public void cancel() {
        canceled = true;
        synchronized (this) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Submits a request for a Request Token to the Request Token Endpoint Url of the OAuth Service
     * Provider and replies the request token.
     *
     * @param monitor a progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @return the OAuth Request Token
     * @throws OsmOAuthAuthorizationException if something goes wrong when retrieving the request token
     * @throws OsmTransferCanceledException if the user canceled the request
     */
    public OAuthToken getRequestToken(ProgressMonitor monitor) throws OsmOAuthAuthorizationException, OsmTransferCanceledException {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Retrieving OAuth Request Token from ''{0}''", oauthProviderParameters.getRequestTokenUrl()));
            provider.retrieveRequestToken(consumer, "");
            return OAuthToken.createToken(consumer);
        } catch (OAuthException e) {
            if (canceled)
                throw new OsmTransferCanceledException(e);
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Submits a request for an Access Token to the Access Token Endpoint Url of the OAuth Service
     * Provider and replies the request token.
     *
     * You must have requested a Request Token using {@link #getRequestToken(ProgressMonitor)} first.
     *
     * @param monitor a progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @return the OAuth Access Token
     * @throws OsmOAuthAuthorizationException if something goes wrong when retrieving the request token
     * @throws OsmTransferCanceledException if the user canceled the request
     * @see #getRequestToken(ProgressMonitor)
     */
    public OAuthToken getAccessToken(ProgressMonitor monitor) throws OsmOAuthAuthorizationException, OsmTransferCanceledException {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Retrieving OAuth Access Token from ''{0}''", oauthProviderParameters.getAccessTokenUrl()));
            provider.retrieveAccessToken(consumer, null);
            return OAuthToken.createToken(consumer);
        } catch (OAuthException e) {
            if (canceled)
                throw new OsmTransferCanceledException(e);
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Builds the authorise URL for a given Request Token. Users can be redirected to this URL.
     * There they can login to OSM and authorise the request.
     *
     * @param requestToken  the request token
     * @return  the authorise URL for this request
     */
    public String getAuthoriseUrl(OAuthToken requestToken) {
        StringBuilder sb = new StringBuilder(32);

        // OSM is an OAuth 1.0 provider and JOSM isn't a web app. We just add the oauth request token to
        // the authorisation request, no callback parameter.
        //
        sb.append(oauthProviderParameters.getAuthoriseUrl()).append('?'+OAuth.OAUTH_TOKEN+'=').append(requestToken.getKey());
        return sb.toString();
    }

    protected String extractToken() {
        try (BufferedReader r = connection.getResponse().getContentReader()) {
            String c;
            Pattern p = Pattern.compile(".*authenticity_token.*value=\"([^\"]+)\".*");
            while ((c = r.readLine()) != null) {
                Matcher m = p.matcher(c);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (IOException e) {
            Logging.error(e);
            return null;
        }
        Logging.warn("No authenticity_token found in response!");
        return null;
    }

    protected SessionId extractOsmSession() throws IOException, URISyntaxException {
        // response headers might not contain the cookie, see #12584
        final List<String> setCookies = CookieHandler.getDefault()
                .get(connection.getURL().toURI(), Collections.<String, List<String>>emptyMap())
                .get("Cookie");
        if (setCookies == null) {
            Logging.warn("No 'Set-Cookie' in response header!");
            return null;
        }

        for (String setCookie: setCookies) {
            String[] kvPairs = setCookie.split(";");
            if (kvPairs.length == 0) {
                continue;
            }
            for (String kvPair : kvPairs) {
                kvPair = kvPair.trim();
                String[] kv = kvPair.split("=");
                if (kv.length != 2) {
                    continue;
                }
                if ("_osm_session".equals(kv[0])) {
                    // osm session cookie found
                    String token = extractToken();
                    if (token == null)
                        return null;
                    SessionId si = new SessionId();
                    si.id = kv[1];
                    si.token = token;
                    return si;
                }
            }
        }
        Logging.warn("No suitable 'Set-Cookie' in response header found! {0}", setCookies);
        return null;
    }

    protected static String buildPostRequest(Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder(32);

        for (Iterator<Entry<String, String>> it = parameters.entrySet().iterator(); it.hasNext();) {
            Entry<String, String> entry = it.next();
            String value = entry.getValue();
            value = (value == null) ? "" : value;
            sb.append(entry.getKey()).append('=').append(Utils.encodeUrl(value));
            if (it.hasNext()) {
                sb.append('&');
            }
        }
        return sb.toString();
    }

    /**
     * Submits a request to the OSM website for a login form. The OSM website replies a session ID in
     * a cookie.
     *
     * @return the session ID structure
     * @throws OsmOAuthAuthorizationException if something went wrong
     */
    protected SessionId fetchOsmWebsiteSessionId() throws OsmOAuthAuthorizationException {
        try {
            final URL url = new URL(oauthProviderParameters.getOsmLoginUrl() + "?cookie_test=true");
            synchronized (this) {
                connection = HttpClient.create(url).useCache(false);
                connection.connect();
            }
            SessionId sessionId = extractOsmSession();
            if (sessionId == null)
                throw new OsmOAuthAuthorizationException(
                        tr("OSM website did not return a session cookie in response to ''{0}'',", url.toString()));
            return sessionId;
        } catch (IOException | URISyntaxException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            synchronized (this) {
                connection = null;
            }
        }
    }

    /**
     * Submits a request to the OSM website for a OAuth form. The OSM website replies a session token in
     * a hidden parameter.
     * @param sessionId session id
     * @param requestToken request token
     *
     * @throws OsmOAuthAuthorizationException if something went wrong
     */
    protected void fetchOAuthToken(SessionId sessionId, OAuthToken requestToken) throws OsmOAuthAuthorizationException {
        try {
            URL url = new URL(getAuthoriseUrl(requestToken));
            synchronized (this) {
                connection = HttpClient.create(url)
                        .useCache(false)
                        .setHeader("Cookie", "_osm_session=" + sessionId.id + "; _osm_username=" + sessionId.userName);
                connection.connect();
            }
            sessionId.token = extractToken();
            if (sessionId.token == null)
                throw new OsmOAuthAuthorizationException(tr("OSM website did not return a session cookie in response to ''{0}'',",
                        url.toString()));
        } catch (IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            synchronized (this) {
                connection = null;
            }
        }
    }

    protected void authenticateOsmSession(SessionId sessionId, String userName, String password) throws OsmLoginFailedException {
        try {
            final URL url = new URL(oauthProviderParameters.getOsmLoginUrl());
            final HttpClient client = HttpClient.create(url, "POST").useCache(false);

            Map<String, String> parameters = new HashMap<>();
            parameters.put("username", userName);
            parameters.put("password", password);
            parameters.put("referer", "/");
            parameters.put("commit", "Login");
            parameters.put("authenticity_token", sessionId.token);
            client.setRequestBody(buildPostRequest(parameters).getBytes(StandardCharsets.UTF_8));

            client.setHeader("Content-Type", "application/x-www-form-urlencoded");
            client.setHeader("Cookie", "_osm_session=" + sessionId.id);
            // make sure we can catch 302 Moved Temporarily below
            client.setMaxRedirects(-1);

            synchronized (this) {
                connection = client;
                connection.connect();
            }

            // after a successful login the OSM website sends a redirect to a follow up page. Everything
            // else, including a 200 OK, is a failed login. A 200 OK is replied if the login form with
            // an error page is sent to back to the user.
            //
            int retCode = connection.getResponse().getResponseCode();
            if (retCode != HttpURLConnection.HTTP_MOVED_TEMP)
                throw new OsmOAuthAuthorizationException(tr("Failed to authenticate user ''{0}'' with password ''***'' as OAuth user",
                        userName));
        } catch (OsmOAuthAuthorizationException | IOException e) {
            throw new OsmLoginFailedException(e);
        } finally {
            synchronized (this) {
                connection = null;
            }
        }
    }

    protected void logoutOsmSession(SessionId sessionId) throws OsmOAuthAuthorizationException {
        try {
            URL url = new URL(oauthProviderParameters.getOsmLogoutUrl());
            synchronized (this) {
                connection = HttpClient.create(url).setMaxRedirects(-1);
                connection.connect();
            }
        } catch (IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            synchronized (this) {
                connection = null;
            }
        }
    }

    protected void sendAuthorisationRequest(SessionId sessionId, OAuthToken requestToken, OsmPrivileges privileges)
            throws OsmOAuthAuthorizationException {
        Map<String, String> parameters = new HashMap<>();
        fetchOAuthToken(sessionId, requestToken);
        parameters.put("oauth_token", requestToken.getKey());
        parameters.put("oauth_callback", "");
        parameters.put("authenticity_token", sessionId.token);
        if (privileges.isAllowWriteApi()) {
            parameters.put("allow_write_api", "yes");
        }
        if (privileges.isAllowWriteGpx()) {
            parameters.put("allow_write_gpx", "yes");
        }
        if (privileges.isAllowReadGpx()) {
            parameters.put("allow_read_gpx", "yes");
        }
        if (privileges.isAllowWritePrefs()) {
            parameters.put("allow_write_prefs", "yes");
        }
        if (privileges.isAllowReadPrefs()) {
            parameters.put("allow_read_prefs", "yes");
        }
        if (privileges.isAllowModifyNotes()) {
            parameters.put("allow_write_notes", "yes");
        }

        parameters.put("commit", "Save changes");

        String request = buildPostRequest(parameters);
        try {
            URL url = new URL(oauthProviderParameters.getAuthoriseUrl());
            final HttpClient client = HttpClient.create(url, "POST").useCache(false);
            client.setHeader("Content-Type", "application/x-www-form-urlencoded");
            client.setHeader("Cookie", "_osm_session=" + sessionId.id + "; _osm_username=" + sessionId.userName);
            client.setMaxRedirects(-1);
            client.setRequestBody(request.getBytes(StandardCharsets.UTF_8));

            synchronized (this) {
                connection = client;
                connection.connect();
            }

            int retCode = connection.getResponse().getResponseCode();
            if (retCode != HttpURLConnection.HTTP_OK)
                throw new OsmOAuthAuthorizationException(tr("Failed to authorize OAuth request  ''{0}''", requestToken.getKey()));
        } catch (IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            synchronized (this) {
                connection = null;
            }
        }
    }

    /**
     * Automatically authorises a request token for a set of privileges.
     *
     * @param requestToken the request token. Must not be null.
     * @param userName the OSM user name. Must not be null.
     * @param password the OSM password. Must not be null.
     * @param privileges the set of privileges. Must not be null.
     * @param monitor a progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException if requestToken is null
     * @throws IllegalArgumentException if osmUserName is null
     * @throws IllegalArgumentException if osmPassword is null
     * @throws IllegalArgumentException if privileges is null
     * @throws OsmOAuthAuthorizationException if the authorisation fails
     * @throws OsmTransferCanceledException if the task is canceled by the user
     */
    public void authorise(OAuthToken requestToken, String userName, String password, OsmPrivileges privileges, ProgressMonitor monitor)
            throws OsmOAuthAuthorizationException, OsmTransferCanceledException {
        CheckParameterUtil.ensureParameterNotNull(requestToken, "requestToken");
        CheckParameterUtil.ensureParameterNotNull(userName, "userName");
        CheckParameterUtil.ensureParameterNotNull(password, "password");
        CheckParameterUtil.ensureParameterNotNull(privileges, "privileges");

        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Authorizing OAuth Request token ''{0}'' at the OSM website ...", requestToken.getKey()));
            monitor.setTicksCount(4);
            monitor.indeterminateSubTask(tr("Initializing a session at the OSM website..."));
            SessionId sessionId = fetchOsmWebsiteSessionId();
            sessionId.userName = userName;
            if (canceled)
                throw new OsmTransferCanceledException("Authorization canceled");
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Authenticating the session for user ''{0}''...", userName));
            authenticateOsmSession(sessionId, userName, password);
            if (canceled)
                throw new OsmTransferCanceledException("Authorization canceled");
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Authorizing request token ''{0}''...", requestToken.getKey()));
            sendAuthorisationRequest(sessionId, requestToken, privileges);
            if (canceled)
                throw new OsmTransferCanceledException("Authorization canceled");
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Logging out session ''{0}''...", sessionId));
            logoutOsmSession(sessionId);
            if (canceled)
                throw new OsmTransferCanceledException("Authorization canceled");
            monitor.worked(1);
        } catch (OsmOAuthAuthorizationException e) {
            if (canceled)
                throw new OsmTransferCanceledException(e);
            throw e;
        } finally {
            monitor.finishTask();
        }
    }
}
