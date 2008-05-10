// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.*;

import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 * Command that adds a relation to an OSM object
 *
 * @author daveh
 */
public class RemoveRelationMemberCommand extends Command {

    // container object in which to replace a sub object
    private final Relation relation;
    // the sub-object to be replaced
    private final RelationMember member;
    // its replacement
    private int location = -1;

    public RemoveRelationMemberCommand(Relation _relation, RelationMember _member) {
        this.relation = _relation;
        this.member = _member;
    }
    public RemoveRelationMemberCommand(Relation _relation, RelationMember _member, int _location) {
        this.relation = _relation;
        this.member = _member;
        location = _location;
    }

    @Override public boolean executeCommand() {
        super.executeCommand();
        int removed_at = relation.members.indexOf(member);
        relation.members.remove(member);
        if ((location != -1) && (removed_at != location)) {
            relation.members.add(removed_at, member);
            Main.debug("error removing relation member");
            return false;
        }
        relation.modified = true;
        return true;
    }

    @Override public void undoCommand() {
        super.undoCommand();
        relation.members.add(member);
        relation.modified = this.getOrig(relation).modified;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {}

    @Override public MutableTreeNode description() {
        NameVisitor v = new NameVisitor();
        relation.visit(v);
        return new DefaultMutableTreeNode(new JLabel(tr("RemoveRelationMember")+" "+tr(v.className)+" "+v.name, v.icon, JLabel.HORIZONTAL));
    }
}
