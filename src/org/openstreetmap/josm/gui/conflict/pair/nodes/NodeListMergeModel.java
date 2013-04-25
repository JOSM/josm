// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.WayNodesConflictResolverCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.ListRole;

public class NodeListMergeModel extends ListMergeModel<Node>{

    /**
     * Populates the model with the nodes in the two {@link Way}s <code>my</code> and
     * <code>their</code>.
     *
     * @param my  my way (i.e. the way in the local dataset)
     * @param their their way (i.e. the way in the server dataset)
     * @param mergedMap The map of merged primitives if the conflict results from merging two layers
     * @exception IllegalArgumentException thrown, if my is null
     * @exception IllegalArgumentException  thrown, if their is null
     */
    public void populate(Way my, Way their, Map<PrimitiveId, PrimitiveId> mergedMap) {
        initPopulate(my, their, mergedMap);

        for (Node n : my.getNodes()) {
            getMyEntries().add(n);
        }
        for (Node n : their.getNodes()) {
            getTheirEntries().add(n);
        }
        if (myAndTheirEntriesEqual()) {
            entries.put(ListRole.MERGED_ENTRIES, new ArrayList<Node>(getMyEntries()));
            setFrozen(true);
        } else {
            setFrozen(false);
        }

        fireModelDataChanged();
    }

    /**
     * Builds the command to resolve conflicts in the node list of a way
     *
     * @param conflict the conflict data set
     * @return the command
     * @exception IllegalStateException thrown, if the merge is not yet frozen
     */
    public WayNodesConflictResolverCommand buildResolveCommand(Conflict<? extends OsmPrimitive> conflict) {
        if (! isFrozen())
            throw new IllegalArgumentException(tr("Merged nodes not frozen yet. Cannot build resolution command."));
        return new WayNodesConflictResolverCommand(conflict, getMergedEntries());
    }

    @Override
    public boolean isEqualEntry(Node e1, Node e2) {
        if (!e1.isNew())
            return e1.getId() == e2.getId();
        else
            return e1 == e2;
    }

    @Override
    protected void setValueAt(DefaultTableModel model, Object value, int row, int col) {
        // do nothing - node list tables are not editable
    }

    @Override
    protected Node cloneEntryForMergedList(Node entry) {
        return (Node) getMyPrimitive(entry);
    }
}
