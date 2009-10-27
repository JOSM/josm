// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.List;
import java.util.concurrent.Future;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;


public interface DownloadTask {
    /**
     * Execute the download using the given bounding box. Set silent on progressMonitor
     * if no error messages should be popped up.
     */
    Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor);

    /**
     * Execute the download using the given URL
     * @param newLayer
     * @param url
     */
    Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor);

    /**
     * Replies the error objects of the task. Empty list, if no error messages are available.
     * 
     * Error objects are either {@see String}s with error messages or {@see Exception}s.
     *
     * WARNING: Never call this in the same thread you requested the download() or it will cause a
     * dead lock. See actions/downloadTasks/DownloadOsmTaskList.java for a proper implementation.
     *
     * @return the list of error objects
     */
    List<Object> getErrorObjects();

    public void cancel();
}
