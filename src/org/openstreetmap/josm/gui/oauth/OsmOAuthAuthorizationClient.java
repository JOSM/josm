// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.DataOutputStream;
import java.io.IOException;
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
import java.util.logging.Logger;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.data.oauth.OsmPrivileges;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferCancelledException;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class OsmOAuthAuthorizationClient {
    @SuppressWarnings("unused")
    static private final Logger logger = Logger.getLogger(OsmOAuthAuthorizationClient.class.getName());

    private OAuthParameters oauthProviderParameters;
    private OAuthConsumer consumer;
    private OAuthProvider provider;
    private boolean canceled;
    private HttpURLConnection connection;

    /**
     * Creates a new authorisation client with default OAuth parameters
     *
     */
    public OsmOAuthAuthorizationClient() {
        oauthProviderParameters = OAuthParameters.createDefault();
        consumer = oauthProviderParameters.buildConsumer();
        provider = oauthProviderParameters.buildProvider(consumer);
    }

    /**
     * Creates a new authorisation client with the parameters <code>parameters</code>.
     *
     * @param parameters the OAuth parameters. Must not be null.
     * @throws IllegalArgumentException thrown if parameters is null
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
     * @throws IllegalArgumentException thrown if parameters is null
     * @throws IllegalArgumentException thrown if requestToken is null
     */
    public OsmOAuthAuthorizationClient(OAuthParameters parameters, OAuthToken requestToken) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        oauthProviderParameters = new OAuthParameters(parameters);
        consumer = oauthProviderParameters.buildConsumer();
        provider = oauthProviderParameters.buildProvider(consumer);
        consumer.setTokenWithSecret(requestToken.getKey(), requestToken.getSecret());
    }

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
            } catch(NoSuchFieldException e) {
                e.printStackTrace();
                System.err.println(tr("Warning: failed to cancel running OAuth operation"));
            } catch(SecurityException e) {
                e.printStackTrace();
                System.err.println(tr("Warning: failed to cancel running OAuth operation"));
            } catch(IllegalAccessException e) {
                e.printStackTrace();
                System.err.println(tr("Warning: failed to cancel running OAuth operation"));
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
     * @param monitor a progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null
     * @return the OAuth Request Token
     * @throws OsmOAuthAuthorizationException thrown if something goes wrong when retrieving the request token
     */
    public OAuthToken getRequestToken(ProgressMonitor monitor) throws OsmOAuthAuthorizationException, OsmTransferCancelledException {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Retrieving OAuth Request Token from ''{0}''", oauthProviderParameters.getRequestTokenUrl()));
            provider.retrieveRequestToken(null);
            return OAuthToken.createToken(consumer);
        } catch(OAuthCommunicationException e){
            if (canceled)
                throw new OsmTransferCancelledException();
            throw new OsmOAuthAuthorizationException(e);
        } catch(OAuthException e){
            if (canceled)
                throw new OsmTransferCancelledException();
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Submits a request for an Access Token to the Access Token Endpoint Url of the OAuth Service
     * Provider and replies the request token.
     *
     * You must have requested a Request Token using {@see #getRequestToken(ProgressMonitor)} first.
     *
     * @param monitor a progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null
     * @return the OAuth Access Token
     * @throws OsmOAuthAuthorizationException thrown if something goes wrong when retrieving the request token
     * @see #getRequestToken(ProgressMonitor)
     */
    public OAuthToken getAccessToken(ProgressMonitor monitor) throws OsmOAuthAuthorizationException, OsmTransferCancelledException {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Retrieving OAuth Access Token from ''{0}''", oauthProviderParameters.getAccessTokenUrl()));
            provider.retrieveAccessToken(null);
            return OAuthToken.createToken(consumer);
        } catch(OAuthCommunicationException e){
            if (canceled)
                throw new OsmTransferCancelledException();
            throw new OsmOAuthAuthorizationException(e);
        } catch(OAuthException e){
            if (canceled)
                throw new OsmTransferCancelledException();
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

    protected String extractOsmSession(HttpURLConnection connection) {
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
                if (kv[0].equals("_osm_session"))
                    // osm session cookie found
                    return kv[1];
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
     * @throws OsmOAuthAuthorizationException thrown if something went wrong, in particular if the
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
     * @throws OsmOAuthAuthorizationException thrown if something went wrong, in particular if the
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
     * @return the session ID
     * @throws OsmOAuthAuthorizationException thrown if something went wrong
     */
    protected String fetchOsmWebsiteSessionId() throws OsmOAuthAuthorizationException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(buildOsmLoginUrl()).append("?cookie_test=true");
            URL url = new URL(sb.toString());
            synchronized(this) {
                connection = (HttpURLConnection)url.openConnection();
            }
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            setHttpRequestParameters(connection);
            connection.connect();
            String sessionId = extractOsmSession(connection);
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

    protected void authenticateOsmSession(String sessionId, String userName, String password) throws OsmLoginFailedException {
        DataOutputStream dout = null;
        try {
            URL url = new URL(buildOsmLoginUrl());
            synchronized(this) {
                connection = (HttpURLConnection)url.openConnection();
            }
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            Map<String,String> parameters = new HashMap<String, String>();
            parameters.put("user[email]", userName);
            parameters.put("user[password]", password);
            parameters.put("referer", "/");
            parameters.put("commit", "Login");

            String request = buildPostRequest(parameters);

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(request.length()));
            connection.setRequestProperty("Cookie", "_osm_session=" + sessionId);
            // make sure we can catch 302 Moved Temporarily below
            connection.setInstanceFollowRedirects(false);
            setHttpRequestParameters(connection);

            connection.connect();

            dout = new DataOutputStream(connection.getOutputStream());
            dout.writeBytes(request);
            dout.flush();
            dout.close();

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
            if (dout != null) {
                try {
                    dout.close();
                } catch(IOException e) { /* ignore */ }
            }
            synchronized(this) {
                connection = null;
            }
        }
    }

    protected void logoutOsmSession(String sessionId) throws OsmOAuthAuthorizationException {
        try {
            URL url = new URL(buildOsmLogoutUrl());
            synchronized(this) {
                connection = (HttpURLConnection)url.openConnection();
            }
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            setHttpRequestParameters(connection);
            connection.connect();
        }catch(MalformedURLException e) {
            throw new OsmOAuthAuthorizationException(e);
        } catch(IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        }  finally {
            synchronized(this) {
                connection = null;
            }
        }
    }

    protected void sendAuthorisationRequest(String sessionId, OAuthToken requestToken, OsmPrivileges privileges) throws OsmOAuthAuthorizationException {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("oauth_token", requestToken.getKey());
        parameters.put("oauth_callback", "");
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

        parameters.put("commit", "Save changes");

        String request = buildPostRequest(parameters);
        DataOutputStream dout = null;
        try {
            URL url = new URL(oauthProviderParameters.getAuthoriseUrl());
            synchronized(this) {
                connection = (HttpURLConnection)url.openConnection();
            }
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(request.length()));
            connection.setRequestProperty("Cookie", "_osm_session=" + sessionId);
            connection.setInstanceFollowRedirects(false);
            setHttpRequestParameters(connection);

            connection.connect();

            dout = new DataOutputStream(connection.getOutputStream());
            dout.writeBytes(request);
            dout.flush();
            dout.close();

            int retCode = connection.getResponseCode();
            if (retCode != HttpURLConnection.HTTP_MOVED_TEMP)
                throw new OsmOAuthAuthorizationException(tr("Failed to authorize OAuth request  ''{0}''", requestToken.getKey()));
        } catch(MalformedURLException e) {
            throw new OsmOAuthAuthorizationException(e);
        } catch(IOException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            if (dout != null) {
                try {
                    dout.close();
                } catch(IOException e) { /* ignore */ }
            }
            synchronized(this) {
                connection = null;
            }
        }
    }

    protected void setHttpRequestParameters(HttpURLConnection connection) {
        connection.setRequestProperty("User-Agent", Version.getInstance().getAgentString());
        connection.setRequestProperty("Host", connection.getURL().getHost());
    }

    /**
     * Automatically authorises a request token for a set of privileges.
     *
     * @param requestToken the request token. Must not be null.
     * @param osmUserName the OSM user name. Must not be null.
     * @param osmPassword the OSM password. Must not be null.
     * @param privileges the set of privileges. Must not be null.
     * @param monitor a progress monitor. Defaults to {@see NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException thrown if requestToken is null
     * @throws IllegalArgumentException thrown if osmUserName is null
     * @throws IllegalArgumentException thrown if osmPassword is null
     * @throws IllegalArgumentException thrown if privileges is null
     * @throws OsmOAuthAuthorizationException thrown if the authorisation fails
     * @throws OsmTransferCancelledException thrown if the task is cancelled by the user
     */
    public void authorise(OAuthToken requestToken, String osmUserName, String osmPassword, OsmPrivileges privileges, ProgressMonitor monitor) throws IllegalArgumentException, OsmOAuthAuthorizationException, OsmTransferCancelledException{
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
            String sessionId = fetchOsmWebsiteSessionId();
            if (canceled)
                throw new OsmTransferCancelledException();
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Authenticating the session for user ''{0}''...", osmUserName));
            authenticateOsmSession(sessionId, osmUserName, osmPassword);
            if (canceled)
                throw new OsmTransferCancelledException();
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Authorizing request token ''{0}''...", requestToken.getKey()));
            sendAuthorisationRequest(sessionId, requestToken, privileges);
            if (canceled)
                throw new OsmTransferCancelledException();
            monitor.worked(1);

            monitor.indeterminateSubTask(tr("Logging out session ''{0}''...", sessionId));
            logoutOsmSession(sessionId);
            if (canceled)
                throw new OsmTransferCancelledException();
            monitor.worked(1);
        } catch(OsmOAuthAuthorizationException e) {
            if (canceled)
                throw new OsmTransferCancelledException();
            throw e;
        } finally {
            monitor.finishTask();
        }
    }
}
