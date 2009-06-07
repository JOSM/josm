// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a the resolution of a conflict between the coordinates of two {@see Node}s
 *
 */
public class CoordinateConflictResolveCommand extends Command {

    /** my node (in the local dataset). merge decisions are applied to this
     *  node
     */
    private final Node my;
    /** their node (in the server dataset) */
    private final Node their;

    /** the merge decision */
    private final MergeDecisionType decision;


    /**
     * replies a (localized) display name for the type of an OSM primitive
     * 
     * @param primitive the primitive
     * @return a localized display name
     */
    protected String getPrimitiveTypeAsString(OsmPrimitive primitive) {
        if (primitive instanceof Node) return tr("node");
        if (primitive instanceof Way) return tr("way");
        if (primitive instanceof Relation) return tr("relation");
        return "";
    }

    /**
     * constructor
     * 
     * @param my  my node
     * @param their  their node
     * @param decision the merge decision
     */
    public CoordinateConflictResolveCommand(Node my, Node their, MergeDecisionType decision) {
        this.my = my;
        this.their = their;
        this.decision = decision;
    }


    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve conflicts in coordinates in {0}",my.id),
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
            my.setCoor(their.getCoor());
        } else
            // should not happen
            throw new IllegalStateException(tr("cannot resolve undecided conflict"));

        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(my);
    }

    @Override
    public void undoCommand() {
        // restore former state of modified primitives
        //
        super.undoCommand();

        // restore a conflict if necessary
        //
        if (!Main.map.conflictDialog.conflicts.containsKey(my)) {
            Main.map.conflictDialog.addConflict(my, their);
        }
    }
}
