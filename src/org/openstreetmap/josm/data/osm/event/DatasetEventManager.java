// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;

/**
 * This class allows to add DatasetListener to currently active dataset. If active
 * layer is changed, listeners are automatically registered at new active dataset
 * (it's no longer necessary to register for layer events and reregister every time
 * new layer is selected)
 *
 * Events in EDT are supported, see {@link #addDatasetListener(DataSetListener, FireMode)}
 *
 */
public class DatasetEventManager implements ActiveLayerChangeListener, Listener {

    private static final DatasetEventManager INSTANCE = new DatasetEventManager();

    private final class EdtRunnable implements Runnable {
        @Override
        public void run() {
            while (!eventsInEDT.isEmpty()) {
                DataSet dataSet = null;
                AbstractDatasetChangedEvent consolidatedEvent = null;
                AbstractDatasetChangedEvent event;

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
                            consolidatedEvent = new DataChangedEvent(dataSet, new ArrayList<>(Arrays.asList(consolidatedEvent)));
                        }
                    }
                }

                // Fire consolidated event
                if (consolidatedEvent != null) {
                    fireConsolidatedEvents(inEDTListeners, consolidatedEvent);
                }
            }
        }
    }

    /**
     * Event firing mode regarding Event Dispatch Thread.
     */
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
         * Fire in event dispatch thread. If more than one event arrived when event queue is checked, merged them to one event
         */
        IN_EDT_CONSOLIDATED
    }

    private static class ListenerInfo {
        private final DataSetListener listener;
        private final boolean consolidate;

        ListenerInfo(DataSetListener listener, boolean consolidate) {
            this.listener = listener;
            this.consolidate = consolidate;
        }

        @Override
        public int hashCode() {
            return Objects.hash(listener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerInfo that = (ListenerInfo) o;
            return Objects.equals(listener, that.listener);
        }
    }

    /**
     * Replies the unique instance.
     * @return the unique instance
     */
    public static DatasetEventManager getInstance() {
        return INSTANCE;
    }

    private final Queue<AbstractDatasetChangedEvent> eventsInEDT = new LinkedBlockingQueue<>();
    private final CopyOnWriteArrayList<ListenerInfo> inEDTListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ListenerInfo> normalListeners = new CopyOnWriteArrayList<>();
    private final DataSetListener myListener = new DataSetListenerAdapter(this);
    private final Runnable edtRunnable = new EdtRunnable();

    /**
     * Constructs a new {@code DatasetEventManager}.
     */
    public DatasetEventManager() {
        Main.getLayerManager().addActiveLayerChangeListener(this);
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

    /**
     * Unregister listener.
     * @param listener listener to remove
     */
    public void removeDatasetListener(DataSetListener listener) {
        ListenerInfo searchListener = new ListenerInfo(listener, false);
        inEDTListeners.remove(searchListener);
        normalListeners.remove(searchListener);
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        DataSet oldData = e.getPreviousEditDataSet();
        if (oldData != null) {
            oldData.removeDataSetListener(myListener);
        }

        DataSet newData = e.getSource().getEditDataSet();
        if (newData != null) {
            newData.addDataSetListener(myListener);
        }
        processDatasetEvent(new DataChangedEvent(newData));
    }

    private static void fireEvents(List<ListenerInfo> listeners, AbstractDatasetChangedEvent event) {
        for (ListenerInfo listener: listeners) {
            if (!listener.consolidate) {
                event.fire(listener.listener);
            }
        }
    }

    private static void fireConsolidatedEvents(List<ListenerInfo> listeners, AbstractDatasetChangedEvent event) {
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
}
