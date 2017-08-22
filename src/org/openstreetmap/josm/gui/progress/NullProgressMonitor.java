// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Logging;

/**
 * A singleton progress monitor that does nothing.
 * @since 1811
 */
public final class NullProgressMonitor implements ProgressMonitor {

    /** The unique instance */
    public static final ProgressMonitor INSTANCE = new NullProgressMonitor();

    private NullProgressMonitor() {
        // Do nothing
    }

    @Override
    public void addCancelListener(CancelListener listener) {
        // Do nothing
    }

    @Override
    public void beginTask(String title) {
        Logging.debug(title);
    }

    @Override
    public void beginTask(String title, int ticks) {
        Logging.debug(title);
    }

    @Override
    public void cancel() {
        // Do nothing
    }

    @Override
    public ProgressMonitor createSubTaskMonitor(int ticks, boolean internal) {
        return INSTANCE;
    }

    @Override
    public void finishTask() {
        // Do nothing
    }

    @Override
    public int getTicks() {
        return 0;
    }

    @Override
    public void indeterminateSubTask(String title) {
        Logging.debug(title);
    }

    @Override
    public void invalidate() {
        // Do nothing
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void removeCancelListener(CancelListener listener) {
        // Do nothing
    }

    @Override
    public void setCustomText(String text) {
        // Do nothing
    }

    @Override
    public void setExtraText(String text) {
        // Do nothing
    }

    @Override
    public void appendLogMessage(String message) {
        // Do nothing
    }

    @Override
    public void setTicks(int ticks) {
        // Do nothing
    }

    @Override
    public void setTicksCount(int ticks) {
        // Do nothing
    }

    @Override
    public void subTask(String title) {
        Logging.debug(title);
    }

    @Override
    public void worked(int ticks) {
        // Do nothing
    }

    @Override
    public int getTicksCount() {
        return 0;
    }

    @Override
    public void setProgressTaskId(ProgressTaskId taskId) {
        // Do nothing
    }

    @Override
    public ProgressTaskId getProgressTaskId() {
        return null;
    }

    @Override
    public Component getWindowParent() {
        return Main.parent;
    }
}
