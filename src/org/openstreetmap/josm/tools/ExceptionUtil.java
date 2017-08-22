// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.io.ChangesetClosedException;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.MissingOAuthAccessTokenException;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Utilities for exception handling.
 * @since 2097
 */
public final class ExceptionUtil {

    private ExceptionUtil() {
        // Hide default constructor for utils classes
    }

    /**
     * Explains an exception caught during OSM API initialization.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainOsmApiInitializationException(OsmApiInitializationException e) {
        Logging.error(e);
        return tr(
                "<html>Failed to initialize communication with the OSM server {0}.<br>"
                + "Check the server URL in your preferences and your internet connection.",
                OsmApi.getOsmApi().getServerUrl())+"</html>";
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because accessing a protected
     * resource was forbidden.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainMissingOAuthAccessTokenException(MissingOAuthAccessTokenException e) {
        Logging.error(e);
        return tr(
                "<html>Failed to authenticate at the OSM server ''{0}''.<br>"
                + "You are using OAuth to authenticate but currently there is no<br>"
                + "OAuth Access Token configured.<br>"
                + "Please open the Preferences Dialog and generate or enter an Access Token."
                + "</html>",
                OsmApi.getOsmApi().getServerUrl()
        );
    }

    /**
     * Parses a precondition failure response from the server and attempts to get more information about it
     * @param msg The message from the server
     * @return The OSM primitive that caused the problem and a collection of primitives that e.g. refer to it
     */
    public static Pair<OsmPrimitive, Collection<OsmPrimitive>> parsePreconditionFailed(String msg) {
        if (msg == null)
            return null;
        final String ids = "(\\d+(?:,\\d+)*)";
        final Collection<OsmPrimitive> refs = new TreeSet<>(); // error message can contain several times the same way
        Matcher m;
        m = Pattern.compile(".*Node (\\d+) is still used by relations? " + ids + ".*").matcher(msg);
        if (m.matches()) {
            OsmPrimitive n = new Node(Long.parseLong(m.group(1)));
            for (String s : m.group(2).split(",")) {
                refs.add(new Relation(Long.parseLong(s)));
            }
            return Pair.create(n, refs);
        }
        m = Pattern.compile(".*Node (\\d+) is still used by ways? " + ids + ".*").matcher(msg);
        if (m.matches()) {
            OsmPrimitive n = new Node(Long.parseLong(m.group(1)));
            for (String s : m.group(2).split(",")) {
                refs.add(new Way(Long.parseLong(s)));
            }
            return Pair.create(n, refs);
        }
        m = Pattern.compile(".*The relation (\\d+) is used in relations? " + ids + ".*").matcher(msg);
        if (m.matches()) {
            OsmPrimitive n = new Relation(Long.parseLong(m.group(1)));
            for (String s : m.group(2).split(",")) {
                refs.add(new Relation(Long.parseLong(s)));
            }
            return Pair.create(n, refs);
        }
        m = Pattern.compile(".*Way (\\d+) is still used by relations? " + ids + ".*").matcher(msg);
        if (m.matches()) {
            OsmPrimitive n = new Way(Long.parseLong(m.group(1)));
            for (String s : m.group(2).split(",")) {
                refs.add(new Relation(Long.parseLong(s)));
            }
            return Pair.create(n, refs);
        }
        m = Pattern.compile(".*Way (\\d+) requires the nodes with id in " + ids + ".*").matcher(msg);
        // ... ", which either do not exist, or are not visible"
        if (m.matches()) {
            OsmPrimitive n = new Way(Long.parseLong(m.group(1)));
            for (String s : m.group(2).split(",")) {
                refs.add(new Node(Long.parseLong(s)));
            }
            return Pair.create(n, refs);
        }
        return null;
    }

    /**
     * Explains an upload error due to a violated precondition, i.e. a HTTP return code 412
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainPreconditionFailed(OsmApiException e) {
        Logging.error(e);
        Pair<OsmPrimitive, Collection<OsmPrimitive>> conflict = parsePreconditionFailed(e.getErrorHeader());
        if (conflict != null) {
            OsmPrimitive firstRefs = conflict.b.iterator().next();
            String objId = Long.toString(conflict.a.getId());
            Collection<Long> refIds = Utils.transform(conflict.b, OsmPrimitive::getId);
            String refIdsString = refIds.size() == 1 ? refIds.iterator().next().toString() : refIds.toString();
            if (conflict.a instanceof Node) {
                if (firstRefs instanceof Node) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>node {0}</strong>."
                            + " It is still referred to by node {1}.<br>"
                            + "Please load the node, remove the reference to the node, and upload again.",
                            "<strong>Failed</strong> to delete <strong>node {0}</strong>."
                            + " It is still referred to by nodes {1}.<br>"
                            + "Please load the nodes, remove the reference to the node, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else if (firstRefs instanceof Way) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>node {0}</strong>."
                            + " It is still referred to by way {1}.<br>"
                            + "Please load the way, remove the reference to the node, and upload again.",
                            "<strong>Failed</strong> to delete <strong>node {0}</strong>."
                            + " It is still referred to by ways {1}.<br>"
                            + "Please load the ways, remove the reference to the node, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else if (firstRefs instanceof Relation) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>node {0}</strong>."
                            + " It is still referred to by relation {1}.<br>"
                            + "Please load the relation, remove the reference to the node, and upload again.",
                            "<strong>Failed</strong> to delete <strong>node {0}</strong>."
                            + " It is still referred to by relations {1}.<br>"
                            + "Please load the relations, remove the reference to the node, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else {
                    throw new IllegalStateException();
                }
            } else if (conflict.a instanceof Way) {
                if (firstRefs instanceof Node) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>way {0}</strong>."
                            + " It is still referred to by node {1}.<br>"
                            + "Please load the node, remove the reference to the way, and upload again.",
                            "<strong>Failed</strong> to delete <strong>way {0}</strong>."
                            + " It is still referred to by nodes {1}.<br>"
                            + "Please load the nodes, remove the reference to the way, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else if (firstRefs instanceof Way) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>way {0}</strong>."
                            + " It is still referred to by way {1}.<br>"
                            + "Please load the way, remove the reference to the way, and upload again.",
                            "<strong>Failed</strong> to delete <strong>way {0}</strong>."
                            + " It is still referred to by ways {1}.<br>"
                            + "Please load the ways, remove the reference to the way, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else if (firstRefs instanceof Relation) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>way {0}</strong>."
                            + " It is still referred to by relation {1}.<br>"
                            + "Please load the relation, remove the reference to the way, and upload again.",
                            "<strong>Failed</strong> to delete <strong>way {0}</strong>."
                            + " It is still referred to by relations {1}.<br>"
                            + "Please load the relations, remove the reference to the way, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else {
                    throw new IllegalStateException();
                }
            } else if (conflict.a instanceof Relation) {
                if (firstRefs instanceof Node) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>relation {0}</strong>."
                            + " It is still referred to by node {1}.<br>"
                            + "Please load the node, remove the reference to the relation, and upload again.",
                            "<strong>Failed</strong> to delete <strong>relation {0}</strong>."
                            + " It is still referred to by nodes {1}.<br>"
                            + "Please load the nodes, remove the reference to the relation, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else if (firstRefs instanceof Way) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>relation {0}</strong>."
                            + " It is still referred to by way {1}.<br>"
                            + "Please load the way, remove the reference to the relation, and upload again.",
                            "<strong>Failed</strong> to delete <strong>relation {0}</strong>."
                            + " It is still referred to by ways {1}.<br>"
                            + "Please load the ways, remove the reference to the relation, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else if (firstRefs instanceof Relation) {
                    return "<html>" + trn(
                            "<strong>Failed</strong> to delete <strong>relation {0}</strong>."
                            + " It is still referred to by relation {1}.<br>"
                            + "Please load the relation, remove the reference to the relation, and upload again.",
                            "<strong>Failed</strong> to delete <strong>relation {0}</strong>."
                            + " It is still referred to by relations {1}.<br>"
                            + "Please load the relations, remove the reference to the relation, and upload again.",
                            conflict.b.size(), objId, refIdsString) + "</html>";
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
        } else {
            return tr(
                    "<html>Uploading to the server <strong>failed</strong> because your current<br>"
                    + "dataset violates a precondition.<br>" + "The error message is:<br>" + "{0}" + "</html>",
                    Utils.escapeReservedCharactersHTML(e.getMessage()));
        }
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because the authentication at
     * the OSM server failed, with basic authentication.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainFailedBasicAuthentication(OsmApiException e) {
        Logging.error(e);
        return tr("<html>"
                + "Authentication at the OSM server with the username ''{0}'' failed.<br>"
                + "Please check the username and the password in the JOSM preferences."
                + "</html>",
                CredentialsManager.getInstance().getUsername()
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because the authentication at
     * the OSM server failed, with OAuth authentication.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainFailedOAuthAuthentication(OsmApiException e) {
        Logging.error(e);
        return tr("<html>"
                + "Authentication at the OSM server with the OAuth token ''{0}'' failed.<br>"
                + "Please launch the preferences dialog and retrieve another OAuth token."
                + "</html>",
                OAuthAccessTokenHolder.getInstance().getAccessTokenKey()
        );
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because accessing a protected
     * resource was forbidden (HTTP 403), without OAuth authentication.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainFailedAuthorisation(OsmApiException e) {
        Logging.error(e);
        String header = e.getErrorHeader();
        String body = e.getErrorBody();
        String msg;
        if (header != null) {
            if (body != null && !header.equals(body)) {
                msg = header + " (" + body + ')';
            } else {
                msg = header;
            }
        } else {
            msg = body;
        }

        if (msg != null && !msg.isEmpty()) {
            return tr("<html>"
                    + "Authorisation at the OSM server failed.<br>"
                    + "The server reported the following error:<br>"
                    + "''{0}''"
                    + "</html>",
                    msg
            );
        } else {
            return tr("<html>"
                    + "Authorisation at the OSM server failed.<br>"
                    + "</html>"
            );
        }
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because accessing a protected
     * resource was forbidden (HTTP 403), with OAuth authentication.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainFailedOAuthAuthorisation(OsmApiException e) {
        Logging.error(e);
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
     * @return The HTML formatted error message to display
     */
    public static String explainClientTimeout(OsmApiException e) {
        Logging.error(e);
        return tr("<html>"
                + "Communication with the OSM server ''{0}'' timed out. Please retry later."
                + "</html>",
                getUrlFromException(e)
        );
    }

    /**
     * Replies a generic error message for an OSM API exception
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainGenericOsmApiException(OsmApiException e) {
        Logging.error(e);
        return tr("<html>"
                + "Communication with the OSM server ''{0}''failed. The server replied<br>"
                + "the following error code and the following error message:<br>"
                + "<strong>Error code:<strong> {1}<br>"
                + "<strong>Error message (untranslated)</strong>: {2}"
                + "</html>",
                getUrlFromException(e),
                e.getResponseCode(),
                Optional.ofNullable(Optional.ofNullable(e.getErrorHeader()).orElseGet(e::getErrorBody))
                    .orElse(tr("no error message available"))
        );
    }

    /**
     * Explains an error due to a 409 conflict
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainConflict(OsmApiException e) {
        Logging.error(e);
        String msg = e.getErrorHeader();
        if (msg != null) {
            Matcher m = Pattern.compile("The changeset (\\d+) was closed at (.*)").matcher(msg);
            if (m.matches()) {
                long changesetId = Long.parseLong(m.group(1));
                Date closeDate = null;
                try {
                    closeDate = DateUtils.newOsmApiDateTimeFormat().parse(m.group(2));
                } catch (ParseException ex) {
                    Logging.error(tr("Failed to parse date ''{0}'' replied by server.", m.group(2)));
                    Logging.error(ex);
                }
                if (closeDate == null) {
                    msg = tr(
                            "<html>Closing of changeset <strong>{0}</strong> failed <br>because it has already been closed.",
                            changesetId
                    );
                } else {
                    msg = tr(
                            "<html>Closing of changeset <strong>{0}</strong> failed<br>"
                            +" because it has already been closed on {1}.",
                            changesetId,
                            DateUtils.formatDateTime(closeDate, DateFormat.DEFAULT, DateFormat.DEFAULT)
                    );
                }
                return msg;
            }
            msg = tr(
                    "<html>The server reported that it has detected a conflict.<br>" +
                    "Error message (untranslated):<br>{0}</html>",
                    msg
            );
        } else {
            msg = tr(
                    "<html>The server reported that it has detected a conflict.");
        }
        return msg.endsWith("</html>") ? msg : (msg + "</html>");
    }

    /**
     * Explains an exception thrown during upload because the changeset which data is
     * uploaded to is already closed.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainChangesetClosedException(ChangesetClosedException e) {
        Logging.error(e);
        return tr(
                "<html>Failed to upload to changeset <strong>{0}</strong><br>"
                +"because it has already been closed on {1}.",
                e.getChangesetId(),
                e.getClosedOn() == null ? "?" : DateUtils.formatDateTime(e.getClosedOn(), DateFormat.DEFAULT, DateFormat.DEFAULT)
        );
    }

    /**
     * Explains an exception with a generic message dialog
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainGeneric(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            msg = e.toString();
        }
        Logging.error(e);
        return Utils.escapeReservedCharactersHTML(msg);
    }

    /**
     * Explains a {@link SecurityException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from within an
     * applet which wasn't loaded from the API server.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainSecurityException(OsmTransferException e) {
        String apiUrl = e.getUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch (MalformedURLException ex) {
            // shouldn't happen
            Logging.trace(ex);
        }

        return tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''<br>"
                + "for security reasons. This is most likely because you are running<br>"
                + "in an applet and because you did not load your applet from ''{1}''.", apiUrl, host)+"</html>";
    }

    /**
     * Explains a {@link SocketException} which has caused an {@link OsmTransferException}.
     * This is most likely because there's not connection to the Internet or because
     * the remote server is not reachable.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainNestedSocketException(OsmTransferException e) {
        Logging.error(e);
        return tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''.<br>"
                + "Please check your internet connection.", e.getUrl())+"</html>";
    }

    /**
     * Explains a {@link IOException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when the communication with the remote server is
     * interrupted for any reason.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainNestedIOException(OsmTransferException e) {
        IOException ioe = getNestedException(e, IOException.class);
        Logging.error(e);
        return tr("<html>Failed to upload data to or download data from<br>" + "''{0}''<br>"
                + "due to a problem with transferring data.<br>"
                + "Details (untranslated): {1}</html>",
                e != null ? e.getUrl() : "null",
                ioe != null ? ioe.getMessage() : "null");
    }

    /**
     * Explains a {@link IllegalDataException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when JOSM tries to load data in an unsupported format.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainNestedIllegalDataException(OsmTransferException e) {
        IllegalDataException ide = getNestedException(e, IllegalDataException.class);
        Logging.error(e);
        return tr("<html>Failed to download data. "
                + "Its format is either unsupported, ill-formed, and/or inconsistent.<br>"
                + "<br>Details (untranslated): {0}</html>", ide != null ? ide.getMessage() : "null");
    }

    /**
     * Explains a {@link OfflineAccessException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when JOSM tries to access OSM API or JOSM website while in offline mode.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     * @since 7434
     */
    public static String explainOfflineAccessException(OsmTransferException e) {
        OfflineAccessException oae = getNestedException(e, OfflineAccessException.class);
        Logging.error(e);
        return tr("<html>Failed to download data.<br>"
                + "<br>Details: {0}</html>", oae != null ? oae.getMessage() : "null");
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of an internal server
     * error in the OSM API server.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainInternalServerError(OsmTransferException e) {
        Logging.error(e);
        return tr("<html>The OSM server<br>" + "''{0}''<br>" + "reported an internal server error.<br>"
                + "This is most likely a temporary problem. Please try again later.", e.getUrl())+"</html>";
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of a bad request.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainBadRequest(OsmApiException e) {
        String message = tr("The OSM server ''{0}'' reported a bad request.<br>", getUrlFromException(e));
        String errorHeader = e.getErrorHeader();
        if (errorHeader != null && (errorHeader.startsWith("The maximum bbox") ||
                        errorHeader.startsWith("You requested too many nodes"))) {
            message += "<br>"
                + tr("The area you tried to download is too big or your request was too large."
                        + "<br>Either request a smaller area or use an export file provided by the OSM community.");
        } else if (errorHeader != null) {
            message += tr("<br>Error message(untranslated): {0}", errorHeader);
        }
        Logging.error(e);
        return "<html>" + message + "</html>";
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because of
     * bandwidth limit exceeded (HTTP error 509)
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainBandwidthLimitExceeded(OsmApiException e) {
        Logging.error(e);
        // TODO: Write a proper error message
        return explainGenericOsmApiException(e);
    }

    /**
     * Explains a {@link OsmApiException} which was thrown because a resource wasn't found.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainNotFound(OsmApiException e) {
        String message = tr("The OSM server ''{0}'' does not know about an object<br>"
                + "you tried to read, update, or delete. Either the respective object<br>"
                + "does not exist on the server or you are using an invalid URL to access<br>"
                + "it. Please carefully check the server''s address ''{0}'' for typos.",
                getUrlFromException(e));
        Logging.error(e);
        return "<html>" + message + "</html>";
    }

    /**
     * Explains a {@link UnknownHostException} which has caused an {@link OsmTransferException}.
     * This is most likely happening when there is an error in the API URL or when
     * local DNS services are not working.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainNestedUnknownHostException(OsmTransferException e) {
        String apiUrl = e.getUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch (MalformedURLException ex) {
            // shouldn't happen
            Logging.trace(e);
        }

        Logging.error(e);
        return tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''.<br>"
                + "Host name ''{1}'' could not be resolved. <br>"
                + "Please check the API URL in your preferences and your internet connection.", apiUrl, host)+"</html>";
    }

    /**
     * Replies the first nested exception of type <code>nestedClass</code> (including
     * the root exception <code>e</code>) or null, if no such exception is found.
     *
     * @param <T> nested exception type
     * @param e the root exception
     * @param nestedClass the type of the nested exception
     * @return the first nested exception of type <code>nestedClass</code> (including
     * the root exception <code>e</code>) or null, if no such exception is found.
     * @since 8470
     */
    public static <T> T getNestedException(Exception e, Class<T> nestedClass) {
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
     * Explains an {@link OsmTransferException} to the user.
     *
     * @param e the {@link OsmTransferException}
     * @return The HTML formatted error message to display
     */
    public static String explainOsmTransferException(OsmTransferException e) {
        Objects.requireNonNull(e, "e");
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
            return explainChangesetClosedException((ChangesetClosedException) e);

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
            if (oae.getResponseCode() == 509)
                return explainBandwidthLimitExceeded(oae);
        }
        return explainGeneric(e);
    }

    /**
     * explains the case of an error due to a delete request on an already deleted
     * {@link OsmPrimitive}, i.e. a HTTP response code 410, where we don't know which
     * {@link OsmPrimitive} is causing the error.
     *
     * @param e the exception
     * @return The HTML formatted error message to display
     */
    public static String explainGoneForUnknownPrimitive(OsmApiException e) {
        return tr(
                "<html>The server reports that an object is deleted.<br>"
                + "<strong>Uploading failed</strong> if you tried to update or delete this object.<br> "
                + "<strong>Downloading failed</strong> if you tried to download this object.<br>"
                + "<br>"
                + "The error message is:<br>" + "{0}"
                + "</html>", Utils.escapeReservedCharactersHTML(e.getMessage()));
    }

    /**
     * Explains an {@link Exception} to the user.
     *
     * @param e the {@link Exception}
     * @return The HTML formatted error message to display
     */
    public static String explainException(Exception e) {
        Logging.error(e);
        if (e instanceof OsmTransferException) {
            return explainOsmTransferException((OsmTransferException) e);
        } else {
            return explainGeneric(e);
        }
    }

    static String getUrlFromException(OsmApiException e) {
        if (e.getAccessedUrl() != null) {
            try {
                return new URL(e.getAccessedUrl()).getHost();
            } catch (MalformedURLException e1) {
                Logging.warn(e1);
            }
        }
        if (e.getUrl() != null) {
            return e.getUrl();
        } else {
            return OsmApi.getOsmApi().getBaseUrl();
        }
    }
}
