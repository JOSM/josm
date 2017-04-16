// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;

/**
 * Similar like {@link DatasetEventManager}, just for selection events. Because currently selection changed
 * event are global, only FIRE_IN_EDT and FIRE_EDT_CONSOLIDATED modes are really useful
 * @since 2912
 */
public class SelectionEventManager implements SelectionChangedListener {

    private static final SelectionEventManager instance = new SelectionEventManager();

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static SelectionEventManager getInstance() {
        return instance;
    }

    private static class ListenerInfo {
        private final SelectionChangedListener listener;

        ListenerInfo(SelectionChangedListener listener) {
            this.listener = listener;
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

    private Collection<? extends OsmPrimitive> selection;
    private final CopyOnWriteArrayList<ListenerInfo> inEDTListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ListenerInfo> normalListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code SelectionEventManager}.
     */
    public SelectionEventManager() {
        DataSet.addSelectionListener(this);
    }

    /**
     * Registers a new {@code SelectionChangedListener}.
     * @param listener listener to add
     * @param fireMode EDT firing mode
     */
    public void addSelectionListener(SelectionChangedListener listener, FireMode fireMode) {
        if (fireMode == FireMode.IN_EDT)
            throw new UnsupportedOperationException("IN_EDT mode not supported, you probably want to use IN_EDT_CONSOLIDATED.");
        if (fireMode == FireMode.IN_EDT || fireMode == FireMode.IN_EDT_CONSOLIDATED) {
            inEDTListeners.addIfAbsent(new ListenerInfo(listener));
        } else {
            normalListeners.addIfAbsent(new ListenerInfo(listener));
        }
    }

    /**
     * Unregisters a {@code SelectionChangedListener}.
     * @param listener listener to remove
     */
    public void removeSelectionListener(SelectionChangedListener listener) {
        ListenerInfo searchListener = new ListenerInfo(listener);
        inEDTListeners.remove(searchListener);
        normalListeners.remove(searchListener);
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        fireEvents(normalListeners, newSelection);
        selection = newSelection;
        SwingUtilities.invokeLater(edtRunnable);
    }

    private static void fireEvents(List<ListenerInfo> listeners, Collection<? extends OsmPrimitive> newSelection) {
        for (ListenerInfo listener: listeners) {
            listener.listener.selectionChanged(newSelection);
        }
    }

    private final Runnable edtRunnable = () -> {
        if (selection != null) {
            fireEvents(inEDTListeners, selection);
        }
    };
}
