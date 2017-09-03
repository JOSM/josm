// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Data;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionAddEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionChangeEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionRemoveEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionReplaceEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionToggleEvent;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.ChangesetIdChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitiveFlagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * DataSet is the data behind the application. It can consists of only a few points up to the whole
 * osm database. DataSet's can be merged together, saved, (up/down/disk)loaded etc.
 *
 * Note that DataSet is not an osm-primitive and so has no key association but a few members to
 * store some information.
 *
 * Dataset is threadsafe - accessing Dataset simultaneously from different threads should never
 * lead to data corruption or ConcurrentModificationException. However when for example one thread
 * removes primitive and other thread try to add another primitive referring to the removed primitive,
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
public final class DataSet extends QuadBucketPrimitiveStore implements Data, ProjectionChangeListener {

    /**
     * Upload policy.
     *
     * Determines if upload to the OSM server is intended, discouraged, or
     * disabled / blocked.
     */
    public enum UploadPolicy {
        /**
         * Normal dataset, upload intended.
         */
        NORMAL("true"),
        /**
         * Upload discouraged, for example when using or distributing a private dataset.
         */
        DISCOURAGED("false"),
        /**
         * Upload blocked.
         * Upload options completely disabled. Intended for special cases
         * where a warning dialog is not enough, see #12731.
         *
         * For the user, it shouldn't be too easy to disable this flag.
         */
        BLOCKED("never");

        final String xmlFlag;

        UploadPolicy(String xmlFlag) {
            this.xmlFlag = xmlFlag;
        }

        /**
         * Get the corresponding value of the <code>upload='...'</code> XML-attribute
         * in the .osm file.
         * @return value of the <code>upload</code> attribute
         */
        public String getXmlFlag() {
            return xmlFlag;
        }
    }

    /**
     * Maximum number of events that can be fired between beginUpdate/endUpdate to be send as single events (ie without DatasetChangedEvent)
     */
    private static final int MAX_SINGLE_EVENTS = 30;

    /**
     * Maximum number of events to kept between beginUpdate/endUpdate. When more events are created, that simple DatasetChangedEvent is sent)
     */
    private static final int MAX_EVENTS = 1000;

    private final Storage<OsmPrimitive> allPrimitives = new Storage<>(new Storage.PrimitiveIdHash(), true);
    private final Map<PrimitiveId, OsmPrimitive> primitivesMap = allPrimitives.foreignKey(new Storage.PrimitiveIdHash());
    private final CopyOnWriteArrayList<DataSetListener> listeners = new CopyOnWriteArrayList<>();

    // provide means to highlight map elements that are not osm primitives
    private Collection<WaySegment> highlightedVirtualNodes = new LinkedList<>();
    private Collection<WaySegment> highlightedWaySegments = new LinkedList<>();
    private final ListenerList<HighlightUpdateListener> highlightUpdateListeners = ListenerList.create();

    // Number of open calls to beginUpdate
    private int updateCount;
    // Events that occurred while dataset was locked but should be fired after write lock is released
    private final List<AbstractDatasetChangedEvent> cachedEvents = new ArrayList<>();

    private String name;
    private UploadPolicy uploadPolicy;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The mutex lock that is used to synchronize selection changes.
     */
    private final Object selectionLock = new Object();
    /**
     * The current selected primitives. This is always a unmodifiable set.
     *
     * The set should be ordered in the order in which the primitives have been added to the selection.
     */
    private Set<OsmPrimitive> currentSelectedPrimitives = Collections.emptySet();

    /**
     * A list of listeners that listen to selection changes on this layer.
     */
    private final ListenerList<DataSelectionListener> selectionListeners = ListenerList.create();

    private Area cachedDataSourceArea;
    private List<Bounds> cachedDataSourceBounds;

    /**
     * All data sources of this DataSet.
     */
    private final Collection<DataSource> dataSources = new LinkedList<>();

    private final ConflictCollection conflicts = new ConflictCollection();

    /**
     * Constructs a new {@code DataSet}.
     */
    public DataSet() {
        // Transparently register as projection change listener. No need to explicitly remove
        // the listener, projection change listeners are managed as WeakReferences.
        Main.addProjectionChangeListener(this);
        addSelectionListener((DataSelectionListener) e -> fireDreprecatedSelectionChange(e.getSelection()));
    }

    /**
     * Creates a new {@link DataSet}.
     * @param copyFrom An other {@link DataSet} to copy the contents of this dataset from.
     * @since 10346
     */
    public DataSet(DataSet copyFrom) {
        this();
        copyFrom.getReadLock().lock();
        try {
            Map<OsmPrimitive, OsmPrimitive> primMap = new HashMap<>();
            for (Node n : copyFrom.getNodes()) {
                Node newNode = new Node(n);
                primMap.put(n, newNode);
                addPrimitive(newNode);
            }
            for (Way w : copyFrom.getWays()) {
                Way newWay = new Way(w);
                primMap.put(w, newWay);
                List<Node> newNodes = new ArrayList<>();
                for (Node n: w.getNodes()) {
                    newNodes.add((Node) primMap.get(n));
                }
                newWay.setNodes(newNodes);
                addPrimitive(newWay);
            }
            // Because relations can have other relations as members we first clone all relations
            // and then get the cloned members
            Collection<Relation> relations = copyFrom.getRelations();
            for (Relation r : relations) {
                Relation newRelation = new Relation(r);
                newRelation.setMembers(null);
                primMap.put(r, newRelation);
                addPrimitive(newRelation);
            }
            for (Relation r : relations) {
                Relation newRelation = (Relation) primMap.get(r);
                List<RelationMember> newMembers = new ArrayList<>();
                for (RelationMember rm: r.getMembers()) {
                    newMembers.add(new RelationMember(rm.getRole(), primMap.get(rm.getMember())));
                }
                newRelation.setMembers(newMembers);
            }
            for (DataSource source : copyFrom.dataSources) {
                dataSources.add(new DataSource(source));
            }
            version = copyFrom.version;
            uploadPolicy = copyFrom.uploadPolicy;
        } finally {
            copyFrom.getReadLock().unlock();
        }
    }

    /**
     * Adds a new data source.
     * @param source data source to add
     * @return {@code true} if the collection changed as a result of the call
     * @since 11626
     */
    public synchronized boolean addDataSource(DataSource source) {
        return addDataSources(Collections.singleton(source));
    }

    /**
     * Adds new data sources.
     * @param sources data sources to add
     * @return {@code true} if the collection changed as a result of the call
     * @since 11626
     */
    public synchronized boolean addDataSources(Collection<DataSource> sources) {
        boolean changed = dataSources.addAll(sources);
        if (changed) {
            cachedDataSourceArea = null;
            cachedDataSourceBounds = null;
        }
        return changed;
    }

    /**
     * Returns the lock used for reading.
     * @return the lock used for reading
     */
    public Lock getReadLock() {
        return lock.readLock();
    }

    /**
     * History of selections - shared by plugins and SelectionListDialog
     */
    private final LinkedList<Collection<? extends OsmPrimitive>> selectionHistory = new LinkedList<>();

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
     * Maintains a list of used tags for autocompletion.
     */
    private AutoCompletionManager autocomplete;

    /**
     * Returns the autocompletion manager, which maintains a list of used tags for autocompletion.
     * @return the autocompletion manager
     */
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
     * @param version the API version, i.e. "0.6"
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Determines if upload is being discouraged.
     * (i.e. this dataset contains private data which should not be uploaded)
     * @return {@code true} if upload is being discouraged, {@code false} otherwise
     * @see #setUploadDiscouraged
     * @deprecated use {@link #getUploadPolicy()}
     */
    @Deprecated
    public boolean isUploadDiscouraged() {
        return uploadPolicy == UploadPolicy.DISCOURAGED || uploadPolicy == UploadPolicy.BLOCKED;
    }

    /**
     * Sets the "upload discouraged" flag.
     * @param uploadDiscouraged {@code true} if this dataset contains private data which should not be uploaded
     * @see #isUploadDiscouraged
     * @deprecated use {@link #setUploadPolicy(UploadPolicy)}
     */
    @Deprecated
    public void setUploadDiscouraged(boolean uploadDiscouraged) {
        if (uploadPolicy != UploadPolicy.BLOCKED) {
            this.uploadPolicy = uploadDiscouraged ? UploadPolicy.DISCOURAGED : UploadPolicy.NORMAL;
        }
    }

    /**
     * Get the upload policy.
     * @return the upload policy
     * @see #setUploadPolicy(UploadPolicy)
     */
    public UploadPolicy getUploadPolicy() {
        return this.uploadPolicy;
    }

    /**
     * Sets the upload policy.
     * @param uploadPolicy the upload policy
     * @see #getUploadPolicy()
     */
    public void setUploadPolicy(UploadPolicy uploadPolicy) {
        this.uploadPolicy = uploadPolicy;
    }

    /**
     * Holding bin for changeset tag information, to be applied when or if this is ever uploaded.
     */
    private final Map<String, String> changeSetTags = new HashMap<>();

    /**
     * Replies the set of changeset tags to be applied when or if this is ever uploaded.
     * @return the set of changeset tags
     * @see #addChangeSetTag
     */
    public Map<String, String> getChangeSetTags() {
        return changeSetTags;
    }

    /**
     * Adds a new changeset tag.
     * @param k Key
     * @param v Value
     * @see #getChangeSetTags
     */
    public void addChangeSetTag(String k, String v) {
        this.changeSetTags.put(k, v);
    }

    /**
     * Gets a filtered collection of primitives matching the given predicate.
     * @param <T> The primitive type.
     * @param predicate The predicate to match
     * @return The list of primtives.
     * @since 10590
     */
    public <T extends OsmPrimitive> Collection<T> getPrimitives(Predicate<? super OsmPrimitive> predicate) {
        return new SubclassFilteredCollection<>(allPrimitives, predicate);
    }

    /**
     * Replies an unmodifiable collection of nodes in this dataset
     *
     * @return an unmodifiable collection of nodes in this dataset
     */
    public Collection<Node> getNodes() {
        return getPrimitives(Node.class::isInstance);
    }

    @Override
    public List<Node> searchNodes(BBox bbox) {
        lock.readLock().lock();
        try {
            return super.searchNodes(bbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Replies an unmodifiable collection of ways in this dataset
     *
     * @return an unmodifiable collection of ways in this dataset
     */
    public Collection<Way> getWays() {
        return getPrimitives(Way.class::isInstance);
    }

    @Override
    public List<Way> searchWays(BBox bbox) {
        lock.readLock().lock();
        try {
            return super.searchWays(bbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Searches for relations in the given bounding box.
     * @param bbox the bounding box
     * @return List of relations in the given bbox. Can be empty but not null
     */
    @Override
    public List<Relation> searchRelations(BBox bbox) {
        lock.readLock().lock();
        try {
            return super.searchRelations(bbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Replies an unmodifiable collection of relations in this dataset
     *
     * @return an unmodifiable collection of relations in this dataset
     */
    public Collection<Relation> getRelations() {
        return getPrimitives(Relation.class::isInstance);
    }

    /**
     * Returns a collection containing all primitives of the dataset.
     * @return A collection containing all primitives of the dataset. Data is not ordered
     */
    public Collection<OsmPrimitive> allPrimitives() {
        return getPrimitives(o -> true);
    }

    /**
     * Returns a collection containing all not-deleted primitives.
     * @return A collection containing all not-deleted primitives.
     * @see OsmPrimitive#isDeleted
     */
    public Collection<OsmPrimitive> allNonDeletedPrimitives() {
        return getPrimitives(p -> !p.isDeleted());
    }

    /**
     * Returns a collection containing all not-deleted complete primitives.
     * @return A collection containing all not-deleted complete primitives.
     * @see OsmPrimitive#isDeleted
     * @see OsmPrimitive#isIncomplete
     */
    public Collection<OsmPrimitive> allNonDeletedCompletePrimitives() {
        return getPrimitives(primitive -> !primitive.isDeleted() && !primitive.isIncomplete());
    }

    /**
     * Returns a collection containing all not-deleted complete physical primitives.
     * @return A collection containing all not-deleted complete physical primitives (nodes and ways).
     * @see OsmPrimitive#isDeleted
     * @see OsmPrimitive#isIncomplete
     */
    public Collection<OsmPrimitive> allNonDeletedPhysicalPrimitives() {
        return getPrimitives(primitive -> !primitive.isDeleted() && !primitive.isIncomplete() && !(primitive instanceof Relation));
    }

    /**
     * Returns a collection containing all modified primitives.
     * @return A collection containing all modified primitives.
     * @see OsmPrimitive#isModified
     */
    public Collection<OsmPrimitive> allModifiedPrimitives() {
        return getPrimitives(OsmPrimitive::isModified);
    }

    /**
     * Adds a primitive to the dataset.
     *
     * @param primitive the primitive.
     */
    @Override
    public void addPrimitive(OsmPrimitive primitive) {
        Objects.requireNonNull(primitive, "primitive");
        beginUpdate();
        try {
            if (getPrimitiveById(primitive) != null)
                throw new DataIntegrityProblemException(
                        tr("Unable to add primitive {0} to the dataset because it is already included", primitive.toString()));

            allPrimitives.add(primitive);
            primitive.setDataset(this);
            primitive.updatePosition(); // Set cached bbox for way and relation (required for reindexWay and reindexRelation to work properly)
            super.addPrimitive(primitive);
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
            removePrimitiveImpl(primitive);
            firePrimitivesRemoved(Collections.singletonList(primitive), false);
        } finally {
            endUpdate();
        }
    }

    private void removePrimitiveImpl(OsmPrimitive primitive) {
        clearSelection(primitive.getPrimitiveId());
        if (primitive.isSelected()) {
            throw new DataIntegrityProblemException("Primitive was re-selected by a selection listener: " + primitive);
        }
        super.removePrimitive(primitive);
        allPrimitives.remove(primitive);
        primitive.setDataset(null);
    }

    @Override
    protected void removePrimitive(OsmPrimitive primitive) {
        beginUpdate();
        try {
            removePrimitiveImpl(primitive);
            firePrimitivesRemoved(Collections.singletonList(primitive), false);
        } finally {
            endUpdate();
        }
    }

    /*---------------------------------------------------
     *   SELECTION HANDLING
     *---------------------------------------------------*/

    /**
     * Add a listener that listens to selection changes in this specific data set.
     * @param listener The listener.
     * @see #removeSelectionListener(DataSelectionListener)
     * @see SelectionEventManager#addSelectionListener(SelectionChangedListener,
     *      org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode)
     *      To add a global listener.
     */
    public void addSelectionListener(DataSelectionListener listener) {
        selectionListeners.addListener(listener);
    }

    /**
     * Remove a listener that listens to selection changes in this specific data set.
     * @param listener The listener.
     * @see #addSelectionListener(DataSelectionListener)
     */
    public void removeSelectionListener(DataSelectionListener listener) {
        selectionListeners.removeListener(listener);
    }

    /*---------------------------------------------------
     *   OLD SELECTION HANDLING
     *---------------------------------------------------*/

    /**
     * A list of listeners to selection changed events. The list is static, as listeners register
     * themselves for any dataset selection changes that occur, regardless of the current active
     * dataset. (However, the selection does only change in the active layer)
     */
    private static final Collection<SelectionChangedListener> selListeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a new selection listener.
     * @param listener The selection listener to add
     * @see #addSelectionListener(DataSelectionListener)
     * @see SelectionEventManager#removeSelectionListener(SelectionChangedListener)
     */
    public static void addSelectionListener(SelectionChangedListener listener) {
        ((CopyOnWriteArrayList<SelectionChangedListener>) selListeners).addIfAbsent(listener);
    }

    /**
     * Removes a selection listener.
     * @param listener The selection listener to remove
     * @see #removeSelectionListener(DataSelectionListener)
     * @see SelectionEventManager#removeSelectionListener(SelectionChangedListener)
     */
    public static void removeSelectionListener(SelectionChangedListener listener) {
        selListeners.remove(listener);
    }

    /**
     * Notifies all registered {@link SelectionChangedListener} about the current selection in
     * this dataset.
     * @deprecated You should never need to do this from the outside.
     */
    @Deprecated
    public void fireSelectionChanged() {
        fireDreprecatedSelectionChange(getAllSelected());
    }

    private static void fireDreprecatedSelectionChange(Collection<? extends OsmPrimitive> currentSelection) {
        for (SelectionChangedListener l : selListeners) {
            l.selectionChanged(currentSelection);
        }
    }

    /**
     * Returns selected nodes and ways.
     * @return selected nodes and ways
     */
    public Collection<OsmPrimitive> getSelectedNodesAndWays() {
        return new SubclassFilteredCollection<>(getSelected(), primitive -> primitive instanceof Node || primitive instanceof Way);
    }

    /**
     * Returns an unmodifiable collection of *WaySegments* whose virtual
     * nodes should be highlighted. WaySegments are used to avoid having
     * to create a VirtualNode class that wouldn't have much purpose otherwise.
     *
     * @return unmodifiable collection of WaySegments
     */
    public Collection<WaySegment> getHighlightedVirtualNodes() {
        return Collections.unmodifiableCollection(highlightedVirtualNodes);
    }

    /**
     * Returns an unmodifiable collection of WaySegments that should be highlighted.
     *
     * @return unmodifiable collection of WaySegments
     */
    public Collection<WaySegment> getHighlightedWaySegments() {
        return Collections.unmodifiableCollection(highlightedWaySegments);
    }

    /**
     * Adds a listener that gets notified whenever way segment / virtual nodes highlights change.
     * @param listener The Listener
     * @since 12014
     */
    public void addHighlightUpdateListener(HighlightUpdateListener listener) {
        highlightUpdateListeners.addListener(listener);
    }

    /**
     * Removes a listener that was added with {@link #addHighlightUpdateListener(HighlightUpdateListener)}
     * @param listener The Listener
     * @since 12014
     */
    public void removeHighlightUpdateListener(HighlightUpdateListener listener) {
        highlightUpdateListeners.removeListener(listener);
    }

    /**
     * Replies an unmodifiable collection of primitives currently selected
     * in this dataset, except deleted ones. May be empty, but not null.
     *
     * When iterating through the set it is ordered by the order in which the primitives were added to the selection.
     *
     * @return unmodifiable collection of primitives
     */
    public Collection<OsmPrimitive> getSelected() {
        return new SubclassFilteredCollection<>(getAllSelected(), p -> !p.isDeleted());
    }

    /**
     * Replies an unmodifiable collection of primitives currently selected
     * in this dataset, including deleted ones. May be empty, but not null.
     *
     * When iterating through the set it is ordered by the order in which the primitives were added to the selection.
     *
     * @return unmodifiable collection of primitives
     */
    public Collection<OsmPrimitive> getAllSelected() {
        return currentSelectedPrimitives;
    }

    /**
     * Returns selected nodes.
     * @return selected nodes
     */
    public Collection<Node> getSelectedNodes() {
        return new SubclassFilteredCollection<>(getSelected(), Node.class::isInstance);
    }

    /**
     * Returns selected ways.
     * @return selected ways
     */
    public Collection<Way> getSelectedWays() {
        return new SubclassFilteredCollection<>(getSelected(), Way.class::isInstance);
    }

    /**
     * Returns selected relations.
     * @return selected relations
     */
    public Collection<Relation> getSelectedRelations() {
        return new SubclassFilteredCollection<>(getSelected(), Relation.class::isInstance);
    }

    /**
     * Determines whether the selection is empty or not
     * @return whether the selection is empty or not
     */
    public boolean selectionEmpty() {
        return currentSelectedPrimitives.isEmpty();
    }

    /**
     * Determines whether the given primitive is selected or not
     * @param osm the primitive
     * @return whether {@code osm} is selected or not
     */
    public boolean isSelected(OsmPrimitive osm) {
        return currentSelectedPrimitives.contains(osm);
    }

    /**
     * set what virtual nodes should be highlighted. Requires a Collection of
     * *WaySegments* to avoid a VirtualNode class that wouldn't have much use
     * otherwise.
     * @param waySegments Collection of way segments
     */
    public void setHighlightedVirtualNodes(Collection<WaySegment> waySegments) {
        if (highlightedVirtualNodes.isEmpty() && waySegments.isEmpty())
            return;

        highlightedVirtualNodes = waySegments;
        fireHighlightingChanged();
    }

    /**
     * set what virtual ways should be highlighted.
     * @param waySegments Collection of way segments
     */
    public void setHighlightedWaySegments(Collection<WaySegment> waySegments) {
        if (highlightedWaySegments.isEmpty() && waySegments.isEmpty())
            return;

        highlightedWaySegments = waySegments;
        fireHighlightingChanged();
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>.
     * Notifies all {@link SelectionChangedListener} if <code>fireSelectionChangeEvent</code> is true.
     *
     * @param selection the selection
     * @param fireSelectionChangeEvent true, if the selection change listeners are to be notified; false, otherwise
     * @deprecated Use {@link #setSelected(Collection)} instead. To bee removed end of 2017. Does not seem to be used by plugins.
     */
    @Deprecated
    public void setSelected(Collection<? extends PrimitiveId> selection, boolean fireSelectionChangeEvent) {
        setSelected(selection);
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param selection the selection
     */
    public void setSelected(Collection<? extends PrimitiveId> selection) {
        setSelected(selection.stream());
    }

    /**
     * Sets the current selection to the primitives in <code>osm</code>
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param osm the primitives to set. <code>null</code> values are ignored for now, but this may be removed in the future.
     */
    public void setSelected(PrimitiveId... osm) {
        setSelected(Stream.of(osm).filter(Objects::nonNull));
    }

    private void setSelected(Stream<? extends PrimitiveId> stream) {
        doSelectionChange(old -> new SelectionReplaceEvent(this, old,
                stream.map(this::getPrimitiveByIdChecked).filter(Objects::nonNull)));
    }

    /**
     * Adds the primitives in <code>selection</code> to the current selection
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param selection the selection
     */
    public void addSelected(Collection<? extends PrimitiveId> selection) {
        addSelected(selection.stream());
    }

    /**
     * Adds the primitives in <code>osm</code> to the current selection
     * and notifies all {@link SelectionChangedListener}.
     *
     * @param osm the primitives to add
     */
    public void addSelected(PrimitiveId... osm) {
        addSelected(Stream.of(osm));
    }

    private void addSelected(Stream<? extends PrimitiveId> stream) {
        doSelectionChange(old -> new SelectionAddEvent(this, old,
                stream.map(this::getPrimitiveByIdChecked).filter(Objects::nonNull)));
    }

    /**
     * Removes the selection from every value in the collection.
     * @param osm The collection of ids to remove the selection from.
     */
    public void clearSelection(PrimitiveId... osm) {
        clearSelection(Stream.of(osm));
    }

    /**
     * Removes the selection from every value in the collection.
     * @param list The collection of ids to remove the selection from.
     */
    public void clearSelection(Collection<? extends PrimitiveId> list) {
        clearSelection(list.stream());
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        setSelected(Stream.empty());
    }

    private void clearSelection(Stream<? extends PrimitiveId> stream) {
        doSelectionChange(old -> new SelectionRemoveEvent(this, old,
                stream.map(this::getPrimitiveByIdChecked).filter(Objects::nonNull)));
    }

    /**
     * Toggles the selected state of the given collection of primitives.
     * @param osm The primitives to toggle
     */
    public void toggleSelected(Collection<? extends PrimitiveId> osm) {
        toggleSelected(osm.stream());
    }

    /**
     * Toggles the selected state of the given collection of primitives.
     * @param osm The primitives to toggle
     */
    public void toggleSelected(PrimitiveId... osm) {
        toggleSelected(Stream.of(osm));
    }

    private void toggleSelected(Stream<? extends PrimitiveId> stream) {
        doSelectionChange(old -> new SelectionToggleEvent(this, old,
                stream.map(this::getPrimitiveByIdChecked).filter(Objects::nonNull)));
    }

    /**
     * Do a selection change.
     * <p>
     * This is the only method that changes the current selection state.
     * @param command A generator that generates the {@link SelectionChangeEvent} for the given base set of currently selected primitives.
     * @return true iff the command did change the selection.
     * @since 12048
     */
    private boolean doSelectionChange(Function<Set<OsmPrimitive>, SelectionChangeEvent> command) {
        synchronized (selectionLock) {
            SelectionChangeEvent event = command.apply(currentSelectedPrimitives);
            if (event.isNop()) {
                return false;
            }
            currentSelectedPrimitives = event.getSelection();
            selectionListeners.fireEvent(l -> l.selectionChanged(event));
            return true;
        }
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

    @Override
    public synchronized Area getDataSourceArea() {
        if (cachedDataSourceArea == null) {
            cachedDataSourceArea = Data.super.getDataSourceArea();
        }
        return cachedDataSourceArea;
    }

    @Override
    public synchronized List<Bounds> getDataSourceBounds() {
        if (cachedDataSourceBounds == null) {
            cachedDataSourceBounds = Data.super.getDataSourceBounds();
        }
        return Collections.unmodifiableList(cachedDataSourceBounds);
    }

    @Override
    public synchronized Collection<DataSource> getDataSources() {
        return Collections.unmodifiableCollection(dataSources);
    }

    /**
     * Returns a primitive with a given id from the data set. null, if no such primitive exists
     *
     * @param id  uniqueId of the primitive. Might be &lt; 0 for newly created primitives
     * @param type the type of  the primitive. Must not be null.
     * @return the primitive
     * @throws NullPointerException if type is null
     */
    public OsmPrimitive getPrimitiveById(long id, OsmPrimitiveType type) {
        return getPrimitiveById(new SimplePrimitiveId(id, type));
    }

    /**
     * Returns a primitive with a given id from the data set. null, if no such primitive exists
     *
     * @param primitiveId type and uniqueId of the primitive. Might be &lt; 0 for newly created primitives
     * @return the primitive
     */
    public OsmPrimitive getPrimitiveById(PrimitiveId primitiveId) {
        return primitiveId != null ? primitivesMap.get(primitiveId) : null;
    }

    /**
     * Show message and stack trace in log in case primitive is not found
     * @param primitiveId primitive id to look for
     * @return Primitive by id.
     */
    private OsmPrimitive getPrimitiveByIdChecked(PrimitiveId primitiveId) {
        OsmPrimitive result = getPrimitiveById(primitiveId);
        if (result == null && primitiveId != null) {
            Logging.warn(tr("JOSM expected to find primitive [{0} {1}] in dataset but it is not there. Please report this "
                    + "at {2}. This is not a critical error, it should be safe to continue in your work.",
                    primitiveId.getType(), Long.toString(primitiveId.getUniqueId()), Main.getJOSMWebsite()));
            Logging.error(new Exception());
        }

        return result;
    }

    private static void deleteWay(Way way) {
        way.setNodes(null);
        way.setDeleted(true);
    }

    /**
     * Removes all references from ways in this dataset to a particular node.
     *
     * @param node the node
     * @return The set of ways that have been modified
     */
    public Set<Way> unlinkNodeFromWays(Node node) {
        Set<Way> result = new HashSet<>();
        beginUpdate();
        try {
            for (Way way : node.getParentWays()) {
                List<Node> wayNodes = way.getNodes();
                if (wayNodes.remove(node)) {
                    if (wayNodes.size() < 2) {
                        deleteWay(way);
                    } else {
                        way.setNodes(wayNodes);
                    }
                    result.add(way);
                }
            }
        } finally {
            endUpdate();
        }
        return result;
    }

    /**
     * removes all references from relations in this dataset  to this primitive
     *
     * @param primitive the primitive
     * @return The set of relations that have been modified
     */
    public Set<Relation> unlinkPrimitiveFromRelations(OsmPrimitive primitive) {
        Set<Relation> result = new HashSet<>();
        beginUpdate();
        try {
            for (Relation relation : getRelations()) {
                List<RelationMember> members = relation.getMembers();

                Iterator<RelationMember> it = members.iterator();
                boolean removed = false;
                while (it.hasNext()) {
                    RelationMember member = it.next();
                    if (member.getMember().equals(primitive)) {
                        it.remove();
                        removed = true;
                    }
                }

                if (removed) {
                    relation.setMembers(members);
                    result.add(relation);
                }
            }
        } finally {
            endUpdate();
        }
        return result;
    }

    /**
     * Removes all references from other primitives to the referenced primitive.
     *
     * @param referencedPrimitive the referenced primitive
     * @return The set of primitives that have been modified
     */
    public Set<OsmPrimitive> unlinkReferencesToPrimitive(OsmPrimitive referencedPrimitive) {
        Set<OsmPrimitive> result = new HashSet<>();
        beginUpdate();
        try {
            if (referencedPrimitive instanceof Node) {
                result.addAll(unlinkNodeFromWays((Node) referencedPrimitive));
            }
            result.addAll(unlinkPrimitiveFromRelations(referencedPrimitive));
        } finally {
            endUpdate();
        }
        return result;
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

    /**
     * Adds a new data set listener.
     * @param dsl The data set listener to add
     */
    public void addDataSetListener(DataSetListener dsl) {
        listeners.addIfAbsent(dsl);
    }

    /**
     * Removes a data set listener.
     * @param dsl The data set listener to remove
     */
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
            List<AbstractDatasetChangedEvent> eventsToFire = Collections.emptyList();
            if (updateCount == 0) {
                eventsToFire = new ArrayList<>(cachedEvents);
                cachedEvents.clear();
            }

            if (!eventsToFire.isEmpty()) {
                lock.readLock().lock();
                lock.writeLock().unlock();
                try {
                    if (eventsToFire.size() < MAX_SINGLE_EVENTS) {
                        for (AbstractDatasetChangedEvent event: eventsToFire) {
                            fireEventToListeners(event);
                        }
                    } else if (eventsToFire.size() == MAX_EVENTS) {
                        fireEventToListeners(new DataChangedEvent(this));
                    } else {
                        fireEventToListeners(new DataChangedEvent(this, eventsToFire));
                    }
                } finally {
                    lock.readLock().unlock();
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

    void firePrimitiveFlagsChanged(OsmPrimitive primitive) {
        fireEvent(new PrimitiveFlagsChangedEvent(this, primitive));
    }

    void fireHighlightingChanged() {
        HighlightUpdateListener.HighlightUpdateEvent e = new HighlightUpdateListener.HighlightUpdateEvent(this);
        highlightUpdateListeners.fireEvent(l -> l.highlightUpdated(e));
    }

    /**
     * Invalidates the internal cache of projected east/north coordinates.
     *
     * This method can be invoked after the globally configured projection method
     * changed.
     */
    public void invalidateEastNorthCache() {
        if (Main.getProjection() == null) return; // sanity check
        beginUpdate();
        try {
            for (Node n: getNodes()) {
                n.invalidateEastNorthCache();
            }
        } finally {
            endUpdate();
        }
    }

    /**
     * Cleanups all deleted primitives (really delete them from the dataset).
     */
    public void cleanupDeletedPrimitives() {
        beginUpdate();
        try {
            Collection<OsmPrimitive> toCleanUp = getPrimitives(
                    primitive -> primitive.isDeleted() && (!primitive.isVisible() || primitive.isNew()));
            if (!toCleanUp.isEmpty()) {
                // We unselect them in advance to not fire a selection change for every primitive
                clearSelection(toCleanUp.stream().map(OsmPrimitive::getPrimitiveId));
                for (OsmPrimitive primitive : toCleanUp) {
                    removePrimitiveImpl(primitive);
                }
                firePrimitivesRemoved(toCleanUp, false);
            }
        } finally {
            endUpdate();
        }
    }

    /**
     * Removes all primitives from the dataset and resets the currently selected primitives
     * to the empty collection. Also notifies selection change listeners if necessary.
     */
    @Override
    public void clear() {
        beginUpdate();
        try {
            clearSelection();
            for (OsmPrimitive primitive:allPrimitives) {
                primitive.setDataset(null);
            }
            super.clear();
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
     * Moves all primitives and datasources from DataSet "from" to this DataSet.
     * @param from The source DataSet
     */
    public void mergeFrom(DataSet from) {
        mergeFrom(from, null);
    }

    /**
     * Moves all primitives and datasources from DataSet "from" to this DataSet.
     * @param from The source DataSet
     * @param progressMonitor The progress monitor
     */
    public synchronized void mergeFrom(DataSet from, ProgressMonitor progressMonitor) {
        if (from != null) {
            new DataSetMerger(this, from).merge(progressMonitor);
            synchronized (from) {
                if (!from.dataSources.isEmpty()) {
                    if (dataSources.addAll(from.dataSources)) {
                        cachedDataSourceArea = null;
                        cachedDataSourceBounds = null;
                    }
                    from.dataSources.clear();
                    from.cachedDataSourceArea = null;
                    from.cachedDataSourceBounds = null;
                }
            }
        }
    }

    /**
     * Replies the set of conflicts currently managed in this layer.
     *
     * @return the set of conflicts currently managed in this layer
     * @since 12672
     */
    public ConflictCollection getConflicts() {
        return conflicts;
    }

    /**
     * Returns the name of this data set (optional).
     * @return the name of this data set. Can be {@code null}
     * @since 12718
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this data set.
     * @param name the new name of this data set. Can be {@code null} to reset it
     * @since 12718
     */
    public void setName(String name) {
        this.name = name;
    }

    /* --------------------------------------------------------------------------------- */
    /* interface ProjectionChangeListner                                                 */
    /* --------------------------------------------------------------------------------- */
    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        invalidateEastNorthCache();
    }

    /**
     * Returns the data sources bounding box.
     * @return the data sources bounding box
     */
    public synchronized ProjectionBounds getDataSourceBoundingBox() {
        BoundingXYVisitor bbox = new BoundingXYVisitor();
        for (DataSource source : dataSources) {
            bbox.visit(source.bounds);
        }
        if (bbox.hasExtend()) {
            return bbox.getBounds();
        }
        return null;
    }
}
