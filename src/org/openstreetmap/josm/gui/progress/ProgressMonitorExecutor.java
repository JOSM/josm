// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.Main;

public class ProgressMonitorExecutor extends ThreadPoolExecutor {

    public ProgressMonitorExecutor() {
        super(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    @Override
    public void execute(Runnable command) {
        if (Main.currentProgressMonitor != null) {
            //TODO show only if this can't be in background or better if always in background is not checked
            Main.currentProgressMonitor.showForegroundDialog();
        }
        super.execute(command);
    }

}
