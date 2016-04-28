// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Locale;

import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;

final class StreamProgressUpdater {

    private final long size;
    private final ProgressMonitor progressMonitor;
    private final String taskTitle;
    private int soFar;
    private int lastDialogUpdate;

    StreamProgressUpdater(long size, ProgressMonitor progressMonitor, String taskTitle) {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        this.size = size;
        this.progressMonitor = progressMonitor;
        this.taskTitle = taskTitle;
        initProgressMonitor();
    }

    private void initProgressMonitor() {
        if (size > 0) {
            progressMonitor.subTask(taskTitle);
            progressMonitor.setTicksCount((int) size);
        } else {
            progressMonitor.indeterminateSubTask(taskTitle);
        }
    }

    /**
     * Increase ticker (progress counter and displayed text) by the given amount.
     *
     * @param amount number of ticks
     */
    void advanceTicker(int amount) {
        soFar += amount;

        if (soFar / 1024 != lastDialogUpdate) {
            lastDialogUpdate++;
            if (size > 0) {
                progressMonitor.setTicks(soFar);
            }
            progressMonitor.setExtraText(Utils.getSizeString(soFar, Locale.getDefault()));
        }
    }

    void finishTask() {
        progressMonitor.finishTask();
    }
}
