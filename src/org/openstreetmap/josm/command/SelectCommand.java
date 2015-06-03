// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Command that selects OSM primitives
 *
 * @author Landwirt
 */
public class SelectCommand extends Command {

    /** the primitives to select when executing the command */
    private final Collection<OsmPrimitive> newSelection;

    /** the selection before applying the new selection */
    private Collection<OsmPrimitive> oldSelection;

    /**
     * Constructs a new select command.
     * @param newSelection the primitives to select when executing the command.
     */
    public SelectCommand(Collection<OsmPrimitive> newSelection) {
        this.newSelection = newSelection;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
    }

    @Override
    public void undoCommand() {
        Main.map.mapView.getEditLayer().data.setSelected(oldSelection);
    }

    @Override
    public boolean executeCommand() {
        oldSelection = Main.map.mapView.getEditLayer().data.getSelected();
        Main.map.mapView.getEditLayer().data.setSelected(newSelection);
        return true;
    }

    @Override
    public String getDescriptionText() {
        int size = newSelection != null ? newSelection.size() : 0;
        return trn("Selected {0} object", "Selected {0} objects", size, size);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((newSelection == null) ? 0 : newSelection.hashCode());
        result = prime * result + ((oldSelection == null) ? 0 : oldSelection.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        SelectCommand other = (SelectCommand) obj;
        if (newSelection == null) {
            if (other.newSelection != null)
                return false;
        } else if (!newSelection.equals(other.newSelection))
            return false;
        if (oldSelection == null) {
            if (other.oldSelection != null)
                return false;
        } else if (!oldSelection.equals(other.oldSelection))
            return false;
        return true;
    }
}
