// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.gui.progress.ProgressMonitor.CancelListener;

public class CancelHandler {

    private boolean isCanceled;
    private List<CancelListener> listeners = new ArrayList<CancelListener>();

    public synchronized void cancel() {
        if (!isCanceled) {
            isCanceled = true;
            for (CancelListener listener:listeners) {
                listener.operationCanceled();
            }
        }
    }

    public synchronized boolean isCanceled() {
        return isCanceled;
    }

    public synchronized void addCancelListener(CancelListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeCancelListener(CancelListener listener) {
        listeners.remove(listener);
    }

}
