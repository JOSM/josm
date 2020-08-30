// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This class handles the layer management.
 * <p>
 * This manager handles a list of layers with the first layer being the front layer.
 * <h1>Threading</h1>
 * Synchronization of the layer manager is done by synchronizing all read/write access. All changes are internally done in the EDT thread.
 *
 * Methods of this manager may be called from any thread in any order.
 * Listeners are called while this layer manager is locked, so they should not block on other threads.
 *
 * @author Michael Zangl
 * @since 10273
 */
public class LayerManager {
    /**
     * Interface to notify listeners of a layer change.
     */
    public interface LayerChangeListener {
        /**
         * Notifies this listener that a layer has been added.
         * <p>
         * Listeners are called in the EDT thread. You should not do blocking or long-running tasks in this method.
         * @param e The new added layer event
         */
        void layerAdded(LayerAddEvent e);

        /**
         * Notifies this listener that a alayer was just removed.
         * <p>
         * Listeners are called in the EDT thread after the layer was removed.
         * Use {@link LayerRemoveEvent#scheduleRemoval(Collection)} to remove more layers.
         * You should not do blocking or long-running tasks in this method.
         * @param e The layer to be removed (as event)
         */
        void layerRemoving(LayerRemoveEvent e);

        /**
         * Notifies this listener that the order of layers was changed.
         * <p>
         * Listeners are called in the EDT thread.
         *  You should not do blocking or long-running tasks in this method.
         * @param e The order change event.
         */
        void layerOrderChanged(LayerOrderChangeEvent e);
    }

    /**
     * Base class of layer manager events.
     */
    protected static class LayerManagerEvent {
        private final LayerManager source;

        LayerManagerEvent(LayerManager source) {
            this.source = source;
        }

        /**
         * Returns the {@code LayerManager} at the origin of this event.
         * @return the {@code LayerManager} at the origin of this event
         */
        public LayerManager getSource() {
            return source;
        }
    }

    /**
     * The event that is fired whenever a layer was added.
     * @author Michael Zangl
     */
    public static class LayerAddEvent extends LayerManagerEvent {
        private final Layer addedLayer;
        private final boolean requiresZoom;

        LayerAddEvent(LayerManager source, Layer addedLayer, boolean requiresZoom) {
            super(source);
            this.addedLayer = addedLayer;
            this.requiresZoom = requiresZoom;
        }

        /**
         * Gets the layer that was added.
         * @return The added layer.
         */
        public Layer getAddedLayer() {
            return addedLayer;
        }

        /**
         * Determines if an initial zoom is required.
         * @return {@code true} if a zoom is required when this layer is added
         * @since 11774
         */
        public final boolean isZoomRequired() {
            return requiresZoom;
        }

        @Override
        public String toString() {
            return "LayerAddEvent [addedLayer=" + addedLayer + ']';
        }
    }

    /**
     * The event that is fired before removing a layer.
     * @author Michael Zangl
     */
    public static class LayerRemoveEvent extends LayerManagerEvent {
        private final Layer removedLayer;
        private final boolean lastLayer;
        private final Collection<Layer> scheduleForRemoval = new ArrayList<>();

        LayerRemoveEvent(LayerManager source, Layer removedLayer) {
            super(source);
            this.removedLayer = removedLayer;
            this.lastLayer = source.getLayers().size() == 1;
        }

        /**
         * Gets the layer that is about to be removed.
         * @return The layer.
         */
        public Layer getRemovedLayer() {
            return removedLayer;
        }

        /**
         * Check if the layer that was removed is the last layer in the list.
         * @return <code>true</code> if this was the last layer.
         * @since 10432
         */
        public boolean isLastLayer() {
            return lastLayer;
        }

        /**
         * Schedule the removal of other layers after this layer has been deleted.
         * <p>
         * Duplicate removal requests are ignored.
         * @param layers The layers to remove.
         * @since 10507
         */
        public void scheduleRemoval(Collection<? extends Layer> layers) {
            for (Layer layer : layers) {
                getSource().checkContainsLayer(layer);
            }
            scheduleForRemoval.addAll(layers);
        }

        @Override
        public String toString() {
            return "LayerRemoveEvent [removedLayer=" + removedLayer + ", lastLayer=" + lastLayer + ']';
        }
    }

    /**
     * An event that is fired whenever the order of layers changed.
     * <p>
     * We currently do not report the exact changes.
     * @author Michael Zangl
     */
    public static class LayerOrderChangeEvent extends LayerManagerEvent {
        LayerOrderChangeEvent(LayerManager source) {
            super(source);
        }

        @Override
        public String toString() {
            return "LayerOrderChangeEvent []";
        }
    }

    /**
     * This is the list of layers we manage. The list is unmodifyable. That way, read access does not need to be synchronized.
     *
     * It is only changed in the EDT.
     * @see LayerManager#updateLayers(Consumer)
     */
    private volatile List<Layer> layers = Collections.emptyList();

    private final List<LayerChangeListener> layerChangeListeners = new CopyOnWriteArrayList<>();

    /**
     * Add a layer. The layer will be added at a given position and the mapview zoomed at its projection bounds.
     * @param layer The layer to add
     */
    public void addLayer(final Layer layer) {
        addLayer(layer, true);
    }

    /**
     * Add a layer. The layer will be added at a given position.
     * @param layer The layer to add
     * @param initialZoom whether if the mapview must be zoomed at layer projection bounds
     */
    public void addLayer(final Layer layer, final boolean initialZoom) {
        // we force this on to the EDT Thread to make events fire from there.
        // The synchronization lock needs to be held by the EDT.
        GuiHelper.runInEDTAndWaitWithException(() -> realAddLayer(layer, initialZoom));
    }

    /**
     * Add a layer (implementation).
     * @param layer The layer to add
     * @param initialZoom whether if the mapview must be zoomed at layer projection bounds
     */
    protected synchronized void realAddLayer(Layer layer, boolean initialZoom) {
        if (containsLayer(layer)) {
            throw new IllegalArgumentException("Cannot add a layer twice: " + layer);
        }
        LayerPositionStrategy positionStrategy = layer.getDefaultLayerPosition();
        int position = positionStrategy.getPosition(this);
        checkPosition(position);
        insertLayerAt(layer, position);
        fireLayerAdded(layer, initialZoom);
        if (MainApplication.getMap() != null) {
            layer.hookUpMapView(); // needs to be after fireLayerAdded
        }
    }

    /**
     * Remove the layer from the mapview. If the layer was in the list before,
     * an LayerChange event is fired.
     * @param layer The layer to remove
     */
    public void removeLayer(final Layer layer) {
        // we force this on to the EDT Thread to make events fire from there.
        // The synchronization lock needs to be held by the EDT.
        GuiHelper.runInEDTAndWaitWithException(() -> realRemoveLayer(layer));
    }

    /**
     * Remove the layer from the mapview (implementation).
     * @param layer The layer to remove
     */
    protected synchronized void realRemoveLayer(Layer layer) {
        GuiHelper.assertCallFromEdt();
        Set<Layer> toRemove = Collections.newSetFromMap(new IdentityHashMap<Layer, Boolean>());
        toRemove.add(layer);

        while (!toRemove.isEmpty()) {
            Iterator<Layer> iterator = toRemove.iterator();
            Layer layerToRemove = iterator.next();
            iterator.remove();
            checkContainsLayer(layerToRemove);

            Collection<Layer> newToRemove = realRemoveSingleLayer(layerToRemove);
            toRemove.addAll(newToRemove);
        }
    }

    /**
     * Remove a single layer from the mapview (implementation).
     * @param layerToRemove The layer to remove
     * @return A list of layers that should be removed afterwards.
     */
    protected Collection<Layer> realRemoveSingleLayer(Layer layerToRemove) {
        updateLayers(mutableLayers -> mutableLayers.remove(layerToRemove));
        return fireLayerRemoving(layerToRemove);
    }

    /**
     * Move a layer to a new position.
     * @param layer The layer to move.
     * @param position The position.
     * @throws IndexOutOfBoundsException if the position is out of bounds.
     */
    public void moveLayer(final Layer layer, final int position) {
        // we force this on to the EDT Thread to make events fire from there.
        // The synchronization lock needs to be held by the EDT.
        GuiHelper.runInEDTAndWaitWithException(() -> realMoveLayer(layer, position));
    }

    /**
     * Move a layer to a new position (implementation).
     * @param layer The layer to move.
     * @param position The position.
     * @throws IndexOutOfBoundsException if the position is out of bounds.
     */
    protected synchronized void realMoveLayer(Layer layer, int position) {
        checkContainsLayer(layer);
        checkPosition(position);

        int curLayerPos = getLayers().indexOf(layer);
        if (position == curLayerPos)
            return; // already in place.
        // update needs to be done in one run
        updateLayers(mutableLayers -> {
            mutableLayers.remove(curLayerPos);
            insertLayerAt(mutableLayers, layer, position);
        });
        fireLayerOrderChanged();
    }

    /**
     * Insert a layer at a given position.
     * @param layer The layer to add.
     * @param position The position on which we should add it.
     */
    private void insertLayerAt(Layer layer, int position) {
        updateLayers(mutableLayers -> insertLayerAt(mutableLayers, layer, position));
    }

    private static void insertLayerAt(List<Layer> layers, Layer layer, int position) {
        if (position == layers.size()) {
            layers.add(layer);
        } else {
            layers.add(position, layer);
        }
    }

    /**
     * Check if the (new) position is valid
     * @param position The position index
     * @throws IndexOutOfBoundsException if it is not.
     */
    private void checkPosition(int position) {
        if (position < 0 || position > getLayers().size()) {
            throw new IndexOutOfBoundsException("Position " + position + " out of range.");
        }
    }

    /**
     * Update the {@link #layers} field. This method should be used instead of a direct field access.
     * @param mutator A method that gets the writable list of layers and should modify it.
     */
    private void updateLayers(Consumer<List<Layer>> mutator) {
        GuiHelper.assertCallFromEdt();
        ArrayList<Layer> newLayers = new ArrayList<>(getLayers());
        mutator.accept(newLayers);
        layers = Collections.unmodifiableList(newLayers);
    }

    /**
     * Gets an unmodifiable list of all layers that are currently in this manager. This list won't update once layers are added or removed.
     * @return The list of layers.
     */
    public List<Layer> getLayers() {
        return layers;
    }

    /**
     * Replies an unmodifiable list of layers of a certain type.
     *
     * Example:
     * <pre>
     *     List&lt;WMSLayer&gt; wmsLayers = getLayersOfType(WMSLayer.class);
     * </pre>
     * @param <T> The layer type
     * @param ofType The layer type.
     * @return an unmodifiable list of layers of a certain type.
     */
    public <T extends Layer> List<T> getLayersOfType(Class<T> ofType) {
        return new ArrayList<>(Utils.filteredCollection(getLayers(), ofType));
    }

    /**
     * replies true if the list of layers managed by this map view contain layer
     *
     * @param layer the layer
     * @return true if the list of layers managed by this map view contain layer
     */
    public boolean containsLayer(Layer layer) {
        return getLayers().contains(layer);
    }

    /**
     * Checks if the specified layer is handled by this layer manager.
     * @param layer layer to check
     * @throws IllegalArgumentException if layer is not handled by this layer manager
     */
    protected void checkContainsLayer(Layer layer) {
        if (!containsLayer(layer)) {
            throw new IllegalArgumentException(layer + " is not managed by us.");
        }
    }

    /**
     * Adds a layer change listener
     *
     * @param listener the listener.
     * @throws IllegalArgumentException If the listener was added twice.
     * @see #addAndFireLayerChangeListener
     */
    public synchronized void addLayerChangeListener(LayerChangeListener listener) {
        if (layerChangeListeners.contains(listener)) {
            throw new IllegalArgumentException("Listener already registered.");
        }
        layerChangeListeners.add(listener);
    }

    /**
     * Adds a layer change listener and fire an add event for every layer in this manager.
     *
     * @param listener the listener.
     * @throws IllegalArgumentException If the listener was added twice.
     * @see #addLayerChangeListener
     * @since 11905
     */
    public synchronized void addAndFireLayerChangeListener(LayerChangeListener listener) {
        addLayerChangeListener(listener);
        for (Layer l : getLayers()) {
            listener.layerAdded(new LayerAddEvent(this, l, true));
        }
    }

    /**
     * Removes a layer change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     * @see #removeAndFireLayerChangeListener
     */
    public synchronized void removeLayerChangeListener(LayerChangeListener listener) {
        if (!layerChangeListeners.remove(listener)) {
            throw new IllegalArgumentException("Listener was not registered before: " + listener);
        }
    }

    /**
     * Removes a layer change listener and fire a remove event for every layer in this manager.
     * The event is fired as if the layer was deleted but
     * {@link LayerRemoveEvent#scheduleRemoval(Collection)} is ignored.
     *
     * @param listener the listener.
     * @see #removeLayerChangeListener
     * @since 11905
     */
    public synchronized void removeAndFireLayerChangeListener(LayerChangeListener listener) {
        removeLayerChangeListener(listener);
        for (Layer l : getLayers()) {
            listener.layerRemoving(new LayerRemoveEvent(this, l));
        }
    }

    private void fireLayerAdded(Layer layer, boolean initialZoom) {
        GuiHelper.assertCallFromEdt();
        LayerAddEvent e = new LayerAddEvent(this, layer, initialZoom);
        for (LayerChangeListener l : layerChangeListeners) {
            try {
                l.layerAdded(e);
            } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException t) {
                throw BugReport.intercept(t).put("listener", l).put("event", e);
            }
        }
    }

    /**
     * Fire the layer remove event
     * @param layer The layer that was removed
     * @return A list of layers that should be removed afterwards.
     */
    private Collection<Layer> fireLayerRemoving(Layer layer) {
        GuiHelper.assertCallFromEdt();
        LayerRemoveEvent e = new LayerRemoveEvent(this, layer);
        for (LayerChangeListener l : layerChangeListeners) {
            try {
                l.layerRemoving(e);
            } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException t) {
                throw BugReport.intercept(t).put("listener", l).put("event", e).put("layer", layer);
            }
        }
        if (layer instanceof OsmDataLayer) {
            ((OsmDataLayer) layer).clear();
        }
        return e.scheduleForRemoval;
    }

    private void fireLayerOrderChanged() {
        GuiHelper.assertCallFromEdt();
        LayerOrderChangeEvent e = new LayerOrderChangeEvent(this);
        for (LayerChangeListener l : layerChangeListeners) {
            try {
                l.layerOrderChanged(e);
            } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException t) {
                throw BugReport.intercept(t).put("listener", l).put("event", e);
            }
        }
    }

    /**
     * Reset all layer manager state. This includes removing all layers and then unregistering all listeners
     * @since 10432
     */
    public void resetState() {
        // we force this on to the EDT Thread to have a clean synchronization
        // The synchronization lock needs to be held by the EDT.
        GuiHelper.runInEDTAndWaitWithException(this::realResetState);
    }

    /**
     * Reset all layer manager state (implementation).
     */
    protected synchronized void realResetState() {
        // The listeners trigger the removal of other layers
        while (!getLayers().isEmpty()) {
            removeLayer(getLayers().get(0));
        }

        layerChangeListeners.clear();
    }
}
