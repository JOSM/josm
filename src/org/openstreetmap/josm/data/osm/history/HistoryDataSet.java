// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A data set holding histories of OSM primitives.
 * @since 1670
 * @since 10386 (new LayerChangeListener interface)
 */
public class HistoryDataSet implements LayerChangeListener {
    /** the unique instance */
    private static HistoryDataSet historyDataSet;

    /**
     * Replies the unique instance of the history data set
     *
     * @return the unique instance of the history data set
     */
    public static synchronized HistoryDataSet getInstance() {
        if (historyDataSet == null) {
            historyDataSet = new HistoryDataSet();
            MainApplication.getLayerManager().addLayerChangeListener(historyDataSet);
        }
        return historyDataSet;
    }

    /** the history data */
    private final Map<PrimitiveId, ArrayList<HistoryOsmPrimitive>> data;
    private final CopyOnWriteArrayList<HistoryDataSetListener> listeners;
    private final Map<Long, Changeset> changesets;

    /**
     * Constructs a new {@code HistoryDataSet}.
     */
    public HistoryDataSet() {
        data = new HashMap<>();
        listeners = new CopyOnWriteArrayList<>();
        changesets = new HashMap<>();
    }

    /**
     * Adds a listener that listens to history data set events.
     * @param listener The listener
     */
    public void addHistoryDataSetListener(HistoryDataSetListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    /**
     * Removes a listener that listens to history data set events.
     * @param listener The listener
     */
    public void removeHistoryDataSetListener(HistoryDataSetListener listener) {
        listeners.remove(listener);
    }

    protected void fireHistoryUpdated(PrimitiveId id) {
        for (HistoryDataSetListener l : listeners) {
            l.historyUpdated(this, id);
        }
    }

    protected void fireCacheCleared() {
        for (HistoryDataSetListener l : listeners) {
            l.historyDataSetCleared(this);
        }
    }

    /**
     * Replies the history primitive for the primitive with id <code>id</code>
     * and version <code>version</code>. null, if no such primitive exists.
     *
     * @param id the id of the primitive. &gt; 0 required.
     * @param type the primitive type. Must not be null.
     * @param version the version of the primitive. &gt; 0 required
     * @return the history primitive for the primitive with id <code>id</code>,
     * type <code>type</code>, and version <code>version</code>
     */
    public HistoryOsmPrimitive get(long id, OsmPrimitiveType type, long version) {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected, got {1}", "id", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        if (version <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected, got {1}", "version", version));

        SimplePrimitiveId pid = new SimplePrimitiveId(id, type);
        List<HistoryOsmPrimitive> versions = data.get(pid);
        if (versions == null)
            return null;
        return versions.stream()
                .filter(primitive -> primitive.matches(id, version))
                .findFirst().orElse(null);
    }

    /**
     * Adds a history primitive to the data set
     *
     * @param primitive  the history primitive to add
     */
    public void put(HistoryOsmPrimitive primitive) {
        PrimitiveId id = new SimplePrimitiveId(primitive.getId(), primitive.getType());
        data.computeIfAbsent(id, k-> new ArrayList<>()).add(primitive);
        fireHistoryUpdated(id);
    }

    /**
     * Adds a changeset to the data set
     *
     * @param changeset the changeset to add
     */
    public void putChangeset(Changeset changeset) {
        changesets.put((long) changeset.getId(), changeset);
        fireHistoryUpdated(null);
    }

    /**
     * Replies the history for a given primitive with id <code>id</code>
     * and type <code>type</code>.
     *
     * @param id the id the if of the primitive. &gt; 0 required
     * @param type the type of the primitive. Must not be null.
     * @return the history. null, if there isn't a history for <code>id</code> and
     * <code>type</code>.
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if type is null
     */
    public History getHistory(long id, OsmPrimitiveType type) {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected, got {1}", "id", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        SimplePrimitiveId pid = new SimplePrimitiveId(id, type);
        return getHistory(pid);
    }

    /**
     * Replies the history for a primitive with id <code>id</code>. null, if no
     * such history exists.
     *
     * @param pid the primitive id. Must not be null.
     * @return the history for a primitive with id <code>id</code>. null, if no
     * such history exists
     * @throws NullPointerException if pid is null
     */
    public History getHistory(PrimitiveId pid) {
        PrimitiveId key = pid instanceof IPrimitive ? ((IPrimitive) pid).getPrimitiveId()
                        : pid instanceof HistoryOsmPrimitive ? ((HistoryOsmPrimitive) pid).getPrimitiveId()
                        : pid;
        List<HistoryOsmPrimitive> versions = data.get(Objects.requireNonNull(key, "key"));
        if (versions == null)
            return null;
        for (HistoryOsmPrimitive i : versions) {
            i.setChangeset(changesets.get(i.getChangesetId()));
        }
        return new History(pid.getUniqueId(), pid.getType(), versions);
    }

    /**
     * merges the histories from the {@link HistoryDataSet} other in this history data set
     *
     * @param other the other history data set. Ignored if null.
     */
    public void mergeInto(HistoryDataSet other) {
        if (other == null)
            return;
        this.data.putAll(other.data);
        this.changesets.putAll(other.changesets);
        fireHistoryUpdated(null);
    }

    /**
     * Gets a unsorted set of all changeset ids that were used by the primitives in this data set
     * @return The ids
     */
    public Collection<Long> getChangesetIds() {
        return data.values().stream()
                .flatMap(Collection::stream)
                .map(HistoryOsmPrimitive::getChangesetId)
                .collect(Collectors.toSet());
    }

    /* ------------------------------------------------------------------------------ */
    /* interface LayerChangeListener                                                  */
    /* ------------------------------------------------------------------------------ */
    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        /* irrelevant in this context */
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        /* irrelevant in this context */
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (!MainApplication.isDisplayingMapView()) return;
        if (MainApplication.getLayerManager().getLayers().isEmpty()) {
            data.clear();
            fireCacheCleared();
        }
    }
}
