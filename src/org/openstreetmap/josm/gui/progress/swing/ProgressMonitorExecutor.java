// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress.swing;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

}
