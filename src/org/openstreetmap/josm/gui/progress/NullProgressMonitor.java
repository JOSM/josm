// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

public class NullProgressMonitor implements ProgressMonitor {

    public static final ProgressMonitor INSTANCE = new NullProgressMonitor();

    private NullProgressMonitor() {

    }

    public void addCancelListener(CancelListener listener) {
    }

    public void beginTask(String title) {
    }

    public void beginTask(String title, int ticks) {
    }

    public void cancel() {
    }

    public ProgressMonitor createSubTaskMonitor(int ticks, boolean internal) {
        return INSTANCE;
    }

    public void finishTask() {
    }

    public String getErrorMessage() {
        return null;
    }

    public int getTicks() {
        return 0;
    }

    public void indeterminateSubTask(String title) {
    }

    public void invalidate() {
    }

    public boolean isCancelled() {
        return false;
    }

    public void removeCancelListener(CancelListener listener) {
    }

    public void setCustomText(String text) {
    }

    public void setErrorMessage(String message) {
    }

    public void setExtraText(String text) {
    }

    public void appendLogMessage(String message) {
    }

    public void setSilent(boolean value) {
    }

    public void setTicks(int ticks) {
    }

    public void setTicksCount(int ticks) {
    }

    public void subTask(String title) {
    }

    public void worked(int ticks) {
    }

    public int getTicksCount() {
        return 0;
    }
}
