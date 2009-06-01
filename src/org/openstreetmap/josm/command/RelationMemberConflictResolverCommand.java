// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represent a command for resolving conflicts in the member lists of two
 * {@see Relation}s.
 *
 */
public class RelationMemberConflictResolverCommand extends Command {

    /** my relation */
    private final Relation my;
    /** their relation */
    private final Relation their;
    /** the list of merged nodes. This becomes the list of news of my way after the
     *  command is executed
     */
    private final List<RelationMember> mergedMembers;

    /**
     * 
     * @param my my relation
     * @param their their relation
     * @param mergedNodeList  the list of merged relation members
     */
    public RelationMemberConflictResolverCommand(Relation my, Relation their, List<RelationMember> mergedMembers) {
        this.my = my;
        this.their = their;
        this.mergedMembers = mergedMembers;
    }


    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve conflicts in member list of of relation {0}", my.id),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of 'my' way
        //
        super.executeCommand();

        // replace the list of nodes of 'my' way by the list of merged
        // nodes
        //
        my.members.clear();
        for (int i=0; i<mergedMembers.size();i++) {
            RelationMember n = mergedMembers.get(i);
            my.members.add(n);
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(my);
    }

    @Override
    public void undoCommand() {
        // restore the former state
        //
        super.undoCommand();

        // restore a conflict if necessary
        //
        if (!Main.map.conflictDialog.conflicts.containsKey(my)) {
            Main.map.conflictDialog.conflicts.put(my,their);
        }
    }
}
