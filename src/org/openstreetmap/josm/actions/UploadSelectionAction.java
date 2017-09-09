// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.UploadSelectionDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmServerBackreferenceReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * Uploads the current selection to the server.
 * @since 2250
 */
public class UploadSelectionAction extends JosmAction {
    /**
     * Constructs a new {@code UploadSelectionAction}.
     */
    public UploadSelectionAction() {
        super(
                tr("Upload selection"),
                "uploadselection",
                tr("Upload all changes in the current selection to the OSM server."),
                // CHECKSTYLE.OFF: LineLength
                Shortcut.registerShortcut("file:uploadSelection", tr("File: {0}", tr("Upload selection")), KeyEvent.VK_U, Shortcut.ALT_CTRL_SHIFT),
                // CHECKSTYLE.ON: LineLength
                true);
        putValue("help", ht("/Action/UploadSelection"));
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    protected Set<OsmPrimitive> getDeletedPrimitives(DataSet ds) {
        Set<OsmPrimitive> ret = new HashSet<>();
        for (OsmPrimitive p: ds.allPrimitives()) {
            if (p.isDeleted() && !p.isNew() && p.isVisible() && p.isModified()) {
                ret.add(p);
            }
        }
        return ret;
    }

    protected Set<OsmPrimitive> getModifiedPrimitives(Collection<OsmPrimitive> primitives) {
        Set<OsmPrimitive> ret = new HashSet<>();
        for (OsmPrimitive p: primitives) {
            if (p.isNewOrUndeleted() || (p.isModified() && !p.isIncomplete())) {
                ret.add(p);
            }
        }
        return ret;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        if (!isEnabled())
            return;
        if (editLayer.isUploadDiscouraged() && UploadAction.warnUploadDiscouraged(editLayer)) {
            return;
        }
        Collection<OsmPrimitive> modifiedCandidates = getModifiedPrimitives(editLayer.data.getAllSelected());
        Collection<OsmPrimitive> deletedCandidates = getDeletedPrimitives(editLayer.data);
        if (modifiedCandidates.isEmpty() && deletedCandidates.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No changes to upload."),
                    tr("Warning"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        UploadSelectionDialog dialog = new UploadSelectionDialog();
        dialog.populate(
                modifiedCandidates,
                deletedCandidates
        );
        dialog.setVisible(true);
        if (dialog.isCanceled())
            return;
        Collection<OsmPrimitive> toUpload = new UploadHullBuilder().build(dialog.getSelectedPrimitives());
        if (toUpload.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No changes to upload."),
                    tr("Warning"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        uploadPrimitives(editLayer, toUpload);
    }

    /**
     * Replies true if there is at least one non-new, deleted primitive in
     * <code>primitives</code>
     *
     * @param primitives the primitives to scan
     * @return true if there is at least one non-new, deleted primitive in
     * <code>primitives</code>
     */
    protected boolean hasPrimitivesToDelete(Collection<OsmPrimitive> primitives) {
        for (OsmPrimitive p: primitives) {
            if (p.isDeleted() && p.isModified() && !p.isNew())
                return true;
        }
        return false;
    }

    /**
     * Uploads the primitives in <code>toUpload</code> to the server. Only
     * uploads primitives which are either new, modified or deleted.
     *
     * Also checks whether <code>toUpload</code> has to be extended with
     * deleted parents in order to avoid precondition violations on the server.
     *
     * @param layer the data layer from which we upload a subset of primitives
     * @param toUpload the primitives to upload. If null or empty returns immediatelly
     */
    public void uploadPrimitives(OsmDataLayer layer, Collection<OsmPrimitive> toUpload) {
        if (toUpload == null || toUpload.isEmpty()) return;
        UploadHullBuilder builder = new UploadHullBuilder();
        toUpload = builder.build(toUpload);
        if (hasPrimitivesToDelete(toUpload)) {
            // runs the check for deleted parents and then invokes
            // processPostParentChecker()
            //
            MainApplication.worker.submit(new DeletedParentsChecker(layer, toUpload));
        } else {
            processPostParentChecker(layer, toUpload);
        }
    }

    protected void processPostParentChecker(OsmDataLayer layer, Collection<OsmPrimitive> toUpload) {
        APIDataSet ds = new APIDataSet(toUpload);
        UploadAction action = new UploadAction();
        action.uploadData(layer, ds);
    }

    /**
     * Computes the collection of primitives to upload, given a collection of candidate
     * primitives.
     * Some of the candidates are excluded, i.e. if they aren't modified.
     * Other primitives are added. A typical case is a primitive which is new and and
     * which is referred by a modified relation. In order to upload the relation the
     * new primitive has to be uploaded as well, even if it isn't included in the
     * list of candidate primitives.
     *
     */
    static class UploadHullBuilder implements OsmPrimitiveVisitor {
        private Set<OsmPrimitive> hull;

        UploadHullBuilder() {
            hull = new HashSet<>();
        }

        @Override
        public void visit(Node n) {
            if (n.isNewOrUndeleted() || n.isModified() || n.isDeleted()) {
                // upload new nodes as well as modified and deleted ones
                hull.add(n);
            }
        }

        @Override
        public void visit(Way w) {
            if (w.isNewOrUndeleted() || w.isModified() || w.isDeleted()) {
                // upload new ways as well as modified and deleted ones
                hull.add(w);
                for (Node n: w.getNodes()) {
                    // we upload modified nodes even if they aren't in the current
                    // selection.
                    n.accept(this);
                }
            }
        }

        @Override
        public void visit(Relation r) {
            if (r.isNewOrUndeleted() || r.isModified() || r.isDeleted()) {
                hull.add(r);
                for (OsmPrimitive p : r.getMemberPrimitives()) {
                    // add new relation members. Don't include modified
                    // relation members. r shouldn't refer to deleted primitives,
                    // so wont check here for deleted primitives here
                    //
                    if (p.isNewOrUndeleted()) {
                        p.accept(this);
                    }
                }
            }
        }

        /**
         * Builds the "hull" of primitives to be uploaded given a base collection
         * of osm primitives.
         *
         * @param base the base collection. Must not be null.
         * @return the "hull"
         * @throws IllegalArgumentException if base is null
         */
        public Set<OsmPrimitive> build(Collection<OsmPrimitive> base) {
            CheckParameterUtil.ensureParameterNotNull(base, "base");
            hull = new HashSet<>();
            for (OsmPrimitive p: base) {
                p.accept(this);
            }
            return hull;
        }
    }

    class DeletedParentsChecker extends PleaseWaitRunnable {
        private boolean canceled;
        private Exception lastException;
        private final Collection<OsmPrimitive> toUpload;
        private final OsmDataLayer layer;
        private OsmServerBackreferenceReader reader;

        /**
         *
         * @param layer the data layer for which a collection of selected primitives is uploaded
         * @param toUpload the collection of primitives to upload
         */
        DeletedParentsChecker(OsmDataLayer layer, Collection<OsmPrimitive> toUpload) {
            super(tr("Checking parents for deleted objects"));
            this.toUpload = toUpload;
            this.layer = layer;
        }

        @Override
        protected void cancel() {
            this.canceled = true;
            synchronized (this) {
                if (reader != null) {
                    reader.cancel();
                }
            }
        }

        @Override
        protected void finish() {
            if (canceled)
                return;
            if (lastException != null) {
                ExceptionUtil.explainException(lastException);
                return;
            }
            SwingUtilities.invokeLater(() -> processPostParentChecker(layer, toUpload));
        }

        /**
         * Replies the collection of deleted OSM primitives for which we have to check whether
         * there are dangling references on the server.
         *
         * @return primitives to check
         */
        protected Set<OsmPrimitive> getPrimitivesToCheckForParents() {
            Set<OsmPrimitive> ret = new HashSet<>();
            for (OsmPrimitive p: toUpload) {
                if (p.isDeleted() && !p.isNewOrUndeleted()) {
                    ret.add(p);
                }
            }
            return ret;
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                Stack<OsmPrimitive> toCheck = new Stack<>();
                toCheck.addAll(getPrimitivesToCheckForParents());
                Set<OsmPrimitive> checked = new HashSet<>();
                while (!toCheck.isEmpty()) {
                    if (canceled) return;
                    OsmPrimitive current = toCheck.pop();
                    synchronized (this) {
                        reader = new OsmServerBackreferenceReader(current);
                    }
                    getProgressMonitor().subTask(tr("Reading parents of ''{0}''", current.getDisplayName(DefaultNameFormatter.getInstance())));
                    DataSet ds = reader.parseOsm(getProgressMonitor().createSubTaskMonitor(1, false));
                    synchronized (this) {
                        reader = null;
                    }
                    checked.add(current);
                    getProgressMonitor().subTask(tr("Checking for deleted parents in the local dataset"));
                    for (OsmPrimitive p: ds.allPrimitives()) {
                        if (canceled) return;
                        OsmPrimitive myDeletedParent = layer.data.getPrimitiveById(p);
                        // our local dataset includes a deleted parent of a primitive we want
                        // to delete. Include this parent in the collection of uploaded primitives
                        if (myDeletedParent != null && myDeletedParent.isDeleted()) {
                            if (!toUpload.contains(myDeletedParent)) {
                                toUpload.add(myDeletedParent);
                            }
                            if (!checked.contains(myDeletedParent)) {
                                toCheck.push(myDeletedParent);
                            }
                        }
                    }
                }
            } catch (OsmTransferException e) {
                if (canceled)
                    // ignore exception
                    return;
                lastException = e;
            }
        }
    }
}
