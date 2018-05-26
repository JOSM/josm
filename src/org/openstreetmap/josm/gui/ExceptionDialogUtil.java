// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.ChangesetClosedException;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.MissingOAuthAccessTokenException;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * This utility class provides static methods which explain various exceptions to the user.
 *
 */
public final class ExceptionDialogUtil {

    /**
     * just static utility functions. no constructor
     */
    private ExceptionDialogUtil() {
        // Hide default constructor for utility classes
    }

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    public static void explainOsmApiInitializationException(OsmApiInitializationException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainOsmApiInitializationException(e),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#OsmApiInitializationException")
        );
    }

    /**
     * handles a ChangesetClosedException
     *
     * @param e the exception
     */
    public static void explainChangesetClosedException(ChangesetClosedException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainChangesetClosedException(e),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE,
                ht("/Action/Upload#ChangesetClosed")
        );
    }

    /**
     * Explains an upload error due to a violated precondition, i.e. a HTTP return code 412
     *
     * @param e the exception
     */
    public static void explainPreconditionFailed(OsmApiException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainPreconditionFailed(e),
                tr("Precondition violation"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#OsmApiException")
        );
    }

    /**
     * Explains an exception with a generic message dialog
     *
     * @param e the exception
     */
    public static void explainGeneric(Exception e) {
        Logging.error(e);
        BugReportExceptionHandler.handleException(e);
    }

    /**
     * Explains a {@link SecurityException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from within an
     * applet which wasn't loaded from the API server.
     *
     * @param e the exception
     */

    public static void explainSecurityException(OsmTransferException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainSecurityException(e),
                tr("Security exception"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#SecurityException")
        );
    }

    /**
     * Explains a {@link SocketException} which has caused an {@link OsmTransferException}.
     * This is most likely because there's not connection to the Internet or because
     * the remote server is not reachable.
     *
     * @param e the exception
     */

    public static void explainNestedSocketException(OsmTransferException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainNestedSocketException(e),
                tr("Network exception"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#NestedSocketException")
        );
    }

    /**
     * Explains a {@link IOException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when the communication with the remote server is
     * interrupted for any reason.
     *
     * @param e the exception
     */

    public static void explainNestedIOException(OsmTransferException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainNestedIOException(e),
                tr("IO Exception"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#NestedIOException")
        );
    }

    /**
     * Explains a {@link IllegalDataException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when JOSM tries to load data in an unsupported format.
     *
     * @param e the exception
     */
    public static void explainNestedIllegalDataException(OsmTransferException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainNestedIllegalDataException(e),
                tr("Illegal Data"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#IllegalDataException")
        );
    }

    /**
     * Explains a {@link OfflineAccessException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when JOSM tries to access OSM API or JOSM website while in offline mode.
     *
     * @param e the exception
     * @since 7434
     */
    public static void explainNestedOfflineAccessException(OsmTransferException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainOfflineAccessException(e),
                tr("Offline mode"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#OfflineAccessException")
        );
    }

    /**
     * Explains a {@link InvocationTargetException }
     *
     * @param e the exception
     */
    public static void explainNestedInvocationTargetException(Exception e) {
        InvocationTargetException ex = ExceptionUtil.getNestedException(e, InvocationTargetException.class);
        if (ex != null) {
            // Users should be able to submit a bug report for an invocation target exception
            BugReportExceptionHandler.handleException(ex);
        }
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of an internal server
     * error in the OSM API server.
     *
     * @param e the exception
     */

    public static void explainInternalServerError(OsmTransferException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainInternalServerError(e),
                tr("Internal Server Error"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#InternalServerError")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of a bad
     * request
     *
     * @param e the exception
     */
    public static void explainBadRequest(OsmApiException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainBadRequest(e),
                tr("Bad Request"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#BadRequest")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because a resource wasn't found
     * on the server
     *
     * @param e the exception
     */
    public static void explainNotFound(OsmApiException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainNotFound(e),
                tr("Not Found"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#NotFound")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of a conflict
     *
     * @param e the exception
     */
    public static void explainConflict(OsmApiException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainConflict(e),
                tr("Conflict"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#Conflict")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because the authentication at
     * the OSM server failed
     *
     * @param e the exception
     */
    public static void explainAuthenticationFailed(OsmApiException e) {
        String msg;
        if (OsmApi.isUsingOAuth()) {
            msg = ExceptionUtil.explainFailedOAuthAuthentication(e);
        } else {
            msg = ExceptionUtil.explainFailedBasicAuthentication(e);
        }

        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Authentication failed"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#AuthenticationFailed")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because accessing a protected
     * resource was forbidden (HTTP 403).
     *
     * @param e the exception
     */
    public static void explainAuthorizationFailed(OsmApiException e) {

        Matcher m;
        String msg;
        String url = e.getAccessedUrl();
        Pattern p = Pattern.compile("https?://.*/api/0.6/(node|way|relation)/(\\d+)/(\\d+)");

        // Special case for individual access to redacted versions
        // See http://wiki.openstreetmap.org/wiki/Open_Database_License/Changes_in_the_API
        if (url != null && (m = p.matcher(url)).matches()) {
            String type = m.group(1);
            String id = m.group(2);
            String version = m.group(3);
            // {1} is the translation of "node", "way" or "relation"
            msg = tr("Access to redacted version ''{0}'' of {1} {2} is forbidden.",
                    version, tr(type), id);
        } else if (OsmApi.isUsingOAuth()) {
            msg = ExceptionUtil.explainFailedOAuthAuthorisation(e);
        } else {
            msg = ExceptionUtil.explainFailedAuthorisation(e);
        }

        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Authorisation Failed"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#AuthorizationFailed")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of a
     * client timeout (HTTP 408)
     *
     * @param e the exception
     */
    public static void explainClientTimeout(OsmApiException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainClientTimeout(e),
                tr("Client Time Out"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#ClientTimeOut")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of a
     * bandwidth limit (HTTP 509)
     *
     * @param e the exception
     */
    public static void explainBandwidthLimitExceeded(OsmApiException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainBandwidthLimitExceeded(e),
                tr("Bandwidth Limit Exceeded"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#BandwidthLimit")
        );
    }

    /**
     * Explains a {@link OsmApiException} with a generic error message.
     *
     * @param e the exception
     */
    public static void explainGenericHttpException(OsmApiException e) {
        String body = e.getErrorBody();
        Object msg = null;
        if ("text/html".equals(e.getContentType()) && body != null && body.startsWith("<") && body.contains("<html>")) {
            msg = new HtmlPanel(body);
        } else {
            msg = ExceptionUtil.explainGeneric(e);
        }
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Communication with OSM server failed"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#GenericCommunicationError")
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because accessing a protected
     * resource was forbidden.
     *
     * @param e the exception
     */
    public static void explainMissingOAuthAccessTokenException(MissingOAuthAccessTokenException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainMissingOAuthAccessTokenException(e),
                tr("Authentication failed"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#MissingOAuthAccessToken")
        );
    }

    /**
     * Explains a {@link UnknownHostException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when there is an error in the API URL or when
     * local DNS services are not working.
     *
     * @param e the exception
     */
    public static void explainNestedUnkonwnHostException(OsmTransferException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainNestedUnknownHostException(e),
                tr("Unknown host"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#UnknownHost")
        );
    }

    /**
     * Explains an {@link OsmTransferException} to the user.
     *
     * @param e the {@link OsmTransferException}
     */
    public static void explainOsmTransferException(OsmTransferException e) {
        if (ExceptionUtil.getNestedException(e, SecurityException.class) != null) {
            explainSecurityException(e);
            return;
        }
        if (ExceptionUtil.getNestedException(e, SocketException.class) != null) {
            explainNestedSocketException(e);
            return;
        }
        if (ExceptionUtil.getNestedException(e, UnknownHostException.class) != null) {
            explainNestedUnkonwnHostException(e);
            return;
        }
        if (ExceptionUtil.getNestedException(e, IOException.class) != null) {
            explainNestedIOException(e);
            return;
        }
        if (ExceptionUtil.getNestedException(e, IllegalDataException.class) != null) {
            explainNestedIllegalDataException(e);
            return;
        }
        if (ExceptionUtil.getNestedException(e, OfflineAccessException.class) != null) {
            explainNestedOfflineAccessException(e);
            return;
        }
        if (e instanceof OsmApiInitializationException) {
            explainOsmApiInitializationException((OsmApiInitializationException) e);
            return;
        }

        if (e instanceof ChangesetClosedException) {
            explainChangesetClosedException((ChangesetClosedException) e);
            return;
        }

        if (e instanceof MissingOAuthAccessTokenException) {
            explainMissingOAuthAccessTokenException((MissingOAuthAccessTokenException) e);
            return;
        }

        if (e instanceof OsmApiException) {
            OsmApiException oae = (OsmApiException) e;
            switch(oae.getResponseCode()) {
            case HttpURLConnection.HTTP_PRECON_FAILED:
                explainPreconditionFailed(oae);
                return;
            case HttpURLConnection.HTTP_GONE:
                explainGoneForUnknownPrimitive(oae);
                return;
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                explainInternalServerError(oae);
                return;
            case HttpURLConnection.HTTP_BAD_REQUEST:
                explainBadRequest(oae);
                return;
            case HttpURLConnection.HTTP_NOT_FOUND:
                explainNotFound(oae);
                return;
            case HttpURLConnection.HTTP_CONFLICT:
                explainConflict(oae);
                return;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                explainAuthenticationFailed(oae);
                return;
            case HttpURLConnection.HTTP_FORBIDDEN:
                explainAuthorizationFailed(oae);
                return;
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                explainClientTimeout(oae);
                return;
            case 509: case 429:
                explainBandwidthLimitExceeded(oae);
                return;
            default:
                explainGenericHttpException(oae);
                return;
            }
        }
        explainGeneric(e);
    }

    /**
     * explains the case of an error due to a delete request on an already deleted
     * {@link OsmPrimitive}, i.e. a HTTP response code 410, where we don't know which
     * {@link OsmPrimitive} is causing the error.
     *
     * @param e the exception
     */
    public static void explainGoneForUnknownPrimitive(OsmApiException e) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                ExceptionUtil.explainGoneForUnknownPrimitive(e),
                tr("Object deleted"),
                JOptionPane.ERROR_MESSAGE,
                ht("/ErrorMessages#GoneForUnknownPrimitive")
        );
    }

    /**
     * Explains an {@link Exception} to the user.
     *
     * @param e the {@link Exception}
     */
    public static void explainException(Exception e) {
        if (ExceptionUtil.getNestedException(e, InvocationTargetException.class) != null) {
            explainNestedInvocationTargetException(e);
            return;
        }
        if (e instanceof OsmTransferException) {
            explainOsmTransferException((OsmTransferException) e);
            return;
        }
        explainGeneric(e);
    }
}
