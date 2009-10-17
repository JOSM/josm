// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command to delete a number of primitives from the dataset.
 * @author imi
 */
public class DeleteCommand extends Command {
    /**
     * The primitives that get deleted.
     */
    private final Collection<? extends OsmPrimitive> toDelete;

    /**
     * Constructor for a collection of data
     */
    public DeleteCommand(Collection<? extends OsmPrimitive> data) {
        super();
        this.toDelete = data;
    }

    /**
     * Constructor for a single data item. Use the collection constructor to delete multiple
     * objects.
     */
    public DeleteCommand(OsmPrimitive data) {
        this.toDelete = Collections.singleton(data);
    }

    /**
     * Constructor for a single data item. Use the collection constructor to delete multiple
     * objects.
     *
     * @param layer the layer context for deleting this primitive
     * @param data the primitive to delete
     */
    public DeleteCommand(OsmDataLayer layer, OsmPrimitive data) {
        super(layer);
        this.toDelete = Collections.singleton(data);
    }

    /**
     * Constructor for a collection of data to be deleted in the context of
     * a specific layer
     *
     * @param layer the layer context for deleting these primitives
     * @param data the primitives to delete
     */
    public DeleteCommand(OsmDataLayer layer, Collection<? extends OsmPrimitive> data) {
        super(layer);
        this.toDelete = data;
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        for (OsmPrimitive osm : toDelete) {
            osm.setDeleted(true);
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        deleted.addAll(toDelete);
    }

    @Override
    public MutableTreeNode description() {
        if (toDelete.size() == 1) {
            OsmPrimitive primitive = toDelete.iterator().next();
            String msg = "";
            switch(OsmPrimitiveType.from(primitive)) {
            case NODE: msg = "Delete node {0}"; break;
            case WAY: msg = "Delete way {0}"; break;
            case RELATION:msg = "Delete relation {0}"; break;
            }

            return new DefaultMutableTreeNode(new JLabel(tr(msg, primitive.getDisplayName(DefaultNameFormatter.getInstance())),
                    ImageProvider.get(OsmPrimitiveType.from(primitive)), JLabel.HORIZONTAL));
        }

        Set<OsmPrimitiveType> typesToDelete = new HashSet<OsmPrimitiveType>();
        for (OsmPrimitive osm : toDelete) {
            typesToDelete.add(OsmPrimitiveType.from(osm));
        }
        String msg = "";
        String apiname = "object";
        if (typesToDelete.size() > 1) {
            msg = trn("Delete {0} object", "Delete {0} objects", toDelete.size(), toDelete.size());
        } else {
            OsmPrimitiveType t = typesToDelete.iterator().next();
            apiname = t.getAPIName();
            switch(t) {
            case NODE: msg = trn("Delete {0} node", "Delete {0} nodes", toDelete.size(), toDelete.size()); break;
            case WAY: msg = trn("Delete {0} way", "Delete {0} ways", toDelete.size(), toDelete.size()); break;
            case RELATION: msg = trn("Delete {0} relation", "Delete {0} relations", toDelete.size(), toDelete.size()); break;
            }
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                new JLabel(msg, ImageProvider.get("data", apiname), JLabel.HORIZONTAL)
        );
        for (OsmPrimitive osm : toDelete) {
            root.add(new DefaultMutableTreeNode(new JLabel(
                    osm.getDisplayName(DefaultNameFormatter.getInstance()),
                    ImageProvider.get(OsmPrimitiveType.from(osm)), JLabel.HORIZONTAL)));
        }
        return root;
    }

    /**
     * Delete the primitives and everything they reference.
     *
     * If a node is deleted, the node and all ways and relations the node is part of are deleted as
     * well.
     *
     * If a way is deleted, all relations the way is member of are also deleted.
     *
     * If a way is deleted, only the way and no nodes are deleted.
     *
     * @param layer
     * @param selection The list of all object to be deleted.
     * @param simulate  Set to true if the user should not be bugged with additional dialogs
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     */
    public static Command deleteWithReferences(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection, boolean simulate) {
        CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data);
        v.initialize();
        for (OsmPrimitive osm : selection) {
            osm.visit(v);
        }
        v.getData().addAll(selection);
        if (v.getData().isEmpty())
            return null;
        if (!checkAndConfirmOutlyingDeletes(layer,v.getData()) && !simulate)
            return null;
        return new DeleteCommand(layer,v.getData());
    }

    public static Command deleteWithReferences(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection) {
        return deleteWithReferences(layer, selection, false);
    }

    private static int testRelation(Relation ref, OsmPrimitive osm, boolean simulate) {
        // If this delete action is simulated, do not bug the user with dialogs
        // and assume the relations should be deleted
        if(simulate)
            return 1;

        String role = "";
        for (RelationMember m : ref.getMembers()) {
            if (m.getMember() == osm) {
                role = m.getRole();
                break;
            }
        }
        ExtendedDialog dialog = new ExtendedDialog(
                Main.parent,
                tr("Conflicting relation"),
                new String[] { tr("Delete from relation"),tr("Cancel") }
        );
        dialog.setButtonIcons( new String[] { "dialogs/delete.png", "cancel.png" });
        if (role.length() > 0) {
            dialog.setContent(
                    tr(
                            "<html>Selection \"{0}\" is used by relation \"{1}\" with role {2}.<br>Delete from relation?</html>",
                            osm.getDisplayName(DefaultNameFormatter.getInstance()),
                            ref.getDisplayName(DefaultNameFormatter.getInstance()),
                            role
                    )
            );
            dialog.showDialog();
            return dialog.getValue();
        } else {
            dialog.setContent(
                    tr(
                            "<html>Selection \"{0}\" is used by relation \"{1}\".<br>Delete from relation?</html>",
                            osm.getDisplayName(DefaultNameFormatter.getInstance()),
                            ref.getDisplayName(DefaultNameFormatter.getInstance())
                    )
            );
            dialog.showDialog();
            return dialog.getValue();
        }
    }

    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection) {
        return delete(layer, selection, true, false);
    }

    /**
     * Replies the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too. A node can be deleted if
     * <ul>
     *    <li>it is untagged (see {@see Node#isTagged()}</li>
     *    <li>it is not referred to by other non-deleted primitives outside of  <code>primitivesToDelete</code></li>
     * <ul>
     * @param layer  the layer in whose context primitives are deleted
     * @param primitivesToDelete  the primitives to delete
     * @return the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too
     */
    protected static Collection<Node> computeNodesToDelete(OsmDataLayer layer, Collection<OsmPrimitive> primitivesToDelete) {
        Collection<Node> nodesToDelete = new HashSet<Node>();
        CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data, false);
        for (OsmPrimitive osm : primitivesToDelete) {
            if (! (osm instanceof Way) ) {
                continue;
            }
            for (Node n : ((Way) osm).getNodes()) {
                if (n.isTagged()) {
                    continue;
                }
                v.initialize();
                n.visit(v);
                Collection<OsmPrimitive> referringPrimitives = v.getData();
                referringPrimitives.removeAll(primitivesToDelete);
                int count = 0;
                for (OsmPrimitive p : referringPrimitives) {
                    if (!p.isDeleted()) {
                        count++;
                    }
                }
                if (count == 0) {
                    nodesToDelete.add(n);
                }
            }
        }
        return nodesToDelete;
    }

    /**
     * Try to delete all given primitives.
     *
     * If a node is used by a way, it's removed from that way. If a node or a way is used by a
     * relation, inform the user and do not delete.
     *
     * If this would cause ways with less than 2 nodes to be created, delete these ways instead. If
     * they are part of a relation, inform the user and do not delete.
     *
     * @param layer the {@see OsmDataLayer} in whose context a primitive the primitives are deleted
     * @param selection The objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @param simulate Set to true if the user should not be bugged with additional questions
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     */
    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection,
            boolean alsoDeleteNodesInWay) {
        return delete(layer, selection, alsoDeleteNodesInWay, false);
    }

    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection,
            boolean alsoDeleteNodesInWay, boolean simulate) {
        if (selection.isEmpty())
            return null;

        Collection<OsmPrimitive> primitivesToDelete = new HashSet<OsmPrimitive>(selection);
        Collection<Way> waysToBeChanged = new HashSet<Way>();
        HashMap<OsmPrimitive, Collection<OsmPrimitive>> relationsToBeChanged = new HashMap<OsmPrimitive, Collection<OsmPrimitive>>();

        if (alsoDeleteNodesInWay) {
            // delete untagged nodes only referenced by primitives in primitivesToDelete,
            // too
            Collection<Node> nodesToDelete = computeNodesToDelete(layer, primitivesToDelete);
            primitivesToDelete.addAll(nodesToDelete);
        }

        if (!simulate && !checkAndConfirmOutlyingDeletes(layer,primitivesToDelete))
            return null;

        CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data, false);
        for (OsmPrimitive osm : primitivesToDelete) {
            v.initialize();
            osm.visit(v);
            for (OsmPrimitive ref : v.getData()) {
                if (primitivesToDelete.contains(ref)) {
                    continue;
                }
                if (ref instanceof Way) {
                    waysToBeChanged.add((Way) ref);
                } else if (ref instanceof Relation) {
                    if (testRelation((Relation) ref, osm, simulate) == 1) {
                        Collection<OsmPrimitive> relset = relationsToBeChanged.get(ref);
                        if (relset == null) {
                            relset = new HashSet<OsmPrimitive>();
                        }
                        relset.add(osm);
                        relationsToBeChanged.put(ref, relset);
                    } else
                        return null;
                } else
                    return null;
            }
        }

        Collection<Command> cmds = new LinkedList<Command>();
        for (Way w : waysToBeChanged) {
            Way wnew = new Way(w);
            wnew.removeNodes(primitivesToDelete);
            if (wnew.getNodesCount() < 2) {
                primitivesToDelete.add(w);

                v.initialize();
                w.visit(v);
                for (OsmPrimitive ref : v.getData()) {
                    if (primitivesToDelete.contains(ref)) {
                        continue;
                    }
                    if (ref instanceof Relation) {
                        Boolean found = false;
                        Collection<OsmPrimitive> relset = relationsToBeChanged.get(ref);
                        if (relset == null) {
                            relset = new HashSet<OsmPrimitive>();
                        } else {
                            for (OsmPrimitive m : relset) {
                                if (m == w) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            if (testRelation((Relation) ref, w, simulate) == 1) {
                                relset.add(w);
                                relationsToBeChanged.put(ref, relset);
                            } else
                                return null;
                        }
                    } else
                        return null;
                }
            } else {
                cmds.add(new ChangeCommand(w, wnew));
            }
        }

        Iterator<OsmPrimitive> iterator = relationsToBeChanged.keySet().iterator();
        while (iterator.hasNext()) {
            Relation cur = (Relation) iterator.next();
            Relation rel = new Relation(cur);
            for (OsmPrimitive osm : relationsToBeChanged.get(cur)) {
                rel.removeMembersFor(osm);
            }
            cmds.add(new ChangeCommand(cur, rel));
        }

        // #2707: ways to be deleted can include new nodes (with node.id == 0).
        // Remove them from the way before the way is deleted. Otherwise the
        // deleted way is saved (or sent to the API) with a dangling reference to a node
        // Example:
        // <node id='2' action='delete' visible='true' version='1' ... />
        // <node id='1' action='delete' visible='true' version='1' ... />
        // <!-- missing node with id -1 because new deleted nodes are not persisted -->
        // <way id='3' action='delete' visible='true' version='1'>
        // <nd ref='1' />
        // <nd ref='-1' /> <!-- heres the problem -->
        // <nd ref='2' />
        // </way>
        for (OsmPrimitive primitive : primitivesToDelete) {
            if (!(primitive instanceof Way)) {
                continue;
            }
            Way w = (Way) primitive;
            if (w.isNew()) { // new ways with id == 0 are fine,
                continue; // process existing ways only
            }
            Way wnew = new Way(w);
            List<Node> nodesToKeep = new ArrayList<Node>();
            // lookup new nodes which have been added to the set of deleted
            // nodes ...
            for (Node n : wnew.getNodes()) {
                if (!n.isNew() || !primitivesToDelete.contains(n)) {
                    nodesToKeep.add(n);
                }
            }
            // .. and remove them from the way
            //
            wnew.setNodes(nodesToKeep);
            if (nodesToKeep.size() < w.getNodesCount()) {
                cmds.add(new ChangeCommand(w, wnew));
            }
        }

        if (!primitivesToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(layer,primitivesToDelete));
        }

        return new SequenceCommand(tr("Delete"), cmds);
    }

    public static Command deleteWaySegment(OsmDataLayer layer, WaySegment ws) {
        if (ws.way.getNodesCount() < 3) {
            // If the way contains less than three nodes, it can't have more
            // than one segment, so the way should be deleted.

            return new DeleteCommand(layer, Collections.singleton(ws.way));
        }

        if (ws.way.firstNode() == ws.way.lastNode()) {
            // If the way is circular (first and last nodes are the same),
            // the way shouldn't be splitted

            List<Node> n = new ArrayList<Node>();

            n.addAll(ws.way.getNodes().subList(ws.lowerIndex + 1, ws.way.getNodesCount() - 1));
            n.addAll(ws.way.getNodes().subList(0, ws.lowerIndex + 1));

            Way wnew = new Way(ws.way);
            wnew.setNodes(n);

            return new ChangeCommand(ws.way, wnew);
        }

        List<Node> n1 = new ArrayList<Node>(), n2 = new ArrayList<Node>();

        n1.addAll(ws.way.getNodes().subList(0, ws.lowerIndex + 1));
        n2.addAll(ws.way.getNodes().subList(ws.lowerIndex + 1, ws.way.getNodesCount()));

        Way wnew = new Way(ws.way);

        if (n1.size() < 2) {
            wnew.setNodes(n2);
            return new ChangeCommand(ws.way, wnew);
        } else if (n2.size() < 2) {
            wnew.setNodes(n1);
            return new ChangeCommand(ws.way, wnew);
        } else {
            Collection<Command> cmds = new LinkedList<Command>();

            wnew.setNodes(n1);
            cmds.add(new ChangeCommand(ws.way, wnew));

            Way wnew2 = new Way();
            wnew2.setKeys(wnew.getKeys());
            wnew2.setNodes(n2);
            cmds.add(new AddCommand(wnew2));

            // FIXME: relation memberships are not handled

            return new SequenceCommand(tr("Split way segment"), cmds);
        }
    }

    /**
     * Check whether user is about to delete data outside of the download area. Request confirmation
     * if he is.
     *
     * @param layer the layer in whose context data is deleted
     * @param primitivesToDelete the primitives to delete
     * @return true, if deleting outlying primitives is OK; false, otherwise
     */
    private static boolean checkAndConfirmOutlyingDeletes(OsmDataLayer layer, Collection<OsmPrimitive> primitivesToDelete) {
        Area a = layer.data.getDataSourceArea();
        if (a != null) {
            for (OsmPrimitive osm : primitivesToDelete) {
                if (osm instanceof Node && !osm.isNew()) {
                    Node n = (Node) osm;
                    if (!a.contains(n.getCoor())) {
                        JPanel msg = new JPanel(new GridBagLayout());
                        msg.add(new JLabel(
                                "<html>" +
                                // leave message in one tr() as there is a grammatical
                                // connection.
                                tr("You are about to delete nodes outside of the area you have downloaded."
                                        + "<br>"
                                        + "This can cause problems because other objects (that you don't see) might use them."
                                        + "<br>" + "Do you really want to delete?") + "</html>"));
                        return ConditionalOptionPaneUtil.showConfirmationDialog(
                                "delete_outside_nodes",
                                Main.parent,
                                msg,
                                tr("Delete confirmation"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.YES_OPTION
                        );
                    }
                }
            }
        }
        return true;
    }
}
