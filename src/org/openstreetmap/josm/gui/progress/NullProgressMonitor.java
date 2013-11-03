// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;

import org.openstreetmap.josm.Main;

public final class NullProgressMonitor implements ProgressMonitor {

    public static final ProgressMonitor INSTANCE = new NullProgressMonitor();

    private NullProgressMonitor() {

    }

    @Override
    public void addCancelListener(CancelListener listener) {
    }

    @Override
    public void beginTask(String title) {
    }

    @Override
    public void beginTask(String title, int ticks) {
    }

    @Override
    public void cancel() {
    }

    @Override
    public ProgressMonitor createSubTaskMonitor(int ticks, boolean internal) {
        return INSTANCE;
    }

    @Override
    public void finishTask() {
    }

    public String getErrorMessage() {
        return null;
    }

    @Override
    public int getTicks() {
        return 0;
    }

    @Override
    public void indeterminateSubTask(String title) {
    }

    @Override
    public void invalidate() {
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void removeCancelListener(CancelListener listener) {
    }

    @Override
    public void setCustomText(String text) {
    }

    public void setErrorMessage(String message) {
    }

    @Override
    public void setExtraText(String text) {
    }

    @Override
    public void appendLogMessage(String message) {
    }

    public void setSilent(boolean value) {
    }

    @Override
    public void setTicks(int ticks) {
    }

    @Override
    public void setTicksCount(int ticks) {
    }

    @Override
    public void subTask(String title) {
    }

    @Override
    public void worked(int ticks) {
    }

    @Override
    public int getTicksCount() {
        return 0;
    }

    @Override
    public void setProgressTaskId(ProgressTaskId taskId) {
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
