// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.oauth.IOAuthParameters;
import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmServerUserInfoReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.openstreetmap.josm.tools.XmlUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthException;

/**
 * Checks whether an OSM API server can be accessed with a specific Access Token.
 *
 * It retrieves the user details for the user which is authorized to access the server with
 * this token.
 *
 */
public class TestAccessTokenTask extends PleaseWaitRunnable {
    private final OAuthToken tokenOAuth1;
    private final IOAuthToken tokenOAuth2;
    private final IOAuthParameters oauthParameters;
    private boolean canceled;
    private final Component parent;
    private final String apiUrl;
    private HttpClient connection;

    /**
     * Create the task
     *
     * @param parent the parent component relative to which the  {@link PleaseWaitRunnable}-Dialog is displayed
     * @param apiUrl the API URL. Must not be null.
     * @param parameters the OAuth parameters. Must not be null.
     * @param accessToken the Access Token. Must not be null.
     */
    public TestAccessTokenTask(Component parent, String apiUrl, OAuthParameters parameters, OAuthToken accessToken) {
        super(parent, tr("Testing OAuth Access Token"), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(apiUrl, "apiUrl");
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        CheckParameterUtil.ensureParameterNotNull(accessToken, "accessToken");
        this.tokenOAuth1 = accessToken;
        this.tokenOAuth2 = null;
        this.oauthParameters = parameters;
        this.parent = parent;
        this.apiUrl = apiUrl;
    }

    /**
     * Create the task
     *
     * @param parent the parent component relative to which the  {@link PleaseWaitRunnable}-Dialog is displayed
     * @param apiUrl the API URL. Must not be null.
     * @param parameters the OAuth parameters. Must not be null.
     * @param accessToken the Access Token. Must not be null.
     * @since xxx
     */
    public TestAccessTokenTask(Component parent, String apiUrl, IOAuthParameters parameters, IOAuthToken accessToken) {
        super(parent, tr("Testing OAuth Access Token"), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(apiUrl, "apiUrl");
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        CheckParameterUtil.ensureParameterNotNull(accessToken, "accessToken");
        this.tokenOAuth1 = null;
        this.tokenOAuth2 = accessToken;
        this.oauthParameters = parameters;
        this.parent = parent;
        this.apiUrl = apiUrl;
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void finish() {
        // Do nothing
    }

    protected void sign(HttpClient con) throws OAuthException {
        if (oauthParameters instanceof OAuthParameters) {
            OAuthConsumer consumer = ((OAuthParameters) oauthParameters).buildConsumer();
            consumer.setTokenWithSecret(tokenOAuth1.getKey(), tokenOAuth1.getSecret());
            consumer.sign(con);
        } else {
            try {
                this.tokenOAuth2.sign(con);
            } catch (org.openstreetmap.josm.data.oauth.OAuthException e) {
                // Adapt our OAuthException to the SignPost OAuth exception
                throw new OAuthException(e) {};
            }
        }
    }

    protected String normalizeApiUrl(String url) {
        // remove leading and trailing white space
        url = url.trim();

        // remove trailing slashes
        while (url.endsWith("/")) {
            url = url.substring(0, url.lastIndexOf('/'));
        }
        return url;
    }

    protected UserInfo getUserDetails() throws OsmOAuthAuthorizationException, XmlParsingException, OsmTransferException {
        boolean authenticatorEnabled = true;
        try {
            URL url = new URL(normalizeApiUrl(apiUrl) + "/0.6/user/details");
            authenticatorEnabled = DefaultAuthenticator.getInstance().isEnabled();
            DefaultAuthenticator.getInstance().setEnabled(false);

            final HttpClient client = HttpClient.create(url);
            sign(client);
            synchronized (this) {
                connection = client;
                connection.connect();
            }

            final String oauthKey = getAuthKey();
            if (connection.getResponse().getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                throw new OsmApiException(HttpURLConnection.HTTP_UNAUTHORIZED,
                        tr("Retrieving user details with Access Token Key ''{0}'' was rejected.",
                                oauthKey), null);

            if (connection.getResponse().getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN)
                throw new OsmApiException(HttpURLConnection.HTTP_FORBIDDEN,
                        tr("Retrieving user details with Access Token Key ''{0}'' was forbidden.", oauthKey), null);

            if (connection.getResponse().getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new OsmApiException(connection.getResponse().getResponseCode(),
                        connection.getResponse().getHeaderField("Error"), null);
            Document d = XmlUtils.parseSafeDOM(connection.getResponse().getContent());
            return OsmServerUserInfoReader.buildFromXML(d);
        } catch (SAXException | ParserConfigurationException e) {
            throw new XmlParsingException(e);
        } catch (IOException e) {
            throw new OsmTransferException(e);
        } catch (OAuthException e) {
            throw new OsmOAuthAuthorizationException(e);
        } finally {
            DefaultAuthenticator.getInstance().setEnabled(authenticatorEnabled);
        }
    }

    protected void notifySuccess(UserInfo userInfo) {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "Successfully used the Access Token ''{0}'' to<br>"
                        + "access the OSM server at ''{1}''.<br>"
                        + "You are accessing the OSM server as user ''{2}'' with id ''{3}''."
                        + "</html>",
                        getAuthKey(),
                        apiUrl,
                        Utils.escapeReservedCharactersHTML(userInfo.getDisplayName()),
                        userInfo.getId()
                ),
                tr("Success"),
                JOptionPane.INFORMATION_MESSAGE,
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#AccessTokenOK")
        );
    }

    protected void alertFailedAuthentication() {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "Failed to access the OSM server ''{0}''<br>"
                        + "with the Access Token ''{1}''.<br>"
                        + "The server rejected the Access Token as unauthorized. You will not<br>"
                        + "be able to access any protected resource on this server using this token."
                        +"</html>",
                        apiUrl,
                        getAuthKey()
                ),
                tr("Test failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#AccessTokenFailed")
        );
    }

    protected void alertFailedAuthorisation() {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "The Access Token ''{1}'' is known to the OSM server ''{0}''.<br>"
                        + "The test to retrieve the user details for this token failed, though.<br>"
                        + "Depending on what rights are granted to this token you may nevertheless use it<br>"
                        + "to upload data, upload GPS traces, and/or access other protected resources."
                        +"</html>",
                        apiUrl,
                        getAuthKey()
                ),
                tr("Token allows restricted access"),
                JOptionPane.WARNING_MESSAGE,
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#AccessTokenFailed")
        );
    }

    protected void alertFailedConnection() {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "Failed to retrieve information about the current user"
                        + " from the OSM server ''{0}''.<br>"
                        + "This is probably not a problem caused by the tested Access Token, but<br>"
                        + "rather a problem with the server configuration. Carefully check the server<br>"
                        + "URL and your Internet connection."
                        +"</html>",
                        apiUrl,
                        getAuthKey()
                ),
                tr("Test failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#AccessTokenFailed")
        );
    }

    protected void alertFailedSigning() {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "Failed to sign the request for the OSM server ''{0}'' with the "
                        + "token ''{1}''.<br>"
                        + "The token ist probably invalid."
                        +"</html>",
                        apiUrl,
                        getAuthKey()
                ),
                tr("Test failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#AccessTokenFailed")
        );
    }

    protected void alertInternalError() {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "The test failed because the server responded with an internal error.<br>"
                        + "JOSM could not decide whether the token is valid. Please try again later."
                        + "</html>",
                        apiUrl,
                        getAuthKey()
                ),
                tr("Test failed"),
                JOptionPane.WARNING_MESSAGE,
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#AccessTokenFailed")
        );
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            getProgressMonitor().indeterminateSubTask(tr("Retrieving user info..."));
            UserInfo userInfo = getUserDetails();
            if (canceled) return;
            notifySuccess(userInfo);
        } catch (OsmOAuthAuthorizationException e) {
            if (canceled) return;
            Logging.error(e);
            alertFailedSigning();
        } catch (OsmApiException e) {
            if (canceled) return;
            Logging.error(e);
            if (e.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                alertInternalError();
                return;
            } else if (e.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                alertFailedAuthentication();
                return;
            } else if (e.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                alertFailedAuthorisation();
                return;
            }
            alertFailedConnection();
        } catch (OsmTransferException e) {
            if (canceled) return;
            Logging.error(e);
            alertFailedConnection();
        }
    }

    private String getAuthKey() {
        if (this.tokenOAuth1 != null) {
            return this.tokenOAuth1.getKey();
        }
        if (this.tokenOAuth2 instanceof OAuth20Token) {
            return ((OAuth20Token) this.tokenOAuth2).getBearerToken();
        }
        throw new IllegalArgumentException("Only OAuth1 and OAuth2 tokens are understood: " + this.tokenOAuth2);
    }
}
