// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.data.osm.BackreferencedDataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.BackreferencedDataSet.RelationToChildReference;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.actionsupport.DeleteFromRelationConfirmationDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command to delete a number of primitives from the dataset.
 *
 */
public class DeleteCommand extends Command {
    /**
     * The primitives that get deleted.
     */
    private final Collection<? extends OsmPrimitive> toDelete;

    /**
     * Constructor. Deletes a collection of primitives in the current edit layer.
     *
     * @param data the primitives to delete. Must neither be null nor empty.
     * @throws IllegalArgumentException thrown if data is null or empty
     */
    public DeleteCommand(Collection<? extends OsmPrimitive> data) throws IllegalArgumentException {
        if (data == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be empty"));
        if (data.isEmpty())
            throw new IllegalArgumentException(tr("At least one object to delete required, got empty collection"));
        this.toDelete = data;
    }

    /**
     * Constructor. Deletes a single primitive in the current edit layer.
     *
     * @param data  the primitive to delete. Must not be null.
     * @throws IllegalArgumentException thrown if data is null
     */
    public DeleteCommand(OsmPrimitive data) throws IllegalArgumentException {
        if (data == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "data"));
        this.toDelete = Collections.singleton(data);
    }

    /**
     * Constructor for a single data item. Use the collection constructor to delete multiple
     * objects.
     *
     * @param layer the layer context for deleting this primitive. Must not be null.
     * @param data the primitive to delete. Must not be null.
     * @throws IllegalArgumentException thrown if data is null
     * @throws IllegalArgumentException thrown if layer is null
     */
    public DeleteCommand(OsmDataLayer layer, OsmPrimitive data) throws IllegalArgumentException {
        super(layer);
        if (data == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "data"));
        this.toDelete = Collections.singleton(data);
    }

    /**
     * Constructor for a collection of data to be deleted in the context of
     * a specific layer
     *
     * @param layer the layer context for deleting these primitives. Must not be null.
     * @param data the primitives to delete. Must neither be null nor empty.
     * @throws IllegalArgumentException thrown if layer is null
     * @throws IllegalArgumentException thrown if data is null or empty
     */
    public DeleteCommand(OsmDataLayer layer, Collection<? extends OsmPrimitive> data) throws IllegalArgumentException{
        super(layer);
        if (data == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be empty"));
        if (data.isEmpty())
            throw new IllegalArgumentException(tr("At least one object to delete requird, got empty collection"));
        this.toDelete = data;
    }

    protected void removeNewNodesFromDeletedWay(Way w) {
        // #2707: ways to be deleted can include new nodes (with node.id == 0).
        // Remove them from the way before the way is deleted. Otherwise the
        // deleted way is saved (or sent to the API) with a dangling reference to a node
        // Example:
        // <node id='2' action='delete' visible='true' version='1' ... />
        // <node id='1' action='delete' visible='true' version='1' ... />
        // <!-- missing node with id -1 because new deleted nodes are not persisted -->
        // <way id='3' action='delete' visible='true' version='1'>
        // <nd ref='1' />
        // <nd ref='-1' /> <!-- here's the problem -->
        // <nd ref='2' />
        // </way>
        if (w.isNew())
            return; // process existing ways only
        List<Node> nodesToKeep = new ArrayList<Node>();
        // lookup new nodes which have been added to the set of deleted
        // nodes ...
        Iterator<Node> it = nodesToKeep.iterator();
        while(it.hasNext()) {
            Node n = it.next();
            if (n.isNew()) {
                it.remove();
            }
        }
        w.setNodes(nodesToKeep);
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        for (OsmPrimitive osm : toDelete) {
            osm.setDeleted(true);
            if (osm instanceof Way) {
                removeNewNodesFromDeletedWay((Way)osm);
            }
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
     * @param layer the {@see OsmDataLayer} in whose context primitives are deleted. Must not be null.
     * @param selection The list of all object to be deleted.
     * @param silent  Set to true if the user should not be bugged with additional dialogs
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     * @throws IllegalArgumentException thrown if layer is null
     */
    public static Command deleteWithReferences(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection, boolean silent) throws IllegalArgumentException {
        if (layer == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "layer"));
        if (selection == null || selection.isEmpty()) return null;
        CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data);
        v.initialize();
        for (OsmPrimitive osm : selection) {
            osm.visit(v);
        }
        v.getData().addAll(selection);
        if (v.getData().isEmpty())
            return null;
        if (!checkAndConfirmOutlyingDeletes(layer,v.getData()) && !silent)
            return null;
        return new DeleteCommand(layer,v.getData());
    }

    public static Command deleteWithReferences(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection) {
        return deleteWithReferences(layer, selection, false);
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
     * @param backreferences backreference data structure
     * @param layer  the layer in whose context primitives are deleted
     * @param primitivesToDelete  the primitives to delete
     * @return the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too
     */
    protected static Collection<Node> computeNodesToDelete(BackreferencedDataSet backreferences, OsmDataLayer layer, Collection<OsmPrimitive> primitivesToDelete) {
        Collection<Node> nodesToDelete = new HashSet<Node>();
        for (Way way : OsmPrimitive.getFilteredList(primitivesToDelete, Way.class)) {
            for (Node n : way.getNodes()) {
                if (n.isTagged()) {
                    continue;
                }
                Collection<OsmPrimitive> referringPrimitives = backreferences.getParents(n);
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
     * @param layer the {@see OsmDataLayer} in whose context the primitives are deleted
     * @param selection the objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     */
    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection,
            boolean alsoDeleteNodesInWay) {
        return delete(layer, selection, alsoDeleteNodesInWay, false /* not silent */);
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
     * @param layer the {@see OsmDataLayer} in whose context the primitives are deleted
     * @param selection the objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @param silent set to true if the user should not be bugged with additional questions
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     */
    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection,
            boolean alsoDeleteNodesInWay, boolean silent) {
        if (selection == null || selection.isEmpty())
            return null;

        BackreferencedDataSet backreferences = new BackreferencedDataSet();
        Set<OsmPrimitive> primitivesToDelete = new HashSet<OsmPrimitive>(selection);
        Collection<Way> waysToBeChanged = new HashSet<Way>();

        if (alsoDeleteNodesInWay) {
            // delete untagged nodes only referenced by primitives in primitivesToDelete,
            // too
            Collection<Node> nodesToDelete = computeNodesToDelete(backreferences, layer, primitivesToDelete);
            primitivesToDelete.addAll(nodesToDelete);
        }

        if (!silent && !checkAndConfirmOutlyingDeletes(layer,primitivesToDelete))
            return null;

        waysToBeChanged.addAll(OsmPrimitive.getFilteredSet(backreferences.getParents(primitivesToDelete), Way.class));

        Collection<Command> cmds = new LinkedList<Command>();
        for (Way w : waysToBeChanged) {
            Way wnew = new Way(w);
            wnew.removeNodes(primitivesToDelete);
            if (wnew.getNodesCount() < 2) {
                primitivesToDelete.add(w);
            } else {
                cmds.add(new ChangeCommand(w, wnew));
            }
        }

        // get a confirmation that the objects to delete can be removed from their parent
        // relations
        //
        if (!silent) {
            Set<RelationToChildReference> references = backreferences.getRelationToChildReferences(primitivesToDelete);
            Iterator<RelationToChildReference> it = references.iterator();
            while(it.hasNext()) {
                RelationToChildReference ref = it.next();
                if (ref.getParent().isDeleted()) {
                    it.remove();
                }
            }
            if (!references.isEmpty()) {
                DeleteFromRelationConfirmationDialog dialog = DeleteFromRelationConfirmationDialog.getInstance();
                dialog.getModel().populate(references);
                dialog.setVisible(true);
                if (dialog.isCanceled())
                    return null;
            }
        }

        // remove the objects from their parent relations
        //
        Iterator<Relation> iterator = OsmPrimitive.getFilteredSet(backreferences.getParents(primitivesToDelete), Relation.class).iterator();
        while (iterator.hasNext()) {
            Relation cur = iterator.next();
            Relation rel = new Relation(cur);
            rel.removeMembersFor(primitivesToDelete);
            cmds.add(new ChangeCommand(cur, rel));
        }

        // build the delete command
        //
        if (!primitivesToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(layer,primitivesToDelete));
        }

        return new SequenceCommand(tr("Delete"), cmds);
    }

    public static Command deleteWaySegment(OsmDataLayer layer, WaySegment ws) {
        if (ws.way.getNodesCount() < 3)
            return delete(layer, Collections.singleton(ws.way));

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
            List<List<Node>> chunks = new ArrayList<List<Node>>(2);
            chunks.add(n1);
            chunks.add(n2);
            return SplitWayAction.splitWay(ws.way, chunks).getCommand();
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
