// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.awt.Component;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerChangesetReader;

/**
 * Common abstract implementation of other changeset download tasks.
 * @since 10124
 */
public abstract class AbstractChangesetDownloadTask extends AbstractDownloadTask<Set<Changeset>> {

    abstract class RunnableDownloadTask extends PleaseWaitRunnable {
        /** the reader object used to read changesets from the API */
        protected final OsmServerChangesetReader reader = new OsmServerChangesetReader();
        /** the set of downloaded changesets */
        protected final Set<Changeset> downloadedChangesets = new HashSet<>();
        /** keeps the last exception thrown in the task, if any */
        protected Exception lastException;

        RunnableDownloadTask(Component parent, String title) {
            super(parent, title, false /* don't ignore exceptions */);
        }

        @Override
        protected void cancel() {
            setCanceled(true);
            synchronized (this) {
                if (reader != null) {
                    reader.cancel();
                }
            }
        }

        protected final void rememberLastException(Exception e) {
            lastException = e;
            setFailed(true);
        }
    }

    private RunnableDownloadTask downloadTaskRunnable;

    protected final void setDownloadTask(RunnableDownloadTask downloadTask) {
        this.downloadTaskRunnable = downloadTask;
    }

    @Override
    public final Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return download();
    }

    /**
     * Asynchronously launches the changeset download task. This is equivalent to {@code download(false, null, null)}.
     *
     * You can wait for the asynchronous download task to finish by synchronizing on the returned
     * {@link Future}, but make sure not to freeze up JOSM. Example:
     * <pre>
     *    Future&lt;?&gt; future = task.download();
     *    // DON'T run this on the Swing EDT or JOSM will freeze
     *    future.get(); // waits for the dowload task to complete
     * </pre>
     *
     * The following example uses a pattern which is better suited if a task is launched from the Swing EDT:
     * <pre>
     *    final Future&lt;?&gt; future = task.download();
     *    Runnable runAfterTask = new Runnable() {
     *       public void run() {
     *           // this is not strictly necessary because of the type of executor service
     *           // Main.worker is initialized with, but it doesn't harm either
     *           //
     *           future.get(); // wait for the download task to complete
     *           doSomethingAfterTheTaskCompleted();
     *       }
     *    }
     *    Main.worker.submit(runAfterTask);
     * </pre>
     *
     * @return the future representing the asynchronous task
     */
    public final Future<?> download() {
        return Main.worker.submit(downloadTaskRunnable);
    }

    @Override
    public final Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        return Main.worker.submit(downloadTaskRunnable);
    }

    @Override
    public final void cancel() {
        downloadTaskRunnable.cancel();
    }

    @Override
    public String getConfirmationMessage(URL url) {
        return null;
    }
}
