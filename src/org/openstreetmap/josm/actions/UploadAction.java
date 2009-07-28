// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.OptionPaneUtil;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.historycombobox.SuggestingJHistoryComboBox;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.tools.GBC;
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
        uploadHooks.add(new UploadHook() {
            public boolean checkUpload(Collection<OsmPrimitive> add, Collection<OsmPrimitive> update, Collection<OsmPrimitive> delete) {

                JPanel p = new JPanel(new GridBagLayout());

                OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();

                if (!add.isEmpty()) {
                    p.add(new JLabel(tr("Objects to add:")), GBC.eol());
                    JList l = new JList(add.toArray());
                    l.setCellRenderer(renderer);
                    l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
                    p.add(new JScrollPane(l), GBC.eol().fill());
                }

                if (!update.isEmpty()) {
                    p.add(new JLabel(tr("Objects to modify:")), GBC.eol());
                    JList l = new JList(update.toArray());
                    l.setCellRenderer(renderer);
                    l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
                    p.add(new JScrollPane(l), GBC.eol().fill());
                }

                if (!delete.isEmpty()) {
                    p.add(new JLabel(tr("Objects to delete:")), GBC.eol());
                    JList l = new JList(delete.toArray());
                    l.setCellRenderer(renderer);
                    l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
                    p.add(new JScrollPane(l), GBC.eol().fill());
                }

                p.add(new JLabel(tr("Provide a brief comment for the changes you are uploading:")), GBC.eol().insets(0, 5, 10, 3));
                SuggestingJHistoryComboBox cmt = new SuggestingJHistoryComboBox();
                List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
                cmt.setHistory(cmtHistory);
                //final JTextField cmt = new JTextField(lastCommitComment);
                p.add(cmt, GBC.eol().fill(GBC.HORIZONTAL));

                while(true) {
                    int result = new ExtendedDialog(Main.parent,
                            tr("Upload these changes?"),
                            p,
                            new String[] {tr("Upload Changes"), tr("Cancel")},
                            new String[] {"upload.png", "cancel.png"}).getValue();

                    // cancel pressed
                    if (result != 1) return false;

                    // don't allow empty commit message
                    if (cmt.getText().trim().length() < 3) {
                        continue;
                    }

                    // store the history of comments
                    cmt.addCurrentItemToHistory();
                    Main.pref.putCollection(HISTORY_KEY, cmt.getHistory());

                    break;
                }
                return true;
            }
        });
    }

    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        if (Main.map == null) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("Nothing to upload. Get some data first."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        ConflictCollection conflicts = Main.map.mapView.getEditLayer().getConflicts();
        if (conflicts !=null && !conflicts.isEmpty()) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("There are unresolved conflicts. You have to resolve these first."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            Main.map.conflictDialog.action.button.setSelected(true);
            Main.map.conflictDialog.action.actionPerformed(null);
            return;
        }

        final LinkedList<OsmPrimitive> add = new LinkedList<OsmPrimitive>();
        final LinkedList<OsmPrimitive> update = new LinkedList<OsmPrimitive>();
        final LinkedList<OsmPrimitive> delete = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : getCurrentDataSet().allPrimitives()) {
            if (osm.get("josm/ignore") != null) {
                continue;
            }
            if (osm.id == 0 && !osm.deleted) {
                add.addLast(osm);
            } else if (osm.modified && !osm.deleted) {
                update.addLast(osm);
            } else if (osm.deleted && osm.id != 0) {
                delete.addFirst(osm);
            }
        }

        if (add.isEmpty() && update.isEmpty() && delete.isEmpty()) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("No changes to upload."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Call all upload hooks in sequence. The upload confirmation dialog
        // is one of these.
        for(UploadHook hook : uploadHooks)
            if(!hook.checkUpload(add, update, delete))
                return;

        final OsmServerWriter server = new OsmServerWriter();
        final Collection<OsmPrimitive> all = new LinkedList<OsmPrimitive>();
        all.addAll(add);
        all.addAll(update);
        all.addAll(delete);

        class UploadDiffTask extends  PleaseWaitRunnable {

            private boolean uploadCancelled = false;
            private boolean uploadFailed = false;
            private Exception lastException = null;

            public UploadDiffTask() {
                super(tr("Uploading"),false /* don't ignore exceptions */);
            }

            @Override protected void realRun() throws SAXException, IOException {
                try {
                    server.uploadOsm(getCurrentDataSet().version, all, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    getEditLayer().cleanData(server.processed, !add.isEmpty());
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
                server.disconnectActiveConnection();
                uploadCancelled = true;
            }
        }

        Main.worker.execute(new UploadDiffTask());
    }

    /**
     * Synchronizes the local state of an {@see OsmPrimitive} with its state on the
     * server. The method uses an individual GET for the primitive.
     *
     * @param id the primitive ID
     */
    protected void synchronizePrimitive(final String id) {

        /**
         * The asynchronous task to update a a specific id
         *
         */
        class UpdatePrimitiveTask extends  PleaseWaitRunnable {

            private boolean uploadCancelled = false;
            private boolean uploadFailed = false;
            private Exception lastException = null;

            public UpdatePrimitiveTask() {
                super(tr("Updating primitive"),false /* don't ignore exceptions */);
            }

            @Override protected void realRun() throws SAXException, IOException {
                try {
                    UpdateSelectionAction act = new UpdateSelectionAction();
                    act.updatePrimitive(Long.parseLong(id));
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

        Main.worker.execute(new UpdatePrimitiveTask());
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
        int ret = OptionPaneUtil.showOptionDialog(
                null,
                msg,
                tr("Conflict detected"),
                optionsType,
                JOptionPane.ERROR_MESSAGE,
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
        int ret = OptionPaneUtil.showOptionDialog(
                null,
                msg,
                tr("Conflict detected"),
                optionsType,
                JOptionPane.ERROR_MESSAGE,
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
     * Handles an upload error due to a violated precondition, i.e. a HTTP return code 412
     *
     * @param e the exception
     */
    protected void handlePreconditionFailed(OsmApiException e) {
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                tr("<html>Uploading to the server <strong>failed</strong> because your current<br>"
                        +"dataset violates a precondition.<br>"
                        +"The error message is:<br>"
                        + "{0}"
                        + "</html>",
                        e.getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                ),
                tr("Precondition violation"),
                JOptionPane.ERROR_MESSAGE
        );
        e.printStackTrace();
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
     * handles the case of an error due to a delete request on an already deleted
     * {@see OsmPrimitive}, i.e. a HTTP response code 410, where we don't know which
     * {@see OsmPrimitive} is causing the error.
     *
     * @param e the exception
     */
    protected void handleGoneForUnknownPrimitive(OsmApiException e) {
        String msg =  tr("<html>Uploading <strong>failed</strong> because a primitive you tried to<br>"
                + "delete on the server is already deleted.<br>"
                + "<br>"
                + "The error message is:<br>"
                + "{0}"
                + "</html>",
                e.getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        );
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                msg,
                tr("Primitive already deleted"),
                JOptionPane.ERROR_MESSAGE
        );

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
            handleGoneForUnknownPrimitive(e);
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
            handleOsmApiInitializationException((OsmApiInitializationException)e);
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
                OptionPaneUtil.showMessageDialog(
                        Main.map,
                        msg,
                        tr("Upload to OSM API failed"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        // For any other exception just notify the user
        //
        String msg = e.getMessage();
        if (msg == null) {
            msg = e.toString();
        }
        e.printStackTrace();
        OptionPaneUtil.showMessageDialog(
                Main.map,
                msg,
                tr("Upload to OSM API failed"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    protected void handleOsmApiInitializationException(OsmApiInitializationException e) {
        OptionPaneUtil.showMessageDialog(
                Main.parent,
                tr(   "Failed to initialize communication with the OSM server {0}.\n"
                        + "Check the server URL in your preferences and your internet connection.",
                        Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api")
                ),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
        e.printStackTrace();
    }
}
