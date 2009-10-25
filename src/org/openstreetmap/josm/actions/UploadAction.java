// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.upload.ApiPreconditionCheckerHook;
import org.openstreetmap.josm.actions.upload.RelationUploadOrderHook;
import org.openstreetmap.josm.actions.upload.UploadHook;
import org.openstreetmap.josm.actions.upload.UploadParameterHook;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.io.UploadDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmApiPrimitiveGoneException;
import org.openstreetmap.josm.io.OsmChangesetCloseException;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;


/**
 * Action that opens a connection to the osm server and uploads all changes.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * If the upload fails this action offers various options to resolve conflicts.
 *
 * @author imi
 */
public class UploadAction extends JosmAction{
    static private Logger logger = Logger.getLogger(UploadAction.class.getName());
    /**
     * The list of upload hooks. These hooks will be called one after the other
     * when the user wants to upload data. Plugins can insert their own hooks here
     * if they want to be able to veto an upload.
     *
     * Be default, the standard upload dialog is the only element in the list.
     * Plugins should normally insert their code before that, so that the upload
     * dialog is the last thing shown before upload really starts; on occasion
     * however, a plugin might also want to insert something after that.
     */
    private static final LinkedList<UploadHook> uploadHooks = new LinkedList<UploadHook>();
    static {
        /**
         * Checks server capabilities before upload.
         */
        uploadHooks.add(new ApiPreconditionCheckerHook());

        /**
         * Adjusts the upload order of new relations
         */
        uploadHooks.add(new RelationUploadOrderHook());

        /**
         * Displays a screen where the actions that would be taken are displayed and
         * give the user the possibility to cancel the upload.
         */
        uploadHooks.add(new UploadParameterHook());
    }

    /**
     * Registers an upload hook. Adds the hook at the first position of the upload hooks.
     * 
     * @param hook the upload hook. Ignored if null.
     */
    public static void registerUploadHook(UploadHook hook) {
        if(hook == null) return;
        if (!uploadHooks.contains(hook)) {
            uploadHooks.add(0,hook);
        }
    }

    /**
     * Unregisters an upload hook. Removes the hook from the list of upload hooks.
     * 
     * @param hook the upload hook. Ignored if null.
     */
    public static void unregisterUploadHook(UploadHook hook) {
        if(hook == null) return;
        if (uploadHooks.contains(hook)) {
            uploadHooks.remove(hook);
        }
    }

    public UploadAction() {
        super(tr("Upload data"), "upload", tr("Upload all changes in the active data layer to the OSM server"),
                Shortcut.registerShortcut("file:upload", tr("File: {0}", tr("Upload data")), KeyEvent.VK_U, Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY), true);
    }

    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

    public boolean checkPreUploadConditions(OsmDataLayer layer) {
        return checkPreUploadConditions(layer, new APIDataSet(layer.data));
    }

    public boolean checkPreUploadConditions(OsmDataLayer layer, APIDataSet apiData) {
        ConflictCollection conflicts = layer.getConflicts();
        if (conflicts !=null && !conflicts.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>There are unresolved conflicts in layer ''{0}''.<br>"
                            + "You have to resolve them first.</html>", layer.getName()),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        // Call all upload hooks in sequence. The upload confirmation dialog
        // is one of these.
        for(UploadHook hook : uploadHooks)
            if(!hook.checkUpload(apiData))
                return false;

        return true;
    }

    public void uploadData(OsmDataLayer layer, APIDataSet apiData) {
        if (apiData.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No changes to upload."),
                    tr("Warning"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        if (!checkPreUploadConditions(layer, apiData))
            return;
        Main.worker.execute(
                createUploadTask(
                        layer,
                        apiData.getPrimitives(),
                        UploadDialog.getUploadDialog().getChangeset(),
                        UploadDialog.getUploadDialog().isDoCloseAfterUpload()
                )
        );
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        if (Main.map == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Nothing to upload. Get some data first."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        APIDataSet apiData = new APIDataSet(Main.main.getCurrentDataSet());
        uploadData(Main.map.mapView.getEditLayer(), apiData);
    }

    /**
     * Synchronizes the local state of an {@see OsmPrimitive} with its state on the
     * server. The method uses an individual GET for the primitive.
     *
     * @param id the primitive ID
     */
    protected void synchronizePrimitive(final OsmPrimitiveType type, final long id) {
        Main.worker.execute(new UpdatePrimitiveTask(type, id));
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
                "Concepts/Conflict"
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
        String msg =  tr("<html>Uploading <strong>failed</strong> because you''ve been using<br>"
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
     * Handles an error due to a delete request on an already deleted
     * {@see OsmPrimitive}, i.e. a HTTP response code 410, where we know what
     * {@see OsmPrimitive} is responsible for the error.
     *
     *  Reuses functionality of the {@see UpdateSelectionAction} to resolve
     *  conflicts due to mismatches in the deleted state.
     *
     * @param primitiveType the type of the primitive
     * @param id the id of the primitive
     *
     * @see UpdateSelectionAction#handlePrimitiveGoneException(long)
     */
    protected void handleGoneForKnownPrimitive(OsmPrimitiveType primitiveType, long id) {
        UpdateSelectionAction act = new UpdateSelectionAction();
        act.handlePrimitiveGoneException(id,primitiveType);
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
            handleGoneForKnownPrimitive(e.getPrimitiveType(), e.getPrimitiveId());
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

        if (e instanceof OsmChangesetCloseException) {
            ExceptionDialogUtil.explainOsmChangesetCloseException((OsmChangesetCloseException)e);
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
            // any other API exception
            //
            else {
                ex.printStackTrace();
                String msg = tr("<html>Uploading <strong>failed</strong>."
                        + "<br>"
                        + "{0}"
                        + "</html>",
                        ex.getDisplayMessage()
                );
                JOptionPane.showMessageDialog(
                        Main.map,
                        msg,
                        tr("Upload to OSM API failed"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        ExceptionDialogUtil.explainException(e);
    }

    /**
     * The asynchronous task to update a specific id
     *
     */
    class UpdatePrimitiveTask extends  PleaseWaitRunnable {

        private boolean uploadCancelled = false;
        private boolean uploadFailed = false;
        private Exception lastException = null;
        private long id;
        private OsmPrimitiveType type;

        public UpdatePrimitiveTask(OsmPrimitiveType type, long id) {
            super(tr("Updating primitive"),false /* don't ignore exceptions */);
            this.id = id;
            this.type = type;
        }

        @Override protected void realRun() throws SAXException, IOException {
            try {
                UpdateSelectionAction act = new UpdateSelectionAction();
                act.updatePrimitive(type, id);
            } catch (Exception sxe) {
                if (uploadCancelled) {
                    System.out.println("Ignoring exception caught because upload is canceled. Exception is: " + sxe.toString());
                    return;
                }
                uploadFailed = true;
                lastException = sxe;
            }
        }

        @Override protected void finish() {
            if (uploadFailed) {
                handleFailedUpload(lastException);
            }
        }

        @Override protected void cancel() {
            OsmApi.getOsmApi().cancel();
            uploadCancelled = true;
        }
    }

    public UploadPrimitivesTask createUploadTask(OsmDataLayer layer, Collection<OsmPrimitive> toUpload, Changeset changeset, boolean closeChangesetAfterUpload) {
        return new UploadPrimitivesTask(layer, toUpload, changeset, closeChangesetAfterUpload);
    }

    /**
     * The task for uploading a collection of primitives
     *
     */
    public class UploadPrimitivesTask extends  PleaseWaitRunnable {
        private boolean uploadCancelled = false;
        private Exception lastException = null;
        private Collection <OsmPrimitive> toUpload;
        private OsmServerWriter writer;
        private OsmDataLayer layer;
        private Changeset changeset;
        private boolean closeChangesetAfterUpload;
        private HashSet<OsmPrimitive> processedPrimitives;

        /**
         * 
         * @param layer  the OSM data layer for which data is uploaded
         * @param toUpload the collection of primitives to upload
         * @param changeset the changeset to use for uploading
         * @param closeChangesetAfterUpload true, if the changeset is to be closed after uploading
         */
        private UploadPrimitivesTask(OsmDataLayer layer, Collection <OsmPrimitive> toUpload, Changeset changeset, boolean closeChangesetAfterUpload) {
            super(tr("Uploading data for layer ''{0}''", layer.getName()),false /* don't ignore exceptions */);
            this.toUpload = toUpload;
            this.layer = layer;
            this.changeset = changeset;
            this.closeChangesetAfterUpload = closeChangesetAfterUpload;
            this.processedPrimitives = new HashSet<OsmPrimitive>();
        }

        protected OsmPrimitive getPrimitive(OsmPrimitiveType type, long id) {
            for (OsmPrimitive p: toUpload) {
                if (OsmPrimitiveType.from(p).equals(type) && p.getId() == id)
                    return p;
            }
            return null;
        }

        /**
         * Retries to recover the upload operation from an exception which was thrown because
         * an uploaded primitive was already deleted on the server.
         * 
         * @param e the exception throw by the API
         * @param monitor a progress monitor
         * @throws OsmTransferException  thrown if we can't recover from the exception
         */
        protected void recoverFromGoneOnServer(OsmApiPrimitiveGoneException e, ProgressMonitor monitor) throws OsmTransferException{
            if (!e.isKnownPrimitive()) throw e;
            OsmPrimitive p = getPrimitive(e.getPrimitiveType(), e.getPrimitiveId());
            if (p == null) throw e;
            if (p.isDeleted()) {
                // we tried to delete an already deleted primitive.
                //
                System.out.println(tr("Warning: object ''{0}'' is already deleted on the server. Skipping this object and retrying to upload.", p.getDisplayName(DefaultNameFormatter.getInstance())));
                monitor.appendLogMessage(tr("Object ''{0}'' is already deleted. Skipping object in upload.",p.getDisplayName(DefaultNameFormatter.getInstance())));
                processedPrimitives.addAll(writer.getProcessedPrimitives());
                processedPrimitives.add(p);
                toUpload.removeAll(processedPrimitives);
                return;
            }
            // exception was thrown because we tried to *update* an already deleted
            // primitive. We can't resolve this automatically. Re-throw exception,
            // a conflict is going to be created later.
            throw e;
        }

        @Override protected void realRun() throws SAXException, IOException {
            writer = new OsmServerWriter();
            try {
                while(true) {
                    try {
                        getProgressMonitor().subTask(tr("Uploading {0} objects ...", toUpload.size()));
                        writer.uploadOsm(layer.data.version, toUpload, changeset, getProgressMonitor().createSubTaskMonitor(1, false));
                        processedPrimitives.addAll(writer.getProcessedPrimitives());
                        // if we get here we've successfully uploaded the data. Exit the loop.
                        //
                        break;
                    } catch(OsmApiPrimitiveGoneException e) {
                        // try to recover from the 410 Gone
                        recoverFromGoneOnServer(e, getProgressMonitor());
                    }
                }
                // if required close the changeset
                //
                if (closeChangesetAfterUpload) {
                    if (changeset != null && changeset.getId() > 0) {
                        OsmApi.getOsmApi().closeChangeset(changeset, progressMonitor.createSubTaskMonitor(0,false));
                    }
                }
            } catch (Exception e) {
                if (uploadCancelled) {
                    System.out.println(tr("Ignoring caught exception because upload is canceled. Exception is: {0}", e.toString()));
                    return;
                }
                lastException = e;
            }
        }

        @Override protected void finish() {
            if (uploadCancelled)
                return;

            // we always clean up the data, even in case of errors. It's possible the data was
            // partially uploaded
            //
            layer.cleanupAfterUpload(processedPrimitives);
            DataSet.fireSelectionChanged(layer.data.getSelected());
            layer.fireDataChange();
            if (lastException != null) {
                handleFailedUpload(lastException);
            }
            layer.onPostUploadToServer();
            UploadDialog.getUploadDialog().setOrUpdateChangeset(changeset);
        }

        @Override protected void cancel() {
            uploadCancelled = true;
            if (writer != null) {
                writer.cancel();
            }
        }
    }
}
