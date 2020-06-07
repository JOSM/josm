// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

/**
 * This is an abstract task for uploading or saving a data layer.
 * @since 2025
 */
public abstract class AbstractIOTask implements Runnable {

    /** indicates whether the task has been canceled */
    private boolean canceled;
    /** indicates whether the task has been failed */
    private boolean failed;
    /** the last exception caught */
    private Exception lastException;

    /**
     * Constructs a new {@code AbstractIOTask}.
     */
    protected AbstractIOTask() {
        canceled = false;
        failed = false;
        lastException = null;
    }

    /**
     * Replies true if the task has been canceled
     *
     * @return true if the task has been canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Set whether this task has been canceled
     *
     * @param canceled true, if the task has been canceled; false otherwise
     */
    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
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
     * canceled and didn't fail
     *
     * @return true if this  task was successful
     */
    public boolean isSuccessful() {
        return !isCanceled() && !isFailed();
    }

    /**
     * Cancel the task
     */
    public abstract void cancel();
}
