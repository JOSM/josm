// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress.swing;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Executor that displays the progress monitor to the user.
 *
 * Similar to Executors.newSingleThreadExecutor(), but displays the
 * progress monitor whenever a new task is executed.
 * @since 12675 (moved from {@code gui.progress} package}
 */
public class ProgressMonitorExecutor extends ThreadPoolExecutor {

    /**
     * Creates a new {@code ProgressMonitorExecutor}
     * @param nameFormat see {@link Utils#newThreadFactory(String, int)}
     * @param threadPriority see {@link Utils#newThreadFactory(String, int)}
     */
    public ProgressMonitorExecutor(final String nameFormat, final int threadPriority) {
        super(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                Utils.newThreadFactory(nameFormat, threadPriority));
    }

    @Override
    public void execute(Runnable command) {
        if (PleaseWaitProgressMonitor.currentProgressMonitor != null) {
            //TODO show only if this can't be in background or better if always in background is not checked
            PleaseWaitProgressMonitor.currentProgressMonitor.showForegroundDialog();
        }
        super.execute(command);
    }

    @Override
    public void afterExecute(final Runnable r, Throwable t) {
        // largely as proposed by JDK8 docs
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            try {
                ((Future<?>) r).get();
            } catch (CancellationException cancellationException) {
                t = cancellationException;
            } catch (ExecutionException executionException) {
                Logging.trace(executionException);
                t = executionException.getCause();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        if (t != null) {
            Logging.error("Thread {0} raised {1}", Thread.currentThread().getName(), t);
        }
    }
}
