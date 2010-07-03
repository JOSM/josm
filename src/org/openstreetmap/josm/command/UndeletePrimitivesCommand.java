// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a command for undeleting an {@see OsmPrimitive} which was deleted on the server.
 * The command remembers the former node id and sets the node id to 0. This turns
 * the node into a new node which can be uploaded to the server.
 *
 */
@Deprecated
public class UndeletePrimitivesCommand extends ConflictResolveCommand {
    //static private final Logger logger = Logger.getLogger(UndeletePrimitivesCommand.class.getName());

    /** the node to undelete */
    private final List<OsmPrimitive> toUndelete = new ArrayList<OsmPrimitive>();
    /** primitives that replaced undeleted primitives */
    private final List<OsmPrimitive> replacedPrimitives = new ArrayList<OsmPrimitive>();

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive node) {
        toUndelete.add(node);
    }

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive ... toUndelete) {
        for (int i=0; i < toUndelete.length; i++) {
            this.toUndelete.add(toUndelete[i]);
        }
    }

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(Collection<OsmPrimitive> toUndelete) {
        this.toUndelete.addAll(toUndelete);
    }

    @Override public JLabel getDescription() {
        return new JLabel(
                        trn("Undelete {0} primitive", "Undelete {0} primitives", toUndelete.size(), toUndelete.size()),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
        );
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();

        replacedPrimitives.clear();
        for(OsmPrimitive primitive: toUndelete) {
            if(getLayer().getConflicts().hasConflictForMy(primitive)) {
                rememberConflict(getLayer().getConflicts().getConflictForMy(primitive));
                getLayer().getConflicts().remove(primitive);
            }
            OsmPrimitive prim;
            switch (primitive.getType()) {
            case NODE:
                prim = new Node((Node)primitive, true);
                break;
            case WAY:
                prim = new Way((Way)primitive, true);
                break;
            case RELATION:
                prim = new Relation((Relation)primitive, true);
                break;
            default:
                throw new AssertionError();
            }
            replacedPrimitives.add(prim);
            replacePrimitive(getLayer().data, primitive, prim);
        }
        return true;
    }

    private void replacePrimitive(DataSet dataSet, OsmPrimitive oldPrim, OsmPrimitive newPrim) {
        dataSet.addPrimitive(newPrim);
        for (OsmPrimitive referrer: oldPrim.getReferrers()) {
            if (referrer instanceof Way) {
                Way w = (Way)referrer;
                List<Node> nodes = w.getNodes();
                Collections.replaceAll(nodes, (Node)oldPrim, (Node)newPrim);
                w.setNodes(nodes);
                w.setModified(true);
            } else if (referrer instanceof Relation) {
                Relation r = (Relation)referrer;
                List<RelationMember> members = r.getMembers();
                ListIterator<RelationMember> it = members.listIterator();
                while (it.hasNext()) {
                    RelationMember rm = it.next();
                    if (rm.getMember() == oldPrim) {
                        it.set(new RelationMember(rm.getRole(), newPrim));
                    }
                }
                r.setMembers(members);
                r.setModified(true);
            }
        }
        dataSet.removePrimitive(oldPrim);
    }

    @Override
    public void undoCommand() {
        for (int i=0; i<toUndelete.size(); i++) {
            replacePrimitive(getLayer().data, replacedPrimitives.get(i), toUndelete.get(i));
        }
        super.undoCommand();
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
    }
}
