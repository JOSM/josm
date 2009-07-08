// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.PurgePrimitivesCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action synchronizes a set of primitives with their state on the server.
 * 
 *
 */
public class UpdateSelectionAction extends JosmAction {

    static public int DEFAULT_MAX_SIZE_UPDATE_SELECTION = 50;

    /**
     * handle an exception thrown because a primitive was deleted on the server
     * 
     * @param id the primitive id
     */
    protected void handlePrimitiveGoneException(long id) {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        reader.append(Main.main.createOrGetEditLayer().data,id);
        DataSet ds = null;
        try {
            ds = reader.parseOsm();
        } catch(Exception e) {
            handleUpdateException(e);
            return;
        }
        Main.main.createOrGetEditLayer().mergeFrom(ds);
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
     * 
     * @param id
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
     * 
     * 
     * 
     */
    public void updatePrimitives(Collection<OsmPrimitive> selection) {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        reader.append(selection);
        DataSet ds = null;
        try {
            ds = reader.parseOsm();
        } catch(Exception e) {
            handleUpdateException(e);
            return;
        }
        Main.main.createOrGetEditLayer().mergeFrom(ds);
    }

    public void updatePrimitive(long id) {
        OsmPrimitive primitive = Main.main.createOrGetEditLayer().data.getPrimitiveById(id);
        Set<OsmPrimitive> s = new HashSet<OsmPrimitive>();
        s.add(primitive);
        updatePrimitives(s);
    }

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


    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = Main.ds.getSelected();
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
