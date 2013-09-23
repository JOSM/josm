// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * ChildRelationBrowser is a UI component which provides a tree-like view on the hierarchical
 * structure of relations
 *
 *
 */
public class ChildRelationBrowser extends JPanel {
    /** the tree with relation children */
    private RelationTree childTree;
    /**  the tree model */
    private RelationTreeModel model;

    /** the osm data layer this browser is related to */
    private OsmDataLayer layer;

    /**
     * Replies the {@link OsmDataLayer} this editor is related to
     *
     * @return the osm data layer
     */
    protected OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * builds the UI
     */
    protected void build() {
        setLayout(new BorderLayout());
        childTree = new RelationTree(model);
        JScrollPane pane = new JScrollPane(childTree);
        add(pane, BorderLayout.CENTER);

        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    /**
     * builds the panel with the command buttons
     *
     * @return the button panel
     */
    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));

        // ---
        DownloadAllChildRelationsAction downloadAction= new DownloadAllChildRelationsAction();
        pnl.add(new JButton(downloadAction));

        // ---
        DownloadSelectedAction downloadSelectedAction= new DownloadSelectedAction();
        childTree.addTreeSelectionListener(downloadSelectedAction);
        pnl.add(new JButton(downloadSelectedAction));

        // ---
        EditAction editAction = new EditAction();
        childTree.addTreeSelectionListener(editAction);
        pnl.add(new JButton(editAction));

        return pnl;
    }

    /**
     * constructor
     *
     * @param layer the {@link OsmDataLayer} this browser is related to. Must not be null.
     * @exception IllegalArgumentException thrown, if layer is null
     */
    public ChildRelationBrowser(OsmDataLayer layer) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        model = new RelationTreeModel();
        build();
    }

    /**
     * constructor
     *
     * @param layer the {@link OsmDataLayer} this browser is related to. Must not be null.
     * @param root the root relation
     * @exception IllegalArgumentException thrown, if layer is null
     */
    public ChildRelationBrowser(OsmDataLayer layer, Relation root) throws IllegalArgumentException {
        this(layer);
        populate(root);
    }

    /**
     * populates the browser with a relation
     *
     * @param r the relation
     */
    public void populate(Relation r) {
        model.populate(r);
    }

    /**
     * populates the browser with a list of relation members
     *
     * @param members the list of relation members
     */

    public void populate(List<RelationMember> members) {
        model.populate(members);
    }

    /**
     * replies the parent dialog this browser is embedded in
     *
     * @return the parent dialog; null, if there is no {@link Dialog} as parent dialog
     */
    protected Dialog getParentDialog() {
        Component c  = this;
        while(c != null && ! (c instanceof Dialog)) {
            c = c.getParent();
        }
        return (Dialog)c;
    }

    /**
     * Action for editing the currently selected relation
     *
     *
     */
    class EditAction extends AbstractAction implements TreeSelectionListener {
        public EditAction() {
            putValue(SHORT_DESCRIPTION, tr("Edit the relation the currently selected relation member refers to."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            putValue(NAME, tr("Edit"));
            refreshEnabled();
        }

        protected void refreshEnabled() {
            TreePath[] selection = childTree.getSelectionPaths();
            setEnabled(selection != null && selection.length > 0);
        }

        public void run() {
            TreePath [] selection = childTree.getSelectionPaths();
            if (selection == null || selection.length == 0) return;
            // do not launch more than 10 relation editors in parallel
            //
            for (int i=0; i < Math.min(selection.length,10);i++) {
                Relation r = (Relation)selection[i].getLastPathComponent();
                if (r.isIncomplete()) {
                    continue;
                }
                RelationEditor editor = RelationEditor.getEditor(getLayer(), r, null);
                editor.setVisible(true);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            run();
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            refreshEnabled();
        }
    }

    /**
     * Action for downloading all child relations for a given parent relation.
     * Recursively.
     */
    class DownloadAllChildRelationsAction extends AbstractAction{
        public DownloadAllChildRelationsAction() {
            putValue(SHORT_DESCRIPTION, tr("Download all child relations (recursively)"));
            putValue(SMALL_ICON, ImageProvider.get("download"));
            putValue(NAME, tr("Download All Children"));
        }

        public void run() {
            Main.worker.submit(new DownloadAllChildrenTask(getParentDialog(), (Relation)model.getRoot()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            run();
        }
    }

    /**
     * Action for downloading all selected relations
     */
    class DownloadSelectedAction extends AbstractAction implements TreeSelectionListener {
        public DownloadSelectedAction() {
            putValue(SHORT_DESCRIPTION, tr("Download selected relations"));
            // FIXME: replace with better icon
            //
            putValue(SMALL_ICON, ImageProvider.get("download"));
            putValue(NAME, tr("Download Selected Children"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            TreePath [] selection = childTree.getSelectionPaths();
            setEnabled(selection != null && selection.length > 0);
        }

        public void run() {
            TreePath [] selection = childTree.getSelectionPaths();
            if (selection == null || selection.length == 0)
                return;
            HashSet<Relation> relations = new HashSet<Relation>();
            for (TreePath aSelection : selection) {
                relations.add((Relation) aSelection.getLastPathComponent());
            }
            Main.worker.submit(new DownloadRelationSetTask(getParentDialog(),relations));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            run();
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * The asynchronous task for downloading relation members.
     *
     *
     */
    class DownloadAllChildrenTask extends PleaseWaitRunnable {
        private boolean canceled;
        private int conflictsCount;
        private Exception lastException;
        private Relation relation;
        private Stack<Relation> relationsToDownload;
        private Set<Long> downloadedRelationIds;

        public DownloadAllChildrenTask(Dialog parent, Relation r) {
            super(tr("Download relation members"), new PleaseWaitProgressMonitor(parent), false /*
             * don't
             * ignore
             * exception
             */);
            this.relation = r;
            relationsToDownload = new Stack<Relation>();
            downloadedRelationIds = new HashSet<Long>();
            relationsToDownload.push(this.relation);
        }

        @Override
        protected void cancel() {
            canceled = true;
            OsmApi.getOsmApi().cancel();
        }

        protected void refreshView(Relation relation){
            for (int i=0; i < childTree.getRowCount(); i++) {
                Relation reference = (Relation)childTree.getPathForRow(i).getLastPathComponent();
                if (reference == relation) {
                    model.refreshNode(childTree.getPathForRow(i));
                }
            }
        }

        @Override
        protected void finish() {
            if (canceled)
                return;
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
                return;
            }

            if (conflictsCount > 0) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        trn("There was {0} conflict during import.",
                                "There were {0} conflicts during import.",
                                conflictsCount, conflictsCount),
                                trn("Conflict in data", "Conflicts in data", conflictsCount),
                                JOptionPane.WARNING_MESSAGE
                );
            }
        }

        /**
         * warns the user if a relation couldn't be loaded because it was deleted on
         * the server (the server replied a HTTP code 410)
         *
         * @param r the relation
         */
        protected void warnBecauseOfDeletedRelation(Relation r) {
            String message = tr("<html>The child relation<br>"
                    + "{0}<br>"
                    + "is deleted on the server. It cannot be loaded</html>",
                    r.getDisplayName(DefaultNameFormatter.getInstance())
            );

            JOptionPane.showMessageDialog(
                    Main.parent,
                    message,
                    tr("Relation is deleted"),
                    JOptionPane.WARNING_MESSAGE
            );
        }

        /**
         * Remembers the child relations to download
         *
         * @param parent the parent relation
         */
        protected void rememberChildRelationsToDownload(Relation parent) {
            downloadedRelationIds.add(parent.getId());
            for (RelationMember member: parent.getMembers()) {
                if (member.isRelation()) {
                    Relation child = member.getRelation();
                    if (!downloadedRelationIds.contains(child.getId())) {
                        relationsToDownload.push(child);
                    }
                }
            }
        }

        /**
         * Merges the primitives in <code>ds</code> to the dataset of the
         * edit layer
         *
         * @param ds the data set
         */
        protected void mergeDataSet(DataSet ds) {
            if (ds != null) {
                final DataSetMerger visitor = new DataSetMerger(getLayer().data, ds);
                visitor.merge();
                if (!visitor.getConflicts().isEmpty()) {
                    getLayer().getConflicts().add(visitor.getConflicts());
                    conflictsCount +=  visitor.getConflicts().size();
                }
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                while(! relationsToDownload.isEmpty() && !canceled) {
                    Relation r = relationsToDownload.pop();
                    if (r.isNew()) {
                        continue;
                    }
                    rememberChildRelationsToDownload(r);
                    progressMonitor.setCustomText(tr("Downloading relation {0}", r.getDisplayName(DefaultNameFormatter.getInstance())));
                    OsmServerObjectReader reader = new OsmServerObjectReader(r.getId(), OsmPrimitiveType.RELATION,
                            true);
                    DataSet dataSet = null;
                    try {
                        dataSet = reader.parseOsm(progressMonitor
                                .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    } catch(OsmApiException e) {
                        if (e.getResponseCode() == HttpURLConnection.HTTP_GONE) {
                            warnBecauseOfDeletedRelation(r);
                            continue;
                        }
                        throw e;
                    }
                    mergeDataSet(dataSet);
                    refreshView(r);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Main.map.repaint();
                    }
                });
            } catch (Exception e) {
                if (canceled) {
                    Main.warn(tr("Ignoring exception because task was canceled. Exception: {0}", e.toString()));
                    return;
                }
                lastException = e;
            }
        }
    }

    /**
     * The asynchronous task for downloading a set of relations
     */
    class DownloadRelationSetTask extends PleaseWaitRunnable {
        private boolean canceled;
        private int conflictsCount;
        private Exception lastException;
        private Set<Relation> relations;

        public DownloadRelationSetTask(Dialog parent, Set<Relation> relations) {
            super(tr("Download relation members"), new PleaseWaitProgressMonitor(parent), false /*
             * don't
             * ignore
             * exception
             */);
            this.relations = relations;
        }

        @Override
        protected void cancel() {
            canceled = true;
            OsmApi.getOsmApi().cancel();
        }

        protected void refreshView(Relation relation){
            for (int i=0; i < childTree.getRowCount(); i++) {
                Relation reference = (Relation)childTree.getPathForRow(i).getLastPathComponent();
                if (reference == relation) {
                    model.refreshNode(childTree.getPathForRow(i));
                }
            }
        }

        @Override
        protected void finish() {
            if (canceled)
                return;
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
                return;
            }

            if (conflictsCount > 0) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        trn("There was {0} conflict during import.",
                                "There were {0} conflicts during import.",
                                conflictsCount, conflictsCount),
                                trn("Conflict in data", "Conflicts in data", conflictsCount),
                                JOptionPane.WARNING_MESSAGE
                );
            }
        }

        protected void mergeDataSet(DataSet dataSet) {
            if (dataSet != null) {
                final DataSetMerger visitor = new DataSetMerger(getLayer().data, dataSet);
                visitor.merge();
                if (!visitor.getConflicts().isEmpty()) {
                    getLayer().getConflicts().add(visitor.getConflicts());
                    conflictsCount +=  visitor.getConflicts().size();
                }
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                Iterator<Relation> it = relations.iterator();
                while(it.hasNext() && !canceled) {
                    Relation r = it.next();
                    if (r.isNew()) {
                        continue;
                    }
                    progressMonitor.setCustomText(tr("Downloading relation {0}", r.getDisplayName(DefaultNameFormatter.getInstance())));
                    OsmServerObjectReader reader = new OsmServerObjectReader(r.getId(), OsmPrimitiveType.RELATION,
                            true);
                    DataSet dataSet = reader.parseOsm(progressMonitor
                            .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    mergeDataSet(dataSet);
                    refreshView(r);
                }
            } catch (Exception e) {
                if (canceled) {
                    Main.warn(tr("Ignoring exception because task was canceled. Exception: {0}", e.toString()));
                    return;
                }
                lastException = e;
            }
        }
    }
}
