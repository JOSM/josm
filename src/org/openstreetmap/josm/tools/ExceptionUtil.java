// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.io.ChangesetClosedException;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.MissingOAuthAccessTokenException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferException;

public class ExceptionUtil {
    private ExceptionUtil() {
    }

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    public static String explainOsmApiInitializationException(OsmApiInitializationException e) {
        e.printStackTrace();
        String msg = tr(
                "<html>Failed to initialize communication with the OSM server {0}.<br>"
                + "Check the server URL in your preferences and your internet connection.</html>", Main.pref.get(
                        "osm-server.url", "http://api.openstreetmap.org/api"));
        return msg;
    }


    /**
     *  Creates the error message
     *
     * @param e the exception
     */
    public static String explainMissingOAuthAccessTokenException(MissingOAuthAccessTokenException e) {
        e.printStackTrace();
        String msg = tr(
                "<html>Failed to authenticate at the OSM server ''{0}''.<br>"
                + "You are using OAuth to authenticate but currently there is no<br>"
                + "OAuth Access Token configured.<br>"
                + "Please open the Preferences Dialog and generate or enter an Access Token."
                + "</html>",
                Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api")
        );
        return msg;
    }

    /**
     * Explains a precondition exception when a child relation could not be deleted because
     * it is still referred to by an undeleted parent relation.
     *
     * @param e the exception
     * @param childRelation the child relation
     * @param parentRelation the parent relation
     * @return
     */
    public static String explainDeletedRelationStillInUse(OsmApiException e, long childRelation, long parentRelation) {
        String msg = tr(
                "<html><strong>Failed</strong> to delete <strong>relation {0}</strong>."
                + " It is still referred to by relation {1}.<br>"
                + "Please load relation {1}, remove the reference to relation {0}, and upload again.</html>",
                childRelation,parentRelation
        );
        return msg;
    }

    /**
     * Explains an upload error due to a violated precondition, i.e. a HTTP return code 412
     *
     * @param e the exception
     */
    public static String explainPreconditionFailed(OsmApiException e) {
        e.printStackTrace();
        String msg = e.getErrorHeader();
        if (msg != null) {
            String pattern = "Precondition failed: The relation (\\d+) is used in relation (\\d+)\\.";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(msg);
            if (m.matches()) {
                long childRelation = Long.parseLong(m.group(1));
                long parentRelation = Long.parseLong(m.group(2));
                return explainDeletedRelationStillInUse(e, childRelation, parentRelation);
            }
        }
        msg = tr(
                "<html>Uploading to the server <strong>failed</strong> because your current<br>"
                + "dataset violates a precondition.<br>" + "The error message is:<br>" + "{0}" + "</html>", e
                .getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
        return msg;
    }

    public static String explainFailedBasicAuthentication(OsmApiException e) {
        e.printStackTrace();
        return tr("<html>"
                + "Authentication at the OSM server with the username ''{0}'' failed.<br>"
                + "Please check the username and the password in the JOSM preferences."
                + "</html>",
                Main.pref.get("osm-server.username")
        );
    }

    public static String explainFailedOAuthAuthentication(OsmApiException e) {
        e.printStackTrace();
        return tr("<html>"
                + "Authentication at the OSM server with the OAuth token ''{0}'' failed.<br>"
                + "Please launch the preferences dialog and retrieve another OAuth token."
                + "</html>",
                OAuthAccessTokenHolder.getInstance().getAccessTokenKey()
        );
    }

    public static String explainFailedOAuthAuthorisation(OsmApiException e) {
        e.printStackTrace();
        return tr("<html>"
                + "Authorisation at the OSM server with the OAuth token ''{0}'' failed.<br>"
                + "The token is not authorised to access the protected resource<br>"
                + "''{1}''.<br>"
                + "Please launch the preferences dialog and retrieve another OAuth token."
                + "</html>",
                OAuthAccessTokenHolder.getInstance().getAccessTokenKey(),
                e.getAccessedUrl() == null ? tr("unknown") : e.getAccessedUrl()
        );
    }

    /**
     * Explains an OSM API exception because of a client timeout (HTTP 408).
     *
     * @param e the exception
     * @return the message
     */
    public static String explainClientTimeout(OsmApiException e) {
        e.printStackTrace();
        return tr("<html>"
                + "Communication with the OSM server ''{0}'' timed out. Please retry later."
                + "</html>",
                OsmApi.getOsmApi().getBaseUrl()
        );
    }

    /**
     * Replies a generic error message for an OSM API exception
     *
     * @param e the exception
     * @return the message
     */
    public static String explainGenericOsmApiException(OsmApiException e) {
        e.printStackTrace();
        String errMsg = e.getErrorHeader();
        if (errMsg == null) {
            errMsg = e.getErrorBody();
        }
        if (errMsg == null) {
            errMsg = tr("no error message available");
        }
        return tr("<html>"
                + "Communication with the OSM server ''{0}''failed. The server replied<br>"
                + "the following error code and the following error message:<br>"
                + "<strong>Error code:<strong> {1}<br>"
                + "<strong>Error message (untranslated)</strong>: {2}"
                + "</html>",
                OsmApi.getOsmApi().getBaseUrl(),
                e.getResponseCode(),
                errMsg
        );
    }

    /**
     * Explains an error due to a 409 conflict
     *
     * @param e the exception
     */
    public static String explainConflict(OsmApiException e) {
        e.printStackTrace();
        String msg = e.getErrorHeader();
        if (msg != null) {
            String pattern = "The changeset (\\d+) was closed at (.*)";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(msg);
            if (m.matches()) {
                long changesetId = Long.parseLong(m.group(1));
                // Example: "2010-09-07 14:39:41 UTC". Always parsed with US locale, regardless
                // of the current locale in JOSM
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
                Date closeDate = null;
                try {
                    closeDate = formatter.parse(m.group(2));
                } catch(ParseException ex) {
                    System.err.println(tr("Failed to parse date ''{0}'' replied by server.", m.group(2)));
                    ex.printStackTrace();
                }
                if (closeDate == null) {
                    msg = tr(
                            "<html>Closing of changeset <strong>{0}</strong> failed <br>because it has already been closed.</html>",
                            changesetId
                    );
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat();
                    msg = tr(
                            "<html>Closing of changeset <strong>{0}</strong> failed<br>"
                            +" because it has already been closed on {1}.</html>",
                            changesetId,
                            dateFormat.format(closeDate)
                    );
                }
                return msg;
            }
            msg = tr(
                    "<html>The server reported that it has detected a conflict.<br>" +
                    "Error message (untranslated):<br>{0}</html>",
                    msg
            );
        }
        msg = tr(
                "<html>The server reported that it has detected a conflict.</html>"
        );
        return msg;
    }

    /**
     * Explains an exception thrown during upload because the changeset which data is
     * uploaded to is already closed.
     *
     * @param e the exception
     */
    public static String explainChangesetClosedException(ChangesetClosedException e) {
        String msg;
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        msg = tr(
                "<html>Failed to upload to changeset <strong>{0}</strong><br>"
                +"because it has already been closed on {1}.</html>",
                e.getChangesetId(),
                e.getClosedOn() == null ? "?" : dateFormat.format(e.getClosedOn())
        );
        e.printStackTrace();
        return msg;
    }

    /**
     * Explains an exception with a generic message dialog
     *
     * @param e the exception
     */
    public static String explainGeneric(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().equals("")) {
            msg = e.toString();
        }
        e.printStackTrace();
        return msg;
    }

    /**
     * Explains a {@see SecurityException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from within an
     * applet which wasn't loaded from the API server.
     *
     * @param e the exception
     */

    public static String explainSecurityException(OsmTransferException e) {
        String apiUrl = e.getUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch (MalformedURLException ex) {
            // shouldn't happen
        }

        String message = tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''<br>"
                + "for security reasons. This is most likely because you are running<br>"
                + "in an applet and because you did not load your applet from ''{1}''.</html>", apiUrl, host);
        return message;
    }

    /**
     * Explains a {@see SocketException} which has caused an {@see OsmTransferException}.
     * This is most likely because there's not connection to the Internet or because
     * the remote server is not reachable.
     *
     * @param e the exception
     */

    public static String explainNestedSocketException(OsmTransferException e) {
        String apiUrl = e.getUrl();
        String message = tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''.<br>"
                + "Please check your internet connection.</html>", apiUrl);
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see IOException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when the communication with the remote server is
     * interrupted for any reason.
     *
     * @param e the exception
     */

    public static String explainNestedIOException(OsmTransferException e) {
        IOException ioe = getNestedException(e, IOException.class);
        String apiUrl = e.getUrl();
        String message = tr("<html>Failed to upload data to or download data from<br>" + "''{0}''<br>"
                + "due to a problem with transferring data.<br>" + "Details(untranslated): {1}</html>", apiUrl, ioe
                .getMessage());
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see IllegalDataException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when JOSM tries to load data in in an unsupported format.
     *
     * @param e the exception
     */
    public static String explainNestedIllegalDataException(OsmTransferException e) {
        IllegalDataException ide = getNestedException(e, IllegalDataException.class);
        String message = tr("<html>Failed to download data. "
                + "Its format is either unsupported, ill-formed, and/or inconsistent.<br>"
                + "<br>Details (untranslated): {0}</html>", ide.getMessage());
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because of an internal server
     * error in the OSM API server..
     *
     * @param e the exception
     */

    public static String explainInternalServerError(OsmTransferException e) {
        String apiUrl = e.getUrl();
        String message = tr("<html>The OSM server<br>" + "''{0}''<br>" + "reported an internal server error.<br>"
                + "This is most likely a temporary problem. Please try again later.</html>", apiUrl);
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because of a bad
     * request
     *
     * @param e the exception
     */
    public static String explainBadRequest(OsmApiException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String message = tr("The OSM server ''{0}'' reported a bad request.<br>", apiUrl);
        if (e.getErrorHeader() != null &&
                (e.getErrorHeader().startsWith("The maximum bbox") ||
                        e.getErrorHeader().startsWith("You requested too many nodes"))) {
            message += "<br>"
                + tr("The area you tried to download is too big or your request was too large."
                        + "<br>Either request a smaller area or use an export file provided by the OSM community.");
        } else if (e.getErrorHeader() != null) {
            message += tr("<br>Error message(untranslated): {0}", e.getErrorHeader());
        }
        message = "<html>" + message + "</html>";
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because a resource wasn't found.
     *
     * @param e the exception
     */
    public static String explainNotFound(OsmApiException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String message = tr("The OSM server ''{0}'' does not know about an object<br>"
                + "you tried to read, update, or delete. Either the respective object<br>"
                + "does not exist on the server or you are using an invalid URL to access<br>"
                + "it. Please carefully check the server''s address ''{0}'' for typos."
                , apiUrl);
        message = "<html>" + message + "</html>";
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see UnknownHostException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when there is an error in the API URL or when
     * local DNS services are not working.
     *
     * @param e the exception
     */

    public static String explainNestedUnknownHostException(OsmTransferException e) {
        String apiUrl = e.getUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch (MalformedURLException ex) {
            // shouldn't happen
        }

        String message = tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''.<br>"
                + "Host name ''{1}'' could not be resolved. <br>"
                + "Please check the API URL in your preferences and your internet connection.</html>", apiUrl, host);
        e.printStackTrace();
        return message;
    }

    /**
     * Replies the first nested exception of type <code>nestedClass</code> (including
     * the root exception <code>e</code>) or null, if no such exception is found.
     *
     * @param <T>
     * @param e the root exception
     * @param nestedClass the type of the nested exception
     * @return the first nested exception of type <code>nestedClass</code> (including
     * the root exception <code>e</code>) or null, if no such exception is found.
     */
    protected static <T> T getNestedException(Exception e, Class<T> nestedClass) {
        Throwable t = e;
        while (t != null && !(nestedClass.isInstance(t))) {
            t = t.getCause();
        }
        if (t == null)
            return null;
        else if (nestedClass.isInstance(t))
            return nestedClass.cast(t);
        return null;
    }

    /**
     * Explains an {@see OsmTransferException} to the user.
     *
     * @param e the {@see OsmTransferException}
     */
    public static String explainOsmTransferException(OsmTransferException e) {
        if (getNestedException(e, SecurityException.class) != null)
            return explainSecurityException(e);
        if (getNestedException(e, SocketException.class) != null)
            return explainNestedSocketException(e);
        if (getNestedException(e, UnknownHostException.class) != null)
            return explainNestedUnknownHostException(e);
        if (getNestedException(e, IOException.class) != null)
            return explainNestedIOException(e);
        if (e instanceof OsmApiInitializationException)
            return explainOsmApiInitializationException((OsmApiInitializationException) e);

        if (e instanceof ChangesetClosedException)
            return explainChangesetClosedException((ChangesetClosedException)e);

        if (e instanceof OsmApiException) {
            OsmApiException oae = (OsmApiException) e;
            if (oae.getResponseCode() == HttpURLConnection.HTTP_PRECON_FAILED)
                return explainPreconditionFailed(oae);
            if (oae.getResponseCode() == HttpURLConnection.HTTP_GONE)
                return explainGoneForUnknownPrimitive(oae);
            if (oae.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)
                return explainInternalServerError(oae);
            if (oae.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST)
                return explainBadRequest(oae);
        }
        return explainGeneric(e);
    }

    /**
     * explains the case of an error due to a delete request on an already deleted
     * {@see OsmPrimitive}, i.e. a HTTP response code 410, where we don't know which
     * {@see OsmPrimitive} is causing the error.
     *
     * @param e the exception
     */
    public static String explainGoneForUnknownPrimitive(OsmApiException e) {
        String msg = tr(
                "<html>The server reports that an object is deleted.<br>"
                + "<strong>Uploading failed</strong> if you tried to update or delete this object.<br> "
                + "<strong>Downloading failed</strong> if you tried to download this object.<br>"
                + "<br>"
                + "The error message is:<br>" + "{0}"
                + "</html>", e.getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
        return msg;

    }

    /**
     * Explains an {@see Exception} to the user.
     *
     * @param e the {@see Exception}
     */
    public static String explainException(Exception e) {
        String msg = "";
        if (e instanceof OsmTransferException) {
            msg = explainOsmTransferException((OsmTransferException) e);
        } else {
            msg = explainGeneric(e);
        }
        e.printStackTrace();
        return msg;
    }
}
