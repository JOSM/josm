// License: GPL. For details, see LICENSE file.
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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
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
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.tools.FilteredCollection;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * DataSet is the data behind the application. It can consists of only a few points up to the whole
 * osm database. DataSet's can be merged together, saved, (up/down/disk)loaded etc.
 *
 * Note that DataSet is not an osm-primitive and so has no key association but a few members to
 * store some information.
 *
 * Dataset is threadsafe - accessing Dataset simultaneously from different threads should never
 * lead to data corruption or ConccurentModificationException. However when for example one thread
 * removes primitive and other thread try to add another primitive reffering to the removed primitive,
 * DataIntegrityException will occur.
 *
 * To prevent such situations, read/write lock is provided. While read lock is used, it's guaranteed that
 * Dataset will not change. Sample usage:
 * <code>
 *   ds.getReadLock().lock();
 *   try {
 *     // .. do something with dataset
 *   } finally {
 *     ds.getReadLock().unlock();
 *   }
 * </code>
 *
 * Write lock should be used in case of bulk operations. In addition to ensuring that other threads can't
 * use dataset in the middle of modifications it also stops sending of dataset events. That's good for performance
 * reasons - GUI can be updated after all changes are done.
 * Sample usage:
 * <code>
 * ds.beginUpdate()
 * try {
 *   // .. do modifications
 * } finally {
 *  ds.endUpdate();
 * }
 * </code>
 *
 * Note that it is not necessary to call beginUpdate/endUpdate for every dataset modification - dataset will get locked
 * automatically.
 *
 * Note that locks cannot be upgraded - if one threads use read lock and and then write lock, dead lock will occur - see #5814 for
 * sample ticket
 *
 * @author imi
 */
public final class DataSet implements Cloneable, ProjectionChangeListener {

    /**
     * Maximum number of events that can be fired between beginUpdate/endUpdate to be send as single events (ie without DatasetChangedEvent)
     */
    private static final int MAX_SINGLE_EVENTS = 30;

    /**
     * Maximum number of events to kept between beginUpdate/endUpdate. When more events are created, that simple DatasetChangedEvent is sent)
     */
    private static final int MAX_EVENTS = 1000;

    private Storage<OsmPrimitive> allPrimitives = new Storage<OsmPrimitive>(new Storage.PrimitiveIdHash(), true);
    private Map<PrimitiveId, OsmPrimitive> primitivesMap = allPrimitives.foreignKey(new Storage.PrimitiveIdHash());
    private CopyOnWriteArrayList<DataSetListener> listeners = new CopyOnWriteArrayList<DataSetListener>();

    // provide means to highlight map elements that are not osm primitives
    private Collection<WaySegment> highlightedVirtualNodes = new LinkedList<WaySegment>();
    private Collection<WaySegment> highlightedWaySegments = new LinkedList<WaySegment>();

    // Number of open calls to beginUpdate
    private int updateCount;
    // Events that occurred while dataset was locked but should be fired after write lock is released
    private final List<AbstractDatasetChangedEvent> cachedEvents = new ArrayList<AbstractDatasetChangedEvent>();

    private int highlightUpdateCount;

    private boolean uploadDiscouraged = false;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Object selectionLock = new Object();

    public DataSet() {
        /*
         * Transparently register as projection change lister. No need to explicitly remove the
         * the listener, projection change listeners are managed as WeakReferences.
         */
        Main.addProjectionChangeListener(this);
    }

    public Lock getReadLock() {
        return lock.readLock();
    }

    /**
     * This method can be used to detect changes in highlight state of primitives. If highlighting was changed
     * then the method will return different number.
     * @return the current highlight counter
     */
    public int getHighlightUpdateCount() {
        return highlightUpdateCount;
    }

    /**
     * History of selections - shared by plugins and SelectionListDialog
     */
    private final LinkedList<Collection<? extends OsmPrimitive>> selectionHistory = new LinkedList<Collection<? extends OsmPrimitive>>();

    /**
     * Replies the history of JOSM selections
     *
     * @return list of history entries
     */
    public LinkedList<Collection<? extends OsmPrimitive>> getSelectionHistory() {
        return selectionHistory;
    }

    /**
     * Clears selection history list
     */
    public void clearSelectionHistory() {
        selectionHistory.clear();
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

    public final boolean isUploadDiscouraged() {
        return uploadDiscouraged;
    }

    public final void setUploadDiscouraged(boolean uploadDiscouraged) {
        this.uploadDiscouraged = uploadDiscouraged;
    }

    /*
     * Holding bin for changeset tag information, to be applied when or if this is ever uploaded.
     */
    private Map<String, String> changeSetTags = new HashMap<String, String>();

    public Map<String, String> getChangeSetTags() {
        return changeSetTags;
    }

    public void addChangeSetTag(String k, String v) {
        this.changeSetTags.put(k,v);
    }

    /**
     * All nodes goes here, even when included in other data (ways etc). This enables the instant
     * conversion of the whole DataSet by iterating over this data structure.
     */
    private QuadBuckets<Node> nodes = new QuadBuckets<Node>();

    private <T extends OsmPrimitive> Collection<T> getPrimitives(Predicate<OsmPrimitive> predicate) {
        return new SubclassFilteredCollection<OsmPrimitive, T>(allPrimitives, predicate);
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
        lock.readLock().lock();
        try {
            return nodes.search(bbox);
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return ways.search(bbox);
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            // QuadBuckets might be useful here (don't forget to do reindexing after some of rm is changed)
            List<Relation> result = new ArrayList<Relation>();
            for (Relation r: relations) {
                if (r.getBBox().intersects(bbox)) {
                    result.add(r);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * All data sources of this DataSet.
     */
    public final Collection<DataSource> dataSources = new LinkedList<DataSource>();

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
            boolean success = false;
            if (primitive instanceof Node) {
                success = nodes.add((Node) primitive);
            } else if (primitive instanceof Way) {
                success = ways.add((Way) primitive);
            } else if (primitive instanceof Relation) {
                success = relations.add((Relation) primitive);
            }
            if (!success)
                throw new RuntimeException("failed to add primitive: "+primitive);
            allPrimitives.add(primitive);
            primitive.setDataset(this);
            firePrimitivesAdded(Collections.singletonList(primitive), false);
        } finally {
            endUpdate();
        }
    }

    /**
     * Removes a primitive from the dataset. This method only removes the
     * primitive form the respective collection of primitives managed
     * by this dataset, i.e. from {@link #nodes}, {@link #ways}, or
     * {@link #relations}. References from other primitives to this
     * primitive are left unchanged.
     *
     * @param primitiveId the id of the primitive
     */
    public void removePrimitive(PrimitiveId primitiveId) {
        beginUpdate();
        try {
            OsmPrimitive primitive = getPrimitiveByIdChecked(primitiveId);
            if (primitive == null)
                return;
            boolean success = false;
            if (primitive instanceof Node) {
                success = nodes.remove(primitive);
            } else if (primitive instanceof Way) {
                success = ways.remove(primitive);
            } else if (primitive instanceof Relation) {
                success = relations.remove(primitive);
            }
            if (!success)
                throw new RuntimeException("failed to remove primitive: "+primitive);
            synchronized (selectionLock) {
                selectedPrimitives.remove(primitive);
                selectionSnapshot = null;
            }
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
    private static final Collection<SelectionChangedListener> selListeners = new CopyOnWriteArrayList<SelectionChangedListener>();

    public static void addSelectionListener(SelectionChangedListener listener) {
        ((CopyOnWriteArrayList<SelectionChangedListener>)selListeners).addIfAbsent(listener);
    }

    public static void removeSelectionListener(SelectionChangedListener listener) {
        selListeners.remove(listener);
    }

    /**
     * Notifies all registered {@link SelectionChangedListener} about the current selection in
     * this dataset.
     *
     */
    public void fireSelectionChanged(){
        Collection<? extends OsmPrimitive> currentSelection = getAllSelected();
        for (SelectionChangedListener l : selListeners) {
            l.selectionChanged(currentSelection);
        }
    }

    private Set<OsmPrimitive> selectedPrimitives = new LinkedHashSet<OsmPrimitive>();
    private Collection<OsmPrimitive> selectionSnapshot;

    public Collection<OsmPrimitive> getSelectedNodesAndWays() {
        return new FilteredCollection<OsmPrimitive>(getSelected(), new Predicate<OsmPrimitive>() {
            @Override
            public boolean evaluate(OsmPrimitive primitive) {
                return primitive instanceof Node || primitive instanceof Way;
            }
        });
    }

    /**
     * returns an unmodifiable collection of *WaySegments* whose virtual
     * nodes should be highlighted. WaySegments are used to avoid having
     * to create a VirtualNode class that wouldn't have much purpose otherwise.
     *
     * @return unmodifiable collection of WaySegments
     */
    public Collection<WaySegment> getHighlightedVirtualNodes() {
        return Collections.unmodifiableCollection(highlightedVirtualNodes);
    }

    /**
     * returns an unmodifiable collection of WaySegments that should be
     * highlighted.
     *
     * @return unmodifiable collection of WaySegments
     */
    public Collection<WaySegment> getHighlightedWaySegments() {
        return Collections.unmodifiableCollection(highlightedWaySegments);
    }

    /**
     * Replies an unmodifiable collection of primitives currently selected
     * in this dataset, except deleted ones. May be empty, but not null.
     *
     * @return unmodifiable collection of primitives
     */
    public Collection<OsmPrimitive> getSelected() {
        return new SubclassFilteredCollection<OsmPrimitive, OsmPrimitive>(getAllSelected(), OsmPrimitive.nonDeletedPredicate);
    }

    /**
     * Replies an unmodifiable collection of primitives currently selected
     * in this dataset, including deleted ones. May be empty, but not null.
     *
     * @return unmodifiable collection of primitives
     */
    public Collection<OsmPrimitive> getAllSelected() {
        Collection<OsmPrimitive> currentList;
        synchronized (selectionLock) {
            if (selectionSnapshot == null) {
                selectionSnapshot = Collections.unmodifiableList(new ArrayList<OsmPrimitive>(selectedPrimitives));
            }
            currentList = selectionSnapshot;
        }
        return currentList;
    }

    /**
     * Return selected nodes.
     */
    public Collection<Node> getSelectedNodes() {
        return new SubclassFilteredCollection<OsmPrimitive, Node>(getSelected(), OsmPrimitive.nodePredicate);
    }

    /**
     * Return selected ways.
     */
    public Collection<Way> getSelectedWays() {
        return new SubclassFilteredCollection<OsmPrimitive, Way>(getSelected(), OsmPrimitive.wayPredicate);
    }

    /**
     * Return selected relations.
     */
    public Collection<Relation> getSelectedRelations() {
        return new SubclassFilteredCollection<OsmPrimitive, Relation>(getSelected(), OsmPrimitive.relationPredicate);
    }

    /**
     * @return whether the selection is empty or not
     */
    public boolean selectionEmpty() {
        return selectedPrimitives.isEmpty();
    }

    public boolean isSelected(OsmPrimitive osm) {
        return selectedPrimitives.contains(osm);
    }

    public void toggleSelected(Collection<? extends PrimitiveId> osm) {
        boolean changed = false;
        synchronized (selectionLock) {
            for (PrimitiveId o : osm) {
                changed = changed | this.__toggleSelected(o);
            }
            if (changed) {
                selectionSnapshot = null;
            }
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
        selectionSnapshot = null;
        return true;
    }

    /**
     * set what virtual nodes should be highlighted. Requires a Collection of
     * *WaySegments* to avoid a VirtualNode class that wouldn't have much use
     * otherwise.
     * @param waySegments Collection of way segments
     */
    public void setHighlightedVirtualNodes(Collection<WaySegment> waySegments) {
        if(highlightedVirtualNodes.isEmpty() && waySegments.isEmpty())
            return;

        highlightedVirtualNodes = waySegments;
        // can't use fireHighlightingChanged because it requires an OsmPrimitive
        highlightUpdateCount++;
    }

    /**
     * set what virtual ways should be highlighted.
     * @param waySegments Collection of way segments
     */
    public void setHighlightedWaySegments(Collection<WaySegment> waySegments) {
        if(highlightedWaySegments.isEmpty() && waySegments.isEmpty())
            return;

        highlightedWaySegments = waySegments;
        // can't use fireHighlightingChanged because it requires an OsmPrimitive
        highlightUpdateCount++;
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>.
     * Notifies all {@link SelectionChangedListener} if <code>fireSelectionChangeEvent</code> is true.
     *
     * @param selection the selection
     * @param fireSelectionChangeEvent true, if the selection change listeners are to be notified; false, otherwise
     */
    public void setSelected(Collection<? extends PrimitiveId> selection, boolean fireSelectionChangeEvent) {
        boolean changed;
        synchronized (selectionLock) {
            LinkedHashSet<OsmPrimitive> oldSelection = new LinkedHashSet<OsmPrimitive>(selectedPrimitives);
            selectedPrimitives = new LinkedHashSet<OsmPrimitive>();
            addSelected(selection, false);
            changed = !oldSelection.equals(selectedPrimitives);
            if (changed) {
                selectionSnapshot = null;
            }
        }

        if (changed && fireSelectionChangeEvent) {
            // If selection is not empty then event was already fired in addSelecteds
            fireSelectionChanged();
        }
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>
     * and notifies all {@link SelectionChangedListener}.
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
     * and notifies all {@link SelectionChangedListener}.
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
     * Notifies all {@link SelectionChangedListener} if <code>fireSelectionChangeEvent</code> is true.
     *
     * @param selection the selection
     * @param fireSelectionChangeEvent true, if the selection change listeners are to be notified; false, otherwise
     * @return if the selection was changed in the process
     */
    private boolean addSelected(Collection<? extends PrimitiveId> selection, boolean fireSelectionChangeEvent) {
        boolean changed = false;
        synchronized (selectionLock) {
            for (PrimitiveId id: selection) {
                OsmPrimitive primitive = getPrimitiveByIdChecked(id);
                if (primitive != null) {
                    changed = changed | selectedPrimitives.add(primitive);
                }
            }
            if (changed) {
                selectionSnapshot = null;
            }
        }
        if (fireSelectionChangeEvent && changed) {
            fireSelectionChanged();
        }
        return changed;
    }

    /**
     * clear all highlights of virtual nodes
     */
    public void clearHighlightedVirtualNodes() {
        setHighlightedVirtualNodes(new ArrayList<WaySegment>());
    }

    /**
     * clear all highlights of way segments
     */
    public void clearHighlightedWaySegments() {
        setHighlightedWaySegments(new ArrayList<WaySegment>());
    }

    /**
     * Remove the selection from every value in the collection.
     * @param osm The collection of ids to remove the selection from.
     */
    public void clearSelection(PrimitiveId... osm) {
        clearSelection(Arrays.asList(osm));
    }
    public void clearSelection(Collection<? extends PrimitiveId> list) {
        boolean changed = false;
        synchronized (selectionLock) {
            for (PrimitiveId id:list) {
                OsmPrimitive primitive = getPrimitiveById(id);
                if (primitive != null) {
                    changed = changed | selectedPrimitives.remove(primitive);
                }
            }
            if (changed) {
                selectionSnapshot = null;
            }
        }
        if (changed) {
            fireSelectionChanged();
        }
    }
    public void clearSelection() {
        if (!selectedPrimitives.isEmpty()) {
            synchronized (selectionLock) {
                selectedPrimitives.clear();
                selectionSnapshot = null;
            }
            fireSelectionChanged();
        }
    }

    @Override public DataSet clone() {
        getReadLock().lock();
        try {
            DataSet ds = new DataSet();
            HashMap<OsmPrimitive, OsmPrimitive> primMap = new HashMap<OsmPrimitive, OsmPrimitive>();
            for (Node n : nodes) {
                Node newNode = new Node(n);
                primMap.put(n, newNode);
                ds.addPrimitive(newNode);
            }
            for (Way w : ways) {
                Way newWay = new Way(w);
                primMap.put(w, newWay);
                List<Node> newNodes = new ArrayList<Node>();
                for (Node n: w.getNodes()) {
                    newNodes.add((Node)primMap.get(n));
                }
                newWay.setNodes(newNodes);
                ds.addPrimitive(newWay);
            }
            // Because relations can have other relations as members we first clone all relations
            // and then get the cloned members
            for (Relation r : relations) {
                Relation newRelation = new Relation(r, r.isNew());
                newRelation.setMembers(null);
                primMap.put(r, newRelation);
                ds.addPrimitive(newRelation);
            }
            for (Relation r : relations) {
                Relation newRelation = (Relation)primMap.get(r);
                List<RelationMember> newMembers = new ArrayList<RelationMember>();
                for (RelationMember rm: r.getMembers()) {
                    newMembers.add(new RelationMember(rm.getRole(), primMap.get(rm.getMember())));
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
     * @param id  uniqueId of the primitive. Might be &lt; 0 for newly created primitives
     * @param type the type of  the primitive. Must not be null.
     * @return the primitive
     * @exception NullPointerException thrown, if type is null
     */
    public OsmPrimitive getPrimitiveById(long id, OsmPrimitiveType type) {
        return getPrimitiveById(new SimplePrimitiveId(id, type));
    }

    public OsmPrimitive getPrimitiveById(PrimitiveId primitiveId) {
        return primitivesMap.get(primitiveId);
    }

    /**
     * Show message and stack trace in log in case primitive is not found
     * @param primitiveId
     * @return Primitive by id.
     */
    private OsmPrimitive getPrimitiveByIdChecked(PrimitiveId primitiveId) {
        OsmPrimitive result = getPrimitiveById(primitiveId);
        if (result == null) {
            Main.warn(tr("JOSM expected to find primitive [{0} {1}] in dataset but it is not there. Please report this "
                    + "at http://josm.openstreetmap.de/. This is not a critical error, it should be safe to continue in your work.",
                    primitiveId.getType(), Long.toString(primitiveId.getUniqueId())));
            Main.error(new Exception());
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
                List<Node> wayNodes = way.getNodes();
                if (wayNodes.remove(node)) {
                    if (wayNodes.size() < 2) {
                        deleteWay(way);
                    } else {
                        way.setNodes(wayNodes);
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
     * removes all references from other primitives to the
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
     * {@link OsmPrimitive#isModified()} == <code>true</code>.
     *
     * @return true if there is at least one primitive in this dataset with
     * {@link OsmPrimitive#isModified()} == <code>true</code>.
     */
    public boolean isModified() {
        for (OsmPrimitive p: allPrimitives) {
            if (p.isModified())
                return true;
        }
        return false;
    }

    private void reindexNode(Node node, LatLon newCoor, EastNorth eastNorth) {
        if (!nodes.remove(node))
            throw new RuntimeException("Reindexing node failed to remove");
        node.setCoorInternal(newCoor, eastNorth);
        if (!nodes.add(node))
            throw new RuntimeException("Reindexing node failed to add");
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
        if (!ways.remove(way))
            throw new RuntimeException("Reindexing way failed to remove");
        way.updatePosition();
        if (!ways.add(way))
            throw new RuntimeException("Reindexing way failed to add");
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
     * {@link DataSetListener#dataChanged(DataChangedEvent event)} event is triggered after end of changes
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

    void fireNodeMoved(Node node, LatLon newCoor, EastNorth eastNorth) {
        reindexNode(node, newCoor, eastNorth);
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

    /**
     * Invalidates the internal cache of projected east/north coordinates.
     *
     * This method can be invoked after the globally configured projection method
     * changed.
     */
    public void invalidateEastNorthCache() {
        if (Main.getProjection() == null) return; // sanity check
        try {
            beginUpdate();
            for (Node n: Utils.filteredCollection(allPrimitives, Node.class)) {
                n.invalidateEastNorthCache();
            }
        } finally {
            endUpdate();
        }
    }

    public void cleanupDeletedPrimitives() {
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
        synchronized (selectionLock) {
            while (it.hasNext()) {
                OsmPrimitive primitive = it.next();
                if (primitive.isDeleted() && (!primitive.isVisible() || primitive.isNew())) {
                    selectedPrimitives.remove(primitive);
                    selectionSnapshot = null;
                    allPrimitives.remove(primitive);
                    primitive.setDataset(null);
                    changed = true;
                    it.remove();
                }
            }
            if (changed) {
                selectionSnapshot = null;
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

    /**
     * <p>Replies the list of data source bounds.</p>
     *
     * <p>Dataset maintains a list of data sources which have been merged into the
     * data set. Each of these sources can optionally declare a bounding box of the
     * data it supplied to the dataset.</p>
     *
     * <p>This method replies the list of defined (non {@code null}) bounding boxes.</p>
     *
     * @return the list of data source bounds. An empty list, if no non-null data source
     * bounds are defined.
     */
    public List<Bounds> getDataSourceBounds() {
        List<Bounds> ret = new ArrayList<Bounds>(dataSources.size());
        for (DataSource ds : dataSources) {
            if (ds.bounds != null) {
                ret.add(ds.bounds);
            }
        }
        return ret;
    }

    /**
     * Moves all primitives and datasources from DataSet "from" to this DataSet
     * @param from The source DataSet
     */
    public void mergeFrom(DataSet from) {
        mergeFrom(from, null);
    }

    /**
     * Moves all primitives and datasources from DataSet "from" to this DataSet
     * @param from The source DataSet
     */
    public void mergeFrom(DataSet from, ProgressMonitor progressMonitor) {
        if (from != null) {
            new DataSetMerger(this, from).merge(progressMonitor);
            dataSources.addAll(from.dataSources);
            from.dataSources.clear();
        }
    }

    /* --------------------------------------------------------------------------------- */
    /* interface ProjectionChangeListner                                                 */
    /* --------------------------------------------------------------------------------- */
    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        invalidateEastNorthCache();
    }
}
