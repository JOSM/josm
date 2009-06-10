// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.CreateOsmChangeVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.historycombobox.SuggestingJHistoryComboBox;
import org.openstreetmap.josm.io.DiffResultReader;
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
 * @author imi
 */
public class UploadAction extends JosmAction {

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

    public void actionPerformed(ActionEvent e) {
        if (Main.map == null) {
            JOptionPane.showMessageDialog(Main.parent,tr("Nothing to upload. Get some data first."));
            return;
        }

        if (!Main.map.conflictDialog.conflicts.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,tr("There are unresolved conflicts. You have to resolve these first."));
            Main.map.conflictDialog.action.button.setSelected(true);
            Main.map.conflictDialog.action.actionPerformed(null);
            return;
        }

        final LinkedList<OsmPrimitive> add = new LinkedList<OsmPrimitive>();
        final LinkedList<OsmPrimitive> update = new LinkedList<OsmPrimitive>();
        final LinkedList<OsmPrimitive> delete = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : Main.ds.allPrimitives()) {
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
            JOptionPane.showMessageDialog(Main.parent,tr("No changes to upload."));
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
                    server.uploadOsm(Main.ds.version, all);
                    Main.main.editLayer().cleanData(server.processed, !add.isEmpty());
                } catch (Exception sxe) {
                    if (uploadCancelled) {
                        System.out.println("Ignoring exception caught because upload is cancelled. Exception is: " + sxe.toString());
                        return;
                    }
                    System.out.println("got exception: " + sxe.toString());
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
            };

        }
        Main.worker.execute(new UploadDiffTask());
    }

    public void handleFailedUpload(Exception e) {
        if (e instanceof OsmApiInitializationException) {
            handleOsmApiInitializationException(e);
            return;
        }
        if (e instanceof OsmApiException) {
            OsmApiException ex = (OsmApiException)e;
            if(ex.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                Pattern p = Pattern.compile("Version mismatch: Provided (\\d+), server had: (\\d+) of (\\S+) (\\d+)");
                Matcher m = p.matcher(ex.getErrorHeader());
                String msg;
                if (m.matches()) {
                    msg =  tr("<html>Uploading <strong>failed</strong> because the server has a newer version of one<br>"
                            + "of your nodes, ways or relations.<br>"
                            + "The conflict is cause by the <strong>{0}</strong> with id <strong>{1}</strong>,<br>"
                            + "the server has version {2}, your version is {3}.<br>"
                            + "Please synchronize your local dataset using <br>"
                            + "<strong>File-&gt;Update Data</strong>, resolve<br>"
                            + "any conflicts and try to upload again.</html>",
                            m.group(3),m.group(4), m.group(2), m.group(1)
                    );
                } else {
                    msg =  tr("<html>Uploading failed because the server has a newer version of one<br>"
                            + "of your nodes, ways or relations.<br>"
                            + "Please synchronize your local dataset using <br>"
                            + "<strong>File-&gt;Update Data</strong>, resolve<br>"
                            + "any conflicts and try to upload again.</html>"
                    );
                }
                JOptionPane.showMessageDialog(
                        null,
                        msg,
                        tr("Upload to OSM API failed"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }
        JOptionPane.showMessageDialog(
                null,
                e.getMessage(),
                tr("Upload to OSM API failed"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    protected void handleOsmApiInitializationException(Exception e) {
        JOptionPane.showMessageDialog(
                null,
                tr(   "Failed to initialize communication with the OSM server {0}.\n"
                        + "Check the server URL in your preferences and your internet connection.",
                        Main.pref.get("osm-server.url")
                ),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
        e.printStackTrace();
    }
}
