// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a the resolution of a conflict between the coordinates of two {@see Node}s
 *
 */
public class DeletedStateConflictResolveCommand extends ConflictResolveCommand {

    /** the conflict to resolve */
    private Conflict<OsmPrimitive> conflict;

    /** the merge decision */
    private final MergeDecisionType decision;

    /**
     * constructor
     *
     * @param my  my node
     * @param their  their node
     * @param decision the merge decision
     */
    public DeletedStateConflictResolveCommand(OsmPrimitive my, OsmPrimitive their, MergeDecisionType decision) {
        this.conflict = new Conflict<OsmPrimitive>(my, their);
        this.decision = decision;
    }

    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve conflicts in deleted state in {0}",conflict.getMy().getId()),
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

        OsmDataLayer layer = getLayer();

        if (decision.equals(MergeDecisionType.KEEP_MINE)) {
            if (conflict.getMy().isDeleted()) {
                // because my was involved in a conflict it my still be referred
                // to from a way or a relation. Fix this now.
                //
                layer.data.unlinkReferencesToPrimitive(conflict.getMy());
            }
        } else if (decision.equals(MergeDecisionType.KEEP_THEIR)) {
            if (conflict.getTheir().isDeleted()) {
                layer.data.unlinkReferencesToPrimitive(conflict.getMy());
                conflict.getMy().setDeleted(true);
            } else {
                conflict.getMy().setDeleted(conflict.getTheir().isDeleted());
            }
        } else
            // should not happen
            throw new IllegalStateException(tr("Cannot resolve undecided conflict."));

        rememberConflict(conflict);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }
}
