// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.historycombobox.SuggestingJHistoryComboBox;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.io.ChangesetProcessingType;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmChangesetCloseException;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.WindowGeometry;
import org.xml.sax.SAXException;

import com.sun.corba.se.spi.orbutil.fsm.Action;


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

    public static final String HISTORY_KEY = "upload.comment.history";

    /** Upload Hook */
    public interface UploadHook {
        /**
         * Checks the upload.
         * @param add The added primitives
         * @param update The updated primitives
         * @param delete The deleted primitives
         * @return true, if the upload can continue
         */
        public boolean checkUpload(Collection<OsmPrimitive> add, Collection<OsmPrimitive> update, Collection<OsmPrimitive> delete);
    }

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
    public final LinkedList<UploadHook> uploadHooks = new LinkedList<UploadHook>();

    public UploadAction() {
        super(tr("Upload to OSM..."), "upload", tr("Upload all changes to the OSM server."),
                Shortcut.registerShortcut("file:upload", tr("File: {0}", tr("Upload to OSM...")), KeyEvent.VK_U, Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY), true);

        /**
         * Checks server capabilities before upload.
         */
        uploadHooks.add(new ApiPreconditionChecker());

        /**
         * Displays a screen where the actions that would be taken are displayed and
         * give the user the possibility to cancel the upload.
         */
        uploadHooks.add(new UploadConfirmationHook());
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
                            + "You have to resolve them first.<html>", layer.getName()),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        // Call all upload hooks in sequence. The upload confirmation dialog
        // is one of these.
        for(UploadHook hook : uploadHooks)
            if(!hook.checkUpload(apiData.getPrimitivesToAdd(), apiData.getPrimitivesToUpdate(), apiData.getPrimitivesToDelete()))
                return false;

        return true;
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
        if (apiData.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No changes to upload."),
                    tr("Warning"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        if (!checkPreUploadConditions(Main.map.mapView.getEditLayer(), apiData))
            return;
        Main.worker.execute(
                createUploadTask(
                        Main.map.mapView.getEditLayer(),
                        apiData.getPrimitives(),
                        UploadConfirmationHook.getUploadDialogPanel().getChangeset(),
                        UploadConfirmationHook.getUploadDialogPanel().getChangesetProcessingType()
                )
        );
    }

    /**
     * Synchronizes the local state of an {@see OsmPrimitive} with its state on the
     * server. The method uses an individual GET for the primitive.
     *
     * @param id the primitive ID
     */
    protected void synchronizePrimitive(final String id) {
        Main.worker.execute(new UpdatePrimitiveTask(Long.parseLong(id)));
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
    protected void handleUploadConflictForKnownConflict(String primitiveType, String id, String serverVersion, String myVersion) {
        Object[] options = new Object[] {
                tr("Synchronize {0} {1} only", tr(primitiveType), id),
                tr("Synchronize entire dataset"),
                tr("Cancel")
        };
        Object defaultOption = options[0];
        String msg =  tr("<html>Uploading <strong>failed</strong> because the server has a newer version of one<br>"
                + "of your nodes, ways, or relations.<br>"
                + "The conflict is caused by the <strong>{0}</strong> with id <strong>{1}</strong>,<br>"
                + "the server has version {2}, your version is {3}.<br>"
                + "<br>"
                + "Click <strong>{4}</strong> to synchronize the conflicting primitive only.<br>"
                + "Click <strong>{5}</strong> to synchronize the entire local dataset with the server.<br>"
                + "Click <strong>{6}</strong> to abort and continue editing.<br></html>",
                tr(primitiveType), id, serverVersion, myVersion,
                options[0], options[1], options[2]
        );
        int optionsType = JOptionPane.YES_NO_CANCEL_OPTION;
        int ret = JOptionPane.showOptionDialog(
                null,
                msg,
                tr("Conflict detected"),
                optionsType,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                defaultOption
        );
        switch(ret) {
            case JOptionPane.CLOSED_OPTION: return;
            case JOptionPane.CANCEL_OPTION: return;
            case 0: synchronizePrimitive(id); break;
            case 1: synchronizeDataSet(); break;
            default:
                // should not happen
                throw new IllegalStateException(tr("unexpected return value. Got {0}", ret));
        }
    }

    /**
     * Handles the case that a conflict was detected while uploading where we don't
     * know what {@see OsmPrimitive} actually caused the conflict (for whatever reason)
     *
     */
    protected void handleUploadConflictForUnknownConflict() {
        Object[] options = new Object[] {
                tr("Synchronize entire dataset"),
                tr("Cancel")
        };
        Object defaultOption = options[0];
        String msg =  tr("<html>Uploading <strong>failed</strong> because the server has a newer version of one<br>"
                + "of your nodes, ways, or relations.<br>"
                + "<br>"
                + "Click <strong>{0}</strong> to synchronize the entire local dataset with the server.<br>"
                + "Click <strong>{1}</strong> to abort and continue editing.<br></html>",
                options[0], options[1]
        );
        int optionsType = JOptionPane.YES_NO_OPTION;
        int ret = JOptionPane.showOptionDialog(
                null,
                msg,
                tr("Conflict detected"),
                optionsType,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                defaultOption
        );
        switch(ret) {
            case JOptionPane.CLOSED_OPTION: return;
            case 1: return;
            case 0: synchronizeDataSet(); break;
            default:
                // should not happen
                throw new IllegalStateException(tr("unexpected return value. Got {0}", ret));
        }
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
            handleUploadConflictForKnownConflict(m.group(3), m.group(4), m.group(2),m.group(1));
        } else {
            logger.warning(tr("Warning: error header \"{0}\" did not match expected pattern \"{1}\"", e.getErrorHeader(),pattern));
            handleUploadConflictForUnknownConflict();
        }
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
    protected void handleGoneForKnownPrimitive(String primitiveType, String id) {
        UpdateSelectionAction act = new UpdateSelectionAction();
        act.handlePrimitiveGoneException(Long.parseLong(id));
    }

    /**
     * Handles an error which is caused by a delete request for an already deleted
     * {@see OsmPrimitive} on the server, i.e. a HTTP response code of 410.
     * Note that an <strong>update</strong> on an already deleted object results
     * in a 409, not a 410.
     *
     * @param e the exception
     */
    protected void handleGone(OsmApiException e) {
        String pattern = "The (\\S+) with the id (\\d+) has already been deleted";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(e.getErrorHeader());
        if (m.matches()) {
            handleGoneForKnownPrimitive(m.group(1), m.group(2));
        } else {
            logger.warning(tr("Error header \"{0}\" does not match expected pattern \"{1}\"",e.getErrorHeader(), pattern));
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
                ExceptionDialogUtil.explainPreconditionFailed(ex);
                return;
            }
            // Tried to delete an already deleted primitive? Let the user
            // decide whether and how to resolve this conflict.
            //
            else if (ex.getResponseCode() == HttpURLConnection.HTTP_GONE) {
                handleGone(ex);
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

        public UpdatePrimitiveTask(long id) {
            super(tr("Updating primitive"),false /* don't ignore exceptions */);
            this.id = id;
        }

        @Override protected void realRun() throws SAXException, IOException {
            try {
                UpdateSelectionAction act = new UpdateSelectionAction();
                act.updatePrimitive(id);
            } catch (Exception sxe) {
                if (uploadCancelled) {
                    System.out.println("Ignoring exception caught because upload is cancelled. Exception is: " + sxe.toString());
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


    static public class UploadConfirmationHook implements UploadHook {
        static private UploadDialogPanel uploadDialogPanel;

        static public UploadDialogPanel getUploadDialogPanel() {
            if (uploadDialogPanel == null) {
                uploadDialogPanel = new UploadDialogPanel();
            }
            return uploadDialogPanel;
        }

        public boolean checkUpload(Collection<OsmPrimitive> add, Collection<OsmPrimitive> update, Collection<OsmPrimitive> delete) {
            final UploadDialogPanel panel = getUploadDialogPanel();
            panel.setUploadedPrimitives(add, update, delete);

            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Upload these changes?"),
                    new String[] {tr("Upload Changes"), tr("Cancel")}
            ) {
                @Override
                public void setVisible(boolean visible) {
                    if (visible) {
                        new WindowGeometry(
                                panel.getClass().getName(),
                                WindowGeometry.centerInWindow(JOptionPane.getFrameForComponent(Main.parent), new Dimension(400,400))
                        ).apply(this);
                        panel.startUserInput();
                    } else {
                        new WindowGeometry(this).remember(panel.getClass().getName());
                    }
                    super.setVisible(visible);
                }
            };

            dialog.setButtonIcons(new String[] {"upload.png", "cancel.png"});
            dialog.setContent(panel, false /* no scroll pane */);
            while(true) {
                dialog.showDialog();
                int result = dialog.getValue();
                // cancel pressed
                if (result != 1) return false;
                // don't allow empty commit message
                if (! panel.hasChangesetComment()) {
                    continue;
                }
                panel.rememberUserInput();
                break;
            }
            return true;
        }
    }

    public UploadDiffTask createUploadTask(OsmDataLayer layer, Collection<OsmPrimitive> toUpload, Changeset changeset, ChangesetProcessingType changesetProcessingType) {
        return new UploadDiffTask(layer, toUpload, changeset, changesetProcessingType);
    }

    public class UploadDiffTask extends  PleaseWaitRunnable {
        private boolean uploadCancelled = false;
        private Exception lastException = null;
        private Collection <OsmPrimitive> toUpload;
        private OsmServerWriter writer;
        private OsmDataLayer layer;
        private Changeset changeset;
        private ChangesetProcessingType changesetProcessingType;

        private UploadDiffTask(OsmDataLayer layer, Collection <OsmPrimitive> toUpload, Changeset changeset, ChangesetProcessingType changesetProcessingType) {
            super(tr("Uploading data for layer ''{0}''", layer.getName()),false /* don't ignore exceptions */);
            this.toUpload = toUpload;
            this.layer = layer;
            this.changeset = changeset;
            this.changesetProcessingType = changesetProcessingType == null ? ChangesetProcessingType.USE_NEW_AND_CLOSE : changesetProcessingType;
        }

        @Override protected void realRun() throws SAXException, IOException {
            writer = new OsmServerWriter();
            try {
                ProgressMonitor monitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
                writer.uploadOsm(layer.data.version, toUpload, changeset,changesetProcessingType, monitor);
            } catch (Exception sxe) {
                if (uploadCancelled) {
                    System.out.println("Ignoring exception caught because upload is cancelled. Exception is: " + sxe.toString());
                    return;
                }
                lastException = sxe;
            }
        }

        @Override protected void finish() {
            if (uploadCancelled)
                return;

            // we always clean the data, even in case of errors. It's possible the data was
            // partially uploaded
            //
            layer.cleanupAfterUpload(writer.getProcessedPrimitives());
            DataSet.fireSelectionChanged(layer.data.getSelected());
            layer.fireDataChange();
            if (lastException != null) {
                handleFailedUpload(lastException);
            } else {
                layer.onPostUploadToServer();
            }
        }

        @Override protected void cancel() {
            uploadCancelled = true;
            if (writer != null) {
                writer.cancel();
            }
        }

        public boolean isSuccessful() {
            return !isCancelled() && !isFailed();
        }

        public boolean isCancelled() {
            return uploadCancelled;
        }

        public boolean isFailed() {
            return lastException != null;
        }
    }

    /**
     * The panel displaying information about primitives to upload and providing
     * UI widgets for entering the changeset comment and other configuration
     * settings.
     * 
     */
    static public class UploadDialogPanel extends JPanel {

        private JList lstAdd;
        private JList lstUpdate;
        private JList lstDelete;
        private JLabel lblAdd;
        private JLabel lblUpdate;
        private JLabel lblDelete;
        private JPanel pnlLists;
        private JCheckBox cbUseAtomicUpload;
        private SuggestingJHistoryComboBox cmt;
        private TagEditorPanel tagEditorPanel;
        private JTabbedPane southTabbedPane;
        private ButtonGroup bgChangesetHandlingOptions;
        private Map<ChangesetProcessingType, JRadioButton> rbChangesetHandlingOptions;

        protected JPanel buildListsPanel() {
            pnlLists = new JPanel();
            pnlLists.setLayout(new GridBagLayout());

            return pnlLists;
        }

        protected JPanel buildChangesetHandlingControlPanel() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
            bgChangesetHandlingOptions = new ButtonGroup();
            rbChangesetHandlingOptions = new HashMap<ChangesetProcessingType, JRadioButton>();
            ChangesetProcessingTypeChangedAction a = new ChangesetProcessingTypeChangedAction();
            for(ChangesetProcessingType type: ChangesetProcessingType.values()) {
                rbChangesetHandlingOptions.put(type, new JRadioButton());
                rbChangesetHandlingOptions.get(type).addActionListener(a);
            }
            JRadioButton rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_CLOSE);
            rb.setText(tr("Use a new changeset and close it"));
            rb.setToolTipText(tr("Select to upload the data using a new changeset and to close the changeset after the upload"));

            rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_LEAVE_OPEN);
            rb.setText(tr("Use a new changeset and leave it open"));
            rb.setToolTipText(tr("Select to upload the data using a new changeset and to leave the changeset open after the upload"));

            pnl.add(new JLabel(tr("Upload to a new or to an existing changeset?")));
            pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_CLOSE));
            pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_LEAVE_OPEN));
            pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_CLOSE));
            pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_LEAVE_OPEN));

            for(ChangesetProcessingType type: ChangesetProcessingType.values()) {
                rbChangesetHandlingOptions.get(type).setVisible(false);
                bgChangesetHandlingOptions.add(rbChangesetHandlingOptions.get(type));
            }
            return pnl;
        }

        protected JPanel buildChangesetControlPanel() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
            pnl.add(cbUseAtomicUpload = new JCheckBox(tr("upload all changes in one request")));
            cbUseAtomicUpload.setToolTipText(tr("Enable to upload all changes in one request, disable to use one request per changed primitive"));
            boolean useAtomicUpload = Main.pref.getBoolean("osm-server.atomic-upload", true);
            cbUseAtomicUpload.setSelected(useAtomicUpload);
            cbUseAtomicUpload.setEnabled(OsmApi.getOsmApi().hasSupportForDiffUploads());

            pnl.add(buildChangesetHandlingControlPanel());
            return pnl;
        }

        protected JPanel buildUploadControlPanel() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new GridBagLayout());
            pnl.add(new JLabel(tr("Provide a brief comment for the changes you are uploading:")), GBC.eol().insets(0, 5, 10, 3));
            cmt = new SuggestingJHistoryComboBox();
            List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
            cmt.setHistory(cmtHistory);
            pnl.add(cmt, GBC.eol().fill(GBC.HORIZONTAL));

            // configuration options for atomic upload
            //
            pnl.add(buildChangesetControlPanel(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
            return pnl;
        }

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(buildListsPanel(), gc);

            southTabbedPane = new JTabbedPane();
            southTabbedPane.add(buildUploadControlPanel());
            tagEditorPanel = new TagEditorPanel();
            southTabbedPane.add(tagEditorPanel);
            southTabbedPane.setTitleAt(0, tr("Settings"));
            southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));
            JPanel pnl = new JPanel();
            pnl.setLayout(new BorderLayout());
            pnl.add(southTabbedPane,BorderLayout.CENTER);
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.gridy = 1;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            add(pnl, gc);
        }


        protected UploadDialogPanel() {
            OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();

            lstAdd = new JList();
            lstAdd.setCellRenderer(renderer);
            lstAdd.setVisibleRowCount(Math.min(lstAdd.getModel().getSize(), 10));

            lstUpdate = new JList();
            lstUpdate.setCellRenderer(renderer);
            lstUpdate.setVisibleRowCount(Math.min(lstUpdate.getModel().getSize(), 10));

            lstDelete = new JList();
            lstDelete.setCellRenderer(renderer);
            lstDelete.setVisibleRowCount(Math.min(lstDelete.getModel().getSize(), 10));
            build();
        }

        public void setUploadedPrimitives(Collection<OsmPrimitive> add, Collection<OsmPrimitive> update, Collection<OsmPrimitive> delete) {
            lstAdd.setListData(add.toArray());
            lstUpdate.setListData(update.toArray());
            lstDelete.setListData(delete.toArray());


            GridBagConstraints gcLabel = new GridBagConstraints();
            gcLabel.fill = GridBagConstraints.HORIZONTAL;
            gcLabel.weightx = 1.0;
            gcLabel.weighty = 0.0;
            gcLabel.anchor = GridBagConstraints.FIRST_LINE_START;

            GridBagConstraints gcList = new GridBagConstraints();
            gcList.fill = GridBagConstraints.BOTH;
            gcList.weightx = 1.0;
            gcList.weighty = 1.0;
            gcList.anchor = GridBagConstraints.CENTER;
            pnlLists.removeAll();
            int y = -1;
            if (!add.isEmpty()) {
                y++;
                gcLabel.gridy = y;
                pnlLists.add(lblAdd = new JLabel(tr("Objects to add:")), gcLabel);
                y++;
                gcList.gridy = y;
                pnlLists.add(new JScrollPane(lstAdd), gcList);
            }
            if (!update.isEmpty()) {
                y++;
                gcLabel.gridy = y;
                pnlLists.add(lblUpdate = new JLabel(tr("Objects to modify:")), gcLabel);
                y++;
                gcList.gridy = y;
                pnlLists.add(new JScrollPane(lstUpdate), gcList);
            }
            if (!delete.isEmpty()) {
                y++;
                gcLabel.gridy = y;
                pnlLists.add(lblDelete = new JLabel(tr("Objects to delete:")), gcLabel);
                y++;
                gcList.gridy = y;
                pnlLists.add(new JScrollPane(lstDelete), gcList);
            }
        }

        public boolean hasChangesetComment() {
            return cmt.getText().trim().length() >= 3;
        }

        public void rememberUserInput() {
            // store the history of comments
            cmt.addCurrentItemToHistory();
            Main.pref.putCollection(HISTORY_KEY, cmt.getHistory());
            Main.pref.put("osm-server.atomic-upload", cbUseAtomicUpload.isSelected());
        }

        public void startUserInput() {
            tagEditorPanel.initAutoCompletion(Main.main.getEditLayer());
            initChangesetProcessingType();
            cmt.getEditor().selectAll();
            cmt.requestFocus();
        }

        public ChangesetProcessingType getChangesetProcessingType() {
            ChangesetProcessingType changesetProcessingType = null;
            for (ChangesetProcessingType type: ChangesetProcessingType.values()) {
                if (rbChangesetHandlingOptions.get(type).isSelected()) {
                    changesetProcessingType = type;
                    break;
                }
            }
            return changesetProcessingType == null ?
                    ChangesetProcessingType.USE_NEW_AND_CLOSE :
                        changesetProcessingType;
        }

        public Changeset getChangeset() {
            Changeset changeset = new Changeset();
            tagEditorPanel.getModel().applyToPrimitive(changeset);
            changeset.put("comment", cmt.getText());
            return changeset;
        }

        protected void initChangesetProcessingType() {
            for (ChangesetProcessingType type: ChangesetProcessingType.values()) {
                // show options for new changeset, disable others
                //
                rbChangesetHandlingOptions.get(type).setVisible(type.isUseNew());
            }
            if (OsmApi.getOsmApi().getCurrentChangeset() != null) {
                Changeset cs = OsmApi.getOsmApi().getCurrentChangeset();
                for (ChangesetProcessingType type: ChangesetProcessingType.values()) {
                    // show options for using existing changeset
                    //
                    if (!type.isUseNew()) {
                        rbChangesetHandlingOptions.get(type).setVisible(true);
                    }
                }
                JRadioButton rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_CLOSE);
                rb.setText(tr("Use the existing changeset {0} and close it after upload",cs.getId()));
                rb.setToolTipText(tr("Select to upload to the existing changeset {0} and to close the changeset after this upload",cs.getId()));

                rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_LEAVE_OPEN);
                rb.setText(tr("Use the existing changeset {0} and leave it open",cs.getId()));
                rb.setToolTipText(tr("Select to upload to the existing changeset {0} and to leave the changeset open for further uploads",cs.getId()));

                rbChangesetHandlingOptions.get(getChangesetProcessingType()).setSelected(true);

            } else {
                ChangesetProcessingType type = getChangesetProcessingType();
                if (!type.isUseNew()) {
                    type = ChangesetProcessingType.USE_NEW_AND_CLOSE;
                }
                rbChangesetHandlingOptions.get(type).setSelected(true);
            }
        }

        class ChangesetProcessingTypeChangedAction implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                ChangesetProcessingType type = getChangesetProcessingType();
                if (type.isUseNew()) {
                    tagEditorPanel.setEnabled(true);
                    southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));
                    cmt.setEnabled(true);
                } else {
                    tagEditorPanel.setEnabled(false);
                    cmt.setEnabled(false);
                    Changeset cs = OsmApi.getOsmApi().getCurrentChangeset();
                    if (cs != null) {
                        tagEditorPanel.getModel().initFromPrimitive(cs);
                        southTabbedPane.setTitleAt(1, tr("Tags of changeset {0} (read-only)", cs.getId()));
                        cmt.setText(cs.get("comment" == null ? "" : cs.get("comment")));
                    }
                }
            }
        }
    }
}
