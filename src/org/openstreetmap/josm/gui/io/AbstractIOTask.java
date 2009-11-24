// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

/**
 * This is an abstract task for uploading or saving a data layer.
 *
 */
public abstract class AbstractIOTask implements Runnable {

    /** indicates whether the task has been cancelled */
    private boolean cancelled;
    /** indicates whether the task has been failed */
    private boolean failed;
    /** the last exception caught */
    private Exception lastException;

    public AbstractIOTask() {
        cancelled = false;
        failed = false;
        lastException = null;
    }

    /**
     * Replies true if the task has been cancelled
     *
     * @return true if the task has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Set whether this task has been cancelled
     *
     * @param cancelled true, if the task has been cancelled; false otherwise
     */
    protected void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Replies true if the task has been failed
     *
     * @return true if the task has been failed
     */
    public boolean isFailed() {
        return failed || lastException != null;
    }

    /**
     * Sets whether the task has been failed
     *
     * @param failed whether the task has been failed
     */
    protected void setFailed(boolean failed) {
        this.failed = failed;
    }

    /**
     * Replies the last exception caught
     *
     * @return the last exception caught; null, if no exception was caught
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * Sets the last exception caught
     *
     * @param lastException the last exception
     */
    protected void setLastException(Exception lastException) {
        this.lastException = lastException;
    }

    /**
     * Replies true if this  task was successful, i.e. if it wasn't
     * cancelled and didn't fail
     *
     * @return true if this  task was successful
     */
    public boolean isSuccessful() {
        return !isCancelled() && !isFailed();
    }

    /**
     * Runs the task
     */
    public abstract void run();

    /**
     * Cancel the task
     */
    public abstract void cancel();
}
