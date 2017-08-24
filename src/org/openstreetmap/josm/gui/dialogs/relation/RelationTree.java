// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dialog;
import java.io.IOException;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * This is a {@link JTree} rendering the hierarchical structure of {@link Relation}s.
 *
 * @see RelationTreeModel
 */
public class RelationTree extends JTree {
    /**
     * builds the UI
     */
    protected void build() {
        setRootVisible(false);
        setCellRenderer(new RelationTreeCellRenderer());
        addTreeWillExpandListener(new LazyRelationLoader());
    }

    /**
     * constructor
     */
    public RelationTree() {
        super();
        build();
    }

    /**
     * constructor
     * @param model the tree model
     */
    public RelationTree(RelationTreeModel model) {
        super(model);
        build();
    }

    /**
     * replies the parent dialog this tree is embedded in.
     *
     * @return the parent dialog; null, if there is no parent dialog
     */
    protected Dialog getParentDialog() {
        Component c = this;
        while (c != null && !(c instanceof Dialog)) {
            c = c.getParent();
        }
        return (Dialog) c;
    }

    /**
     * An adapter for TreeWillExpand-events. If a node is to be expanded which is
     * not loaded yet this will trigger asynchronous loading of the respective
     * relation.
     *
     */
    class LazyRelationLoader implements TreeWillExpandListener {

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            // do nothing
        }

        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            TreePath path = event.getPath();
            Relation parent = (Relation) event.getPath().getLastPathComponent();
            if (!parent.isIncomplete() || parent.isNew())
                // we don't load complete  or new relations
                return;
            // launch the download task
            MainApplication.worker.submit(new RelationLoader(getParentDialog(), parent, path));
        }
    }

    /**
     * Asynchronous download task for a specific relation
     *
     */
    class RelationLoader extends PleaseWaitRunnable {
        private boolean canceled;
        private Exception lastException;
        private final Relation relation;
        private DataSet ds;
        private final TreePath path;

        RelationLoader(Dialog dialog, Relation relation, TreePath path) {
            super(
                    tr("Load relation"),
                    new PleaseWaitProgressMonitor(
                            dialog
                    ),
                    false /* don't ignore exceptions */
            );
            this.relation = relation;
            this.path = path;
        }

        @Override
        protected void cancel() {
            OsmApi.getOsmApi().cancel();
            this.canceled = true;
        }

        @Override
        protected void finish() {
            if (canceled)
                return;
            if (lastException != null) {
                Logging.error(lastException);
                return;
            }
            DataSetMerger visitor = new DataSetMerger(MainApplication.getLayerManager().getEditLayer().data, ds);
            visitor.merge();
            if (!visitor.getConflicts().isEmpty()) {
                MainApplication.getLayerManager().getEditLayer().getConflicts().add(visitor.getConflicts());
            }
            final RelationTreeModel model = (RelationTreeModel) getModel();
            SwingUtilities.invokeLater(() -> model.refreshNode(path));
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            try {
                OsmServerObjectReader reader = new OsmServerObjectReader(relation.getId(), OsmPrimitiveType.from(relation), true);
                ds = reader.parseOsm(progressMonitor
                        .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            } catch (OsmTransferException e) {
                if (canceled) {
                    Logging.warn(tr("Ignoring exception because task was canceled. Exception: {0}", e.toString()));
                    return;
                }
                this.lastException = e;
            }
        }
    }
}
