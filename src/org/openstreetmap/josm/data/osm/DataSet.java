// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.ChangesetIdChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.tools.Predicate;

/**
 * DataSet is the data behind the application. It can consists of only a few points up to the whole
 * osm database. DataSet's can be merged together, saved, (up/down/disk)loaded etc.
 *
 * Note that DataSet is not an osm-primitive and so has no key association but a few members to
 * store some information.
 *
 * @author imi
 */
public class DataSet implements Cloneable {

    /**
     * Maximum number of events that can be fired between beginUpdate/endUpdate to be send as single events (ie without DatasetChangedEvent)
     */
    private static final int MAX_SINGLE_EVENTS = 30;

    /**
     * Maximum number of events to kept between beginUpdate/endUpdate. When more events are created, that simple DatasetChangedEvent is sent)
     */
    private static final int MAX_EVENTS = 1000;

    private static class IdHash implements Hash<PrimitiveId,OsmPrimitive> {

        public int getHashCode(PrimitiveId k) {
            return (int)k.getUniqueId() ^ k.getType().hashCode();
        }

        public boolean equals(PrimitiveId key, OsmPrimitive value) {
            if (key == null || value == null) return false;
            return key.getUniqueId() == value.getUniqueId() && key.getType() == value.getType();
        }
    }

    private Storage<OsmPrimitive> allPrimitives = new Storage<OsmPrimitive>(new IdHash(), 16, true);
    private Map<PrimitiveId, OsmPrimitive> primitivesMap = allPrimitives.foreignKey(new IdHash());
    private CopyOnWriteArrayList<DataSetListener> listeners = new CopyOnWriteArrayList<DataSetListener>();

    // Number of open calls to beginUpdate
    private int updateCount;
    // Events that occurred while dataset was locked but should be fired after write lock is released
    private final List<AbstractDatasetChangedEvent> cachedEvents = new ArrayList<AbstractDatasetChangedEvent>();

    private int highlightUpdateCount;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Lock getReadLock() {
        return lock.readLock();
    }

    /**
     * This method can be used to detect changes in highlight state of primitives. If highlighting was changed
     * then the method will return different number.
     * @return
     */
    public int getHighlightUpdateCount() {
        return highlightUpdateCount;
    }

    /**
     * Maintain a list of used tags for autocompletion
     */
    private AutoCompletionManager autocomplete;

    public AutoCompletionManager getAutoCompletionManager() {
        if (autocomplete == null) {
            autocomplete = new AutoCompletionManager(this);
            addDataSetListener(autocomplete);
        }
        return autocomplete;
    }

    /**
     * The API version that created this data set, if any.
     */
    private String version;

    /**
     * Replies the API version this dataset was created from. May be null.
     *
     * @return the API version this dataset was created from. May be null.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the API version this dataset was created from.
     *
     * @param version the API version, i.e. "0.5" or "0.6"
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * All nodes goes here, even when included in other data (ways etc). This enables the instant
     * conversion of the whole DataSet by iterating over this data structure.
     */
    private QuadBuckets<Node> nodes = new QuadBuckets<Node>();

    private <T extends OsmPrimitive> Collection<T> getPrimitives(Predicate<OsmPrimitive> predicate) {
        return new DatasetCollection<T>(allPrimitives, predicate);
    }

    /**
     * Replies an unmodifiable collection of nodes in this dataset
     *
     * @return an unmodifiable collection of nodes in this dataset
     */
    public Collection<Node> getNodes() {
        return getPrimitives(OsmPrimitive.nodePredicate);
    }

    public List<Node> searchNodes(BBox bbox) {
        return nodes.search(bbox);
    }

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The way nodes are stored only in the way list.
     */
    private QuadBuckets<Way> ways = new QuadBuckets<Way>();

    /**
     * Replies an unmodifiable collection of ways in this dataset
     *
     * @return an unmodifiable collection of ways in this dataset
     */
    public Collection<Way> getWays() {
        return getPrimitives(OsmPrimitive.wayPredicate);
    }

    public List<Way> searchWays(BBox bbox) {
        return ways.search(bbox);
    }

    /**
     * All relations/relationships
     */
    private Collection<Relation> relations = new ArrayList<Relation>();

    /**
     * Replies an unmodifiable collection of relations in this dataset
     *
     * @return an unmodifiable collection of relations in this dataset
     */
    public Collection<Relation> getRelations() {
        return getPrimitives(OsmPrimitive.relationPredicate);
    }

    public List<Relation> searchRelations(BBox bbox) {
        // QuadBuckets might be useful here (don't forget to do reindexing after some of rm is changed)
        List<Relation> result = new ArrayList<Relation>();
        for (Relation r: relations) {
            if (r.getBBox().intersects(bbox)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * All data sources of this DataSet.
     */
    public Collection<DataSource> dataSources = new LinkedList<DataSource>();

    /**
     * @return A collection containing all primitives of the dataset. Data are not ordered
     */
    public Collection<OsmPrimitive> allPrimitives() {
        return getPrimitives(OsmPrimitive.allPredicate);
    }

    /**
     * @return A collection containing all not-deleted primitives (except keys).
     */
    public Collection<OsmPrimitive> allNonDeletedPrimitives() {
        return getPrimitives(OsmPrimitive.nonDeletedPredicate);
    }

    public Collection<OsmPrimitive> allNonDeletedCompletePrimitives() {
        return getPrimitives(OsmPrimitive.nonDeletedCompletePredicate);
    }

    public Collection<OsmPrimitive> allNonDeletedPhysicalPrimitives() {
        return getPrimitives(OsmPrimitive.nonDeletedPhysicalPredicate);
    }

    public Collection<OsmPrimitive> allModifiedPrimitives() {
        return getPrimitives(OsmPrimitive.modifiedPredicate);
    }

    /**
     * Adds a primitive to the dataset
     *
     * @param primitive the primitive.
     */
    public void addPrimitive(OsmPrimitive primitive) {
        beginUpdate();
        try {
            if (getPrimitiveById(primitive) != null)
                throw new DataIntegrityProblemException(
                        tr("Unable to add primitive {0} to the dataset because it is already included", primitive.toString()));

            primitive.updatePosition(); // Set cached bbox for way and relation (required for reindexWay and reinexRelation to work properly)
            if (primitive instanceof Node) {
                nodes.add((Node) primitive);
            } else if (primitive instanceof Way) {
                ways.add((Way) primitive);
            } else if (primitive instanceof Relation) {
                relations.add((Relation) primitive);
            }
            allPrimitives.add(primitive);
            primitive.setDataset(this);
            firePrimitivesAdded(Collections.singletonList(primitive), false);
        } finally {
            endUpdate();
        }
    }

    public OsmPrimitive addPrimitive(PrimitiveData data) {
        beginUpdate();
        try {
            OsmPrimitive result;
            if (data instanceof NodeData) {
                result = new Node();
            } else if (data instanceof WayData) {
                result = new Way();
            } else if (data instanceof RelationData) {
                result = new Relation();
            } else
                throw new AssertionError();
            result.setDataset(this);
            result.load(data);
            addPrimitive(result);
            return result;
        } finally {
            endUpdate();
        }
    }

    /**
     * Removes a primitive from the dataset. This method only removes the
     * primitive form the respective collection of primitives managed
     * by this dataset, i.e. from {@see #nodes}, {@see #ways}, or
     * {@see #relations}. References from other primitives to this
     * primitive are left unchanged.
     *
     * @param primitive the primitive
     */
    public void removePrimitive(PrimitiveId primitiveId) {
        beginUpdate();
        try {
            OsmPrimitive primitive = getPrimitiveByIdChecked(primitiveId);
            if (primitive == null)
                return;
            if (primitive instanceof Node) {
                nodes.remove(primitive);
            } else if (primitive instanceof Way) {
                ways.remove(primitive);
            } else if (primitive instanceof Relation) {
                relations.remove(primitive);
            }
            selectedPrimitives.remove(primitive);
            allPrimitives.remove(primitive);
            primitive.setDataset(null);
            firePrimitivesRemoved(Collections.singletonList(primitive), false);
        } finally {
            endUpdate();
        }
    }

    /*---------------------------------------------------
     *   SELECTION HANDLING
     *---------------------------------------------------*/

    /**
     * A list of listeners to selection changed events. The list is static, as listeners register
     * themselves for any dataset selection changes that occur, regardless of the current active
     * dataset. (However, the selection does only change in the active layer)
     */
    public static final Collection<SelectionChangedListener> selListeners =
        Collections.synchronizedList(new LinkedList<SelectionChangedListener>());

    /**
     * Notifies all registered {@see SelectionChangedListener} about the current selection in
     * this dataset.
     *
     */
    public void fireSelectionChanged(){
        synchronized (selListeners) {
            List<? extends OsmPrimitive> currentSelection = Collections.unmodifiableList(new ArrayList<OsmPrimitive>(selectedPrimitives));
            for (SelectionChangedListener l : selListeners) {
                l.selectionChanged(currentSelection);
            }
        }
    }

    LinkedHashSet<OsmPrimitive> selectedPrimitives = new LinkedHashSet<OsmPrimitive>();

    public Collection<OsmPrimitive> getSelectedNodesAndWays() {
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : selectedPrimitives) {
            if (osm instanceof Way ||
                    osm instanceof Node) {
                sel.add(osm);
            }
        }
        return sel;
    }

    /**
     * Replies an unmodifiable collection of primitives currently selected
     * in this dataset
     *
     * @return unmodifiable collection of primitives
     */
    public Collection<OsmPrimitive> getSelected() {
        return Collections.unmodifiableSet(selectedPrimitives);
    }

    /**
     * Return selected nodes.
     */
    public Collection<Node> getSelectedNodes() {
        List<Node> result = new ArrayList<Node>(selectedPrimitives.size());
        for (OsmPrimitive primitive:selectedPrimitives) {
            if (primitive instanceof Node) {
                result.add((Node)primitive);
            }
        }
        return result;
    }

    /**
     * Return selected ways.
     */
    public Collection<Way> getSelectedWays() {
        List<Way> result = new ArrayList<Way>(selectedPrimitives.size());
        for (OsmPrimitive primitive:selectedPrimitives) {
            if (primitive instanceof Way) {
                result.add((Way)primitive);
            }
        }
        return result;
    }

    /**
     * Return selected relations.
     */
    public Collection<Relation> getSelectedRelations() {
        List<Relation> result = new ArrayList<Relation>(selectedPrimitives.size() / 10);
        for (OsmPrimitive primitive:selectedPrimitives) {
            if (primitive instanceof Relation) {
                result.add((Relation)primitive);
            }
        }
        return result;
    }

    public boolean isSelected(OsmPrimitive osm) {
        return selectedPrimitives.contains(osm);
    }

    public void toggleSelected(Collection<? extends PrimitiveId> osm) {
        boolean changed = false;
        for (PrimitiveId o : osm) {
            changed = changed | this.__toggleSelected(o);
        }
        if (changed) {
            fireSelectionChanged();
        }
    }
    public void toggleSelected(PrimitiveId... osm) {
        toggleSelected(Arrays.asList(osm));
    }
    private boolean __toggleSelected(PrimitiveId primitiveId) {
        OsmPrimitive primitive = getPrimitiveByIdChecked(primitiveId);
        if (primitive == null)
            return false;
        if (!selectedPrimitives.remove(primitive)) {
            selectedPrimitives.add(primitive);
        }
        return true;
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>.
     * Notifies all {@see SelectionChangedListener} if <code>fireSelectionChangeEvent</code> is true.
     *
     * @param selection the selection
     * @param fireSelectionChangeEvent true, if the selection change listeners are to be notified; false, otherwise
     */
    public void setSelected(Collection<? extends PrimitiveId> selection, boolean fireSelectionChangeEvent) {
        boolean wasEmpty = selectedPrimitives.isEmpty();
        selectedPrimitives = new LinkedHashSet<OsmPrimitive>();
        addSelected(selection, fireSelectionChangeEvent);
        if (!wasEmpty && selectedPrimitives.isEmpty() && fireSelectionChangeEvent) {
            fireSelectionChanged();
        }
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>
     * and notifies all {@see SelectionChangedListener}.
     *
     * @param selection the selection
     */
    public void setSelected(Collection<? extends PrimitiveId> selection) {
        setSelected(selection, true /* fire selection change event */);
    }

    public void setSelected(PrimitiveId... osm) {
        if (osm.length == 1 && osm[0] == null) {
            setSelected();
            return;
        }
        List<PrimitiveId> list = Arrays.asList(osm);
        setSelected(list);
    }

    /**
     * Adds   the primitives in <code>selection</code> to the current selection
     * and notifies all {@see SelectionChangedListener}.
     *
     * @param selection the selection
     */
    public void addSelected(Collection<? extends PrimitiveId> selection) {
        addSelected(selection, true /* fire selection change event */);
    }

    public void addSelected(PrimitiveId... osm) {
        addSelected(Arrays.asList(osm));
    }

    /**
     * Adds the primitives in <code>selection</code> to the current selection.
     * Notifies all {@see SelectionChangedListener} if <code>fireSelectionChangeEvent</code> is true.
     *
     * @param selection the selection
     * @param fireSelectionChangeEvent true, if the selection change listeners are to be notified; false, otherwise
     */
    public void addSelected(Collection<? extends PrimitiveId> selection, boolean fireSelectionChangeEvent) {
        boolean changed = false;
        for (PrimitiveId id: selection) {
            OsmPrimitive primitive = getPrimitiveByIdChecked(id);
            if (primitive != null) {
                changed = changed | selectedPrimitives.add(primitive);
            }
        }
        if (fireSelectionChangeEvent && changed) {
            fireSelectionChanged();
        }
    }

    /**
     * Remove the selection from every value in the collection.
     * @param list The collection to remove the selection from.
     */
    public void clearSelection(PrimitiveId... osm) {
        clearSelection(Arrays.asList(osm));
    }
    public void clearSelection(Collection<? extends PrimitiveId> list) {
        boolean changed = false;
        for (PrimitiveId id:list) {
            OsmPrimitive primitive = getPrimitiveById(id);
            if (primitive != null) {
                changed = changed | selectedPrimitives.remove(primitive);
            }
        }
        if (changed) {
            fireSelectionChanged();
        }
    }
    public void clearSelection() {
        if (!selectedPrimitives.isEmpty()) {
            selectedPrimitives.clear();
            fireSelectionChanged();
        }
    }

    /*------------------------------------------------------
     * FILTERED / DISABLED HANDLING
     *-----------------------------------------------------*/

    /**
     * TODO: can be removed if no longer needed
     *
     * Currently, the filter flags are updated directly for the primitives.
     * On the long run there might be listeners for filter changes,
     * so the control needs to be moved to this place again.
     */

    //    public void setDisabled(OsmPrimitive... osm) {
    //        if (osm.length == 1 && osm[0] == null) {
    //            setDisabled();
    //            return;
    //        }
    //        clearDisabled(allPrimitives());
    //        for (OsmPrimitive o : osm)
    //            if (o != null) {
    //                o.setDisabled(true);
    //            }
    //    }
    //
    //    public void setDisabled(Collection<? extends OsmPrimitive> selection) {
    //        clearDisabled(nodes);
    //        clearDisabled(ways);
    //        clearDisabled(relations);
    //        for (OsmPrimitive osm : selection) {
    //            osm.setDisabled(true);
    //        }
    //    }
    //
    //    /**
    //     * Remove the disabled parameter from every value in the collection.
    //     * @param list The collection to remove the disabled parameter from.
    //     */
    //    private void clearDisabled(Collection<? extends OsmPrimitive> list) {
    //        for (OsmPrimitive osm : list) {
    //            osm.setDisabled(false);
    //        }
    //    }
    //
    //
    //    public void setFiltered(Collection<? extends OsmPrimitive> selection) {
    //        clearFiltered(nodes);
    //        clearFiltered(ways);
    //        clearFiltered(relations);
    //        for (OsmPrimitive osm : selection) {
    //            osm.setFiltered(true);
    //        }
    //    }
    //
    //    public void setFiltered(OsmPrimitive... osm) {
    //        if (osm.length == 1 && osm[0] == null) {
    //            setFiltered();
    //            return;
    //        }
    //        clearFiltered(nodes);
    //        clearFiltered(ways);
    //        clearFiltered(relations);
    //        for (OsmPrimitive o : osm)
    //            if (o != null) {
    //                o.setFiltered(true);
    //            }
    //    }
    //
    //    /**
    //     * Remove the filtered parameter from every value in the collection.
    //     * @param list The collection to remove the filtered parameter from.
    //     */
    //    private void clearFiltered(Collection<? extends OsmPrimitive> list) {
    //        if (list == null)
    //            return;
    //        for (OsmPrimitive osm : list) {
    //            osm.setFiltered(false);
    //        }
    //    }

    @Override public DataSet clone() {
        getReadLock().lock();
        try {
            DataSet ds = new DataSet();
            HashMap<OsmPrimitive, OsmPrimitive> primitivesMap = new HashMap<OsmPrimitive, OsmPrimitive>();
            for (Node n : nodes) {
                Node newNode = new Node(n);
                primitivesMap.put(n, newNode);
                ds.addPrimitive(newNode);
            }
            for (Way w : ways) {
                Way newWay = new Way(w);
                primitivesMap.put(w, newWay);
                List<Node> newNodes = new ArrayList<Node>();
                for (Node n: w.getNodes()) {
                    newNodes.add((Node)primitivesMap.get(n));
                }
                newWay.setNodes(newNodes);
                ds.addPrimitive(newWay);
            }
            // Because relations can have other relations as members we first clone all relations
            // and then get the cloned members
            for (Relation r : relations) {
                Relation newRelation = new Relation(r, r.isNew());
                newRelation.setMembers(null);
                primitivesMap.put(r, newRelation);
                ds.addPrimitive(newRelation);
            }
            for (Relation r : relations) {
                Relation newRelation = (Relation)primitivesMap.get(r);
                List<RelationMember> newMembers = new ArrayList<RelationMember>();
                for (RelationMember rm: r.getMembers()) {
                    newMembers.add(new RelationMember(rm.getRole(), primitivesMap.get(rm.getMember())));
                }
                newRelation.setMembers(newMembers);
            }
            for (DataSource source : dataSources) {
                ds.dataSources.add(new DataSource(source.bounds, source.origin));
            }
            ds.version = version;
            return ds;
        } finally {
            getReadLock().unlock();
        }
    }

    /**
     * Returns the total area of downloaded data (the "yellow rectangles").
     * @return Area object encompassing downloaded data.
     */
    public Area getDataSourceArea() {
        if (dataSources.isEmpty()) return null;
        Area a = new Area();
        for (DataSource source : dataSources) {
            // create area from data bounds
            a.add(new Area(source.bounds.asRect()));
        }
        return a;
    }

    /**
     * returns a  primitive with a given id from the data set. null, if no such primitive
     * exists
     *
     * @param id  uniqueId of the primitive. Might be < 0 for newly created primitives
     * @param type the type of  the primitive. Must not be null.
     * @return the primitive
     * @exception NullPointerException thrown, if type is null
     */
    public OsmPrimitive getPrimitiveById(long id, OsmPrimitiveType type) {
        return getPrimitiveById(new SimplePrimitiveId(id, type), false);
    }

    public OsmPrimitive getPrimitiveById(PrimitiveId primitiveId) {
        return getPrimitiveById(primitiveId, false);
    }

    public OsmPrimitive getPrimitiveById(PrimitiveId primitiveId, boolean createNew) {
        OsmPrimitive result = primitivesMap.get(primitiveId);

        if (result == null && createNew) {
            switch (primitiveId.getType()) {
            case NODE: result = new Node(primitiveId.getUniqueId(), true); break;
            case WAY: result = new Way(primitiveId.getUniqueId(), true); break;
            case RELATION: result = new Relation(primitiveId.getUniqueId(), true); break;
            }
            addPrimitive(result);
        }

        return result;
    }

    /**
     * Show message and stack trace in log in case primitive is not found
     * @param primitiveId
     * @return Primitive by id.
     */
    private OsmPrimitive getPrimitiveByIdChecked(PrimitiveId primitiveId) {
        OsmPrimitive result = getPrimitiveById(primitiveId);
        if (result == null) {
            System.out.println(tr("JOSM expected to find primitive [{0} {1}] in dataset but it is not there. Please report this "
                    + " at http://josm.openstreetmap.de . This is not a critical error, it should be safe to continue in your work.",
                    primitiveId.getType(), Long.toString(primitiveId.getUniqueId())));
            new Exception().printStackTrace();
        }

        return result;
    }

    private void deleteWay(Way way) {
        way.setNodes(null);
        way.setDeleted(true);
    }

    /**
     * removes all references from ways in this dataset to a particular node
     *
     * @param node the node
     */
    public void unlinkNodeFromWays(Node node) {
        beginUpdate();
        try {
            for (Way way: ways) {
                List<Node> nodes = way.getNodes();
                if (nodes.remove(node)) {
                    if (nodes.size() < 2) {
                        deleteWay(way);
                    } else {
                        way.setNodes(nodes);
                    }
                }
            }
        } finally {
            endUpdate();
        }
    }

    /**
     * removes all references from relations in this dataset  to this primitive
     *
     * @param primitive the primitive
     */
    public void unlinkPrimitiveFromRelations(OsmPrimitive primitive) {
        beginUpdate();
        try {
            for (Relation relation : relations) {
                List<RelationMember> members = relation.getMembers();

                Iterator<RelationMember> it = members.iterator();
                boolean removed = false;
                while(it.hasNext()) {
                    RelationMember member = it.next();
                    if (member.getMember().equals(primitive)) {
                        it.remove();
                        removed = true;
                    }
                }

                if (removed) {
                    relation.setMembers(members);
                }
            }
        } finally {
            endUpdate();
        }
    }

    /**
     * removes all references from from other primitives  to the
     * referenced primitive
     *
     * @param referencedPrimitive the referenced primitive
     */
    public void unlinkReferencesToPrimitive(OsmPrimitive referencedPrimitive) {
        beginUpdate();
        try {
            if (referencedPrimitive instanceof Node) {
                unlinkNodeFromWays((Node)referencedPrimitive);
                unlinkPrimitiveFromRelations(referencedPrimitive);
            } else {
                unlinkPrimitiveFromRelations(referencedPrimitive);
            }
        } finally {
            endUpdate();
        }
    }

    /**
     * Replies true if there is at least one primitive in this dataset with
     * {@see OsmPrimitive#isModified()} == <code>true</code>.
     *
     * @return true if there is at least one primitive in this dataset with
     * {@see OsmPrimitive#isModified()} == <code>true</code>.
     */
    public boolean isModified() {
        for (Node n: nodes) {
            if (n.isModified()) return true;
        }
        for (Way w: ways) {
            if (w.isModified()) return true;
        }
        for (Relation r: relations) {
            if (r.isModified()) return true;
        }
        return false;
    }

    private void reindexNode(Node node, LatLon newCoor) {
        nodes.remove(node);
        node.setCoorInternal(newCoor);
        nodes.add(node);
        for (OsmPrimitive primitive: node.getReferrers()) {
            if (primitive instanceof Way) {
                reindexWay((Way)primitive);
            } else {
                reindexRelation((Relation) primitive);
            }
        }
    }

    private void reindexWay(Way way) {
        BBox before = way.getBBox();
        ways.remove(way);
        way.updatePosition();
        ways.add(way);
        if (!way.getBBox().equals(before)) {
            for (OsmPrimitive primitive: way.getReferrers()) {
                reindexRelation((Relation)primitive);
            }
        }
    }

    private void reindexRelation(Relation relation) {
        BBox before = relation.getBBox();
        relation.updatePosition();
        if (!before.equals(relation.getBBox())) {
            for (OsmPrimitive primitive: relation.getReferrers()) {
                reindexRelation((Relation) primitive);
            }
        }
    }

    public void addDataSetListener(DataSetListener dsl) {
        listeners.addIfAbsent(dsl);
    }

    public void removeDataSetListener(DataSetListener dsl) {
        listeners.remove(dsl);
    }

    /**
     * Can be called before bigger changes on dataset. Events are disabled until {@link #endUpdate()}.
     * {@link DataSetListener#dataChanged()} event is triggered after end of changes
     * <br>
     * Typical usecase should look like this:
     * <pre>
     * ds.beginUpdate();
     * try {
     *   ...
     * } finally {
     *   ds.endUpdate();
     * }
     * </pre>
     */
    public void beginUpdate() {
        lock.writeLock().lock();
        updateCount++;
    }

    /**
     * @see DataSet#beginUpdate()
     */
    public void endUpdate() {
        if (updateCount > 0) {
            updateCount--;
            if (updateCount == 0) {
                List<AbstractDatasetChangedEvent> eventsCopy = new ArrayList<AbstractDatasetChangedEvent>(cachedEvents);
                cachedEvents.clear();
                lock.writeLock().unlock();

                if (!eventsCopy.isEmpty()) {
                    lock.readLock().lock();
                    try {
                        if (eventsCopy.size() < MAX_SINGLE_EVENTS) {
                            for (AbstractDatasetChangedEvent event: eventsCopy) {
                                fireEventToListeners(event);
                            }
                        } else if (eventsCopy.size() == MAX_EVENTS) {
                            fireEventToListeners(new DataChangedEvent(this));
                        } else {
                            fireEventToListeners(new DataChangedEvent(this, eventsCopy));
                        }
                    } finally {
                        lock.readLock().unlock();
                    }
                }
            } else {
                lock.writeLock().unlock();
            }

        } else
            throw new AssertionError("endUpdate called without beginUpdate");
    }

    private void fireEventToListeners(AbstractDatasetChangedEvent event) {
        for (DataSetListener listener: listeners) {
            event.fire(listener);
        }
    }

    private void fireEvent(AbstractDatasetChangedEvent event) {
        if (updateCount == 0)
            throw new AssertionError("dataset events can be fired only when dataset is locked");
        if (cachedEvents.size() < MAX_EVENTS) {
            cachedEvents.add(event);
        }
    }

    void firePrimitivesAdded(Collection<? extends OsmPrimitive> added, boolean wasIncomplete) {
        fireEvent(new PrimitivesAddedEvent(this, added, wasIncomplete));
    }

    void firePrimitivesRemoved(Collection<? extends OsmPrimitive> removed, boolean wasComplete) {
        fireEvent(new PrimitivesRemovedEvent(this, removed, wasComplete));
    }

    void fireTagsChanged(OsmPrimitive prim, Map<String, String> originalKeys) {
        fireEvent(new TagsChangedEvent(this, prim, originalKeys));
    }

    void fireRelationMembersChanged(Relation r) {
        reindexRelation(r);
        fireEvent(new RelationMembersChangedEvent(this, r));
    }

    void fireNodeMoved(Node node, LatLon newCoor) {
        reindexNode(node, newCoor);
        fireEvent(new NodeMovedEvent(this, node));
    }

    void fireWayNodesChanged(Way way) {
        reindexWay(way);
        fireEvent(new WayNodesChangedEvent(this, way));
    }

    void fireChangesetIdChanged(OsmPrimitive primitive, int oldChangesetId, int newChangesetId) {
        fireEvent(new ChangesetIdChangedEvent(this, Collections.singletonList(primitive), oldChangesetId, newChangesetId));
    }

    void fireHighlightingChanged(OsmPrimitive primitive) {
        highlightUpdateCount++;
    }

    public void clenupDeletedPrimitives() {
        beginUpdate();
        try {
            if (cleanupDeleted(nodes.iterator())
                    | cleanupDeleted(ways.iterator())
                    | cleanupDeleted(relations.iterator())) {
                fireSelectionChanged();
            }
        } finally {
            endUpdate();
        }
    }

    private boolean cleanupDeleted(Iterator<? extends OsmPrimitive> it) {
        boolean changed = false;
        while (it.hasNext()) {
            OsmPrimitive primitive = it.next();
            if (primitive.isDeleted()) {
                selectedPrimitives.remove(primitive);
                allPrimitives.remove(primitive);
                primitive.setDataset(null);
                changed = true;
                it.remove();
            }
        }
        return changed;
    }

    /**
     * Removes all primitives from the dataset and resets the currently selected primitives
     * to the empty collection. Also notifies selection change listeners if necessary.
     *
     */
    public void clear() {
        beginUpdate();
        try {
            clearSelection();
            for (OsmPrimitive primitive:allPrimitives) {
                primitive.setDataset(null);
            }
            nodes.clear();
            ways.clear();
            relations.clear();
            allPrimitives.clear();
        } finally {
            endUpdate();
        }
    }

    /**
     * Marks all "invisible" objects as deleted. These objects should be always marked as
     * deleted when downloaded from the server. They can be undeleted later if necessary.
     *
     */
    public void deleteInvisible() {
        for (OsmPrimitive primitive:allPrimitives) {
            if (!primitive.isVisible()) {
                primitive.setDeleted(true);
            }
        }
    }
}
