// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.WayNodesConflictResolverCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.ListRole;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class NodeListMergeModel extends ListMergeModel<Node>{
    //private static final Logger logger = Logger.getLogger(NodeListMergeModel.class.getName());

    private DataSet myDataset;

    /**
     * Populates the model with the nodes in the two {@see Way}s <code>my</code> and
     * <code>their</code>.
     *
     * @param my  my way (i.e. the way in the local dataset)
     * @param their their way (i.e. the way in the server dataset)
     * @exception IllegalArgumentException thrown, if my is null
     * @exception IllegalArgumentException  thrown, if their is null
     */
    public void populate(Way my, Way their) {
        this.myDataset = my.getDataSet();

        CheckParameterUtil.ensureParameterNotNull(my, "my");
        CheckParameterUtil.ensureParameterNotNull(their, "their");
        getMergedEntries().clear();
        getMyEntries().clear();
        getTheirEntries().clear();
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
     * @param my  my way. Must not be null.
     * @param their  their way. Must not be null
     * @return the command
     * @exception IllegalArgumentException thrown, if my is null or not a {@see Way}
     * @exception IllegalArgumentException thrown, if their is null or not a {@see Way}
     * @exception IllegalStateException thrown, if the merge is not yet frozen
     */
    public WayNodesConflictResolverCommand buildResolveCommand(Way my, Way their) {
        CheckParameterUtil.ensureParameterNotNull(my, "my");
        CheckParameterUtil.ensureParameterNotNull(their, "their");
        if (! isFrozen())
            throw new IllegalArgumentException(tr("Merged nodes not frozen yet. Cannot build resolution command."));
        return new WayNodesConflictResolverCommand(my, their, getMergedEntries());
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
        Node node = (Node) myDataset.getPrimitiveById(entry);
        if (node.isDeleted()) {
            JOptionPane.showMessageDialog(null, tr("Node {0} cannot be added to the way because it was deleted", node.getDisplayName(DefaultNameFormatter.getInstance())));
            return null;
        } else
            return node;
    }
}
