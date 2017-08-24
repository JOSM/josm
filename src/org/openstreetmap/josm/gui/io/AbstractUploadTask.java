// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmApiPrimitiveGoneException;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Abstract base class for the task of uploading primitives via OSM API.
 *
 * Mainly handles conflicts and certain error situations.
 */
public abstract class AbstractUploadTask extends PleaseWaitRunnable {

    /**
     * Constructs a new {@code AbstractUploadTask}.
     * @param title message for the user
     * @param ignoreException If true, exception will be silently ignored. If false then
     * exception will be handled by showing a dialog. When this runnable is executed using executor framework
     * then use false unless you read result of task (because exception will get lost if you don't)
     */
    public AbstractUploadTask(String title, boolean ignoreException) {
        super(title, ignoreException);
    }

    /**
     * Constructs a new {@code AbstractUploadTask}.
     * @param title message for the user
     * @param progressMonitor progress monitor
     * @param ignoreException If true, exception will be silently ignored. If false then
     * exception will be handled by showing a dialog. When this runnable is executed using executor framework
     * then use false unless you read result of task (because exception will get lost if you don't)
     */
    public AbstractUploadTask(String title, ProgressMonitor progressMonitor, boolean ignoreException) {
        super(title, progressMonitor, ignoreException);
    }

    /**
     * Constructs a new {@code AbstractUploadTask}.
     * @param title message for the user
     */
    public AbstractUploadTask(String title) {
        super(title);
    }

    /**
     * Synchronizes the local state of an {@link OsmPrimitive} with its state on the
     * server. The method uses an individual GET for the primitive.
     * @param type the primitive type
     * @param id the primitive ID
     */
    protected void synchronizePrimitive(final OsmPrimitiveType type, final long id) {
        // FIXME: should now about the layer this task is running for. might
        // be different from the current edit layer
        OsmDataLayer layer = Main.getLayerManager().getEditLayer();
        if (layer == null)
            throw new IllegalStateException(tr("Failed to update primitive with id {0} because current edit layer is null", id));
        OsmPrimitive p = layer.data.getPrimitiveById(id, type);
        if (p == null)
            throw new IllegalStateException(
                    tr("Failed to update primitive with id {0} because current edit layer does not include such a primitive", id));
        MainApplication.worker.execute(new UpdatePrimitivesTask(layer, Collections.singleton(p)));
    }

    /**
     * Synchronizes the local state of the dataset with the state on the server.
     *
     * Reuses the functionality of {@link UpdateDataAction}.
     *
     * @see UpdateDataAction#actionPerformed(ActionEvent)
     */
    protected void synchronizeDataSet() {
        UpdateDataAction act = new UpdateDataAction();
        act.actionPerformed(new ActionEvent(this, 0, ""));
    }

    /**
     * Handles the case that a conflict in a specific {@link OsmPrimitive} was detected while
     * uploading
     *
     * @param primitiveType  the type of the primitive, either <code>node</code>, <code>way</code> or
     *    <code>relation</code>
     * @param id  the id of the primitive
     * @param serverVersion  the version of the primitive on the server
     * @param myVersion  the version of the primitive in the local dataset
     */
    protected void handleUploadConflictForKnownConflict(final OsmPrimitiveType primitiveType, final long id, String serverVersion,
            String myVersion) {
        String lbl;
        switch(primitiveType) {
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        case NODE:     lbl = tr("Synchronize node {0} only", id); break;
        case WAY:      lbl = tr("Synchronize way {0} only", id); break;
        case RELATION: lbl = tr("Synchronize relation {0} only", id); break;
        // CHECKSTYLE.ON: SingleSpaceSeparator
        default: throw new AssertionError();
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
        String msg = tr("<html>Uploading <strong>failed</strong> because the server has a newer version of one<br>"
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
     * know what {@link OsmPrimitive} actually caused the conflict (for whatever reason)
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
        String msg = tr("<html>Uploading <strong>failed</strong> because the server has a newer version of one<br>"
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
                ht("/Concepts/Conflict")
        );
        if (ret == 0) {
            synchronizeDataSet();
        }
    }

    /**
     * Handles the case that a conflict was detected while uploading where we don't
     * know what {@link OsmPrimitive} actually caused the conflict (for whatever reason)
     * @param changesetId changeset ID
     * @param d changeset date
     */
    protected void handleUploadConflictForClosedChangeset(long changesetId, Date d) {
        String msg = tr("<html>Uploading <strong>failed</strong> because you have been using<br>"
                + "changeset {0} which was already closed at {1}.<br>"
                + "Please upload again with a new or an existing open changeset.</html>",
                changesetId, DateUtils.formatDateTime(d, DateFormat.SHORT, DateFormat.SHORT)
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
     * @param e exception
     * @param conflict conflict
     */
    protected void handleUploadPreconditionFailedConflict(OsmApiException e, Pair<OsmPrimitive, Collection<OsmPrimitive>> conflict) {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Prepare conflict resolution"),
                        ImageProvider.get("ok"),
                        tr("Click to download all referring objects for {0}", conflict.a),
                        null /* no specific help context */
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        tr("Click to cancel and to resume editing the map"),
                        null /* no specific help context */
                )
        };
        String msg = ExceptionUtil.explainPreconditionFailed(e).replace("</html>", "<br><br>" + tr(
                "Click <strong>{0}</strong> to load them now.<br>"
                + "If necessary JOSM will create conflicts which you can resolve in the Conflict Resolution Dialog.",
                options[0].text)) + "</html>";
        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                msg,
                tr("Object still in use"),
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0],
                "/Action/Upload#NodeStillInUseInWay"
        );
        if (ret == 0) {
            DownloadReferrersAction.downloadReferrers(Main.getLayerManager().getEditLayer(), Arrays.asList(conflict.a));
        }
    }

    /**
     * handles an upload conflict, i.e. an error indicated by a HTTP return code 409.
     *
     * @param e  the exception
     */
    protected void handleUploadConflict(OsmApiException e) {
        final String errorHeader = e.getErrorHeader();
        if (errorHeader != null) {
            Pattern p = Pattern.compile("Version mismatch: Provided (\\d+), server had: (\\d+) of (\\S+) (\\d+)");
            Matcher m = p.matcher(errorHeader);
            if (m.matches()) {
                handleUploadConflictForKnownConflict(OsmPrimitiveType.from(m.group(3)), Long.parseLong(m.group(4)), m.group(2), m.group(1));
                return;
            }
            p = Pattern.compile("The changeset (\\d+) was closed at (.*)");
            m = p.matcher(errorHeader);
            if (m.matches()) {
                handleUploadConflictForClosedChangeset(Long.parseLong(m.group(1)), DateUtils.fromString(m.group(2)));
                return;
            }
        }
        Logging.warn(tr("Error header \"{0}\" did not match with an expected pattern", errorHeader));
        handleUploadConflictForUnknownConflict();
    }

    /**
     * handles an precondition failed conflict, i.e. an error indicated by a HTTP return code 412.
     *
     * @param e  the exception
     */
    protected void handlePreconditionFailed(OsmApiException e) {
        // in the worst case, ExceptionUtil.parsePreconditionFailed is executed trice - should not be too expensive
        Pair<OsmPrimitive, Collection<OsmPrimitive>> conflict = ExceptionUtil.parsePreconditionFailed(e.getErrorHeader());
        if (conflict != null) {
            handleUploadPreconditionFailedConflict(e, conflict);
        } else {
            Logging.warn(tr("Error header \"{0}\" did not match with an expected pattern", e.getErrorHeader()));
            ExceptionDialogUtil.explainPreconditionFailed(e);
        }
    }

    /**
     * Handles an error which is caused by a delete request for an already deleted
     * {@link OsmPrimitive} on the server, i.e. a HTTP response code of 410.
     * Note that an <strong>update</strong> on an already deleted object results
     * in a 409, not a 410.
     *
     * @param e the exception
     */
    protected void handleGone(OsmApiPrimitiveGoneException e) {
        if (e.isKnownPrimitive()) {
            UpdateSelectionAction.handlePrimitiveGoneException(e.getPrimitiveId(), e.getPrimitiveType());
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
            ExceptionDialogUtil.explainOsmApiInitializationException((OsmApiInitializationException) e);
            return;
        }

        if (e instanceof OsmApiPrimitiveGoneException) {
            handleGone((OsmApiPrimitiveGoneException) e);
            return;
        }
        if (e instanceof OsmApiException) {
            OsmApiException ex = (OsmApiException) e;
            if (ex.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                // There was an upload conflict. Let the user decide whether and how to resolve it
                handleUploadConflict(ex);
                return;
            } else if (ex.getResponseCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
                // There was a precondition failed. Notify the user.
                handlePreconditionFailed(ex);
                return;
            } else if (ex.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                // Tried to update or delete a primitive which never existed on the server?
                ExceptionDialogUtil.explainNotFound(ex);
                return;
            }
        }

        ExceptionDialogUtil.explainException(e);
    }
}
