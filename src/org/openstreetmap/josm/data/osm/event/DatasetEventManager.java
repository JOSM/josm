// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;


/**
 * This class allows to add DatasetListener to currently active dataset. If active
 * layer is changed, listeners are automatically registered at new active dataset
 * (it's no longer necessary to register for layer events and reregister every time
 * new layer is selected)
 * 
 * Events in EDT are supported, see {@link #addDatasetListener(DataSetListener, boolean)}
 *
 */
public class DatasetEventManager implements MapView.EditLayerChangeListener, Listener {

    private static final DatasetEventManager instance = new DatasetEventManager();

    public static DatasetEventManager getInstance() {
        return instance;
    }

    private final Queue<AbstractDatasetChangedEvent> eventsInEDT = new LinkedBlockingQueue<AbstractDatasetChangedEvent>();
    private final CopyOnWriteArrayList<DataSetListener> inEDTListeners = new CopyOnWriteArrayList<DataSetListener>();
    private final CopyOnWriteArrayList<DataSetListener> normalListeners = new CopyOnWriteArrayList<DataSetListener>();
    private final DataSetListener myListener = new DataSetListenerAdapter(this);

    public DatasetEventManager() {
        MapView.addEditLayerChangeListener(this);
    }

    /**
     * Register listener, that will receive events from currently active dataset
     * @param listener
     * @param fireInEDT If true, listener will be notified in event dispatch thread
     * instead of thread that caused the dataset change
     */
    public void addDatasetListener(DataSetListener listener, boolean fireInEDT) {
        if (fireInEDT) {
            inEDTListeners.addIfAbsent(listener);
        } else {
            normalListeners.addIfAbsent(listener);
        }
    }

    public void removeDatasetListener(DataSetListener listener) {
        inEDTListeners.remove(listener);
        normalListeners.remove(listener);
    }

    public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        if (oldLayer != null) {
            oldLayer.data.removeDataSetListener(myListener);
        }

        if (newLayer != null) {
            newLayer.data.addDataSetListener(myListener);
            processDatasetEvent(new DataChangedEvent(newLayer.data));
        } else {
            processDatasetEvent(new DataChangedEvent(null));
        }
    }

    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        for (DataSetListener listener: normalListeners) {
            event.fire(listener);
        }
        eventsInEDT.add(event);
        SwingUtilities.invokeLater(edtRunnable);
    }

    private final Runnable edtRunnable = new Runnable() {
        public void run() {
            AbstractDatasetChangedEvent event = null;
            while ((event = eventsInEDT.poll()) != null) {
                for (DataSetListener listener: inEDTListeners) {
                    event.fire(listener);
                }
            }
        }
    };
}
