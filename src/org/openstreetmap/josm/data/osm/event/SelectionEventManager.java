// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

/**
 * Similar like {@link DatasetEventManager}, just for selection events.
 *
 * It allows to register listeners to global selection events for the selection in the current edit layer.
 *
 * If you want to listen to selections to a specific data layer,
 * you can register a listener to that layer by using {@link DataSet#addSelectionListener(DataSelectionListener)}
 *
 * @since 2912
 */
public class SelectionEventManager implements DataSelectionListener, ActiveLayerChangeListener {

    private static final SelectionEventManager INSTANCE = new SelectionEventManager();

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static SelectionEventManager getInstance() {
        return INSTANCE;
    }

    private interface ListenerInfo {
        void fire(SelectionChangeEvent event);
    }

    private static class OldListenerInfo implements ListenerInfo {
        private final SelectionChangedListener listener;

        OldListenerInfo(SelectionChangedListener listener) {
            this.listener = listener;
        }

        @Override
        public void fire(SelectionChangeEvent event) {
            listener.selectionChanged(event.getSelection());
        }

        @Override
        public int hashCode() {
            return Objects.hash(listener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OldListenerInfo that = (OldListenerInfo) o;
            return Objects.equals(listener, that.listener);
        }

        @Override
        public String toString() {
            return "OldListenerInfo [listener=" + listener + ']';
        }
    }

    private static class DataListenerInfo implements ListenerInfo {
        private final DataSelectionListener listener;

        DataListenerInfo(DataSelectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void fire(SelectionChangeEvent event) {
            listener.selectionChanged(event);
        }

        @Override
        public int hashCode() {
            return Objects.hash(listener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataListenerInfo that = (DataListenerInfo) o;
            return Objects.equals(listener, that.listener);
        }

        @Override
        public String toString() {
            return "DataListenerInfo [listener=" + listener + ']';
        }
    }

    private final CopyOnWriteArrayList<ListenerInfo> inEDTListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ListenerInfo> immedatelyListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code SelectionEventManager}.
     */
    protected SelectionEventManager() {
        MainLayerManager layerManager = MainApplication.getLayerManager();
        // We do not allow for destructing this object.
        // Currently, this is a singleton class, so this is not required.
        layerManager.addAndFireActiveLayerChangeListener(this);
    }

    /**
     * Registers a new {@code SelectionChangedListener}.
     *
     * It is preferred to add a DataSelectionListener - that listener will receive more information about the event.
     * @param listener listener to add
     * @param fireMode Set this to IN_EDT_CONSOLIDATED if you want the event to be fired in the EDT thread.
     *                 Set it to IMMEDIATELY if you want the event to fire in the thread that caused the selection update.
     */
    public void addSelectionListener(SelectionChangedListener listener, FireMode fireMode) {
        if (fireMode == FireMode.IN_EDT) {
            throw new UnsupportedOperationException("IN_EDT mode not supported, you probably want to use IN_EDT_CONSOLIDATED.");
        } else if (fireMode == FireMode.IN_EDT_CONSOLIDATED) {
            inEDTListeners.addIfAbsent(new OldListenerInfo(listener));
        } else {
            immedatelyListeners.addIfAbsent(new OldListenerInfo(listener));
        }
    }

    /**
     * Adds a selection listener that gets notified for selections immediately.
     * @param listener The listener to add.
     * @since 12098
     */
    public void addSelectionListener(DataSelectionListener listener) {
        immedatelyListeners.addIfAbsent(new DataListenerInfo(listener));
    }

    /**
     * Adds a selection listener that gets notified for selections later in the EDT thread.
     * Events are sent in the right order but may be delayed.
     * @param listener The listener to add.
     * @since 12098
     */
    public void addSelectionListenerForEdt(DataSelectionListener listener) {
        inEDTListeners.addIfAbsent(new DataListenerInfo(listener));
    }

    /**
     * Unregisters a {@code SelectionChangedListener}.
     * @param listener listener to remove
     */
    public void removeSelectionListener(SelectionChangedListener listener) {
        remove(new OldListenerInfo(listener));
    }

    /**
     * Unregisters a {@code DataSelectionListener}.
     * @param listener listener to remove
     * @since 12098
     */
    public void removeSelectionListener(DataSelectionListener listener) {
        remove(new DataListenerInfo(listener));
    }

    private void remove(ListenerInfo searchListener) {
        inEDTListeners.remove(searchListener);
        immedatelyListeners.remove(searchListener);
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        DataSet oldDataSet = e.getPreviousDataSet();
        if (oldDataSet != null) {
            // Fake a selection removal
            // Relying on this allows components to not have to monitor layer changes.
            // If we would not do this, e.g. the move command would have a hard time tracking which layer
            // the last moved selection was in.
            selectionChanged(new SelectionReplaceEvent(oldDataSet,
                    new HashSet<>(oldDataSet.getAllSelected()), Stream.empty()));
            oldDataSet.removeSelectionListener(this);
        }
        DataSet newDataSet = e.getSource().getActiveDataSet();
        if (newDataSet != null) {
            newDataSet.addSelectionListener(this);
            // Fake a selection add
            selectionChanged(new SelectionReplaceEvent(newDataSet,
                    Collections.emptySet(), newDataSet.getAllSelected().stream()));
        }
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        fireEvent(immedatelyListeners, event);
        try {
            GuiHelper.runInEDTAndWaitWithException(() -> fireEvent(inEDTListeners, event));
        } catch (ReportedException e) {
            throw BugReport.intercept(e).put("event", event).put("inEDTListeners", inEDTListeners);
        }
    }

    private static void fireEvent(List<ListenerInfo> listeners, SelectionChangeEvent event) {
        for (ListenerInfo listener: listeners) {
            try {
                listener.fire(event);
            } catch (DataIntegrityProblemException e) {
                throw BugReport.intercept(e).put("event", event).put("listeners", listeners);
            }
        }
    }

    /**
     * Only to be used during unit tests, to reset the state. Do not use it in plugins/other code.
     * Called after the layer manager was reset by the test framework.
     */
    public void resetState() {
        inEDTListeners.clear();
        immedatelyListeners.clear();
        MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(this);
    }
}
