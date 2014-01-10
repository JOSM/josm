// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.data.oauth.OsmPrivileges;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * An OAuth 1.0 authorization client.
 * @since 2746
 */
public class OsmOAuthAuthorizationClient {
    private final OAuthParameters oauthProviderParameters;
    private final OAuthConsumer consumer;
    private final OAuthProvider provider;
    private boolean canceled;
    private HttpURLConnection connection;

    private static class SessionId {
        String id;
        String token;
        String userName;
    }

    /**
     * Creates a new authorisation client with default OAuth parameters
     *
     */
    public OsmOAuthAuthorizationClient() {
        oauthProviderParameters = OAuthParameters.createDefault(Main.pref.get("osm-server.url"));
        consumer = oauthProviderParameters.buildConsumer();
        provider = oauthProviderParameters.buildProvider(consumer);
    }

    /**
     * Creates a new authorisation client with the parameters <code>parameters</code>.
     *
     * @param parameters the OAuth parameters. Must not be null.
     * @throws IllegalArgumentException if parameters is null
     */
    public OsmOAuthAuthorizationClient(OAuthParameters parameters) throws IllegalArgumentException {
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
    public OsmOAuthAuthorizationClient(OAuthParameters parameters, OAuthToken requestToken) throws IllegalArgumentException {
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
        DefaultOAuthProvider p  = (DefaultOAuthProvider)provider;
        canceled = true;
        if (p != null) {
            try {
                Field f =  p.getClass().getDeclaredField("connection");
                f.setAccessible(true);
                HttpURLConnection con = (HttpURLConnection)f.get(p);
                if (con != null) {
                    con.disconnect();
                }
            } catch (NoSuchFieldException e) {
                Main.error(e);
                Main.warn(tr("Failed to cancel running OAuth operation"));
            } catch (SecurityException e) {
                Main.error(e);
                Main.warn(tr("Failed to cancel running OAuth operation"));
            } catch (IllegalAccessException e) {
                Main.error(e);
                Main.warn(tr("Failed to cancel running OAuth operation"));
            }
        }
        synchronized(this) {
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
        } catch(OAuthCommunicationException e){
            if (canceled)
                throw new OsmTransferCanceledException();
            throw new OsmOAuthAuthorizationException(e);
        } catch(OAuthException e){
            if (canceled)
                throw new OsmTransferCanceledException();
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
        } catch(OAuthCommunicationException e){
            if (canceled)
                throw new OsmTransferCanceledException();
            throw new OsmOAuthAuthorizationException(e);
        } catch(OAuthException e){
            if (canceled)
                throw new OsmTransferCanceledException();
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
        StringBuilder sb = new StringBuilder();

        // OSM is an OAuth 1.0 provider and JOSM isn't a web app. We just add the oauth request token to
        // the authorisation request, no callback parameter.
        //
        sb.append(oauthProviderParameters.getAuthoriseUrl()).append("?")
        .append(OAuth.OAUTH_TOKEN).append("=").append(requestToken.getKey());
        return sb.toString();
    }

    protected String extractToken(HttpURLConnection connection) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String c;
            Pattern p = Pattern.compile(".*authenticity_token.*value=\"([^\"]+)\".*");
            while ((c = r.readLine()) != null) {
                Matcher m = p.matcher(c);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (IOException e) {
            Main.error(e);
            return null;
        } finally {
            Utils.close(r);
        }
        return null;
    }

    protected SessionId extractOsmSession(HttpURLConnection connection) {
        List<String> setCookies = connection.getHeaderFields().get("Set-Cookie");
        if (setCookies == null)
            // no cookies set
            return null;

        for (String setCookie: setCookies) {
            String[] kvPairs = setCookie.split(";");
            if (kvPairs == null || kvPairs.length == 0) {
                continue;
            }
            for (String kvPair : kvPairs) {
                kvPair = kvPair.trim();
                String [] kv = kvPair.split("=");
                if (kv == null || kv.length != 2) {
                    continue;
                }
                if (kv[0].equals("_osm_session")) {
                    // osm session cookie found
                    String token = extractToken(connection);
                    if(token == null)
                        return null;
                    SessionId si = new SessionId();
                    si.id = kv[1];
                    si.token = token;
                    return si;
                }
            }
        }
        return null;
    }

    protected String buildPostRequest(Map<String,String> parameters) throws OsmOAuthAuthorizationException {
        try {
            StringBuilder sb = new StringBuilder();

            for(Iterator<Entry<String,String>> it = parameters.entrySet().iterator(); it.hasNext();) {
                Entry<String,String> entry = it.next();
                String value = entry.getValue();
                value = (value == null) ? "" : value;
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(value, "UTF-8"));
                if (it.hasNext()) {
                    sb.append("&");
                }
            }
            return sb.toString();
        } catch(UnsupportedEncodingException e) {
            throw new OsmOAuthAuthorizationException(e);
        }
    }

    /**
     * Derives the OSM login URL from the OAuth Authorization Website URL
     *
     * @return the OSM login URL
     * @throws OsmOAuthAuthorizationException if something went wrong, in particular if the
     * URLs are malformed
     */
    public String buildOsmLoginUrl() throws OsmOAuthAuthorizationException{
        try {
            URL autUrl = new URL(oauthProviderParameters.getAuthoriseUrl());
            URL url = new URL(Main.pref.get("oauth.protocol", "https"), autUrl.getHost(), autUrl.getPort(), "/login");
            return url.toString();
        } catch(MalformedURLException e) {
            throw new OsmOAuthAuthorizationException(e);
        }
    }

    /**
     * Derives the OSM logout URL from the OAuth Authorization Website URL
     *
     * @return the OSM logout URL
     * @throws OsmOAuthAuthorizationException if something went wrong, in particular if the
     * URLs are malformed
     */
    protected String buildOsmLogoutUrl() throws OsmOAuthAuthorizationException{
        try {
            URL autUrl = new URL(oauthProviderParameters.getAuthoriseUrl());
            URL url = new URL("http", autUrl.getHost(), autUrl.getPort(), "/logout");
            return url.toString();
        } catch(MalformedURLException e) {
            throw new OsmOAuthAuthorizationException(e);
        }
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
            StringBuilder sb = new StringBuilder();
            sb.append(buildOsmLoginUrl()).append("?cookie_test=true");
            URL url = new URL(sb.toString());
            synchronized(this) {
                connection = Utils.openHttpConnection(url);
            }
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            SessionId sessionId = extractOsmSession(connection);
            if (sessionId == null)
                throw new OsmOAuthAuthorizationException(tr("OSM website did not return a session cookie in response to ''{0}'',", url.toString()));
            return sessionId;
        } catch(IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            synchronized(this) {
                connection = null;
            }
        }
    }

    /**
     * Submits a request to the OSM website for a OAuth form. The OSM website replies a session token in
     * a hidden parameter.
     *
     * @throws OsmOAuthAuthorizationException if something went wrong
     */
    protected void fetchOAuthToken(SessionId sessionId, OAuthToken requestToken) throws OsmOAuthAuthorizationException {
        try {
            URL url = new URL(getAuthoriseUrl(requestToken));
            synchronized(this) {
                connection = Utils.openHttpConnection(url);
            }
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setRequestProperty("Cookie", "_osm_session=" + sessionId.id + "; _osm_username=" + sessionId.userName);
            connection.connect();
            sessionId.token = extractToken(connection);
            if (sessionId.token == null)
                throw new OsmOAuthAuthorizationException(tr("OSM website did not return a session cookie in response to ''{0}'',", url.toString()));
        } catch(IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            synchronized(this) {
                connection = null;
            }
        }
    }

    protected void authenticateOsmSession(SessionId sessionId, String userName, String password) throws OsmLoginFailedException {
        DataOutputStream dout = null;
        try {
            URL url = new URL(buildOsmLoginUrl());
            synchronized(this) {
                connection = Utils.openHttpConnection(url);
            }
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            Map<String,String> parameters = new HashMap<String, String>();
            parameters.put("username", userName);
            parameters.put("password", password);
            parameters.put("referer", "/");
            parameters.put("commit", "Login");
            parameters.put("authenticity_token", sessionId.token);

            String request = buildPostRequest(parameters);

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(request.length()));
            connection.setRequestProperty("Cookie", "_osm_session=" + sessionId.id);
            // make sure we can catch 302 Moved Temporarily below
            connection.setInstanceFollowRedirects(false);

            connection.connect();

            dout = new DataOutputStream(connection.getOutputStream());
            dout.writeBytes(request);
            dout.flush();
            Utils.close(dout);

            // after a successful login the OSM website sends a redirect to a follow up page. Everything
            // else, including a 200 OK, is a failed login. A 200 OK is replied if the login form with
            // an error page is sent to back to the user.
            //
            int retCode = connection.getResponseCode();
            if (retCode != HttpURLConnection.HTTP_MOVED_TEMP)
                throw new OsmOAuthAuthorizationException(tr("Failed to authenticate user ''{0}'' with password ''***'' as OAuth user", userName));
        } catch(OsmOAuthAuthorizationException e) {
            throw new OsmLoginFailedException(e.getCause());
        } catch(IOException e) {
            throw new OsmLoginFailedException(e);
        } finally {
            Utils.close(dout);
            synchronized(this) {
                connection = null;
            }
        }
    }

    protected void logoutOsmSession(SessionId sessionId) throws OsmOAuthAuthorizationException {
        try {
            URL url = new URL(buildOsmLogoutUrl());
            synchronized(this) {
                connection = Utils.openHttpConnection(url);
            }
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
        } catch(MalformedURLException e) {
            throw new OsmOAuthAuthorizationException(e);
        } catch(IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        }  finally {
            synchronized(this) {
                connection = null;
            }
        }
    }

    protected void sendAuthorisationRequest(SessionId sessionId, OAuthToken requestToken, OsmPrivileges privileges) throws OsmOAuthAuthorizationException {
        Map<String, String> parameters = new HashMap<String, String>();
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
        if(privileges.isAllowModifyNotes()) {
            parameters.put("allow_write_notes", "yes");
        }

        parameters.put("commit", "Save changes");

        String request = buildPostRequest(parameters);
        DataOutputStream dout = null;
        try {
            URL url = new URL(oauthProviderParameters.getAuthoriseUrl());
            synchronized(this) {
                connection = Utils.openHttpConnection(url);
            }
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(request.length()));
            connection.setRequestProperty("Cookie", "_osm_session=" + sessionId.id + "; _osm_username=" + sessionId.userName);
            connection.setInstanceFollowRedirects(false);

            connection.connect();

            dout = new DataOutputStream(connection.getOutputStream());
            dout.writeBytes(request);
            dout.flush();

            int retCode = connection.getResponseCode();
            if (retCode != HttpURLConnection.HTTP_OK)
                throw new OsmOAuthAuthorizationException(tr("Failed to authorize OAuth request  ''{0}''", requestToken.getKey()));
        } catch(MalformedURLException e) {
            throw new OsmOAuthAuthorizationException(e);
        } catch(IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            Utils.close(dout);
            synchronized(this) {
                connection = null;
            }
        }
    }

    /**
     * Automatically authorises a request token for a set of privileges.
     *
     * @param requestToken the request token. Must not be null.
     * @param osmUserName the OSM user name. Must not be null.
     * @param osmPassword the OSM password. Must not be null.
     * @param privileges the set of privileges. Must not be null.
     * @param monitor a progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException if requestToken is null
     * @throws IllegalArgumentException if osmUserName is null
     * @throws IllegalArgumentException if osmPassword is null
     * @throws IllegalArgumentException if privileges is null
     * @throws OsmOAuthAuthorizationException if the authorisation fails
     * @throws OsmTransferCanceledException if the task is canceled by the user
     */
    public void authorise(OAuthToken requestToken, String osmUserName, String osmPassword, OsmPrivileges privileges, ProgressMonitor monitor) throws IllegalArgumentException, OsmOAuthAuthorizationException, OsmTransferCanceledException{
        CheckParameterUtil.ensureParameterNotNull(requestToken, "requestToken");
        CheckParameterUtil.ensureParameterNotNull(osmUserName, "osmUserName");
        CheckParameterUtil.ensureParameterNotNull(osmPassword, "osmPassword");
        CheckParameterUtil.ensureParameterNotNull(privileges, "privileges");

        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Authorizing OAuth Request token ''{0}'' at the OSM website ...", requestToken.getKey()));
            monitor.setTicksCount(4);
            monitor.indeterminateSubTask(tr("Initializing a session at the OSM website..."));
            SessionId sessionId = fetchOsmWebsiteSessionId();
            sessionId.userName = osmUserName;
            if (canceled)
                throw new OsmTransferCanceledException();
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Authenticating the session for user ''{0}''...", osmUserName));
            authenticateOsmSession(sessionId, osmUserName, osmPassword);
            if (canceled)
                throw new OsmTransferCanceledException();
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Authorizing request token ''{0}''...", requestToken.getKey()));
            sendAuthorisationRequest(sessionId, requestToken, privileges);
            if (canceled)
                throw new OsmTransferCanceledException();
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Logging out session ''{0}''...", sessionId));
            logoutOsmSession(sessionId);
            if (canceled)
                throw new OsmTransferCanceledException();
            monitor.worked(1);
        } catch(OsmOAuthAuthorizationException e) {
            if (canceled)
                throw new OsmTransferCanceledException();
            throw e;
        } finally {
            monitor.finishTask();
        }
    }
}
