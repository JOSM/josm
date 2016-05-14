// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * Replacement to {@code java.util.Observable} class, deprecated with Java 9.
 * @since 10210
 */
public class ChangeNotifier {

    /** Stores the listeners on this model. */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Only one {@code ChangeEvent} is needed per button model
     * instance since the event's only state is the source property.
     * The source of events generated is always "this".
     */
    private ChangeEvent changeEvent;

    /**
     * Adds a {@code ChangeListener}.
     * @param l the listener to add
     */
    public final void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a {@code ChangeListener}.
     * @param l the listener to add
     */
    public final void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type.
     * The event instance is created lazily.
     */
    protected final void fireStateChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length-2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((ChangeListener) listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }
}
