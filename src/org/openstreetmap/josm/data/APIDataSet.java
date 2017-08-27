// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.CyclicUploadDependencyException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Utils;

/**
 * Represents a collection of {@link OsmPrimitive}s which should be uploaded to the API.
 * The collection is derived from the modified primitives of an {@link DataSet} and it provides methods
 * for sorting the objects in upload order.
 * @since 2025
 */
public class APIDataSet {
    private List<OsmPrimitive> toAdd;
    private List<OsmPrimitive> toUpdate;
    private List<OsmPrimitive> toDelete;

    /**
     * creates a new empty data set
     */
    public APIDataSet() {
        toAdd = new LinkedList<>();
        toUpdate = new LinkedList<>();
        toDelete = new LinkedList<>();
    }

    /**
     * initializes the API data set with the modified primitives in <code>ds</code>
     *
     * @param ds the data set. Ignored, if null.
     */
    public void init(DataSet ds) {
        if (ds == null) return;
        init(ds.allPrimitives());
    }

    /**
     * Initializes the API data set with the modified primitives, ignores unmodified primitives.
     *
     * @param primitives the primitives
     */
    public final void init(Collection<OsmPrimitive> primitives) {
        toAdd.clear();
        toUpdate.clear();
        toDelete.clear();

        for (OsmPrimitive osm :primitives) {
            if (osm.isNewOrUndeleted() && !osm.isDeleted()) {
                toAdd.add(osm);
            } else if (osm.isModified() && !osm.isDeleted()) {
                toUpdate.add(osm);
            } else if (osm.isDeleted() && !osm.isNew() && osm.isModified() && osm.isVisible()) {
                toDelete.add(osm);
            }
        }
        final Comparator<OsmPrimitive> orderingNodesWaysRelations = OsmPrimitiveComparator.orderingNodesWaysRelations();
        final Comparator<OsmPrimitive> byUniqueId = OsmPrimitiveComparator.comparingUniqueId();
        toAdd.sort(orderingNodesWaysRelations.thenComparing(byUniqueId));
        toUpdate.sort(orderingNodesWaysRelations.thenComparing(byUniqueId));
        toDelete.sort(orderingNodesWaysRelations.reversed().thenComparing(byUniqueId));
    }

    /**
     * initializes the API data set with the modified primitives in <code>ds</code>
     *
     * @param ds the data set. Ignored, if null.
     */
    public APIDataSet(DataSet ds) {
        this();
        init(ds);
    }

    /**
     * Replies true if one of the primitives to be updated or to be deleted
     * participates in at least one conflict in <code>conflicts</code>
     *
     * @param conflicts the collection of conflicts
     * @return true if one of the primitives to be updated or to be deleted
     * participates in at least one conflict in <code>conflicts</code>
     */
    public boolean participatesInConflict(ConflictCollection conflicts) {
        if (conflicts == null || conflicts.isEmpty()) return false;
        Set<PrimitiveId> idsParticipatingInConflicts = conflicts.get().stream()
                .flatMap(c -> Stream.of(c.getMy(), c.getTheir()))
                .map(OsmPrimitive::getPrimitiveId)
                .collect(Collectors.toSet());
        return Stream.of(toUpdate, toDelete)
                .flatMap(Collection::stream)
                .map(OsmPrimitive::getPrimitiveId)
                .anyMatch(idsParticipatingInConflicts::contains);
    }

    /**
     * initializes the API data set with the primitives in <code>primitives</code>
     *
     * @param primitives the collection of primitives
     */
    public APIDataSet(Collection<OsmPrimitive> primitives) {
        this();
        init(primitives);
    }

    /**
     * Replies true if there are no primitives to upload
     *
     * @return true if there are no primitives to upload
     */
    public boolean isEmpty() {
        return toAdd.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty();
    }

    /**
     * Replies the primitives which should be added to the OSM database
     *
     * @return the primitives which should be added to the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToAdd() {
        return toAdd;
    }

    /**
     * Replies the primitives which should be updated in the OSM database
     *
     * @return the primitives which should be updated in the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToUpdate() {
        return toUpdate;
    }

    /**
     * Replies the primitives which should be deleted in the OSM database
     *
     * @return the primitives which should be deleted in the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToDelete() {
        return toDelete;
    }

    /**
     * Replies all primitives
     *
     * @return all primitives
     */
    public List<OsmPrimitive> getPrimitives() {
        List<OsmPrimitive> ret = new LinkedList<>();
        ret.addAll(toAdd);
        ret.addAll(toUpdate);
        ret.addAll(toDelete);
        return ret;
    }

    /**
     * Replies the number of objects to upload
     *
     * @return the number of objects to upload
     */
    public int getSize() {
        return toAdd.size() + toUpdate.size() + toDelete.size();
    }

    /**
     * Removes the given primitives from this {@link APIDataSet}
     * @param processed The primitives to remove
     */
    public void removeProcessed(Collection<IPrimitive> processed) {
        if (processed == null) return;
        toAdd.removeAll(processed);
        toUpdate.removeAll(processed);
        toDelete.removeAll(processed);
    }

    /**
     * Adjusts the upload order for new relations. Child relations are uploaded first,
     * parent relations second.
     *
     * This method detects cyclic dependencies in new relation. Relations with cyclic
     * dependencies can't be uploaded.
     *
     * @throws CyclicUploadDependencyException if a cyclic dependency is detected
     */
    public void adjustRelationUploadOrder() throws CyclicUploadDependencyException {
        List<OsmPrimitive> newToAdd = new LinkedList<>();
        newToAdd.addAll(Utils.filteredCollection(toAdd, Node.class));
        newToAdd.addAll(Utils.filteredCollection(toAdd, Way.class));

        List<Relation> relationsToAdd = new ArrayList<>(Utils.filteredCollection(toAdd, Relation.class));
        List<Relation> noProblemRelations = filterRelationsNotReferringToNewRelations(relationsToAdd);
        newToAdd.addAll(noProblemRelations);
        relationsToAdd.removeAll(noProblemRelations);

        RelationUploadDependencyGraph graph = new RelationUploadDependencyGraph(relationsToAdd, true);
        newToAdd.addAll(graph.computeUploadOrder());
        toAdd = newToAdd;

        List<OsmPrimitive> newToDelete = new LinkedList<>();
        graph = new RelationUploadDependencyGraph(Utils.filteredCollection(toDelete, Relation.class), false);
        newToDelete.addAll(graph.computeUploadOrder());
        newToDelete.addAll(Utils.filteredCollection(toDelete, Way.class));
        newToDelete.addAll(Utils.filteredCollection(toDelete, Node.class));
        toDelete = newToDelete;
    }

    /**
     * Replies the subset of relations in <code>relations</code> which are not referring to any
     * new relation
     *
     * @param relations a list of relations
     * @return the subset of relations in <code>relations</code> which are not referring to any
     * new relation
     */
    protected List<Relation> filterRelationsNotReferringToNewRelations(Collection<Relation> relations) {
        List<Relation> ret = new LinkedList<>();
        for (Relation relation: relations) {
            boolean refersToNewRelation = false;
            for (RelationMember m : relation.getMembers()) {
                if (m.isRelation() && m.getMember().isNewOrUndeleted()) {
                    refersToNewRelation = true;
                    break;
                }
            }
            if (!refersToNewRelation) {
                ret.add(relation);
            }
        }
        return ret;
    }

    /**
     * Utility class to sort a collection of new relations with their dependencies
     * topologically.
     *
     */
    private static class RelationUploadDependencyGraph {
        private final Map<Relation, Set<Relation>> children = new HashMap<>();
        private Collection<Relation> relations;
        private Set<Relation> visited = new HashSet<>();
        private List<Relation> uploadOrder;
        private final boolean newOrUndeleted;

        RelationUploadDependencyGraph(Collection<Relation> relations, boolean newOrUndeleted) {
            this.newOrUndeleted = newOrUndeleted;
            build(relations);
        }

        public final void build(Collection<Relation> relations) {
            this.relations = new HashSet<>();
            for (Relation relation: relations) {
                if (newOrUndeleted ? !relation.isNewOrUndeleted() : !relation.isDeleted()) {
                    continue;
                }
                this.relations.add(relation);
                for (RelationMember m: relation.getMembers()) {
                    if (m.isRelation() && (newOrUndeleted ? m.getMember().isNewOrUndeleted() : m.getMember().isDeleted())) {
                        addDependency(relation, (Relation) m.getMember());
                    }
                }
            }
        }

        public Set<Relation> getChildren(Relation relation) {
            Set<Relation> p = children.get(relation);
            if (p == null) {
                p = new HashSet<>();
                children.put(relation, p);
            }
            return p;
        }

        public void addDependency(Relation relation, Relation child) {
            getChildren(relation).add(child);
        }

        protected void visit(Stack<Relation> path, Relation current) throws CyclicUploadDependencyException {
            if (path.contains(current)) {
                path.push(current);
                throw new CyclicUploadDependencyException(path);
            }
            if (!visited.contains(current)) {
                path.push(current);
                visited.add(current);
                for (Relation dependent : getChildren(current)) {
                    visit(path, dependent);
                }
                uploadOrder.add(current);
                path.pop();
            }
        }

        public List<Relation> computeUploadOrder() throws CyclicUploadDependencyException {
            visited = new HashSet<>();
            uploadOrder = new LinkedList<>();
            Stack<Relation> path = new Stack<>();
            for (Relation relation: relations) {
                visit(path, relation);
            }
            List<Relation> ret = new ArrayList<>(relations);
            ret.sort(Comparator.comparingInt(uploadOrder::indexOf));
            return ret;
        }
    }
}
