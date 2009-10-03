// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmChangesetCloseException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ExceptionUtil;

/**
 * This utility class provides static methods which explain various exceptions to the user.
 * 
 */
public class ExceptionDialogUtil {

    /**
     * just static utility functions. no constructor
     */
    private ExceptionDialogUtil() {
    }

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    public static void explainOsmApiInitializationException(OsmApiInitializationException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainOsmApiInitializationException(e), tr("Error"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    public static void explainOsmChangesetCloseException(OsmChangesetCloseException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainOsmChangesetCloseException(e), tr("Error"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains an upload error due to a violated precondition, i.e. a HTTP return code 412
     *
     * @param e the exception
     */
    public static void explainPreconditionFailed(OsmApiException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainPreconditionFailed(e),
                tr("Precondition violation"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains an exception with a generic message dialog
     * 
     * @param e the exception
     */
    public static void explainGeneric(Exception e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainGeneric(e), tr("Error"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains a {@see SecurityException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from within an
     * applet which wasn't loaded from the API server.
     * 
     * @param e the exception
     */

    public static void explainSecurityException(OsmTransferException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainSecurityException(e), tr("Security exception"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains a {@see SocketException} which has caused an {@see OsmTransferException}.
     * This is most likely because there's not connection to the Internet or because
     * the remote server is not reachable.
     * 
     * @param e the exception
     */

    public static void explainNestedSocketException(OsmTransferException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainNestedSocketException(e),
                tr("Network exception"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains a {@see IOException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when the communication with the remote server is
     * interrupted for any reason.
     * 
     * @param e the exception
     */

    public static void explainNestedIOException(OsmTransferException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainNestedIOException(e), tr("IO Exception"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because of an internal server
     * error in the OSM API server..
     * 
     * @param e the exception
     */

    public static void explainInternalServerError(OsmTransferException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainInternalServerError(e),
                tr("Internal Server Error"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because of a bad
     * request
     * 
     * @param e the exception
     */
    public static void explainBadRequest(OsmApiException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainBadRequest(e), tr("Bad Request"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because a resource wasn't found
     * on the server
     * 
     * @param e the exception
     */
    public static void explainNotFound(OsmApiException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainNotFound(e), tr("Not Found"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Explains a {@see UnknownHostException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when there is an error in the API URL or when
     * local DNS services are not working.
     * 
     * @param e the exception
     */

    public static void explainNestedUnkonwnHostException(OsmTransferException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainNestedUnkonwnHostException(e),
                tr("Unknown host"), JOptionPane.ERROR_MESSAGE);
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
    public static void explainOsmTransferException(OsmTransferException e) {
        if (getNestedException(e, SecurityException.class) != null) {
            explainSecurityException(e);
            return;
        }
        if (getNestedException(e, SocketException.class) != null) {
            explainNestedSocketException(e);
            return;
        }
        if (getNestedException(e, UnknownHostException.class) != null) {
            explainNestedUnkonwnHostException(e);
            return;
        }
        if (getNestedException(e, IOException.class) != null) {
            explainNestedIOException(e);
            return;
        }
        if (e instanceof OsmApiInitializationException) {
            explainOsmApiInitializationException((OsmApiInitializationException) e);
            return;
        }
        if (e instanceof OsmChangesetCloseException) {
            explainOsmChangesetCloseException((OsmChangesetCloseException) e);
            return;
        }

        if (e instanceof OsmApiException) {
            OsmApiException oae = (OsmApiException) e;
            if (oae.getResponseCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
                explainPreconditionFailed(oae);
                return;
            }
            if (oae.getResponseCode() == HttpURLConnection.HTTP_GONE) {
                explainGoneForUnknownPrimitive(oae);
                return;
            }
            if (oae.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                explainInternalServerError(oae);
                return;
            }
            if (oae.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                explainBadRequest(oae);
                return;
            }
            if (oae.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                explainNotFound(oae);
                return;
            }

        }
        explainGeneric(e);
    }

    /**
     * explains the case of an error due to a delete request on an already deleted
     * {@see OsmPrimitive}, i.e. a HTTP response code 410, where we don't know which
     * {@see OsmPrimitive} is causing the error.
     *
     * @param e the exception
     */
    public static void explainGoneForUnknownPrimitive(OsmApiException e) {
        JOptionPane.showMessageDialog(Main.parent, ExceptionUtil.explainGoneForUnknownPrimitive(e),
                tr("Object deleted"), JOptionPane.ERROR_MESSAGE);

    }

    /**
     * Explains an {@see Exception} to the user.
     * 
     * @param e the {@see Exception}
     */
    public static void explainException(Exception e) {
        if (e instanceof OsmTransferException) {
            explainOsmTransferException((OsmTransferException) e);
            return;
        }
        explainGeneric(e);
    }
}
