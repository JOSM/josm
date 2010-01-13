// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadReferrersAction;
import org.openstreetmap.josm.actions.UpdateDataAction;
import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmApiPrimitiveGoneException;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.ImageProvider;

public abstract class AbstractUploadTask extends PleaseWaitRunnable {
    private static final Logger logger = Logger.getLogger(AbstractUploadTask.class.getName());

    public AbstractUploadTask(String title, boolean ignoreException) {
        super(title, ignoreException);
    }

    public AbstractUploadTask(String title, ProgressMonitor progressMonitor, boolean ignoreException) {
        super(title, progressMonitor, ignoreException);
    }

    public AbstractUploadTask(String title) {
        super(title);
    }

    /**
     * Synchronizes the local state of an {@see OsmPrimitive} with its state on the
     * server. The method uses an individual GET for the primitive.
     *
     * @param id the primitive ID
     */
    protected void synchronizePrimitive(final OsmPrimitiveType type, final long id) {
        // FIXME: should now about the layer this task is running for. might
        // be different from the current edit layer
        OsmDataLayer layer = Main.main.getEditLayer();
        if (layer == null)
            throw new IllegalStateException(tr("Failed to update primitive with id {0} because current edit layer is null", id));
        OsmPrimitive p = layer.data.getPrimitiveById(id, type);
        if (p == null)
            throw new IllegalStateException(tr("Failed to update primitive with id {0} because current edit layer does not include such a primitive", id));
        Main.worker.execute(new UpdatePrimitivesTask(layer, Collections.singleton(p)));
    }

    /**
     * Synchronizes the local state of the dataset with the state on the server.
     *
     * Reuses the functionality of {@see UpdateDataAction}.
     *
     * @see UpdateDataAction#actionPerformed(ActionEvent)
     */
    protected void synchronizeDataSet() {
        UpdateDataAction act = new UpdateDataAction();
        act.actionPerformed(new ActionEvent(this,0,""));
    }

    /**
     * Handles the case that a conflict in a specific {@see OsmPrimitive} was detected while
     * uploading
     *
     * @param primitiveType  the type of the primitive, either <code>node</code>, <code>way</code> or
     *    <code>relation</code>
     * @param id  the id of the primitive
     * @param serverVersion  the version of the primitive on the server
     * @param myVersion  the version of the primitive in the local dataset
     */
    protected void handleUploadConflictForKnownConflict(final OsmPrimitiveType primitiveType, final long id, String serverVersion, String myVersion) {
        String lbl = "";
        switch(primitiveType) {
        case NODE: lbl =  tr("Synchronize node {0} only", id); break;
        case WAY: lbl =  tr("Synchronize way {0} only", id); break;
        case RELATION: lbl =  tr("Synchronize relation {0} only", id); break;
        }
        ButtonSpec[] spec = new ButtonSpec[] {
                new ButtonSpec(
                        lbl,
                        ImageProvider.get("updatedata"),
                        null,
                        null
                ),
                new ButtonSpec(
                        tr("Synchronize entire dataset"),
                        ImageProvider.get("updatedata"),
                        null,
                        null
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        null,
                        null
                )
        };
        String msg =  tr("<html>Uploading <strong>failed</strong> because the server has a newer version of one<br>"
                + "of your nodes, ways, or relations.<br>"
                + "The conflict is caused by the <strong>{0}</strong> with id <strong>{1}</strong>,<br>"
                + "the server has version {2}, your version is {3}.<br>"
                + "<br>"
                + "Click <strong>{4}</strong> to synchronize the conflicting primitive only.<br>"
                + "Click <strong>{5}</strong> to synchronize the entire local dataset with the server.<br>"
                + "Click <strong>{6}</strong> to abort and continue editing.<br></html>",
                tr(primitiveType.getAPIName()), id, serverVersion, myVersion,
                spec[0].text, spec[1].text, spec[2].text
        );
        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Conflicts detected"),
                JOptionPane.ERROR_MESSAGE,
                null,
                spec,
                spec[0],
                "/Concepts/Conflict"
        );
        switch(ret) {
        case 0: synchronizePrimitive(primitiveType, id); break;
        case 1: synchronizeDataSet(); break;
        default: return;
        }
    }

    /**
     * Handles the case that a conflict was detected while uploading where we don't
     * know what {@see OsmPrimitive} actually caused the conflict (for whatever reason)
     *
     */
    protected void handleUploadConflictForUnknownConflict() {
        ButtonSpec[] spec = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Synchronize entire dataset"),
                        ImageProvider.get("updatedata"),
                        null,
                        null
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        null,
                        null
                )
        };
        String msg =  tr("<html>Uploading <strong>failed</strong> because the server has a newer version of one<br>"
                + "of your nodes, ways, or relations.<br>"
                + "<br>"
                + "Click <strong>{0}</strong> to synchronize the entire local dataset with the server.<br>"
                + "Click <strong>{1}</strong> to abort and continue editing.<br></html>",
                spec[0].text, spec[1].text
        );
        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Conflicts detected"),
                JOptionPane.ERROR_MESSAGE,
                null,
                spec,
                spec[0],
                ht("Concepts/Conflict")
        );
        if (ret == 0) {
            synchronizeDataSet();
        }
    }

    /**
     * Handles the case that a conflict was detected while uploading where we don't
     * know what {@see OsmPrimitive} actually caused the conflict (for whatever reason)
     *
     */
    protected void handleUploadConflictForClosedChangeset(long changsetId, Date d) {
        String msg =  tr("<html>Uploading <strong>failed</strong> because you have been using<br>"
                + "changeset {0} which was already closed at {1}.<br>"
                + "Please upload again with a new or an existing open changeset.</html>",
                changsetId, new SimpleDateFormat().format(d)
        );
        JOptionPane.showMessageDialog(
                Main.parent,
                msg,
                tr("Changeset closed"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Handles the case where deleting a node failed because it is still in use in
     * a non-deleted way on the server.
     */
    protected void handleUploadConflictForNodeStillInUse(long nodeId, long wayId) {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Prepare conflict resolution"),
                        ImageProvider.get("ok"),
                        tr("Click to download all parent ways for node {0}", nodeId),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        tr("Click to cancel and to resume editing the map", nodeId),
                        null /* no specific help context */
                )
        };
        String msg =  tr("<html>Uploading <strong>failed</strong> because you tried "
                + "to delete node {0} which is still in use in way {1}.<br><br>"
                + "Click <strong>{2}</strong> to download all parent ways of node {0}.<br>"
                + "If necessary JOSM will create conflicts which you can resolve in the Conflict Resolution Dialog."
                + "</html>",
                nodeId, wayId, options[0].text
        );

        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Node still in use"),
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0],
                "/Action/Upload#NodeStillInUseInWay"
        );
        if (ret != 0) return;
        DownloadReferrersAction.downloadReferrers(Main.map.mapView.getEditLayer(), nodeId, OsmPrimitiveType.NODE);
    }

    /**
     * handles an upload conflict, i.e. an error indicated by a HTTP return code 409.
     *
     * @param e  the exception
     */
    protected void handleUploadConflict(OsmApiException e) {
        String pattern = "Version mismatch: Provided (\\d+), server had: (\\d+) of (\\S+) (\\d+)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(e.getErrorHeader());
        if (m.matches()) {
            handleUploadConflictForKnownConflict(OsmPrimitiveType.from(m.group(3)), Long.parseLong(m.group(4)), m.group(2),m.group(1));
            return;
        }
        pattern ="The changeset (\\d+) was closed at (.*)";
        p = Pattern.compile(pattern);
        m = p.matcher(e.getErrorHeader());
        if (m.matches()) {
            handleUploadConflictForClosedChangeset(Long.parseLong(m.group(1)), DateUtils.fromString(m.group(2)));
            return;
        }
        pattern = "Node (\\d+) is still used by way (\\d+).";
        p = Pattern.compile(pattern);
        m = p.matcher(e.getErrorHeader());
        if (m.matches()) {
            handleUploadConflictForNodeStillInUse(Long.parseLong(m.group(1)), Long.parseLong(m.group(2)));
            return;
        }
        logger.warning(tr("Warning: error header \"{0}\" did not match with an expected pattern", e.getErrorHeader()));
        handleUploadConflictForUnknownConflict();
    }

    /**
     * handles an precondition failed conflict, i.e. an error indicated by a HTTP return code 412.
     *
     * @param e  the exception
     */
    protected void handlePreconditionFailed(OsmApiException e) {
        String pattern = "Precondition failed: Node (\\d+) is still used by way (\\d+).";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(e.getErrorHeader());
        if (m.matches()) {
            handleUploadConflictForNodeStillInUse(Long.parseLong(m.group(1)), Long.parseLong(m.group(2)));
            return;
        }
        logger.warning(tr("Warning: error header \"{0}\" did not match with an expected pattern", e.getErrorHeader()));
        ExceptionDialogUtil.explainPreconditionFailed(e);
    }

    /**
     * Handles an error which is caused by a delete request for an already deleted
     * {@see OsmPrimitive} on the server, i.e. a HTTP response code of 410.
     * Note that an <strong>update</strong> on an already deleted object results
     * in a 409, not a 410.
     *
     * @param e the exception
     */
    protected void handleGone(OsmApiPrimitiveGoneException e) {
        if (e.isKnownPrimitive()) {
            new UpdateSelectionAction().handlePrimitiveGoneException(e.getPrimitiveId(),e.getPrimitiveType());
        } else {
            ExceptionDialogUtil.explainGoneForUnknownPrimitive(e);
        }
    }

    /**
     * error handler for any exception thrown during upload
     *
     * @param e the exception
     */
    protected void handleFailedUpload(Exception e) {
        // API initialization failed. Notify the user and return.
        //
        if (e instanceof OsmApiInitializationException) {
            ExceptionDialogUtil.explainOsmApiInitializationException((OsmApiInitializationException)e);
            return;
        }

        if (e instanceof OsmApiPrimitiveGoneException) {
            handleGone((OsmApiPrimitiveGoneException)e);
            return;
        }
        if (e instanceof OsmApiException) {
            OsmApiException ex = (OsmApiException)e;
            // There was an upload conflict. Let the user decide whether
            // and how to resolve it
            //
            if(ex.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                handleUploadConflict(ex);
                return;
            }
            // There was a precondition failed. Notify the user.
            //
            else if (ex.getResponseCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
                handlePreconditionFailed(ex);
                return;
            }
            // Tried to update or delete a primitive which never existed on
            // the server?
            //
            else if (ex.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                ExceptionDialogUtil.explainNotFound(ex);
                return;
            }
        }

        ExceptionDialogUtil.explainException(e);
    }
}
