// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents the resolution of conflicts in the member list of two {@link Relation}s.
 *
 */
public class RelationMemberConflictResolverCommand extends ConflictResolveCommand {
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
     * @param mergedMembers the list of merged relation members
     */
    public RelationMemberConflictResolverCommand(Relation my, Relation their, List<RelationMember> mergedMembers) {
        this.my = my;
        this.their = their;
        this.mergedMembers = mergedMembers;
    }

    @Override
    public String getDescriptionText() {
        return tr("Resolve conflicts in member list of relation {0}", my.getId());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "object");
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of 'my' way
        //
        super.executeCommand();

        // replace the list of members of 'my' relation by the list of merged members
        //
        my.setMembers(mergedMembers);

        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(my);
    }

    @Override
    public void undoCommand() {
        OsmDataLayer layer = getLayer();
        if (!Main.getLayerManager().containsLayer(layer)) {
            Main.warn(tr("Cannot undo command ''{0}'' because layer ''{1}'' is not present any more",
                    this.toString(),
                    layer.toString()
            ));
            return;
        }

        Main.getLayerManager().setActiveLayer(layer);
        OsmDataLayer editLayer = Main.main.getEditLayer();

        // restore the former state
        //
        super.undoCommand();

        // restore a conflict if necessary
        //
        if (!editLayer.getConflicts().hasConflictForMy(my)) {
            editLayer.getConflicts().add(my, their);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), my, their, mergedMembers);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        RelationMemberConflictResolverCommand that = (RelationMemberConflictResolverCommand) obj;
        return Objects.equals(my, that.my) &&
                Objects.equals(their, that.their) &&
                Objects.equals(mergedMembers, that.mergedMembers);
    }
}
