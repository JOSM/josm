// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Interface defining a general download task used to download geographic data (OSM data, GPX tracks, etc.) for a given URL or geographic area.
 */
public interface DownloadTask {

    /**
     * Asynchronously launches the download task for a given bounding box.
     *
     * Set <code>progressMonitor</code> to null, if the task should create, open, and close a progress monitor.
     * Set progressMonitor to {@link NullProgressMonitor#INSTANCE} if progress information is to
     * be discarded.
     *
     * You can wait for the asynchronous download task to finish by synchronizing on the returned
     * {@link Future}, but make sure not to freeze up JOSM. Example:
     * <pre>
     *    Future&lt;?&gt; future = task.download(...);
     *    // DON'T run this on the Swing EDT or JOSM will freeze
     *    future.get(); // waits for the dowload task to complete
     * </pre>
     *
     * The following example uses a pattern which is better suited if a task is launched from
     * the Swing EDT:
     * <pre>
     *    final Future&lt;?&gt; future = task.download(...);
     *    Runnable runAfterTask = new Runnable() {
     *       public void run() {
     *           // this is not strictly necessary because of the type of executor service
     *           // Main.worker is initialized with, but it doesn't harm either
     *           //
     *           future.get(); // wait for the download task to complete
     *           doSomethingAfterTheTaskCompleted();
     *       }
     *    }
     *    MainApplication.worker.submit(runAfterTask);
     * </pre>
     *
     * @param newLayer true, if the data is to be downloaded into a new layer. If false, the task
     * selects one of the existing layers as download layer, preferably the active layer.
     *
     * @param downloadArea the area to download
     * @param progressMonitor the progressMonitor
     * @return the future representing the asynchronous task
     */
    Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor);

    /**
     * Asynchronously launches the download task for a given bounding URL.
     *
     * Set progressMonitor to null, if the task should create, open, and close a progress monitor.
     * Set progressMonitor to {@link NullProgressMonitor#INSTANCE} if progress information is to
     * be discarded.

     * @param newLayer newLayer true, if the data is to be downloaded into a new layer. If false, the task
     * selects one of the existing layers as download layer, preferably the active layer.
     * @param url the url to download from
     * @param progressMonitor the progressMonitor
     * @return the future representing the asynchronous task
     *
     * @see #download(boolean, Bounds, ProgressMonitor)
     */
    Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor);

    /**
     * Returns true if the task is able to open the given URL, false otherwise.
     * @param url the url to download from
     * @param isRemotecontrol True if download request comes from remotecontrol.
     * @return True if the task is able to open the given URL, false otherwise.
     * Return false, if the request comes from remotecontrol, but the task is not
     * safe for remotecontrol.
     * A task is not safe for remotecontrol if it is possible to prepare a file
     * for download which does something unintended, e.g. gain access to the
     * local file system.
     */
    boolean acceptsUrl(String url, boolean isRemotecontrol);

    /**
     * Returns a short HTML documentation string, describing acceptable URLs.
     * @return The HTML documentation
     * @since 6031
     */
    String acceptsDocumentationSummary();

    /**
     * Returns human-readable description of the task
     * @return The task description
     * @since 6031
     */
    String getTitle();

    /**
     * Returns regular expressions that match the URLs
     * @return The array of accepted URL patterns
     * @since 6031
     */
    String[] getPatterns();

    /**
     * Replies the error objects of the task. Empty list, if no error messages are available.
     *
     * Error objects are either {@link String}s with error messages or {@link Exception}s.
     *
     * @return the list of error objects
     */
    List<Object> getErrorObjects();

    /**
     * Cancels the asynchronous download task.
     *
     */
    void cancel();

    /**
     * Replies the HTML-formatted confirmation message to be shown to user when the given URL needs to be confirmed before loading.
     * @param url The URL to be confirmed
     * @return The HTML-formatted confirmation message to be shown to user
     * @since 5691
     */
    String getConfirmationMessage(URL url);
}
