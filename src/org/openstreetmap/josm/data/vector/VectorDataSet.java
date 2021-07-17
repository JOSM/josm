// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTTile;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.HighlightUpdateListener;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.event.IDataSelectionEventSource;
import org.openstreetmap.josm.data.osm.event.IDataSelectionListener;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * A data class for Vector Data
 *
 * @author Taylor Smock
 * @since 17862
 */
public class VectorDataSet implements OsmData<VectorPrimitive, VectorNode, VectorWay, VectorRelation>,
       IDataSelectionEventSource<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet> {
    // Note: In Java 8, computeIfAbsent is blocking for both pre-existing and new values. In Java 9, it is only blocking
    // for new values (perf increase). See JDK-8161372 for more info.
    private final Map<Integer, Storage<MVTTile>> dataStoreMap = new ConcurrentHashMap<>();
    // This is for "custom" data
    private final VectorDataStore customDataStore = new VectorDataStore();
    // Both of these listener lists are useless, since they expect OsmPrimitives at this time
    private final ListenerList<HighlightUpdateListener> highlightUpdateListenerListenerList = ListenerList.create();
    private final ListenerList<DataSelectionListener> dataSelectionListenerListenerList = ListenerList.create();
    private boolean lock = true;
    private String name;
    private short mappaintCacheIdx = 1;

    private final Object selectionLock = new Object();
    /**
     * The current selected primitives. This is always a unmodifiable set.
     *
     * The set should be ordered in the order in which the primitives have been added to the selection.
     */
    private Set<PrimitiveId> currentSelectedPrimitives = Collections.emptySet();

    private final ListenerList<IDataSelectionListener<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet>> listeners =
            ListenerList.create();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * The distance to consider nodes duplicates -- mostly a memory saving measure.
     * 0.000_000_1 ~1.2 cm (+- 5.57 mm)
     * Descriptions from <a href="https://xkcd.com/2170/">https://xkcd.com/2170/</a>
     * Notes on <a href="https://wiki.openstreetmap.org/wiki/Node">https://wiki.openstreetmap.org/wiki/Node</a> indicate
     * that IEEE 32-bit floats should not be used at high longitude (0.000_01 precision)
     */
    protected static final float DUPE_NODE_DISTANCE = 0.000_000_1f;

    /**
     * The current zoom we are getting/adding to
     */
    private int zoom;
    /**
     * Default to normal download policy
     */
    private DownloadPolicy downloadPolicy = DownloadPolicy.NORMAL;
    /**
     * Default to a blocked upload policy
     */
    private UploadPolicy uploadPolicy = UploadPolicy.BLOCKED;
    /**
     * The paint style for this layer
     */
    private ElemStyles styles;
    private final Collection<PrimitiveId> highlighted = new HashSet<>();

    @Override
    public Collection<DataSource> getDataSources() {
        // TODO
        return Collections.emptyList();
    }

    @Override
    public void lock() {
        this.lock = true;
    }

    @Override
    public void unlock() {
        this.lock = false;
    }

    @Override
    public boolean isLocked() {
        return this.lock;
    }

    @Override
    public String getVersion() {
        return "8"; // TODO get this dynamically. Not critical, as this is currently the _only_ version.
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add a primitive to the custom data store
     * @param primitive the primitive to add
     */
    @Override
    public void addPrimitive(VectorPrimitive primitive) {
        tryWrite(this.readWriteLock, () -> {
            this.customDataStore.addPrimitive(primitive);
            primitive.setDataSet(this);
        });
    }

    /**
     * Remove a primitive from the custom data store
     * @param primitive The primitive to add to the custom data store
     */
    public void removePrimitive(VectorPrimitive primitive) {
        this.customDataStore.removePrimitive(primitive);
        primitive.setDataSet(null);
    }

    @Override
    public void clear() {
        synchronized (this.dataStoreMap) {
            this.dataStoreMap.clear();
        }
    }

    @Override
    public List<VectorNode> searchNodes(BBox bbox) {
        return tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            final Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
            return Stream.concat(dataStoreStream, Stream.of(this.customDataStore)).map(VectorDataStore::getStore)
                    .flatMap(store -> store.searchNodes(bbox).stream()).collect(Collectors.toList());
        }).orElseGet(Collections::emptyList);
    }

    @Override
    public boolean containsNode(VectorNode vectorNode) {
        return tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            final Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
            return Stream.concat(dataStoreStream, Stream.of(this.customDataStore)).map(VectorDataStore::getStore)
                    .anyMatch(store -> store.containsNode(vectorNode));
        }).orElse(Boolean.FALSE);
    }

    @Override
    public List<VectorWay> searchWays(BBox bbox) {
        return tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            final Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
            return Stream.concat(dataStoreStream, Stream.of(this.customDataStore)).map(VectorDataStore::getStore)
                    .flatMap(store -> store.searchWays(bbox).stream()).collect(Collectors.toList());
        }).orElseGet(Collections::emptyList);
    }

    @Override
    public boolean containsWay(VectorWay vectorWay) {
        return tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            final Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
            return Stream.concat(dataStoreStream, Stream.of(this.customDataStore)).map(VectorDataStore::getStore)
                    .anyMatch(store -> store.containsWay(vectorWay));
        }).orElse(Boolean.FALSE);
    }

    @Override
    public List<VectorRelation> searchRelations(BBox bbox) {
        return tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            final Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
            return Stream.concat(dataStoreStream, Stream.of(this.customDataStore)).map(VectorDataStore::getStore)
                    .flatMap(store -> store.searchRelations(bbox).stream()).collect(Collectors.toList());
        }).orElseGet(Collections::emptyList);
    }

    @Override
    public boolean containsRelation(VectorRelation vectorRelation) {
        return tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            final Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
            return Stream.concat(dataStoreStream, Stream.of(this.customDataStore)).map(VectorDataStore::getStore)
                    .anyMatch(store -> store.containsRelation(vectorRelation));
        }).orElse(Boolean.FALSE);
    }

    /**
     * Get a primitive for an id
     * @param primitiveId type and uniqueId of the primitive. Might be &lt; 0 for newly created primitives
     * @return The primitive for the id. Please note that since this is vector data, there may be more primitives with this id.
     * Please use {@link #getPrimitivesById(PrimitiveId...)} to get all primitives for that {@link PrimitiveId}.
     */
    @Override
    public VectorPrimitive getPrimitiveById(PrimitiveId primitiveId) {
        return this.getPrimitivesById(primitiveId).findFirst().orElse(null);
    }

    /**
     * Get all primitives for ids
     * @param primitiveIds The ids to search for
     * @return The primitives for the ids (note: as this is vector data, a {@link PrimitiveId} may have multiple associated primitives)
     */
    public Stream<VectorPrimitive> getPrimitivesById(PrimitiveId... primitiveIds) {
        final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
        return Stream.concat(dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty(),
                Stream.of(this.customDataStore)).map(VectorDataStore::getPrimitivesMap)
                .flatMap(m -> Stream.of(primitiveIds).map(m::get)).filter(Objects::nonNull);
    }

    @Override
    public <T extends VectorPrimitive> Collection<T> getPrimitives(Predicate<? super VectorPrimitive> predicate) {
        Collection<VectorPrimitive> primitives = tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            final Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
            return Stream.concat(dataStoreStream, Stream.of(this.customDataStore))
                    .map(VectorDataStore::getAllPrimitives).flatMap(Collection::stream).distinct().collect(Collectors.toList());

        }).orElseGet(Collections::emptyList);
        return new SubclassFilteredCollection<>(primitives, predicate);
    }

    @Override
    public Collection<VectorNode> getNodes() {
        return this.getPrimitives(VectorNode.class::isInstance);
    }

    @Override
    public Collection<VectorWay> getWays() {
        return this.getPrimitives(VectorWay.class::isInstance);
    }

    @Override
    public Collection<VectorRelation> getRelations() {
        return this.getPrimitives(VectorRelation.class::isInstance);
    }

    @Override
    public DownloadPolicy getDownloadPolicy() {
        return this.downloadPolicy;
    }

    @Override
    public void setDownloadPolicy(DownloadPolicy downloadPolicy) {
        this.downloadPolicy = downloadPolicy;
    }

    @Override
    public UploadPolicy getUploadPolicy() {
        return this.uploadPolicy;
    }

    @Override
    public void setUploadPolicy(UploadPolicy uploadPolicy) {
        this.uploadPolicy = uploadPolicy;
    }

    /**
     * Get the current Read/Write lock
     * @implNote This changes based off of zoom level. Please do not use this in a finally block
     * @return The current read/write lock
     */
    @Override
    public Lock getReadLock() {
        return this.readWriteLock.readLock();
    }

    @Override
    public Collection<WaySegment> getHighlightedVirtualNodes() {
        // TODO? This requires a change to WaySegment so that it isn't Way/Node specific
        return Collections.emptyList();
    }

    @Override
    public void setHighlightedVirtualNodes(Collection<WaySegment> waySegments) {
        // TODO? This requires a change to WaySegment so that it isn't Way/Node specific
    }

    @Override
    public Collection<WaySegment> getHighlightedWaySegments() {
        // TODO? This requires a change to WaySegment so that it isn't Way/Node specific
        return Collections.emptyList();
    }

    @Override
    public void setHighlightedWaySegments(Collection<WaySegment> waySegments) {
        // TODO? This requires a change to WaySegment so that it isn't Way/Node specific
    }

    /**
     * Mark some primitives as highlighted
     * @param primitives The primitives to highlight
     * @apiNote This is *highly likely* to change, as the inherited methods are modified to accept primitives other than OSM primitives.
     */
    public void setHighlighted(Collection<PrimitiveId> primitives) {
        this.highlighted.clear();
        this.highlighted.addAll(primitives);
        // The highlight event updates are very OSM specific, and require a DataSet.
        this.highlightUpdateListenerListenerList.fireEvent(event -> event.highlightUpdated(null));
    }

    /**
     * Get the highlighted objects
     * @return The highlighted objects
     */
    public Collection<PrimitiveId> getHighlighted() {
        return Collections.unmodifiableCollection(this.highlighted);
    }

    @Override
    public void addHighlightUpdateListener(HighlightUpdateListener listener) {
        this.highlightUpdateListenerListenerList.addListener(listener);
    }

    @Override
    public void removeHighlightUpdateListener(HighlightUpdateListener listener) {
        this.highlightUpdateListenerListenerList.removeListener(listener);
    }

    @Override
    public Collection<VectorPrimitive> getAllSelected() {
        return tryRead(this.readWriteLock, () -> {
            final Storage<MVTTile> dataStore = this.getBestZoomDataStore().orElse(null);
            Stream<VectorDataStore> dataStoreStream = dataStore != null ? dataStore.stream().map(MVTTile::getData) : Stream.empty();
                return Stream.concat(dataStoreStream, Stream.of(this.customDataStore)).map(VectorDataStore::getPrimitivesMap)
                  .flatMap(dataMap -> {
                    // Synchronize on dataMap to avoid concurrent modification errors
                    synchronized (dataMap) {
                        return this.currentSelectedPrimitives.stream().map(dataMap::get).filter(Objects::nonNull);
                    }
                }).collect(Collectors.toList());
        }).orElseGet(Collections::emptyList);
    }

    /**
     * Get the best zoom datastore
     * @return A datastore with data, or {@code null} if no good datastore exists.
     */
    private Optional<Storage<MVTTile>> getBestZoomDataStore() {
        final int currentZoom = this.zoom;
        if (this.dataStoreMap.containsKey(currentZoom)) {
            return Optional.of(this.dataStoreMap.get(currentZoom));
        }
        // Check up to two zooms higher (may cause perf hit)
        for (int tZoom = currentZoom + 1; tZoom < currentZoom + 3; tZoom++) {
            if (this.dataStoreMap.containsKey(tZoom)) {
                return Optional.of(this.dataStoreMap.get(tZoom));
            }
        }
        // Return *any* lower zoom data (shouldn't cause a perf hit...)
        for (int tZoom = currentZoom - 1; tZoom >= 0; tZoom--) {
            if (this.dataStoreMap.containsKey(tZoom)) {
                return Optional.of(this.dataStoreMap.get(tZoom));
            }
        }
        // Check higher level zooms. May cause perf issues if selected datastore has a lot of data.
        for (int tZoom = currentZoom + 3; tZoom < 34; tZoom++) {
            if (this.dataStoreMap.containsKey(tZoom)) {
                return Optional.of(this.dataStoreMap.get(tZoom));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean selectionEmpty() {
        return this.currentSelectedPrimitives.isEmpty();
    }

    @Override
    public boolean isSelected(VectorPrimitive osm) {
        return this.currentSelectedPrimitives.contains(osm.getPrimitiveId());
    }

    @Override
    public void toggleSelected(Collection<? extends PrimitiveId> osm) {
        this.toggleSelectedImpl(osm.stream());
    }

    @Override
    public void toggleSelected(PrimitiveId... osm) {
        this.toggleSelectedImpl(Stream.of(osm));
    }

    private void toggleSelectedImpl(Stream<? extends PrimitiveId> osm) {
        this.doSelectionChange(old -> new IDataSelectionListener.SelectionToggleEvent<>(this, old,
                osm.flatMap(this::getPrimitivesById).filter(Objects::nonNull)));
    }

    @Override
    public void setSelected(Collection<? extends PrimitiveId> selection) {
        this.setSelectedImpl(selection.stream());
    }

    @Override
    public void setSelected(PrimitiveId... osm) {
        this.setSelectedImpl(Stream.of(osm));
    }

    private void setSelectedImpl(Stream<? extends PrimitiveId> osm) {
        this.doSelectionChange(old -> new IDataSelectionListener.SelectionReplaceEvent<>(this, old,
                osm.filter(Objects::nonNull).flatMap(this::getPrimitivesById).filter(Objects::nonNull)));
    }

    @Override
    public void addSelected(Collection<? extends PrimitiveId> selection) {
        this.addSelectedImpl(selection.stream());
    }

    @Override
    public void addSelected(PrimitiveId... osm) {
        this.addSelectedImpl(Stream.of(osm));
    }

    private void addSelectedImpl(Stream<? extends PrimitiveId> osm) {
        this.doSelectionChange(old -> new IDataSelectionListener.SelectionAddEvent<>(this, old,
                osm.flatMap(this::getPrimitivesById).filter(Objects::nonNull)));
    }

    @Override
    public void clearSelection(PrimitiveId... osm) {
        this.clearSelectionImpl(Stream.of(osm));
    }

    @Override
    public void clearSelection(Collection<? extends PrimitiveId> list) {
        this.clearSelectionImpl(list.stream());
    }

    @Override
    public void clearSelection() {
        this.clearSelectionImpl(new ArrayList<>(this.currentSelectedPrimitives).stream());
    }

    private void clearSelectionImpl(Stream<? extends PrimitiveId> osm) {
        this.doSelectionChange(old -> new IDataSelectionListener.SelectionRemoveEvent<>(this, old,
                osm.flatMap(this::getPrimitivesById).filter(Objects::nonNull)));
    }

    /**
     * Do a selection change.
     * <p>
     * This is the only method that changes the current selection state.
     * @param command A generator that generates the {@link DataSelectionListener.SelectionChangeEvent}
     *                for the given base set of currently selected primitives.
     * @return true iff the command did change the selection.
     */
    private boolean doSelectionChange(final Function<Set<VectorPrimitive>,
            IDataSelectionListener.SelectionChangeEvent<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet>> command) {
        synchronized (this.selectionLock) {
            IDataSelectionListener.SelectionChangeEvent<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet> event =
                    command.apply(currentSelectedPrimitives.stream().map(this::getPrimitiveById).collect(Collectors.toSet()));
            if (event.isNop()) {
                return false;
            }
            this.currentSelectedPrimitives = event.getSelection().stream().map(IPrimitive::getPrimitiveId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            this.listeners.fireEvent(l -> l.selectionChanged(event));
            return true;
        }
    }

    @Override
    public void addSelectionListener(DataSelectionListener listener) {
        this.dataSelectionListenerListenerList.addListener(listener);
    }

    @Override
    public void removeSelectionListener(DataSelectionListener listener) {
        this.dataSelectionListenerListenerList.removeListener(listener);
    }

    public short getMappaintCacheIndex() {
        return this.mappaintCacheIdx;
    }

    @Override
    public void clearMappaintCache() {
        this.mappaintCacheIdx++;
    }

    public void setZoom(int zoom) {
        if (zoom == this.zoom) {
            return; // Do nothing -- zoom isn't actually changing
        }
        this.zoom = zoom;
        this.clearMappaintCache();
        final int[] nearestZoom = {-1, -1, -1, -1};
        nearestZoom[0] = zoom;
        // Create a new list to avoid concurrent modification issues
        synchronized (this.dataStoreMap) {
            final int[] keys = new ArrayList<>(this.dataStoreMap.keySet()).stream().filter(Objects::nonNull)
              .mapToInt(Integer::intValue).sorted().toArray();
            final int index;
            if (this.dataStoreMap.containsKey(zoom)) {
                index = Arrays.binarySearch(keys, zoom);
            } else {
                // (-(insertion point) - 1) = return -> insertion point = -(return + 1)
                index = -(Arrays.binarySearch(keys, zoom) + 1);
            }
            if (index > 0) {
                nearestZoom[1] = keys[index - 1];
            }
            if (index < keys.length - 2) {
                nearestZoom[2] = keys[index + 1];
            }

            // TODO cleanup zooms for memory
        }
    }

    public int getZoom() {
        return this.zoom;
    }

    /**
     * Add tile data to this dataset
     * @param tile The tile to add
     */
    public void addTileData(MVTTile tile) {
        tryWrite(this.readWriteLock, () -> {
            final int currentZoom = tile.getZoom();
            // computeIfAbsent should be thread safe (ConcurrentHashMap indicates it is, anyway)
            final Storage<MVTTile> dataStore = this.dataStoreMap.computeIfAbsent(currentZoom, tZoom -> new Storage<>());
            tile.getData().getAllPrimitives().forEach(primitive -> primitive.setDataSet(this));
            dataStore.add(tile);
        });
    }

    /**
     * Try to read something (here to avoid boilerplate)
     *
     * @param supplier The reading function
     * @param <T>      The return type
     * @return The optional return
     */
    private static <T> Optional<T> tryRead(ReentrantReadWriteLock lock, Supplier<T> supplier) {
        try {
            lock.readLock().lockInterruptibly();
            return Optional.ofNullable(supplier.get());
        } catch (InterruptedException e) {
            Logging.error(e);
            Thread.currentThread().interrupt();
        } finally {
            lock.readLock().unlock();
        }
        return Optional.empty();
    }

    /**
     * Try to write something (here to avoid boilerplate)
     *
     * @param runnable The writing function
     */
    private static void tryWrite(ReentrantReadWriteLock lock, Runnable runnable) {
        try {
            lock.writeLock().lockInterruptibly();
            runnable.run();
        } catch (InterruptedException e) {
            Logging.error(e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isWriteLockedByCurrentThread()) {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Get the styles for this layer
     *
     * @return The styles
     */
    public ElemStyles getStyles() {
        return this.styles;
    }

    /**
     * Set the styles for this layer
     * @param styles The styles to set for this layer
     */
    public void setStyles(Collection<ElemStyles> styles) {
        if (styles.size() == 1) {
            this.styles = styles.iterator().next();
        } else if (!styles.isEmpty()) {
            this.styles = new ElemStyles(styles.stream().flatMap(style -> style.getStyleSources().stream()).collect(Collectors.toList()));
        } else {
            this.styles = null;
        }
    }

    /**
     * Mark some layers as invisible
     * @param invisibleLayers The layer to not show
     */
    public void setInvisibleLayers(Collection<String> invisibleLayers) {
        String[] currentInvisibleLayers = invisibleLayers.stream().filter(Objects::nonNull).toArray(String[]::new);
        List<String> temporaryList = Arrays.asList(currentInvisibleLayers);
        this.dataStoreMap.values().stream().flatMap(Collection::stream).map(MVTTile::getData)
          .forEach(dataStore -> dataStore.getAllPrimitives().parallelStream()
            .forEach(primitive -> primitive.setVisible(!temporaryList.contains(primitive.getLayer()))));
    }

    @Override
    public boolean addSelectionListener(IDataSelectionListener<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet> listener) {
        if (!this.listeners.containsListener(listener)) {
            this.listeners.addListener(listener);
        }
        return this.listeners.containsListener(listener);
    }

    @Override
    public boolean removeSelectionListener(
            IDataSelectionListener<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet> listener) {
        if (this.listeners.containsListener(listener)) {
            this.listeners.removeListener(listener);
        }
        return this.listeners.containsListener(listener);
    }
}
