// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This action synchronizes a set of primitives with their state on the server.
 *
 */
public class UpdateSelectionAction extends JosmAction {

    /**
     * handle an exception thrown because a primitive was deleted on the server
     *
     * @param id the primitive id
     */
    protected void handlePrimitiveGoneException(long id, OsmPrimitiveType type) {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        reader.append(getCurrentDataSet(),id, type);
        DataSet ds = null;
        try {
            ds = reader.parseOsm(NullProgressMonitor.INSTANCE);
        } catch(Exception e) {
            ExceptionDialogUtil.explainException(e);
        }
        Main.map.mapView.getEditLayer().mergeFrom(ds);
    }

    /**
     * Updates the data for for the {@see OsmPrimitive}s in <code>selection</code>
     * with the data currently kept on the server.
     *
     * @param selection a collection of {@see OsmPrimitive}s to update
     *
     */
    public void updatePrimitives(final Collection<OsmPrimitive> selection) {
        UpdatePrimitivesTask task = new UpdatePrimitivesTask(selection);
        Main.worker.submit(task);
    }

    /**
     * Updates the data for  the {@see OsmPrimitive}s with id <code>id</code>
     * with the data currently kept on the server.
     *
     * @param id  the id of a primitive in the {@see DataSet} of the current edit layer
     * @exception IllegalStateException thrown if there is no primitive with <code>id</code> in
     *   the current dataset
     * @exception IllegalStateException thrown if there is no current dataset
     *
     */
    public void updatePrimitive(OsmPrimitiveType type, long id) throws IllegalStateException{
        if (getEditLayer() == null)
            throw new IllegalStateException(tr("No current dataset found"));
        OsmPrimitive primitive = getEditLayer().data.getPrimitiveById(id, type);
        if (primitive == null)
            throw new IllegalStateException(tr("Didn''t find an object with id {0} in the current dataset", id));
        updatePrimitives(Collections.singleton(primitive));
    }

    /**
     * constructor
     */
    public UpdateSelectionAction() {
        super(tr("Update selection"),
                "updateselection",
                tr("Updates the currently selected objects from the server (re-downloads data)"),
                Shortcut.registerShortcut("file:updateselection",
                        tr("Update Selection"),
                        KeyEvent.VK_U,
                        Shortcut.GROUP_HOTKEY + Shortcut.GROUPS_ALT2),
                        true);
        putValue("help", ht("UpdateSelection"));
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    /**
     * action handler
     */
    public void actionPerformed(ActionEvent e) {
        if (! isEnabled())
            return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        if (selection.size() == 0) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("There are no selected objects to update."),
                    tr("Selection empty"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        updatePrimitives(selection);
    }

    /**
     * The asynchronous task for updating the data using multi fetch.
     *
     */
    static class UpdatePrimitivesTask extends PleaseWaitRunnable {
        static private final Logger logger = Logger.getLogger(UpdatePrimitivesTask.class.getName());

        private DataSet ds;
        private boolean canceled;
        private Exception lastException;
        private Collection<? extends OsmPrimitive> toUpdate;
        private MultiFetchServerObjectReader reader;

        /**
         *
         * @param toUpdate a collection of primitives to update from the server
         */
        public UpdatePrimitivesTask(Collection<? extends OsmPrimitive> toUpdate) {
            super(tr("Update objects"), false /* don't ignore exception*/);
            canceled = false;
            this.toUpdate = toUpdate;
        }

        @Override
        protected void cancel() {
            canceled = true;
            if (reader != null) {
                reader.cancel();
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
            if (ds != null && Main.main.getEditLayer() != null) {
                Main.main.getEditLayer().mergeFrom(ds);
                Main.main.getEditLayer().onPostDownloadFromServer();
            }
        }

        protected void initMultiFetchReaderWithNodes(MultiFetchServerObjectReader reader) {
            for (OsmPrimitive primitive : toUpdate) {
                if (primitive instanceof Node && !primitive.isNew()) {
                    reader.append((Node)primitive);
                } else if (primitive instanceof Way) {
                    Way way = (Way)primitive;
                    for (Node node: way.getNodes()) {
                        if (!node.isNew()) {
                            reader.append(node);
                        }
                    }
                }
            }
        }

        protected void initMultiFetchReaderWithWays(MultiFetchServerObjectReader reader) {
            for (OsmPrimitive primitive : toUpdate) {
                if (primitive instanceof Way && !primitive.isNew()) {
                    reader.append((Way)primitive);
                }
            }
        }

        protected void initMultiFetchReaderWithRelations(MultiFetchServerObjectReader reader) {
            for (OsmPrimitive primitive : toUpdate) {
                if (primitive instanceof Relation && !primitive.isNew()) {
                    reader.append((Relation)primitive);
                }
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            progressMonitor.indeterminateSubTask("");
            this.ds = new DataSet();
            DataSet theirDataSet;
            try {
                reader = new MultiFetchServerObjectReader();
                initMultiFetchReaderWithNodes(reader);
                initMultiFetchReaderWithWays(reader);
                initMultiFetchReaderWithRelations(reader);
                theirDataSet = reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                DataSetMerger merger = new DataSetMerger(ds, theirDataSet);
                merger.merge();
                // a way loaded with MultiFetch may be incomplete because at least one of its
                // nodes isn't present in the local data set. We therefore fully load all
                // incomplete ways.
                //
                for (Way w : ds.getWays()) {
                    if (w.incomplete) {
                        OsmServerObjectReader reader = new OsmServerObjectReader(w.getId(), OsmPrimitiveType.WAY, true /* full */);
                        theirDataSet = reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                        merger = new DataSetMerger(ds, theirDataSet);
                        merger.merge();
                    }
                }
            } catch(Exception e) {
                if (canceled)
                    return;
                lastException = e;
            }
        }
    }
}
