// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This action synchronizes a set of primitives with their state on the server.
 *
 *
 */
public class UpdateSelectionAction extends JosmAction {

    /**
     * handle an exception thrown because a primitive was deleted on the server
     *
     * @param id the primitive id
     */
    protected void handlePrimitiveGoneException(long id) {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        reader.append(getCurrentDataSet(),id);
        DataSet ds = null;
        try {
            ds = reader.parseOsm(NullProgressMonitor.INSTANCE);
        } catch(Exception e) {
            handleUpdateException(e);
            return;
        }
        Main.map.mapView.getEditLayer().mergeFrom(ds);
    }


    /**
     * handle an exception thrown during updating a primitive
     *
     * @param id the id of the primitive
     * @param e the exception
     */
    protected void handleUpdateException(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("Failed to update the selected primitives."),
                tr("Update failed"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * handles an exception case: primitive with id <code>id</code> is not in the current
     * data set
     *
     * @param id the primitive id
     */
    protected void handleMissingPrimitive(long id) {
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("Could not find primitive with id {0} in the current dataset", new Long(id).toString()),
                tr("Missing primitive"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Updates the data for for the {@see OsmPrimitive}s in <code>selection</code>
     * with the data currently kept on the server.
     *
     * @param selection a collection of {@see OsmPrimitive}s to update
     *
     */
    public void updatePrimitives(final Collection<OsmPrimitive> selection) {

        /**
         * The asynchronous task for updating the data using multi fetch.
         *
         */
        class UpdatePrimitiveTask extends PleaseWaitRunnable {
            private DataSet ds;
            private boolean cancelled;
            Exception lastException;

            public UpdatePrimitiveTask() {
                super("Update primitives", false /* don't ignore exception*/);
                cancelled = false;
            }

            protected void showLastException() {
                String msg = lastException.getMessage();
                if (msg == null) {
                    msg = lastException.toString();
                }
                JOptionPane.showMessageDialog(
                        Main.map,
                        msg,
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }

            @Override
            protected void cancel() {
                cancelled = true;
                OsmApi.getOsmApi().cancel();
            }

            @Override
            protected void finish() {
                if (cancelled)
                    return;
                if (lastException != null) {
                    showLastException();
                    return;
                }
                if (ds != null) {
                    Main.map.mapView.getEditLayer().mergeFrom(ds);
                }
            }

            @Override
            protected void realRun() throws SAXException, IOException, OsmTransferException {
                progressMonitor.indeterminateSubTask("");
                try {
                    MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
                    reader.append(selection);
                    ds = reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                } catch(Exception e) {
                    if (cancelled)
                        return;
                    lastException = e;
                }
            }
        }

        Main.worker.submit(new UpdatePrimitiveTask());
    }

    /**
     * Updates the data for for the {@see OsmPrimitive}s with id <code>id</code>
     * with the data currently kept on the server.
     *
     * @param id  the id of a primitive in the {@see DataSet} of the current edit layser
     *
     */
    public void updatePrimitive(long id) {
        OsmPrimitive primitive = Main.map.mapView.getEditLayer().data.getPrimitiveById(id);
        Set<OsmPrimitive> s = new HashSet<OsmPrimitive>();
        s.add(primitive);
        updatePrimitives(s);
    }

    /**
     * constructor
     */
    public UpdateSelectionAction() {
        super(tr("Update Selection"),
                "updateselection",
                tr("Updates the currently selected primitives from the server"),
                Shortcut.registerShortcut("file:updateselection",
                        tr("Update Selection"),
                        KeyEvent.VK_U,
                        Shortcut.GROUP_HOTKEY + Shortcut.GROUPS_ALT2),
                        true);
    }

    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null
                && ! getCurrentDataSet().getSelected().isEmpty()
        );
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
                    tr("There are no selected primitives to update."),
                    tr("Selection empty"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        updatePrimitives(selection);
    }
}
