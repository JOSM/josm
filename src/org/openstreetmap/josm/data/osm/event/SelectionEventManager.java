// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;

/**
 * Similar like {@link DatasetEventManager}, just for selection events. Because currently selection changed
 * event are global, only FIRE_IN_EDT and FIRE_EDT_CONSOLIDATED modes are really useful
 *
 */
public class SelectionEventManager implements SelectionChangedListener {

    private static final SelectionEventManager instance = new SelectionEventManager();

    public static SelectionEventManager getInstance() {
        return instance;
    }

    private static class ListenerInfo {
        final SelectionChangedListener listener;

        public ListenerInfo(SelectionChangedListener listener) {
            this.listener = listener;
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

    private Collection<? extends OsmPrimitive> selection;
    private final CopyOnWriteArrayList<ListenerInfo> inEDTListeners = new CopyOnWriteArrayList<ListenerInfo>();
    private final CopyOnWriteArrayList<ListenerInfo> normalListeners = new CopyOnWriteArrayList<ListenerInfo>();

    /**
     * Constructs a new {@code SelectionEventManager}.
     */
    public SelectionEventManager() {
        DataSet.addSelectionListener(this);
    }

    public void addSelectionListener(SelectionChangedListener listener, FireMode fireMode) {
        if (fireMode == FireMode.IN_EDT)
            throw new UnsupportedOperationException("IN_EDT mode not supported, you probably want to use IN_EDT_CONSOLIDATED.");
        if (fireMode == FireMode.IN_EDT || fireMode == FireMode.IN_EDT_CONSOLIDATED) {
            inEDTListeners.addIfAbsent(new ListenerInfo(listener));
        } else {
            normalListeners.addIfAbsent(new ListenerInfo(listener));
        }
    }

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

    private void fireEvents(List<ListenerInfo> listeners, Collection<? extends OsmPrimitive> newSelection) {
        for (ListenerInfo listener: listeners) {
            listener.listener.selectionChanged(newSelection);
        }
    }

    private final Runnable edtRunnable = new Runnable() {
        @Override
        public void run() {
            if (selection != null) {
                fireEvents(inEDTListeners, selection);
            }
        }
    };

}
