// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;

/**
 * Typical use case is:
 * <pre>
 *   monitor.beginTask()
 *   try {
 *     .. do some work
 *     monitor.worked()
 *     monitor.subTask()/monitor.intermediateTask()
 *     .. do some work
 *     monitor.worked()
 *   } finally {
 *     monitor.finishTask();
 *   }
 * </pre>
 *
 * {@link #subTask(String)} and {@link #indeterminateSubTask(String)} has nothing to do with logical
 * structure of the work, they just show task title to the user.
 *
 * If task consists of multiple tasks then {@link #createSubTaskMonitor(int, boolean)} may be used. It
 * will create new ProgressMonitor, then can be passed to the subtask. Subtask doesn't know whether
 * it runs standalone or as a part of other task. Progressbar will be updated so that total progress is
 * shown, not just progress of the subtask
 *
 * All ProgressMonitor implementations should be thread safe.
 *
 */
public interface ProgressMonitor {

    public interface CancelListener {
        void operationCanceled();
    }

    /** Ticks count used, when no other value is supplied */
    public final int DEFAULT_TICKS = 10000;

    /**
     * Can be used with {@link #worked(int)} and {@link #createSubTaskMonitor(int, boolean)} to
     * express that the task should use all remaining ticks
     */
    public final int ALL_TICKS = -1;

    /**
     * Starts this progress monitor. Must be called exactly once
     * Ticks count is set to default value
     * @param title title text of the task
     */
    void beginTask(String title);

    /**
     * Starts this progress monitor. Must be called exactly once
     * @param title title text of the task
     * @param ticks number of work units (see {@link #setTicksCount(int ticks)})
     */
    void beginTask(String title, int ticks);

    /**
     * Finish this progress monitor, close the dialog or inform the parent progress monitor
     * that it can continue with other tasks. Must be called at least once (if called multiply times
     * then further calls are ignored)
     */
    void finishTask();

    /**
     * Can be used if method receive ProgressMonitor but it's not interested progress monitoring.
     * Basically replaces {@link #beginTask(String)} and {@link #finishTask()}
     *
     * This method can be also used in finally section if method expects that some exception
     * might prevent it from passing progressMonitor away. If {@link #beginTask(String)} was
     * already called then this method does nothing.
     */
    void invalidate();

    /**
     * Set the total number of work units
     * @param ticks Number of total work units
     */
    void setTicksCount(int ticks);

    /**
     * Get the total number of work units
     * @return Number of total work units
     */
    int getTicksCount();

    /**
     * Set the current number of work units
     * @param ticks Number of work units already done
     */
    void setTicks(int ticks);

    /**
     * Get the current number of work units
     * @return Number of work units already done
     */
    int getTicks();

    /**
     * Increase number of already done work units by ticks
     * @param ticks number of ticks to add
     */
    void worked(int ticks);

    /**
     * Subtask that will show progress running back and forth
     * @param title Can be {@code null}, in that case task title is not changed
     */
    void indeterminateSubTask(String title);

    /**
     * Normal subtask
     * @param title Can be {@code null}, in that case task title is not changed
     */

    void subTask(String title);
    /**
     * Shows additional text
     */

    void setCustomText(String text);
    /**
     * Show extra text after normal task title. Hack for ProgressInputStream to show number of kB
     * already downloaded
     * @param text
     */
    void setExtraText(String text);

    /**
     * Creates subtasks monitor.
     * @param ticks Number of work units that should be done when subtask finishes
     * @param internal If true then subtask can't modify task title/custom text
     */
    ProgressMonitor createSubTaskMonitor(int ticks, boolean internal);

    /**
     * Returns the state of user aborts
     * @return {@code true} if user aborted operation
     */
    boolean isCanceled();

    /**
     * Abort current operation, usually called when user somehow requested an abort
     */
    void cancel();

    /**
     * Add listener for user abort action
     * @param listener the listener for cancel operation
     */
    void addCancelListener(CancelListener listener);

    /**
     * Remove listener for user abort action
     * @param listener the listener for cancel operation
     */
    void removeCancelListener(CancelListener listener);

    /**
     * Appends a message to the log managed by the progress monitor.
     *
     * @param message the log message. Ignored if null or white space only.
     */
    void appendLogMessage(String message);

    /**
     * Set the task ID of the progress dialog
     * Should be used only by PleaseWaitRunnable. If taskId {@code <> null} then "In background" button will be shown
     * @param taskId the task ID
     */
    void setProgressTaskId(ProgressTaskId taskId);

    /**
     * Returns the task ID of the progress dialog
     * Should be used only by PleaseWaitRunnable
     * @return the task ID
     */
    ProgressTaskId getProgressTaskId();

    /**
     * Return the parent windows of progress dialog
     * @return component suitable as parent for dialogs that wants to be shown in front of progress dialog
     */
    Component getWindowParent();
}
