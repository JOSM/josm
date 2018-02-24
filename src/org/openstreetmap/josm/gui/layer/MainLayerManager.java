// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.AsynchronousUploadPrimitivesTask;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;

/**
 * This class extends the layer manager by adding an active and an edit layer.
 * <p>
 * The active layer is the layer the user is currently working on.
 * <p>
 * The edit layer is an data layer that we currently work with.
 * @author Michael Zangl
 * @since 10279
 */
public class MainLayerManager extends LayerManager {
    /**
     * This listener listens to changes of the active or the edit layer.
     * @author Michael Zangl
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface ActiveLayerChangeListener {
        /**
         * Called whenever the active or edit layer changed.
         * <p>
         * You can be sure that this layer is still contained in this set.
         * <p>
         * Listeners are called in the EDT thread and you can manipulate the layer manager in the current thread.
         * @param e The change event.
         */
        void activeOrEditLayerChanged(ActiveLayerChangeEvent e);
    }

    /**
     * This event is fired whenever the active or the data layer changes.
     * @author Michael Zangl
     */
    public static class ActiveLayerChangeEvent extends LayerManagerEvent {

        private final OsmDataLayer previousDataLayer;

        private final Layer previousActiveLayer;

        /**
         * Create a new {@link ActiveLayerChangeEvent}
         * @param source The source
         * @param previousDataLayer the previous data layer
         * @param previousActiveLayer the previous active layer
         */
        ActiveLayerChangeEvent(MainLayerManager source, OsmDataLayer previousDataLayer,
                Layer previousActiveLayer) {
            super(source);
            this.previousDataLayer = previousDataLayer;
            this.previousActiveLayer = previousActiveLayer;
        }

        /**
         * Gets the data layer that was previously used.
         * @return The old data layer, <code>null</code> if there is none.
         * @deprecated use {@link #getPreviousDataLayer}
         */
        @Deprecated
        public OsmDataLayer getPreviousEditLayer() {
            return getPreviousDataLayer();
        }

        /**
         * Gets the data layer that was previously used.
         * @return The old data layer, <code>null</code> if there is none.
         * @since 13434
         */
        public OsmDataLayer getPreviousDataLayer() {
            return previousDataLayer;
        }

        /**
         * Gets the active layer that was previously used.
         * @return The old active layer, <code>null</code> if there is none.
         */
        public Layer getPreviousActiveLayer() {
            return previousActiveLayer;
        }

        /**
         * Gets the data set that was previously used.
         * @return The data set of {@link #getPreviousDataLayer()}.
         * @deprecated use {@link #getPreviousDataSet}
         */
        @Deprecated
        public DataSet getPreviousEditDataSet() {
            return getPreviousDataSet();
        }

        /**
         * Gets the data set that was previously used.
         * @return The data set of {@link #getPreviousDataLayer()}.
         * @since 13434
         */
        public DataSet getPreviousDataSet() {
            if (previousDataLayer != null) {
                return previousDataLayer.data;
            } else {
                return null;
            }
        }

        @Override
        public MainLayerManager getSource() {
            return (MainLayerManager) super.getSource();
        }
    }

    /**
     * This event is fired for {@link LayerAvailabilityListener}
     * @author Michael Zangl
     * @since 10508
     */
    public static class LayerAvailabilityEvent extends LayerManagerEvent {
        private final boolean hasLayers;

        LayerAvailabilityEvent(LayerManager source, boolean hasLayers) {
            super(source);
            this.hasLayers = hasLayers;
        }

        /**
         * Checks if this layer manager will have layers afterwards
         * @return true if layers will be added.
         */
        public boolean hasLayers() {
            return hasLayers;
        }
    }

    /**
     * A listener that gets informed before any layer is displayed and after all layers are removed.
     * @author Michael Zangl
     * @since 10508
     */
    public interface LayerAvailabilityListener {
        /**
         * This method is called in the UI thread right before the first layer is added.
         * @param e The event.
         */
        void beforeFirstLayerAdded(LayerAvailabilityEvent e);

        /**
         * This method is called in the UI thread after the last layer was removed.
         * @param e The event.
         */
        void afterLastLayerRemoved(LayerAvailabilityEvent e);
    }

    /**
     * The layer from the layers list that is currently active.
     */
    private Layer activeLayer;

    /**
     * The current active data layer. It might be editable or not, based on its read-only status.
     */
    private OsmDataLayer dataLayer;

    private final List<ActiveLayerChangeListener> activeLayerChangeListeners = new CopyOnWriteArrayList<>();
    private final List<LayerAvailabilityListener> layerAvailabilityListeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a active/edit layer change listener
     *
     * @param listener the listener.
     */
    public synchronized void addActiveLayerChangeListener(ActiveLayerChangeListener listener) {
        if (activeLayerChangeListeners.contains(listener)) {
            throw new IllegalArgumentException("Attempted to add listener that was already in list: " + listener);
        }
        activeLayerChangeListeners.add(listener);
    }

    /**
     * Adds a active/edit layer change listener. Fire a fake active-layer-changed-event right after adding
     * the listener. The previous layers will be null. The listener is notified in the current thread.
     * @param listener the listener.
     */
    public synchronized void addAndFireActiveLayerChangeListener(ActiveLayerChangeListener listener) {
        addActiveLayerChangeListener(listener);
        listener.activeOrEditLayerChanged(new ActiveLayerChangeEvent(this, null, null));
    }

    /**
     * Removes an active/edit layer change listener.
     * @param listener the listener.
     */
    public synchronized void removeActiveLayerChangeListener(ActiveLayerChangeListener listener) {
        if (!activeLayerChangeListeners.contains(listener)) {
            throw new IllegalArgumentException("Attempted to remove listener that was not in list: " + listener);
        }
        activeLayerChangeListeners.remove(listener);
    }

    /**
     * Add a new {@link LayerAvailabilityListener}.
     * @param listener The listener
     * @since 10508
     */
    public synchronized void addLayerAvailabilityListener(LayerAvailabilityListener listener) {
        if (!layerAvailabilityListeners.add(listener)) {
            throw new IllegalArgumentException("Attempted to add listener that was already in list: " + listener);
        }
    }

    /**
     * Remove an {@link LayerAvailabilityListener}.
     * @param listener The listener
     * @since 10508
     */
    public synchronized void removeLayerAvailabilityListener(LayerAvailabilityListener listener) {
        if (!layerAvailabilityListeners.remove(listener)) {
            throw new IllegalArgumentException("Attempted to remove listener that was not in list: " + listener);
        }
    }

    /**
     * Set the active layer, unless the layer is being uploaded.
     * If the layer is an OsmDataLayer, the edit layer is also changed.
     * @param layer The active layer.
     */
    public void setActiveLayer(final Layer layer) {
        // we force this on to the EDT Thread to make events fire from there.
        // The synchronization lock needs to be held by the EDT.
        if (layer instanceof OsmDataLayer && ((OsmDataLayer) layer).isUploadInProgress()) {
            GuiHelper.runInEDT(() ->
                    JOptionPane.showMessageDialog(
                            MainApplication.parent,
                            tr("Trying to set a read only data layer as edit layer"),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE));
        } else {
            GuiHelper.runInEDTAndWaitWithException(() -> realSetActiveLayer(layer));
        }
    }

    protected synchronized void realSetActiveLayer(final Layer layer) {
        // to be called in EDT thread
        checkContainsLayer(layer);
        setActiveLayer(layer, false);
    }

    private void setActiveLayer(Layer layer, boolean forceEditLayerUpdate) {
        ActiveLayerChangeEvent event = new ActiveLayerChangeEvent(this, dataLayer, activeLayer);
        activeLayer = layer;
        if (activeLayer instanceof OsmDataLayer) {
            dataLayer = (OsmDataLayer) activeLayer;
        } else if (forceEditLayerUpdate) {
            dataLayer = null;
        }
        fireActiveLayerChange(event);
    }

    private void fireActiveLayerChange(ActiveLayerChangeEvent event) {
        GuiHelper.assertCallFromEdt();
        if (event.getPreviousActiveLayer() != activeLayer || event.getPreviousDataLayer() != dataLayer) {
            for (ActiveLayerChangeListener l : activeLayerChangeListeners) {
                l.activeOrEditLayerChanged(event);
            }
        }
    }

    @Override
    protected synchronized void realAddLayer(Layer layer, boolean initialZoom) {
        if (getLayers().isEmpty()) {
            LayerAvailabilityEvent e = new LayerAvailabilityEvent(this, true);
            for (LayerAvailabilityListener l : layerAvailabilityListeners) {
                l.beforeFirstLayerAdded(e);
            }
        }
        super.realAddLayer(layer, initialZoom);

        // update the active layer automatically.
        if (layer instanceof OsmDataLayer || activeLayer == null) {
            setActiveLayer(layer);
        }
    }

    @Override
    protected Collection<Layer> realRemoveSingleLayer(Layer layer) {
        if ((layer instanceof OsmDataLayer) && (((OsmDataLayer) layer).isUploadInProgress())) {
            GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(MainApplication.parent,
                    tr("Trying to delete the layer with background upload. Please wait until the upload is finished.")));

            // Return an empty collection for allowing to delete other layers
            return new ArrayList<>();
        }

        if (layer == activeLayer || layer == dataLayer) {
            Layer nextActive = suggestNextActiveLayer(layer);
            setActiveLayer(nextActive, true);
        }

        Collection<Layer> toDelete = super.realRemoveSingleLayer(layer);
        if (getLayers().isEmpty()) {
            LayerAvailabilityEvent e = new LayerAvailabilityEvent(this, false);
            for (LayerAvailabilityListener l : layerAvailabilityListeners) {
                l.afterLastLayerRemoved(e);
            }
        }
        return toDelete;
    }

    /**
     * Determines the next active data layer according to the following
     * rules:
     * <ul>
     *   <li>if there is at least one {@link OsmDataLayer} the first one
     *     becomes active</li>
     *   <li>otherwise, the top most layer of any type becomes active</li>
     * </ul>
     *
     * @param except A layer to ignore.
     * @return the next active data layer
     */
    private Layer suggestNextActiveLayer(Layer except) {
        List<Layer> layersList = new ArrayList<>(getLayers());
        layersList.remove(except);
        // First look for data layer
        for (Layer layer : layersList) {
            if (layer instanceof OsmDataLayer) {
                return layer;
            }
        }

        // Then any layer
        if (!layersList.isEmpty())
            return layersList.get(0);

        // and then give up
        return null;
    }

    /**
     * Replies the currently active layer
     *
     * @return the currently active layer (may be null)
     */
    public synchronized Layer getActiveLayer() {
        if (activeLayer instanceof OsmDataLayer) {
            if (!((OsmDataLayer) activeLayer).isUploadInProgress()) {
                return activeLayer;
            } else {
                return null;
            }
        } else {
            return activeLayer;
        }
    }

    /**
     * Replies the current edit layer, if present and not readOnly
     *
     * @return the current edit layer. May be null.
     * @see #getActiveDataLayer
     */
    public synchronized OsmDataLayer getEditLayer() {
        if (dataLayer != null && !dataLayer.isLocked())
            return dataLayer;
        else
            return null;
    }

    /**
     * Replies the active data layer. The layer can be read-only.
     *
     * @return the current data layer. May be null or read-only.
     * @see #getEditLayer
     * @since 13434
     */
    public synchronized OsmDataLayer getActiveDataLayer() {
        if (dataLayer != null)
            return dataLayer;
        else
            return null;
    }

    /**
     * Gets the data set of the active edit layer, if not readOnly.
     * @return That data set, <code>null</code> if there is no edit layer.
     * @see #getActiveDataSet
     */
    public synchronized DataSet getEditDataSet() {
        if (dataLayer != null && !dataLayer.isLocked()) {
            return dataLayer.data;
        } else {
            return null;
        }
    }

    /**
     * Gets the data set of the active data layer. The dataset can be read-only.
     * @return That data set, <code>null</code> if there is no active data layer.
     * @see #getEditDataSet
     * @since 13434
     */
    public synchronized DataSet getActiveDataSet() {
        if (dataLayer != null) {
            return dataLayer.data;
        } else {
            return null;
        }
    }

    /**
     * Returns the unique note layer, if present.
     * @return the unique note layer, or null
     * @since 13437
     */
    public NoteLayer getNoteLayer() {
        List<NoteLayer> col = getLayersOfType(NoteLayer.class);
        return col.isEmpty() ? null : col.get(0);
    }

    /**
     * Creates a list of the visible layers in Z-Order, the layer with the lowest Z-Order
     * first, layer with the highest Z-Order last.
     * <p>
     * The active data layer is pulled above all adjacent data layers.
     *
     * @return a list of the visible in Z-Order, the layer with the lowest Z-Order
     * first, layer with the highest Z-Order last.
     */
    public synchronized List<Layer> getVisibleLayersInZOrder() {
        List<Layer> ret = new ArrayList<>();
        // This is set while we delay the addition of the active layer.
        boolean activeLayerDelayed = false;
        List<Layer> layers = getLayers();
        for (ListIterator<Layer> iterator = layers.listIterator(layers.size()); iterator.hasPrevious();) {
            Layer l = iterator.previous();
            if (!l.isVisible()) {
                // ignored
            } else if (l == activeLayer && l instanceof OsmDataLayer) {
                // delay and add after the current block of OsmDataLayer
                activeLayerDelayed = true;
            } else {
                if (activeLayerDelayed && !(l instanceof OsmDataLayer)) {
                    // add active layer before the current one.
                    ret.add(activeLayer);
                    activeLayerDelayed = false;
                }
                // Add this layer now
                ret.add(l);
            }
        }
        if (activeLayerDelayed) {
            ret.add(activeLayer);
        }
        return ret;
    }

    /**
     * Invalidates current edit layer, if any. Does nothing if there is no edit layer.
     * @since 13150
     */
    public void invalidateEditLayer() {
        if (dataLayer != null) {
            dataLayer.invalidate();
        }
    }

    @Override
    protected synchronized void realResetState() {
        // Reset state if no asynchronous upload is under progress
        if (!AsynchronousUploadPrimitivesTask.getCurrentAsynchronousUploadTask().isPresent()) {
            // active and edit layer are unset automatically
            super.realResetState();

            activeLayerChangeListeners.clear();
            layerAvailabilityListeners.clear();
        } else {
            String msg = tr("A background upload is already in progress. Cannot reset state until the upload is finished.");
            Logging.warn(msg);
            if (!GraphicsEnvironment.isHeadless()) {
                GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(MainApplication.parent, msg));
            }
        }
    }

    /**
     * Prepares an OsmDataLayer for upload. The layer to be uploaded is locked and
     * if the layer to be uploaded is the current editLayer then editLayer is reset
     * to null for disallowing any changes to the layer. An ActiveLayerChangeEvent
     * is fired to notify the listeners
     *
     * @param layer The OsmDataLayer to be uploaded
     */
    public synchronized void prepareLayerForUpload(OsmDataLayer layer) {
        GuiHelper.assertCallFromEdt();
        layer.setUploadInProgress();
        layer.lock();

        // Reset only the edit layer as empty
        if (dataLayer == layer) {
            ActiveLayerChangeEvent activeLayerChangeEvent = new ActiveLayerChangeEvent(this, dataLayer, activeLayer);
            dataLayer = null;
            fireActiveLayerChange(activeLayerChangeEvent);
        }
    }

    /**
     * Post upload processing of the OsmDataLayer.
     * If the current edit layer is empty this function sets the layer uploaded as the
     * current editLayer. An ActiveLayerChangeEvent is fired to notify the listeners
     *
     * @param layer The OsmDataLayer uploaded
     */
    public synchronized void processLayerAfterUpload(OsmDataLayer layer) {
        GuiHelper.assertCallFromEdt();
        layer.unlock();
        layer.unsetUploadInProgress();

        // Set the layer as edit layer if the edit layer is empty.
        if (dataLayer == null) {
            ActiveLayerChangeEvent layerChangeEvent = new ActiveLayerChangeEvent(this, dataLayer, activeLayer);
            dataLayer = layer;
            fireActiveLayerChange(layerChangeEvent);
        }
    }
}
