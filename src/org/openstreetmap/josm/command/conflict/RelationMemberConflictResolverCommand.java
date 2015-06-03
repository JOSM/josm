// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;

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
        if (!Main.map.mapView.hasLayer(layer)) {
            Main.warn(tr("Cannot undo command ''{0}'' because layer ''{1}'' is not present any more",
                    this.toString(),
                    layer.toString()
            ));
            return;
        }

        Main.map.mapView.setActiveLayer(layer);
        OsmDataLayer editLayer = Main.main.getEditLayer();

        // restore the former state
        //
        super.undoCommand();

        // restore a conflict if necessary
        //
        if (!editLayer.getConflicts().hasConflictForMy(my)) {
            editLayer.getConflicts().add(my,their);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((mergedMembers == null) ? 0 : mergedMembers.hashCode());
        result = prime * result + ((my == null) ? 0 : my.hashCode());
        result = prime * result + ((their == null) ? 0 : their.hashCode());
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
        RelationMemberConflictResolverCommand other = (RelationMemberConflictResolverCommand) obj;
        if (mergedMembers == null) {
            if (other.mergedMembers != null)
                return false;
        } else if (!mergedMembers.equals(other.mergedMembers))
            return false;
        if (my == null) {
            if (other.my != null)
                return false;
        } else if (!my.equals(other.my))
            return false;
        if (their == null) {
            if (other.their != null)
                return false;
        } else if (!their.equals(other.their))
            return false;
        return true;
    }
}
