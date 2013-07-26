// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This class allows to add DatasetListener to currently active dataset. If active
 * layer is changed, listeners are automatically registered at new active dataset
 * (it's no longer necessary to register for layer events and reregister every time
 * new layer is selected)
 *
 * Events in EDT are supported, see {@link #addDatasetListener(DataSetListener, FireMode)}
 *
 */
public class DatasetEventManager implements MapView.EditLayerChangeListener, Listener {

    private static final DatasetEventManager instance = new DatasetEventManager();

    public enum FireMode {
        /**
         * Fire in calling thread immediately.
         */
        IMMEDIATELY,
        /**
         * Fire in event dispatch thread.
         */
        IN_EDT,
        /**
         * Fire in event dispatch thread. If more than one event arrived when event queue is checked, merged them to
         * one event
         */
        IN_EDT_CONSOLIDATED}

    private static class ListenerInfo {
        final DataSetListener listener;
        final boolean consolidate;

        public ListenerInfo(DataSetListener listener, boolean consolidate) {
            this.listener = listener;
            this.consolidate = consolidate;
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ListenerInfo && ((ListenerInfo)o).listener == listener;
        }
    }

    public static DatasetEventManager getInstance() {
        return instance;
    }

    private final Queue<AbstractDatasetChangedEvent> eventsInEDT = new LinkedBlockingQueue<AbstractDatasetChangedEvent>();
    private final CopyOnWriteArrayList<ListenerInfo> inEDTListeners = new CopyOnWriteArrayList<ListenerInfo>();
    private final CopyOnWriteArrayList<ListenerInfo> normalListeners = new CopyOnWriteArrayList<ListenerInfo>();
    private final DataSetListener myListener = new DataSetListenerAdapter(this);

    public DatasetEventManager() {
        MapView.addEditLayerChangeListener(this);
    }

    /**
     * Register listener, that will receive events from currently active dataset
     * @param listener the listener to be registered
     * @param fireMode If {@link FireMode#IN_EDT} or {@link FireMode#IN_EDT_CONSOLIDATED},
     * listener will be notified in event dispatch thread instead of thread that caused
     * the dataset change
     */
    public void addDatasetListener(DataSetListener listener, FireMode fireMode) {
        if (fireMode == FireMode.IN_EDT || fireMode == FireMode.IN_EDT_CONSOLIDATED) {
            inEDTListeners.addIfAbsent(new ListenerInfo(listener, fireMode == FireMode.IN_EDT_CONSOLIDATED));
        } else {
            normalListeners.addIfAbsent(new ListenerInfo(listener, false));
        }
    }

    public void removeDatasetListener(DataSetListener listener) {
        ListenerInfo searchListener = new ListenerInfo(listener, false);
        inEDTListeners.remove(searchListener);
        normalListeners.remove(searchListener);
    }

    @Override
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

    private void fireEvents(List<ListenerInfo> listeners, AbstractDatasetChangedEvent event) {
        for (ListenerInfo listener: listeners) {
            if (!listener.consolidate) {
                event.fire(listener.listener);
            }
        }
    }

    private void fireConsolidatedEvents(List<ListenerInfo> listeners, AbstractDatasetChangedEvent event) {
        for (ListenerInfo listener: listeners) {
            if (listener.consolidate) {
                event.fire(listener.listener);
            }
        }
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        fireEvents(normalListeners, event);
        eventsInEDT.add(event);
        SwingUtilities.invokeLater(edtRunnable);
    }

    private final Runnable edtRunnable = new Runnable() {
        @Override
        public void run() {
            while (!eventsInEDT.isEmpty()) {
                List<AbstractDatasetChangedEvent> events = new ArrayList<AbstractDatasetChangedEvent>();
                events.addAll(eventsInEDT);

                DataSet dataSet = null;
                AbstractDatasetChangedEvent consolidatedEvent = null;
                AbstractDatasetChangedEvent event = null;

                while ((event = eventsInEDT.poll()) != null) {
                    fireEvents(inEDTListeners, event);

                    // DataSet changed - fire consolidated event early
                    if (consolidatedEvent != null && dataSet != event.getDataset()) {
                        fireConsolidatedEvents(inEDTListeners, consolidatedEvent);
                        consolidatedEvent = null;
                    }

                    dataSet = event.getDataset();

                    // Build consolidated event
                    if (event instanceof DataChangedEvent) {
                        // DataChangeEvent can contains other events, so it gets special handling
                        DataChangedEvent dataEvent = (DataChangedEvent) event;
                        if (dataEvent.getEvents() == null) {
                            consolidatedEvent = dataEvent; // Dataset was completely changed, we can ignore older events
                        } else {
                            if (consolidatedEvent == null) {
                                consolidatedEvent = new DataChangedEvent(dataSet, dataEvent.getEvents());
                            } else if (consolidatedEvent instanceof DataChangedEvent) {
                                List<AbstractDatasetChangedEvent> evts = ((DataChangedEvent) consolidatedEvent).getEvents();
                                if (evts != null) {
                                    evts.addAll(dataEvent.getEvents());
                                }
                            } else {
                                AbstractDatasetChangedEvent oldConsolidateEvent = consolidatedEvent;
                                consolidatedEvent = new DataChangedEvent(dataSet, dataEvent.getEvents());
                                ((DataChangedEvent) consolidatedEvent).getEvents().add(oldConsolidateEvent);
                            }
                        }
                    } else {
                        // Normal events
                        if (consolidatedEvent == null) {
                            consolidatedEvent = event;
                        } else if (consolidatedEvent instanceof DataChangedEvent) {
                            List<AbstractDatasetChangedEvent> evs = ((DataChangedEvent) consolidatedEvent).getEvents();
                            if (evs != null) {
                                evs.add(event);
                            }
                        } else {
                            consolidatedEvent = new DataChangedEvent(dataSet,
                                    new ArrayList<AbstractDatasetChangedEvent>(Arrays.asList(consolidatedEvent)));
                        }

                    }
                }

                // Fire consolidated event
                fireConsolidatedEvents(inEDTListeners, consolidatedEvent);
            }
        }
    };
}
