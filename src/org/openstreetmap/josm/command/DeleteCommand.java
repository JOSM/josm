// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * A command to delete a number of primitives from the dataset.
 * To be used correctly, this class requires an initial call to {@link #setDeletionCallback(DeletionCallback)} to
 * allow interactive confirmation actions.
 * @since 23
 */
public class DeleteCommand extends Command {
    private static final class DeleteChildCommand implements PseudoCommand {
        private final OsmPrimitive osm;

        private DeleteChildCommand(OsmPrimitive osm) {
            this.osm = osm;
        }

        @Override
        public String getDescriptionText() {
            return tr("Deleted ''{0}''", osm.getDisplayName(DefaultNameFormatter.getInstance()));
        }

        @Override
        public Icon getDescriptionIcon() {
            return ImageProvider.get(osm.getDisplayType());
        }

        @Override
        public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
            return Collections.singleton(osm);
        }

        @Override
        public String toString() {
            return "DeleteChildCommand [osm=" + osm + ']';
        }
    }

    /**
     * Called when a deletion operation must be checked and confirmed by user.
     * @since 12749
     */
    public interface DeletionCallback {
        /**
         * Check whether user is about to delete data outside of the download area.
         * Request confirmation if he is.
         * @param primitives the primitives to operate on
         * @param ignore {@code null} or a primitive to be ignored
         * @return true, if operating on outlying primitives is OK; false, otherwise
         */
        boolean checkAndConfirmOutlyingDelete(Collection<? extends OsmPrimitive> primitives, Collection<? extends OsmPrimitive> ignore);

        /**
         * Confirm before deleting a relation, as it is a common newbie error.
         * @param relations relation to check for deletion
         * @return {@code true} if user confirms the deletion
         * @since 12760
         */
        boolean confirmRelationDeletion(Collection<Relation> relations);

        /**
         * Confirm before removing a collection of primitives from their parent relations.
         * @param references the list of relation-to-child references
         * @return {@code true} if user confirms the deletion
         * @since 12763
         */
        boolean confirmDeletionFromRelation(Collection<RelationToChildReference> references);
    }

    private static volatile DeletionCallback callback;

    /**
     * Sets the global {@link DeletionCallback}.
     * @param deletionCallback the new {@code DeletionCallback}. Must not be null
     * @throws NullPointerException if {@code deletionCallback} is null
     * @since 12749
     */
    public static void setDeletionCallback(DeletionCallback deletionCallback) {
        callback = Objects.requireNonNull(deletionCallback);
    }

    /**
     * The primitives that get deleted.
     */
    private final Collection<? extends OsmPrimitive> toDelete;
    private final Map<OsmPrimitive, PrimitiveData> clonedPrimitives = new HashMap<>();

    /**
     * Constructor. Deletes a collection of primitives in the current edit layer.
     *
     * @param data the primitives to delete. Must neither be null nor empty, and belong to a data set
     * @throws IllegalArgumentException if data is null or empty
     */
    public DeleteCommand(Collection<? extends OsmPrimitive> data) {
        this(data.iterator().next().getDataSet(), data);
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
     * Constructor for a single data item. Use the collection constructor to delete multiple objects.
     *
     * @param dataset the data set context for deleting this primitive. Must not be null.
     * @param data the primitive to delete. Must not be null.
     * @throws IllegalArgumentException if data is null
     * @throws IllegalArgumentException if layer is null
     * @since 12718
     */
    public DeleteCommand(DataSet dataset, OsmPrimitive data) {
        this(dataset, Collections.singleton(data));
    }

    /**
     * Constructor for a collection of data to be deleted in the context of a specific data set
     *
     * @param dataset the dataset context for deleting these primitives. Must not be null.
     * @param data the primitives to delete. Must neither be null nor empty.
     * @throws IllegalArgumentException if dataset is null
     * @throws IllegalArgumentException if data is null or empty
     * @since 11240
     */
    public DeleteCommand(DataSet dataset, Collection<? extends OsmPrimitive> data) {
        super(dataset);
        CheckParameterUtil.ensureParameterNotNull(data, "data");
        this.toDelete = data;
        checkConsistency();
    }

    private void checkConsistency() {
        if (toDelete.isEmpty()) {
            throw new IllegalArgumentException(tr("At least one object to delete required, got empty collection"));
        }
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
        ensurePrimitivesAreInDataset();

        getAffectedDataSet().update(() -> {
            // Make copy and remove all references (to prevent inconsistent dataset (delete referenced) while command is executed)
            for (OsmPrimitive osm : toDelete) {
                if (osm.isDeleted())
                    throw new IllegalArgumentException(osm + " is already deleted");
                clonedPrimitives.put(osm, osm.save());

                if (osm instanceof Way) {
                    ((Way) osm).setNodes(null);
                } else if (osm instanceof Relation) {
                    ((Relation) osm).setMembers(null);
                }
            }

            for (OsmPrimitive osm : toDelete) {
                osm.setDeleted(true);
            }
        });
        return true;
    }

    @Override
    public void undoCommand() {
        ensurePrimitivesAreInDataset();

        getAffectedDataSet().update(() -> {
            for (OsmPrimitive osm : toDelete) {
                osm.setDeleted(false);
            }

            for (Entry<OsmPrimitive, PrimitiveData> entry : clonedPrimitives.entrySet()) {
                entry.getKey().load(entry.getValue());
            }
        });
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        // Do nothing
    }

    private Set<OsmPrimitiveType> getTypesToDelete() {
        return toDelete.stream().map(OsmPrimitiveType::from).collect(Collectors.toSet());
    }

    @Override
    public String getDescriptionText() {
        if (toDelete.size() == 1) {
            OsmPrimitive primitive = toDelete.iterator().next();
            String msg;
            switch(OsmPrimitiveType.from(primitive)) {
            case NODE: msg = marktr("Delete node {0}"); break;
            case WAY: msg = marktr("Delete way {0}"); break;
            case RELATION:msg = marktr("Delete relation {0}"); break;
            default: throw new AssertionError();
            }

            return tr(msg, primitive.getDisplayName(DefaultNameFormatter.getInstance()));
        } else {
            Set<OsmPrimitiveType> typesToDelete = getTypesToDelete();
            String msg;
            if (typesToDelete.size() > 1) {
                msg = trn("Delete {0} object", "Delete {0} objects", toDelete.size(), toDelete.size());
            } else {
                OsmPrimitiveType t = typesToDelete.iterator().next();
                switch(t) {
                case NODE: msg = trn("Delete {0} node", "Delete {0} nodes", toDelete.size(), toDelete.size()); break;
                case WAY: msg = trn("Delete {0} way", "Delete {0} ways", toDelete.size(), toDelete.size()); break;
                case RELATION: msg = trn("Delete {0} relation", "Delete {0} relations", toDelete.size(), toDelete.size()); break;
                default: throw new AssertionError();
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
            return toDelete.stream().map(DeleteChildCommand::new).collect(Collectors.toList());
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
     * @param selection The list of all object to be deleted.
     * @param silent  Set to true if the user should not be bugged with additional dialogs
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     * @throws IllegalArgumentException if layer is null
     * @since 12718
     */
    public static Command deleteWithReferences(Collection<? extends OsmPrimitive> selection, boolean silent) {
        if (selection == null || selection.isEmpty()) return null;
        Set<OsmPrimitive> parents = OsmPrimitive.getReferrer(selection);
        parents.addAll(selection);

        if (parents.isEmpty())
            return null;
        if (!silent && !callback.checkAndConfirmOutlyingDelete(parents, null))
            return null;
        return new DeleteCommand(parents.iterator().next().getDataSet(), parents);
    }

    /**
     * Delete the primitives and everything they reference.
     *
     * If a node is deleted, the node and all ways and relations the node is part of are deleted as well.
     * If a way is deleted, all relations the way is member of are also deleted.
     * If a way is deleted, only the way and no nodes are deleted.
     *
     * @param selection The list of all object to be deleted.
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     * @throws IllegalArgumentException if layer is null
     * @since 12718
     */
    public static Command deleteWithReferences(Collection<? extends OsmPrimitive> selection) {
        return deleteWithReferences(selection, false);
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
     * @param selection the objects to delete.
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     * @since 12718
     */
    public static Command delete(Collection<? extends OsmPrimitive> selection) {
        return delete(selection, true, false);
    }

    /**
     * Replies the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too. A node can be deleted if
     * <ul>
     *    <li>it is untagged (see {@link Node#isTagged()}</li>
     *    <li>it is not referred to by other non-deleted primitives outside of  <code>primitivesToDelete</code></li>
     * </ul>
     * @param primitivesToDelete  the primitives to delete
     * @return the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too
     */
    protected static Collection<Node> computeNodesToDelete(Collection<OsmPrimitive> primitivesToDelete) {
        Collection<Node> nodesToDelete = new HashSet<>();
        for (Way way : Utils.filteredCollection(primitivesToDelete, Way.class)) {
            for (Node n : way.getNodes()) {
                if (n.isTagged()) {
                    continue;
                }
                Collection<OsmPrimitive> referringPrimitives = n.getReferrers();
                referringPrimitives.removeAll(primitivesToDelete);
                if (referringPrimitives.stream().allMatch(OsmPrimitive::isDeleted)) {
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
     * @param selection the objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     * @since 12718
     */
    public static Command delete(Collection<? extends OsmPrimitive> selection, boolean alsoDeleteNodesInWay) {
        return delete(selection, alsoDeleteNodesInWay, false /* not silent */);
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
     * @param selection the objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @param silent set to true if the user should not be bugged with additional questions
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     * @since 12718
     */
    public static Command delete(Collection<? extends OsmPrimitive> selection, boolean alsoDeleteNodesInWay, boolean silent) {
        if (selection == null || selection.isEmpty())
            return null;

        Set<OsmPrimitive> primitivesToDelete = new HashSet<>(selection);

        Collection<Relation> relationsToDelete = Utils.filteredCollection(primitivesToDelete, Relation.class);
        if (!relationsToDelete.isEmpty() && !silent && !callback.confirmRelationDeletion(relationsToDelete))
            return null;

        if (alsoDeleteNodesInWay) {
            // delete untagged nodes only referenced by primitives in primitivesToDelete, too
            Collection<Node> nodesToDelete = computeNodesToDelete(primitivesToDelete);
            primitivesToDelete.addAll(nodesToDelete);
        }

        if (!silent && !callback.checkAndConfirmOutlyingDelete(
                primitivesToDelete, Utils.filteredCollection(primitivesToDelete, Way.class)))
            return null;

        Collection<Way> waysToBeChanged = primitivesToDelete.stream()
                .flatMap(p -> p.referrers(Way.class))
                .collect(Collectors.toSet());

        Collection<Command> cmds = new LinkedList<>();
        Set<Node> nodesToRemove = new HashSet<>(Utils.filteredCollection(primitivesToDelete, Node.class));
        for (Way w : waysToBeChanged) {
            if (primitivesToDelete.contains(w))
                continue;
            List<Node> remainingNodes = w.calculateRemoveNodes(nodesToRemove);
            if (remainingNodes.size() < 2) {
                primitivesToDelete.add(w);
            } else {
                cmds.add(new ChangeNodesCommand(w, remainingNodes));
            }
        }

        // get a confirmation that the objects to delete can be removed from their parent relations
        //
        if (!silent) {
            Set<RelationToChildReference> references = RelationToChildReference.getRelationToChildReferences(primitivesToDelete);
            references.removeIf(ref -> ref.getParent().isDeleted());
            if (!references.isEmpty() && !callback.confirmDeletionFromRelation(references)) {
                return null;
            }
        }

        // remove the objects from their parent relations
        //
        final Set<Relation> relationsToBeChanged = primitivesToDelete.stream()
                .flatMap(p -> p.referrers(Relation.class))
                .collect(Collectors.toSet());
        for (Relation cur : relationsToBeChanged) {
            Relation rel = new Relation(cur);
            rel.removeMembersFor(primitivesToDelete);
            cmds.add(new ChangeCommand(cur, rel));
        }

        // build the delete command
        //
        if (!primitivesToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(primitivesToDelete.iterator().next().getDataSet(), primitivesToDelete));
        }

        return SequenceCommand.wrapIfNeeded(tr("Delete"), cmds);
    }

    /**
     * Create a command that deletes a single way segment. The way may be split by this.
     * @param ws The way segment that should be deleted
     * @return A matching command to safely delete that segment.
     * @since 12718
     */
    public static Command deleteWaySegment(WaySegment ws) {
        if (ws.way.getNodesCount() < 3)
            return delete(Collections.singleton(ws.way), false);

        if (ws.way.isClosed()) {
            // If the way is circular (first and last nodes are the same), the way shouldn't be splitted

            List<Node> n = new ArrayList<>();

            n.addAll(ws.way.getNodes().subList(ws.lowerIndex + 1, ws.way.getNodesCount() - 1));
            n.addAll(ws.way.getNodes().subList(0, ws.lowerIndex + 1));

            Way wnew = new Way(ws.way);
            wnew.setNodes(n);

            return new ChangeCommand(ws.way, wnew);
        }

        List<Node> n1 = new ArrayList<>();
        List<Node> n2 = new ArrayList<>();

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
            return SplitWayCommand.splitWay(ws.way, Arrays.asList(n1, n2), Collections.<OsmPrimitive>emptyList());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toDelete, clonedPrimitives);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        DeleteCommand that = (DeleteCommand) obj;
        return Objects.equals(toDelete, that.toDelete) &&
                Objects.equals(clonedPrimitives, that.clonedPrimitives);
    }
}
