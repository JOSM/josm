// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * This is a list of listeners. It does error checking and allows you to fire all listeners.
 *
 * @author Michael Zangl
 * @param <T> The type of listener contained in this list.
 * @since 10824
 */
public class ListenerList<T> {
    /**
     * This is a function that can be invoked for every listener.
     * @param <T> the listener type.
     */
    @FunctionalInterface
    public interface EventFirerer<T> {
        /**
         * Should fire the event for the given listener.
         * @param listener The listener to fire the event for.
         */
        void fire(T listener);
    }

    private static final class WeakListener<T> {

        private final WeakReference<T> listener;

        WeakListener(T listener) {
            this.listener = new WeakReference<>(listener);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj.getClass() == WeakListener.class) {
                return Objects.equals(listener.get(), ((WeakListener<?>) obj).listener.get());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            T l = listener.get();
            if (l == null) {
                return 0;
            } else {
                return l.hashCode();
            }
        }

        @Override
        public String toString() {
            return "WeakListener [listener=" + listener + ']';
        }
    }

    private final CopyOnWriteArrayList<T> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<WeakListener<T>> weakListeners = new CopyOnWriteArrayList<>();

    protected ListenerList() {
        // hide
    }

    /**
     * Adds a listener. The listener will not prevent the object from being garbage collected.
     *
     * This should be used with care. It is better to add good cleanup code.
     * @param listener The listener.
     */
    public synchronized void addWeakListener(T listener) {
        if (ensureNotInList(listener)) {
            // clean the weak listeners, just to be sure...
            while (weakListeners.remove(new WeakListener<T>(null))) {
                // continue
            }
            weakListeners.add(new WeakListener<>(listener));
        }
    }

    /**
     * Adds a listener.
     * @param listener The listener to add.
     */
    public synchronized void addListener(T listener) {
        if (ensureNotInList(listener)) {
            listeners.add(listener);
        }
    }

    private boolean ensureNotInList(T listener) {
        CheckParameterUtil.ensureParameterNotNull(listener, "listener");
        if (containsListener(listener)) {
            failAdd(listener);
            return false;
        } else {
            return true;
        }
    }

    protected void failAdd(T listener) {
        throw new IllegalArgumentException(
                MessageFormat.format("Listener {0} (instance of {1}) was already registered.", listener,
                        listener.getClass().getName()));
    }

    private boolean containsListener(T listener) {
        return listeners.contains(listener) || weakListeners.contains(new WeakListener<>(listener));
    }

    /**
     * Removes a listener.
     * @param listener The listener to remove.
     * @throws IllegalArgumentException if the listener was not registered before
     */
    public synchronized void removeListener(T listener) {
        if (!listeners.remove(listener) && !weakListeners.remove(new WeakListener<>(listener))) {
            failRemove(listener);
        }
    }

    protected void failRemove(T listener) {
        throw new IllegalArgumentException(
                MessageFormat.format("Listener {0} (instance of {1}) was not registered before or already removed.",
                        listener, listener.getClass().getName()));
    }

    /**
     * Check if any listeners are registered.
     * @return <code>true</code> if any are registered.
     */
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    /**
     * Fires an event to every listener.
     * @param eventFirerer The firerer to invoke the event method of the listener.
     */
    public void fireEvent(EventFirerer<T> eventFirerer) {
        for (T l : listeners) {
            eventFirerer.fire(l);
        }
        for (Iterator<WeakListener<T>> iterator = weakListeners.iterator(); iterator.hasNext();) {
            WeakListener<T> weakLink = iterator.next();
            T l = weakLink.listener.get();
            if (l != null) {
                // cleanup during add() should be enough to not cause memory leaks
                // therefore, we ignore null listeners.
                eventFirerer.fire(l);
            }
        }
    }

    /**
     * This is a special {@link ListenerList} that traces calls to the add/remove methods. This may cause memory leaks.
     * @author Michael Zangl
     *
     * @param <T> The type of listener contained in this list
     */
    public static class TracingListenerList<T> extends ListenerList<T> {
        private final HashMap<T, StackTraceElement[]> listenersAdded = new HashMap<>();
        private final HashMap<T, StackTraceElement[]> listenersRemoved = new HashMap<>();

        protected TracingListenerList() {
            // hidden
        }

        @Override
        public synchronized void addListener(T listener) {
            super.addListener(listener);
            listenersRemoved.remove(listener);
            listenersAdded.put(listener, Thread.currentThread().getStackTrace());
        }

        @Override
        public synchronized void addWeakListener(T listener) {
            super.addWeakListener(listener);
            listenersRemoved.remove(listener);
            listenersAdded.put(listener, Thread.currentThread().getStackTrace());
        }

        @Override
        public synchronized void removeListener(T listener) {
            super.removeListener(listener);
            listenersAdded.remove(listener);
            listenersRemoved.put(listener, Thread.currentThread().getStackTrace());
        }

        @Override
        protected void failAdd(T listener) {
            Logging.trace("Previous addition of the listener");
            dumpStack(listenersAdded.get(listener));
            super.failAdd(listener);
        }

        @Override
        protected void failRemove(T listener) {
            Logging.trace("Previous removal of the listener");
            dumpStack(listenersRemoved.get(listener));
            super.failRemove(listener);
        }

        private static void dumpStack(StackTraceElement... stackTraceElements) {
            if (stackTraceElements == null) {
                Logging.trace("  - (no trace recorded)");
            } else {
                Stream.of(stackTraceElements).limit(20).forEach(
                        e -> Logging.trace(e.getClassName() + "." + e.getMethodName() + " line " + e.getLineNumber()));
            }
        }
    }

    private static class UncheckedListenerList<T> extends ListenerList<T> {
        @Override
        protected void failAdd(T listener) {
            Logging.warn("Listener was alreaady added: {0}", listener);
            // ignore
        }

        @Override
        protected void failRemove(T listener) {
            Logging.warn("Listener was removed twice or not added: {0}", listener);
            // ignore
        }
    }

    /**
     * Create a new listener list
     * @param <T> The listener type the list should hold.
     * @return A new list. A tracing list is created if trace is enabled.
     */
    public static <T> ListenerList<T> create() {
        if (Logging.isTraceEnabled()) {
            return new TracingListenerList<>();
        } else {
            return new ListenerList<>();
        }
    }

    /**
     * Creates a new listener list that does not fail if listeners are added ore removed twice.
     * <p>
     * Use of this list is discouraged. You should always use {@link #create()} in new implementations and check your listeners.
     * @param <T> The listener type
     * @return A new list.
     * @since 11224
     */
    public static <T> ListenerList<T> createUnchecked() {
        return new UncheckedListenerList<>();
    }
}
