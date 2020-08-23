// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This is a special weak reference that notifies a listener when it is no longer available.
 *
 * A special dereferenced-thread is used for this, so make sure your code is thread-safe.
 * @author Michael Zangl
 * @param <T> The weak reference
 * @since 12181
 */
public class ListenableWeakReference<T> extends WeakReference<T> {
    private static final ReferenceQueue<Object> GLOBAL_QUEUE = new ReferenceQueue<>();
    private static Thread thread;
    private final Runnable runOnDereference;

    /**
     * Create a new {@link ListenableWeakReference}
     * @param referent The object that is referenced
     */
    public ListenableWeakReference(T referent) {
        this(referent, () -> { });
    }

    /**
     * Create a new {@link ListenableWeakReference}
     * @param referent The object that is referenced
     * @param runOnDereference The runnable to run when the object is no longer referenced.
     */
    public ListenableWeakReference(T referent, Runnable runOnDereference) {
        super(referent, GLOBAL_QUEUE);
        this.runOnDereference = runOnDereference;
        ensureQueueStarted();
    }

    /**
     * This method is called after the object is dereferenced.
     */
    protected void onDereference() {
        this.runOnDereference.run();
    }

    private static synchronized void ensureQueueStarted() {
        if (thread == null) {
            thread = new Thread(ListenableWeakReference::clean, "Weak reference cleaner");
            thread.start();
        }
    }

    private static void clean() {
        boolean running = true;
        try {
            while (running) {
                Reference<? extends Object> ref = GLOBAL_QUEUE.remove();
                if (ref instanceof ListenableWeakReference) {
                    ((ListenableWeakReference<?>) ref).onDereference();
                }
            }
        } catch (InterruptedException e) {
            running = false;
            BugReport.intercept(e).warn();
            Thread.currentThread().interrupt();
        }
    }
}
