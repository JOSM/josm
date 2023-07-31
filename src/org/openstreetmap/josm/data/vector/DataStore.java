// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.QuadBucketPrimitiveStore;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.tools.Logging;

/**
 * A class that stores data (essentially a simple {@link DataSet})
 * @author Taylor Smock
 * @param <O> Type of OSM primitive
 * @param <N> Type of node
 * @param <W> Type of way
 * @param <R> Type of relation
 * @since 17862
 */
class DataStore<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>> {
    /**
     * This literally only exists to make {@link QuadBucketPrimitiveStore#removePrimitive} public
     *
     * @param <N> The node type
     * @param <W> The way type
     * @param <R> The relation type
     */
    static class LocalQuadBucketPrimitiveStore<N extends INode, W extends IWay<N>, R extends IRelation<?>>
      extends QuadBucketPrimitiveStore<N, W, R> {
        // Allow us to remove primitives (protected in {@link QuadBucketPrimitiveStore})
        @Override
        public void removePrimitive(IPrimitive primitive) {
            super.removePrimitive(primitive);
        }
    }

    protected final LocalQuadBucketPrimitiveStore<N, W, R> store = new LocalQuadBucketPrimitiveStore<>();
    protected final Storage<O> allPrimitives = new Storage<>(new Storage.PrimitiveIdHash(), true);
    // TODO what happens when I use hashCode?
    protected final Set<Tile> addedTiles = Collections.synchronizedSet(new HashSet<>());
    protected final Map<PrimitiveId, O> primitivesMap = Collections.synchronizedMap(allPrimitives
      .foreignKey(new Storage.PrimitiveIdHash()));
    protected final Collection<DataSource> dataSources = new LinkedList<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public QuadBucketPrimitiveStore<N, W, R> getStore() {
        return this.store;
    }

    public Storage<O> getAllPrimitives() {
        return this.allPrimitives;
    }

    /**
     * Get the primitives map.
     * The returned map is a {@link Collections#synchronizedMap}. Please synchronize on it.
     * @return The Primitives map.
     */
    public Map<PrimitiveId, O> getPrimitivesMap() {
        return this.primitivesMap;
    }

    public Collection<DataSource> getDataSources() {
        return Collections.unmodifiableCollection(dataSources);
    }

    /**
     * Add a datasource to this data set
     * @param dataSource The datasource to add
     */
    public void addDataSource(DataSource dataSource) {
        this.dataSources.add(dataSource);
    }

    /**
     * Add a primitive to this dataset
     * @param primitive The primitive to remove
     */
    protected void removePrimitive(O primitive) {
        if (primitive == null) {
            return;
        }
        try {
            this.readWriteLock.writeLock().lockInterruptibly();
            if (this.allPrimitives.contains(primitive)) {
                this.store.removePrimitive(primitive);
                this.allPrimitives.remove(primitive);
                this.primitivesMap.remove(primitive.getPrimitiveId());
            }
        } catch (InterruptedException e) {
            Logging.error(e);
            Thread.currentThread().interrupt();
        } finally {
            if (this.readWriteLock.isWriteLockedByCurrentThread()) {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    /**
     * Add a primitive to this dataset
     * @param primitive The primitive to add
     */
    protected void addPrimitive(O primitive) {
        this.store.addPrimitive(primitive);
        this.allPrimitives.add(primitive);
        this.primitivesMap.put(primitive.getPrimitiveId(), primitive);
    }

    /**
     * Get the read/write lock for this dataset
     * @return The read/write lock
     */
    protected ReentrantReadWriteLock getReadWriteLock() {
        return this.readWriteLock;
    }
}
