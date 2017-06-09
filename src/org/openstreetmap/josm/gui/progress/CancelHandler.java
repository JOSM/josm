// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.gui.progress.ProgressMonitor.CancelListener;

/**
 * A handler that notifies all listeners that a given operation was canceled.
 */
public class CancelHandler {

    private boolean isCanceled;
    private final List<CancelListener> listeners = new ArrayList<>();

    /**
     * Cancels the operation. This call is ignored if the operation was already canceled.
     */
    public synchronized void cancel() {
        if (!isCanceled) {
            isCanceled = true;
            for (CancelListener listener:listeners) {
                listener.operationCanceled();
            }
        }
    }

    /**
     * Checks if the operation was canceled
     * @return <code>true</code> if {@link #cancel()} was called.
     */
    public synchronized boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Adds a new cancel listener
     * @param listener The listener to add
     */
    public synchronized void addCancelListener(CancelListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a cancel listener
     * @param listener The listener to remove
     */
    public synchronized void removeCancelListener(CancelListener listener) {
        listeners.remove(listener);
    }

}
