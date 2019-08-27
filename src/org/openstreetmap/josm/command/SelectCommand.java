// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.DataSet;
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
     * @param dataset The dataset the selection belongs to
     * @param newSelection the primitives to select when executing the command.
     * @since 12349
     */
    public SelectCommand(DataSet dataset, Collection<OsmPrimitive> newSelection) {
        super(dataset);
        if (newSelection == null || newSelection.isEmpty()) {
            this.newSelection = Collections.emptySet();
        } else if (newSelection.contains(null)) {
            throw new IllegalArgumentException("null primitive in selection");
        } else {
            this.newSelection = new HashSet<>(newSelection);
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        // Do nothing
    }

    @Override
    public void undoCommand() {
        ensurePrimitivesAreInDataset();

        getAffectedDataSet().setSelected(oldSelection);
    }

    @Override
    public boolean executeCommand() {
        ensurePrimitivesAreInDataset();

        oldSelection = getAffectedDataSet().getSelected();
        getAffectedDataSet().setSelected(newSelection);
        return true;
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return Collections.unmodifiableCollection(newSelection);
    }

    @Override
    public String getDescriptionText() {
        int size = newSelection != null ? newSelection.size() : 0;
        return trn("Selected {0} object", "Selected {0} objects", size, size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), newSelection, oldSelection);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        SelectCommand that = (SelectCommand) obj;
        return Objects.equals(newSelection, that.newSelection) &&
                Objects.equals(oldSelection, that.oldSelection);
    }
}
