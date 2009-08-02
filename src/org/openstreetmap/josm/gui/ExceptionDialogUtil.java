// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * This utility class provides static methods which explain various exceptions to the user.
 * 
 */
public class ExceptionDialogUtil {

    /**
     * just static utility functions. no constructor
     */
    private ExceptionDialogUtil() {}


    /**
     * Explains an exception with a generic message dialog
     * 
     * @param e the exception
     */
    public static void explainGeneric(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = e.toString();
        }
        e.printStackTrace();
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                msg,
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Explains a {@see SecurityException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from whitin an
     * applet which wasn't loaded from the API server.
     * 
     * @param e the exception
     */

    public static void explainSecurityException(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch(MalformedURLException ex) {
            // shouldn't happen
        }

        String message = tr("<html>Failed to open a connection to the remote server<br>"
                + "''{0}''<br>"
                + "for security reasons. This is most likely because you are running<br>"
                + "in an applet and because you didn''t load your applet from ''{1}''.</html>",
                apiUrl, host
        );
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                message,
                tr("Security exception"),
                JOptionPane.ERROR_MESSAGE
        );

    }

    /**
     * Replies the first {@see SecurityException} in a chain of nested exceptions.
     * null, if no {@see SecurityException} is in this chain.
     * 
     * @param e the root exception
     * @return the first {@see SecurityException} in a chain of nested exceptions
     */
    protected static SecurityException getSecurityChildException(Exception e) {
        Throwable t = e;
        while(t != null && ! (t instanceof SecurityException)) {
            t = t.getCause();
        }
        return (SecurityException)t;
    }

    /**
     * Explains an {@see OsmTransferException} to the user.
     * 
     * @param e the {@see OsmTransferException}
     */
    public static void explainOsmTransferException(OsmTransferException e) {
        if (getSecurityChildException(e) != null) {
            explainSecurityException(e);
            return;
        }
        explainGeneric(e);
    }

    /**
     * Explains an {@see Exception} to the user.
     * 
     * @param e the {@see Exception}
     */
    public static void explainException(Exception e) {
        if (e instanceof OsmTransferException) {
            explainOsmTransferException((OsmTransferException)e);
            return;
        }
        explainGeneric(e);
    }
}
