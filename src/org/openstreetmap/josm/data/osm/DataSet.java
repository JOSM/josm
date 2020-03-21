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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.APIDataSet.APIOperation;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData.XMLNamespace;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionAddEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionChangeEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionRemoveEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionReplaceEvent;
import org.openstreetmap.josm.data.osm.DataSelectionListener.SelectionToggleEvent;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.ChangesetIdChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DataSourceAddedEvent;
import org.openstreetmap.josm.data.osm.event.DataSourceRemovedEvent;
import org.openstreetmap.josm.data.osm.event.FilterChangedEvent;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitiveFlagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
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
public final class DataSet implements OsmData<OsmPrimitive, Node, Way, Relation>, ProjectionChangeListener {

    /**
     * Maximum number of events that can be fired between beginUpdate/endUpdate to be send as single events (ie without DatasetChangedEvent)
     */
    private static final int MAX_SINGLE_EVENTS = 30;

    /**
     * Maximum number of events to kept between beginUpdate/endUpdate. When more events are created, that simple DatasetChangedEvent is sent)
     */
    private static final int MAX_EVENTS = 1000;

    private final QuadBucketPrimitiveStore<Node, Way, Relation> store = new QuadBucketPrimitiveStore<>();

    private final Storage<OsmPrimitive> allPrimitives = new Storage<>(new Storage.PrimitiveIdHash(), true);
    private final Map<PrimitiveId, OsmPrimitive> primitivesMap = allPrimitives
            .foreignKey(new Storage.PrimitiveIdHash());
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
    private DownloadPolicy downloadPolicy = DownloadPolicy.NORMAL;
    private UploadPolicy uploadPolicy = UploadPolicy.NORMAL;
    /** Flag used to know if the dataset should not be editable */
    private final AtomicBoolean isReadOnly = new AtomicBoolean(false);

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

    /**
     * A list of listeners that listen to DataSource changes on this layer
     */
    private final ListenerList<DataSourceListener> dataSourceListeners = ListenerList.create();

    private final ConflictCollection conflicts = new ConflictCollection();

    private short mappaintCacheIdx = 1;
    private String remark;

    /**
     * Used to temporarily store namespaces from the GPX file in case the user converts back and forth.
     * Will not be saved to .osm files, but that's not necessary because GPX files won't automatically be overridden after that.
     */
    private List<XMLNamespace> gpxNamespaces;

    /**
     * Constructs a new {@code DataSet}.
     */
    public DataSet() {
        // Transparently register as projection change listener. No need to explicitly remove
        // the listener, projection change listeners are managed as WeakReferences.
        ProjectionRegistry.addProjectionChangeListener(this);
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
                for (Node n : w.getNodes()) {
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
                newRelation.setMembers(r.getMembers().stream()
                        .map(rm -> new RelationMember(rm.getRole(), primMap.get(rm.getMember())))
                        .collect(Collectors.toList()));
            }
            DataSourceAddedEvent addedEvent = new DataSourceAddedEvent(this,
                    new LinkedHashSet<>(dataSources), copyFrom.dataSources.stream());
            for (DataSource source : copyFrom.dataSources) {
                dataSources.add(new DataSource(source));
            }
            dataSourceListeners.fireEvent(d -> d.dataSourceChange(addedEvent));
            version = copyFrom.version;
            uploadPolicy = copyFrom.uploadPolicy;
            downloadPolicy = copyFrom.downloadPolicy;
            isReadOnly.set(copyFrom.isReadOnly.get());
        } finally {
            copyFrom.getReadLock().unlock();
        }
    }

    /**
     * Constructs a new {@code DataSet} initially filled with the given primitives.
     * @param osmPrimitives primitives to add to this data set
     * @since 12726
     */
    public DataSet(OsmPrimitive... osmPrimitives) {
        this();
        update(() -> {
            for (OsmPrimitive o : osmPrimitives) {
                addPrimitive(o);
            }
        });
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
        DataSourceAddedEvent addedEvent = new DataSourceAddedEvent(this,
                new LinkedHashSet<>(dataSources), sources.stream());
        boolean changed = dataSources.addAll(sources);
        if (changed) {
            cachedDataSourceArea = null;
            cachedDataSourceBounds = null;
        }
        dataSourceListeners.fireEvent(d -> d.dataSourceChange(addedEvent));
        return changed;
    }

    @Override
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
     * The API version that created this data set, if any.
     */
    private String version;

    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Sets the API version this dataset was created from.
     *
     * @param version the API version, i.e. "0.6"
     * @throws IllegalStateException if the dataset is read-only
     */
    public void setVersion(String version) {
        checkModifiable();
        this.version = version;
    }

    @Override
    public DownloadPolicy getDownloadPolicy() {
        return this.downloadPolicy;
    }

    @Override
    public void setDownloadPolicy(DownloadPolicy downloadPolicy) {
        this.downloadPolicy = Objects.requireNonNull(downloadPolicy);
    }

    @Override
    public UploadPolicy getUploadPolicy() {
        return this.uploadPolicy;
    }

    @Override
    public void setUploadPolicy(UploadPolicy uploadPolicy) {
        this.uploadPolicy = Objects.requireNonNull(uploadPolicy);
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

    @Override
    public <T extends OsmPrimitive> Collection<T> getPrimitives(Predicate<? super OsmPrimitive> predicate) {
        return new SubclassFilteredCollection<>(allPrimitives, predicate);
    }

    @Override
    public Collection<Node> getNodes() {
        return getPrimitives(Node.class::isInstance);
    }

    @Override
    public List<Node> searchNodes(BBox bbox) {
        lock.readLock().lock();
        try {
            return store.searchNodes(bbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Way> getWays() {
        return getPrimitives(Way.class::isInstance);
    }

    @Override
    public List<Way> searchWays(BBox bbox) {
        lock.readLock().lock();
        try {
            return store.searchWays(bbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Relation> searchRelations(BBox bbox) {
        lock.readLock().lock();
        try {
            return store.searchRelations(bbox);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Searches for all primitives in the given bounding box
     *
     * @param bbox the bounding box
     * @return List of primitives in the given bbox. Can be empty but not null
     * @since 15891
     */
    public List<OsmPrimitive> searchPrimitives(BBox bbox) {
        List<OsmPrimitive> primitiveList = new ArrayList<>();
        primitiveList.addAll(searchNodes(bbox));
        primitiveList.addAll(searchWays(bbox));
        primitiveList.addAll(searchRelations(bbox));
        return primitiveList;
    }

    @Override
    public Collection<Relation> getRelations() {
        return getPrimitives(Relation.class::isInstance);
    }

    /**
     * Determines if the given node can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * For efficiency reasons this method does not lock the dataset, you have to lock it manually.
     *
     * @param n The node to search
     * @return {@code true} if {@code n} can be retrieved in this data set, {@code false} otherwise
     * @since 7501
     */
    @Override
    public boolean containsNode(Node n) {
        return store.containsNode(n);
    }

    /**
     * Determines if the given way can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * For efficiency reasons this method does not lock the dataset, you have to lock it manually.
     *
     * @param w The way to search
     * @return {@code true} if {@code w} can be retrieved in this data set, {@code false} otherwise
     * @since 7501
     */
    @Override
    public boolean containsWay(Way w) {
        return store.containsWay(w);
    }

    /**
     * Determines if the given relation can be retrieved in the data set through its bounding box. Useful for dataset consistency test.
     * For efficiency reasons this method does not lock the dataset, you have to lock it manually.
     *
     * @param r The relation to search
     * @return {@code true} if {@code r} can be retrieved in this data set, {@code false} otherwise
     * @since 7501
     */
    @Override
    public boolean containsRelation(Relation r) {
        return store.containsRelation(r);
    }

    /**
     * Adds a primitive to the dataset.
     *
     * @param primitive the primitive.
     * @throws IllegalStateException if the dataset is read-only
     */
    @Override
    public void addPrimitive(OsmPrimitive primitive) {
        Objects.requireNonNull(primitive, "primitive");
        checkModifiable();
        update(() -> {
            if (getPrimitiveById(primitive) != null)
                throw new DataIntegrityProblemException(
                        tr("Unable to add primitive {0} to the dataset because it is already included",
                                primitive.toString()));

            allPrimitives.add(primitive);
            primitive.setDataset(this);
            primitive.updatePosition(); // Set cached bbox for way and relation (required for reindexWay and reindexRelation to work properly)
            store.addPrimitive(primitive);
            firePrimitivesAdded(Collections.singletonList(primitive), false);
        });
    }

    /**
     * Removes a primitive from the dataset. This method only removes the
     * primitive form the respective collection of primitives managed
     * by this dataset, i.e. from {@code store.nodes}, {@code store.ways}, or
     * {@code store.relations}. References from other primitives to this
     * primitive are left unchanged.
     *
     * @param primitiveId the id of the primitive
     * @throws IllegalStateException if the dataset is read-only
     */
    public void removePrimitive(PrimitiveId primitiveId) {
        checkModifiable();
        update(() -> {
            OsmPrimitive primitive = getPrimitiveByIdChecked(primitiveId);
            if (primitive == null)
                return;
            removePrimitiveImpl(primitive);
            firePrimitivesRemoved(Collections.singletonList(primitive), false);
        });
    }

    private void removePrimitiveImpl(OsmPrimitive primitive) {
        clearSelection(primitive.getPrimitiveId());
        if (primitive.isSelected()) {
            throw new DataIntegrityProblemException("Primitive was re-selected by a selection listener: " + primitive);
        }
        store.removePrimitive(primitive);
        allPrimitives.remove(primitive);
        primitive.setDataset(null);
    }

    void removePrimitive(OsmPrimitive primitive) {
        checkModifiable();
        update(() -> {
            removePrimitiveImpl(primitive);
            firePrimitivesRemoved(Collections.singletonList(primitive), false);
        });
    }

    /*---------------------------------------------------
     *   SELECTION HANDLING
     *---------------------------------------------------*/

    @Override
    public void addSelectionListener(DataSelectionListener listener) {
        selectionListeners.addListener(listener);
    }

    @Override
    public void removeSelectionListener(DataSelectionListener listener) {
        selectionListeners.removeListener(listener);
    }

    /**
     * Returns selected nodes and ways.
     * @return selected nodes and ways
     */
    public Collection<OsmPrimitive> getSelectedNodesAndWays() {
        return new SubclassFilteredCollection<>(getSelected(),
                primitive -> primitive instanceof Node || primitive instanceof Way);
    }

    @Override
    public Collection<WaySegment> getHighlightedVirtualNodes() {
        return Collections.unmodifiableCollection(highlightedVirtualNodes);
    }

    @Override
    public Collection<WaySegment> getHighlightedWaySegments() {
        return Collections.unmodifiableCollection(highlightedWaySegments);
    }

    @Override
    public void addHighlightUpdateListener(HighlightUpdateListener listener) {
        highlightUpdateListeners.addListener(listener);
    }

    @Override
    public void removeHighlightUpdateListener(HighlightUpdateListener listener) {
        highlightUpdateListeners.removeListener(listener);
    }

    /**
     * Adds a listener that gets notified whenever the data sources change
     *
     * @param listener The listener
     * @see #removeDataSourceListener
     * @see #getDataSources
     * @since 15609
     */
    public void addDataSourceListener(DataSourceListener listener) {
        dataSourceListeners.addListener(listener);
    }

    /**
     * Removes a listener that gets notified whenever the data sources change
     *
     * @param listener The listener
     * @see #addDataSourceListener
     * @see #getDataSources
     * @since 15609
     */
    public void removeDataSourceListener(DataSourceListener listener) {
        dataSourceListeners.removeListener(listener);
    }

    @Override
    public Collection<OsmPrimitive> getAllSelected() {
        return currentSelectedPrimitives;
    }

    @Override
    public boolean selectionEmpty() {
        return currentSelectedPrimitives.isEmpty();
    }

    @Override
    public boolean isSelected(OsmPrimitive osm) {
        return currentSelectedPrimitives.contains(osm);
    }

    @Override
    public void setHighlightedVirtualNodes(Collection<WaySegment> waySegments) {
        if (highlightedVirtualNodes.isEmpty() && waySegments.isEmpty())
            return;

        highlightedVirtualNodes = waySegments;
        fireHighlightingChanged();
    }

    @Override
    public void setHighlightedWaySegments(Collection<WaySegment> waySegments) {
        if (highlightedWaySegments.isEmpty() && waySegments.isEmpty())
            return;

        highlightedWaySegments = waySegments;
        fireHighlightingChanged();
    }

    @Override
    public void setSelected(Collection<? extends PrimitiveId> selection) {
        setSelected(selection.stream());
    }

    @Override
    public void setSelected(PrimitiveId... osm) {
        setSelected(Stream.of(osm).filter(Objects::nonNull));
    }

    private void setSelected(Stream<? extends PrimitiveId> stream) {
        doSelectionChange(old -> new SelectionReplaceEvent(this, old,
                stream.map(this::getPrimitiveByIdChecked).filter(Objects::nonNull)));
    }

    @Override
    public void addSelected(Collection<? extends PrimitiveId> selection) {
        addSelected(selection.stream());
    }

    @Override
    public void addSelected(PrimitiveId... osm) {
        addSelected(Stream.of(osm));
    }

    private void addSelected(Stream<? extends PrimitiveId> stream) {
        doSelectionChange(old -> new SelectionAddEvent(this, old,
                stream.map(this::getPrimitiveByIdChecked).filter(Objects::nonNull)));
    }

    @Override
    public void clearSelection(PrimitiveId... osm) {
        clearSelection(Stream.of(osm));
    }

    @Override
    public void clearSelection(Collection<? extends PrimitiveId> list) {
        clearSelection(list.stream());
    }

    @Override
    public void clearSelection() {
        setSelected(Stream.empty());
    }

    private void clearSelection(Stream<? extends PrimitiveId> stream) {
        doSelectionChange(old -> new SelectionRemoveEvent(this, old,
                stream.map(this::getPrimitiveByIdChecked).filter(Objects::nonNull)));
    }

    @Override
    public void toggleSelected(Collection<? extends PrimitiveId> osm) {
        toggleSelected(osm.stream());
    }

    @Override
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

    @Override
    public synchronized Area getDataSourceArea() {
        if (cachedDataSourceArea == null) {
            cachedDataSourceArea = OsmData.super.getDataSourceArea();
        }
        return cachedDataSourceArea;
    }

    @Override
    public synchronized List<Bounds> getDataSourceBounds() {
        if (cachedDataSourceBounds == null) {
            cachedDataSourceBounds = OsmData.super.getDataSourceBounds();
        }
        return Collections.unmodifiableList(cachedDataSourceBounds);
    }

    @Override
    public synchronized Collection<DataSource> getDataSources() {
        return Collections.unmodifiableCollection(dataSources);
    }

    @Override
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
            Logging.warn(tr(
                    "JOSM expected to find primitive [{0} {1}] in dataset but it is not there. Please report this "
                            + "at {2}. This is not a critical error, it should be safe to continue in your work.",
                    primitiveId.getType(), Long.toString(primitiveId.getUniqueId()), Config.getUrls().getJOSMWebsite()));
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
     * @throws IllegalStateException if the dataset is read-only
     */
    public Set<Way> unlinkNodeFromWays(Node node) {
        checkModifiable();
        return update(() -> {
            Set<Way> result = new HashSet<>();
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
            return result;
        });
    }

    /**
     * removes all references from relations in this dataset  to this primitive
     *
     * @param primitive the primitive
     * @return The set of relations that have been modified
     * @throws IllegalStateException if the dataset is read-only
     */
    public Set<Relation> unlinkPrimitiveFromRelations(OsmPrimitive primitive) {
        checkModifiable();
        return update(() -> {
            Set<Relation> result = new HashSet<>();
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
            return result;
        });
    }

    /**
     * Removes all references from other primitives to the referenced primitive.
     *
     * @param referencedPrimitive the referenced primitive
     * @return The set of primitives that have been modified
     * @throws IllegalStateException if the dataset is read-only
     */
    public Set<OsmPrimitive> unlinkReferencesToPrimitive(OsmPrimitive referencedPrimitive) {
        checkModifiable();
        return update(() -> {
            Set<OsmPrimitive> result = new HashSet<>();
            if (referencedPrimitive instanceof Node) {
                result.addAll(unlinkNodeFromWays((Node) referencedPrimitive));
            }
            result.addAll(unlinkPrimitiveFromRelations(referencedPrimitive));
            return result;
        });
    }

    @Override
    public boolean isModified() {
        return allPrimitives.parallelStream().anyMatch(OsmPrimitive::isModified);
    }

    /**
     * Replies true if there is at least one primitive in this dataset which requires to be uploaded to server.
     * @return true if there is at least one primitive in this dataset which requires to be uploaded to server
     * @since 13161
     */
    public boolean requiresUploadToServer() {
        return allPrimitives.parallelStream().anyMatch(p -> APIOperation.of(p) != null);
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
     * @see #endUpdate()
     */
    public void beginUpdate() {
        lock.writeLock().lock();
        updateCount++;
    }

    /**
     * Must be called after a previous call to {@link #beginUpdate()} to fire change events.
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
                try {
                    lock.writeLock().unlock();
                    if (eventsToFire.size() < MAX_SINGLE_EVENTS) {
                        for (AbstractDatasetChangedEvent event : eventsToFire) {
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

    /**
     * Performs the update runnable between {@link #beginUpdate()} / {@link #endUpdate()} calls.
     * @param runnable update action
     * @since 16187
     */
    public void update(Runnable runnable) {
        beginUpdate();
        try {
            runnable.run();
        } finally {
            endUpdate();
        }
    }

    /**
     * Performs the update function between {@link #beginUpdate()} / {@link #endUpdate()} calls.
     * @param function update function
     * @param t function argument
     * @param <T> argument type
     * @param <R> result type
     * @return function result
     * @since 16187
     */
    public <T, R> R update(Function<T, R> function, T t) {
        beginUpdate();
        try {
            return function.apply(t);
        } finally {
            endUpdate();
        }
    }

    /**
     * Performs the update supplier between {@link #beginUpdate()} / {@link #endUpdate()} calls.
     * @param supplier update supplier
     * @param <R> result type
     * @return supplier result
     * @since 16187
     */
    public <R> R update(Supplier<R> supplier) {
        beginUpdate();
        try {
            return supplier.get();
        } finally {
            endUpdate();
        }
    }

    private void fireEventToListeners(AbstractDatasetChangedEvent event) {
        for (DataSetListener listener : listeners) {
            Logging.trace("Firing {0} to {1} (dataset)", event, listener);
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
        store.reindexRelation(r, Relation::updatePosition);
        fireEvent(new RelationMembersChangedEvent(this, r));
    }

    void fireNodeMoved(Node node, LatLon newCoor, EastNorth eastNorth) {
        store.reindexNode(node, n -> n.setCoorInternal(newCoor, eastNorth), Way::updatePosition, Relation::updatePosition);
        fireEvent(new NodeMovedEvent(this, node));
    }

    void fireWayNodesChanged(Way way) {
        if (!way.isEmpty()) {
            store.reindexWay(way, Way::updatePosition, Relation::updatePosition);
        }
        fireEvent(new WayNodesChangedEvent(this, way));
    }

    void fireChangesetIdChanged(OsmPrimitive primitive, int oldChangesetId, int newChangesetId) {
        fireEvent(new ChangesetIdChangedEvent(this, Collections.singletonList(primitive), oldChangesetId,
                newChangesetId));
    }

    void firePrimitiveFlagsChanged(OsmPrimitive primitive) {
        fireEvent(new PrimitiveFlagsChangedEvent(this, primitive));
    }

    void fireFilterChanged() {
        fireEvent(new FilterChangedEvent(this));
    }

    void fireHighlightingChanged() {
        HighlightUpdateListener.HighlightUpdateEvent e = new HighlightUpdateListener.HighlightUpdateEvent(this);
        highlightUpdateListeners.fireEvent(l -> l.highlightUpdated(e));
    }

    /**
     * Invalidates the internal cache of projected east/north coordinates.
     *
     * This method can be invoked after the globally configured projection method changed.
     */
    public void invalidateEastNorthCache() {
        if (ProjectionRegistry.getProjection() == null)
            return; // sanity check
        update(() -> getNodes().forEach(Node::invalidateEastNorthCache));
    }

    /**
     * Cleanups all deleted primitives (really delete them from the dataset).
     */
    public void cleanupDeletedPrimitives() {
        update(() -> {
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
        });
    }

    /**
     * Removes all primitives from the dataset and resets the currently selected primitives
     * to the empty collection. Also notifies selection change listeners if necessary.
     * @throws IllegalStateException if the dataset is read-only
     */
    @Override
    public void clear() {
        //TODO: Why can't we clear a dataset that is locked?
        //TODO: Report listeners that are still active (should be none)
        checkModifiable();
        update(() -> {
            clearSelection();
            for (OsmPrimitive primitive : allPrimitives) {
                primitive.setDataset(null);
            }
            store.clear();
            allPrimitives.clear();
        });
    }

    /**
     * Marks all "invisible" objects as deleted. These objects should be always marked as
     * deleted when downloaded from the server. They can be undeleted later if necessary.
     * @throws IllegalStateException if the dataset is read-only
     */
    public void deleteInvisible() {
        checkModifiable();
        for (OsmPrimitive primitive : allPrimitives) {
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
     * @throws IllegalStateException if the dataset is read-only
     */
    public synchronized void mergeFrom(DataSet from, ProgressMonitor progressMonitor) {
        if (from != null) {
            checkModifiable();
            new DataSetMerger(this, from).merge(progressMonitor);
            synchronized (from) {
                if (!from.dataSources.isEmpty()) {
                    DataSourceAddedEvent addedEvent = new DataSourceAddedEvent(
                            this, new LinkedHashSet<>(dataSources), from.dataSources.stream());
                    DataSourceRemovedEvent clearEvent = new DataSourceRemovedEvent(
                            this, new LinkedHashSet<>(from.dataSources), from.dataSources.stream());
                    if (from.dataSources.stream().filter(dataSource -> !dataSources.contains(dataSource))
                            .map(dataSources::add).filter(Boolean.TRUE::equals).count() > 0) {
                        cachedDataSourceArea = null;
                        cachedDataSourceBounds = null;
                    }
                    from.dataSources.clear();
                    from.cachedDataSourceArea = null;
                    from.cachedDataSourceBounds = null;
                    dataSourceListeners.fireEvent(d -> d.dataSourceChange(addedEvent));
                    from.dataSourceListeners.fireEvent(d -> d.dataSourceChange(clearEvent));
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

    @Override
    public String getName() {
        return name;
    }

    @Override
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

    @Override
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

    /**
     * Returns mappaint cache index for this DataSet.
     *
     * If the {@link OsmPrimitive#mappaintCacheIdx} is not equal to the DataSet mappaint
     * cache index, this means the cache for that primitive is out of date.
     * @return mappaint cache index
     * @since 13420
     */
    public short getMappaintCacheIndex() {
        return mappaintCacheIdx;
    }

    @Override
    public void clearMappaintCache() {
        mappaintCacheIdx++;
    }

    @Override
    public void lock() {
        if (!isReadOnly.compareAndSet(false, true)) {
            Logging.warn("Trying to set readOnly flag on a readOnly dataset ", getName());
        }
    }

    @Override
    public void unlock() {
        if (!isReadOnly.compareAndSet(true, false)) {
            Logging.warn("Trying to unset readOnly flag on a non-readOnly dataset ", getName());
        }
    }

    @Override
    public boolean isLocked() {
        return isReadOnly.get();
    }

    /**
     * Checks the dataset is modifiable (not read-only).
     * @throws IllegalStateException if the dataset is read-only
     */
    private void checkModifiable() {
        if (isLocked()) {
            throw new IllegalStateException("DataSet is read-only");
        }
    }

    /**
     * Returns an optional remark about this data set (used by Overpass API).
     * @return a remark about this data set, or {@code null}
     * @since 14219
     */
    public String getRemark() {
        return remark;
    }

    /**
     * Sets an optional remark about this data set (used by Overpass API).
     * @param remark a remark about this data set, or {@code null}
     * @since 14219
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * Gets the GPX (XML) namespaces if this DataSet was created from a GPX file
     * @return the GPXNamespaces or <code>null</code>
     */
    public List<XMLNamespace> getGPXNamespaces() {
        return gpxNamespaces;
    }

    /**
     * Sets the GPX (XML) namespaces
     * @param gpxNamespaces the GPXNamespaces to set
     */
    public void setGPXNamespaces(List<XMLNamespace> gpxNamespaces) {
        this.gpxNamespaces = gpxNamespaces;
    }

    /**
     * @return true if this Dataset contains no primitives
     * @since 14835
     */
    public boolean isEmpty() {
        return allPrimitives.isEmpty();
    }
}
