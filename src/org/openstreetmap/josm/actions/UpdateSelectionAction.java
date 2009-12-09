// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.CheckParameterUtil.ensureParameterNotNull;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.io.UpdatePrimitivesTask;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.tools.Shortcut;

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
    public void handlePrimitiveGoneException(long id, OsmPrimitiveType type) {
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
        UpdatePrimitivesTask task = new UpdatePrimitivesTask(Main.main.getEditLayer(),selection);
        Main.worker.submit(task);
    }

    /**
     * Updates the data for  the {@see OsmPrimitive}s with id <code>id</code>
     * with the data currently kept on the server.
     *
     * @param id  the id of a primitive in the {@see DataSet} of the current edit layer. Must not be null.
     * @throws IllegalArgumentException thrown if id is null
     * @exception IllegalStateException thrown if there is no primitive with <code>id</code> in
     *   the current dataset
     * @exception IllegalStateException thrown if there is no current dataset
     *
     */
    public void updatePrimitive(PrimitiveId id) throws IllegalStateException, IllegalArgumentException{
        ensureParameterNotNull(id, "id");
        if (getEditLayer() == null)
            throw new IllegalStateException(tr("No current dataset found"));
        OsmPrimitive primitive = getEditLayer().data.getPrimitiveById(id);
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
}
