// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a the resolution of a conflict between the coordinates of two {@see Node}s
 *
 */
public class CoordinateConflictResolveCommand extends ConflictResolveCommand {

    /** the conflict to resolve */
    private Conflict<Node> conflict;

    /** the merge decision */
    private final MergeDecisionType decision;

    /**
     * constructor
     *
     * @param my  my node
     * @param their  their node
     * @param decision the merge decision
     */
    public CoordinateConflictResolveCommand(Node my, Node their, MergeDecisionType decision) {
        this.conflict = new Conflict<Node>(my,their);
        this.decision = decision;
    }

    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve conflicts in coordinates in {0}",conflict.getMy().getId()),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of modified primitives, i.e. of
        // OSM primitive 'my'
        //
        super.executeCommand();

        if (decision.equals(MergeDecisionType.KEEP_MINE)) {
            // do nothing
        } else if (decision.equals(MergeDecisionType.KEEP_THEIR)) {
            Node my = conflict.getMy();
            Node their = conflict.getTheir();
            my.setCoor(their.getCoor());
        } else
            // should not happen
            throw new IllegalStateException(tr("Cannot resolve undecided conflict."));

        // remember the layer this command was applied to
        //
        rememberConflict(conflict);

        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }
}
