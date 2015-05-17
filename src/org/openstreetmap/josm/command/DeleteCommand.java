// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.actionsupport.DeleteFromRelationConfirmationDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * A command to delete a number of primitives from the dataset.
 * @since 23
 */
public class DeleteCommand extends Command {
    /**
     * The primitives that get deleted.
     */
    private final Collection<? extends OsmPrimitive> toDelete;
    private final Map<OsmPrimitive, PrimitiveData> clonedPrimitives = new HashMap<>();

    /**
     * Constructor. Deletes a collection of primitives in the current edit layer.
     *
     * @param data the primitives to delete. Must neither be null nor empty.
     * @throws IllegalArgumentException if data is null or empty
     */
    public DeleteCommand(Collection<? extends OsmPrimitive> data) {
        CheckParameterUtil.ensureParameterNotNull(data, "data");
        if (data.isEmpty())
            throw new IllegalArgumentException(tr("At least one object to delete required, got empty collection"));
        this.toDelete = data;
        checkConsistency();
    }

    /**
     * Constructor. Deletes a single primitive in the current edit layer.
     *
     * @param data  the primitive to delete. Must not be null.
     * @throws IllegalArgumentException if data is null
     */
    public DeleteCommand(OsmPrimitive data) {
        this(Collections.singleton(data));
    }

    /**
     * Constructor for a single data item. Use the collection constructor to delete multiple
     * objects.
     *
     * @param layer the layer context for deleting this primitive. Must not be null.
     * @param data the primitive to delete. Must not be null.
     * @throws IllegalArgumentException if data is null
     * @throws IllegalArgumentException if layer is null
     */
    public DeleteCommand(OsmDataLayer layer, OsmPrimitive data) {
        this(layer, Collections.singleton(data));
    }

    /**
     * Constructor for a collection of data to be deleted in the context of
     * a specific layer
     *
     * @param layer the layer context for deleting these primitives. Must not be null.
     * @param data the primitives to delete. Must neither be null nor empty.
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if data is null or empty
     */
    public DeleteCommand(OsmDataLayer layer, Collection<? extends OsmPrimitive> data) {
        super(layer);
        CheckParameterUtil.ensureParameterNotNull(data, "data");
        if (data.isEmpty())
            throw new IllegalArgumentException(tr("At least one object to delete required, got empty collection"));
        this.toDelete = data;
        checkConsistency();
    }

    private void checkConsistency() {
        for (OsmPrimitive p : toDelete) {
            if (p == null) {
                throw new IllegalArgumentException("Primitive to delete must not be null");
            } else if (p.getDataSet() == null) {
                throw new IllegalArgumentException("Primitive to delete must be in a dataset");
            }
        }
    }

    @Override
    public boolean executeCommand() {
        // Make copy and remove all references (to prevent inconsistent dataset (delete referenced) while command is executed)
        for (OsmPrimitive osm: toDelete) {
            if (osm.isDeleted())
                throw new IllegalArgumentException(osm + " is already deleted");
            clonedPrimitives.put(osm, osm.save());

            if (osm instanceof Way) {
                ((Way) osm).setNodes(null);
            } else if (osm instanceof Relation) {
                ((Relation) osm).setMembers(null);
            }
        }

        for (OsmPrimitive osm: toDelete) {
            osm.setDeleted(true);
        }

        return true;
    }

    @Override
    public void undoCommand() {
        for (OsmPrimitive osm: toDelete) {
            osm.setDeleted(false);
        }

        for (Entry<OsmPrimitive, PrimitiveData> entry: clonedPrimitives.entrySet()) {
            entry.getKey().load(entry.getValue());
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
    }

    private Set<OsmPrimitiveType> getTypesToDelete() {
        Set<OsmPrimitiveType> typesToDelete = EnumSet.noneOf(OsmPrimitiveType.class);
        for (OsmPrimitive osm : toDelete) {
            typesToDelete.add(OsmPrimitiveType.from(osm));
        }
        return typesToDelete;
    }

    @Override
    public String getDescriptionText() {
        if (toDelete.size() == 1) {
            OsmPrimitive primitive = toDelete.iterator().next();
            String msg = "";
            switch(OsmPrimitiveType.from(primitive)) {
            case NODE: msg = marktr("Delete node {0}"); break;
            case WAY: msg = marktr("Delete way {0}"); break;
            case RELATION:msg = marktr("Delete relation {0}"); break;
            }

            return tr(msg, primitive.getDisplayName(DefaultNameFormatter.getInstance()));
        } else {
            Set<OsmPrimitiveType> typesToDelete = getTypesToDelete();
            String msg = "";
            if (typesToDelete.size() > 1) {
                msg = trn("Delete {0} object", "Delete {0} objects", toDelete.size(), toDelete.size());
            } else {
                OsmPrimitiveType t = typesToDelete.iterator().next();
                switch(t) {
                case NODE: msg = trn("Delete {0} node", "Delete {0} nodes", toDelete.size(), toDelete.size()); break;
                case WAY: msg = trn("Delete {0} way", "Delete {0} ways", toDelete.size(), toDelete.size()); break;
                case RELATION: msg = trn("Delete {0} relation", "Delete {0} relations", toDelete.size(), toDelete.size()); break;
                }
            }
            return msg;
        }
    }

    @Override
    public Icon getDescriptionIcon() {
        if (toDelete.size() == 1)
            return ImageProvider.get(toDelete.iterator().next().getDisplayType());
        Set<OsmPrimitiveType> typesToDelete = getTypesToDelete();
        if (typesToDelete.size() > 1)
            return ImageProvider.get("data", "object");
        else
            return ImageProvider.get(typesToDelete.iterator().next());
    }

    @Override public Collection<PseudoCommand> getChildren() {
        if (toDelete.size() == 1)
            return null;
        else {
            List<PseudoCommand> children = new ArrayList<>(toDelete.size());
            for (final OsmPrimitive osm : toDelete) {
                children.add(new PseudoCommand() {

                    @Override public String getDescriptionText() {
                        return tr("Deleted ''{0}''", osm.getDisplayName(DefaultNameFormatter.getInstance()));
                    }

                    @Override public Icon getDescriptionIcon() {
                        return ImageProvider.get(osm.getDisplayType());
                    }

                    @Override public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
                        return Collections.singleton(osm);
                    }

                });
            }
            return children;

        }
    }

    @Override public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return toDelete;
    }

    /**
     * Delete the primitives and everything they reference.
     *
     * If a node is deleted, the node and all ways and relations the node is part of are deleted as well.
     * If a way is deleted, all relations the way is member of are also deleted.
     * If a way is deleted, only the way and no nodes are deleted.
     *
     * @param layer the {@link OsmDataLayer} in whose context primitives are deleted. Must not be null.
     * @param selection The list of all object to be deleted.
     * @param silent  Set to true if the user should not be bugged with additional dialogs
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     * @throws IllegalArgumentException if layer is null
     */
    public static Command deleteWithReferences(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection, boolean silent) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        if (selection == null || selection.isEmpty()) return null;
        Set<OsmPrimitive> parents = OsmPrimitive.getReferrer(selection);
        parents.addAll(selection);

        if (parents.isEmpty())
            return null;
        if (!silent && !checkAndConfirmOutlyingDelete(parents, null))
            return null;
        return new DeleteCommand(layer,parents);
    }

    /**
     * Delete the primitives and everything they reference.
     *
     * If a node is deleted, the node and all ways and relations the node is part of are deleted as well.
     * If a way is deleted, all relations the way is member of are also deleted.
     * If a way is deleted, only the way and no nodes are deleted.
     *
     * @param layer the {@link OsmDataLayer} in whose context primitives are deleted. Must not be null.
     * @param selection The list of all object to be deleted.
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     * @throws IllegalArgumentException if layer is null
     */
    public static Command deleteWithReferences(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection) {
        return deleteWithReferences(layer, selection, false);
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
     * @param layer the {@link OsmDataLayer} in whose context the primitives are deleted
     * @param selection the objects to delete.
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     */
    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection) {
        return delete(layer, selection, true, false);
    }

    /**
     * Replies the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too. A node can be deleted if
     * <ul>
     *    <li>it is untagged (see {@link Node#isTagged()}</li>
     *    <li>it is not referred to by other non-deleted primitives outside of  <code>primitivesToDelete</code></li>
     * </ul>
     * @param layer  the layer in whose context primitives are deleted
     * @param primitivesToDelete  the primitives to delete
     * @return the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too
     */
    protected static Collection<Node> computeNodesToDelete(OsmDataLayer layer, Collection<OsmPrimitive> primitivesToDelete) {
        Collection<Node> nodesToDelete = new HashSet<>();
        for (Way way : OsmPrimitive.getFilteredList(primitivesToDelete, Way.class)) {
            for (Node n : way.getNodes()) {
                if (n.isTagged()) {
                    continue;
                }
                Collection<OsmPrimitive> referringPrimitives = n.getReferrers();
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
     * @param layer the {@link OsmDataLayer} in whose context the primitives are deleted
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
     * @param layer the {@link OsmDataLayer} in whose context the primitives are deleted
     * @param selection the objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @param silent set to true if the user should not be bugged with additional questions
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     */
    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection,
            boolean alsoDeleteNodesInWay, boolean silent) {
        if (selection == null || selection.isEmpty())
            return null;

        Set<OsmPrimitive> primitivesToDelete = new HashSet<>(selection);

        Collection<Relation> relationsToDelete = Utils.filteredCollection(primitivesToDelete, Relation.class);
        if (!relationsToDelete.isEmpty() && !silent && !confirmRelationDeletion(relationsToDelete))
            return null;

        Collection<Way> waysToBeChanged = new HashSet<>();

        if (alsoDeleteNodesInWay) {
            // delete untagged nodes only referenced by primitives in primitivesToDelete, too
            Collection<Node> nodesToDelete = computeNodesToDelete(layer, primitivesToDelete);
            primitivesToDelete.addAll(nodesToDelete);
        }

        if (!silent && !checkAndConfirmOutlyingDelete(
                primitivesToDelete, Utils.filteredCollection(primitivesToDelete, Way.class)))
            return null;

        waysToBeChanged.addAll(OsmPrimitive.getFilteredSet(OsmPrimitive.getReferrer(primitivesToDelete), Way.class));

        Collection<Command> cmds = new LinkedList<>();
        for (Way w : waysToBeChanged) {
            Way wnew = new Way(w);
            wnew.removeNodes(OsmPrimitive.getFilteredSet(primitivesToDelete, Node.class));
            if (wnew.getNodesCount() < 2) {
                primitivesToDelete.add(w);
            } else {
                cmds.add(new ChangeNodesCommand(w, wnew.getNodes()));
            }
        }

        // get a confirmation that the objects to delete can be removed from their parent relations
        //
        if (!silent) {
            Set<RelationToChildReference> references = RelationToChildReference.getRelationToChildReferences(primitivesToDelete);
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
        for (Relation cur : OsmPrimitive.getFilteredSet(OsmPrimitive.getReferrer(primitivesToDelete), Relation.class)) {
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
            return delete(layer, Collections.singleton(ws.way), false);

        if (ws.way.firstNode() == ws.way.lastNode()) {
            // If the way is circular (first and last nodes are the same),
            // the way shouldn't be splitted

            List<Node> n = new ArrayList<>();

            n.addAll(ws.way.getNodes().subList(ws.lowerIndex + 1, ws.way.getNodesCount() - 1));
            n.addAll(ws.way.getNodes().subList(0, ws.lowerIndex + 1));

            Way wnew = new Way(ws.way);
            wnew.setNodes(n);

            return new ChangeCommand(ws.way, wnew);
        }

        List<Node> n1 = new ArrayList<>(), n2 = new ArrayList<>();

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
            List<List<Node>> chunks = new ArrayList<>(2);
            chunks.add(n1);
            chunks.add(n2);
            return SplitWayAction.splitWay(layer,ws.way, chunks, Collections.<OsmPrimitive>emptyList()).getCommand();
        }
    }

    public static boolean checkAndConfirmOutlyingDelete(Collection<? extends OsmPrimitive> primitives, Collection<? extends OsmPrimitive> ignore) {
        return Command.checkAndConfirmOutlyingOperation("delete",
                tr("Delete confirmation"),
                tr("You are about to delete nodes outside of the area you have downloaded."
                        + "<br>"
                        + "This can cause problems because other objects (that you do not see) might use them."
                        + "<br>"
                        + "Do you really want to delete?"),
                tr("You are about to delete incomplete objects."
                        + "<br>"
                        + "This will cause problems because you don''t see the real object."
                        + "<br>" + "Do you really want to delete?"),
                primitives, ignore);
    }

    private static boolean confirmRelationDeletion(Collection<Relation> relations) {
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JMultilineLabel("<html>" + trn(
                "You are about to delete {0} relation: {1}"
                + "<br/>"
                + "This step is rarely necessary and cannot be undone easily after being uploaded to the server."
                + "<br/>"
                + "Do you really want to delete?",
                "You are about to delete {0} relations: {1}"
                + "<br/>"
                + "This step is rarely necessary and cannot be undone easily after being uploaded to the server."
                + "<br/>"
                + "Do you really want to delete?",
                relations.size(), relations.size(), DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(relations))
                + "</html>"));
        return ConditionalOptionPaneUtil.showConfirmationDialog(
                "delete_relations",
                Main.parent,
                msg,
                tr("Delete relation?"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_OPTION);
    }
}
