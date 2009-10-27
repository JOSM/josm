// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

/**
 * Typical usecase is:
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
 * it runs standalono or as a part of other task. Progressbar will be updated so that total progress is
 * shown, not just progress of the subtask
 *
 * All ProgressMonitor implemenenatations should be thread safe.
 *
 */
public interface ProgressMonitor {

    public interface CancelListener {
        void operationCanceled();
    }

    public final int DEFAULT_TICKS = 100;

    /**
     * Can be used with {@link #worked(int)} and {@link #createSubTaskMonitor(int, boolean)} to
     * express that the task should use all remaining ticks
     */
    public final int ALL_TICKS = -1;

    void beginTask(String title);

    /**
     * Starts this progress monitor. Must be called excatly once
     * @param title
     * @param ticks
     */
    void beginTask(String title, int ticks);
    /**
     * Finish this progress monitor, close the dialog or inform the parent progress monitor
     * that it can continue with other tasks. Must be called at least once (if called multiply times
     * then futher calls are ignored)
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
     *
     * @param ticks Number of total work units
     */
    void setTicksCount(int ticks);
    /**
     *
     * @param ticks Number of work units already done
     */
    void setTicks(int ticks);

    int getTicks();
    int getTicksCount();

    /**
     * Increase number of already done work units by ticks
     * @param ticks
     */
    void worked(int ticks);

    /**
     * Subtask that will show progress runing back and forworth
     * @param title Can be null, in that case task title is not changed
     */
    void indeterminateSubTask(String title);
    /**
     * Normal subtask
     * @param title Can be null, in that case task title is not changed
     */
    void subTask(String title);
    /**
     * Shows additonal text
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
     * @return
     */
    ProgressMonitor createSubTaskMonitor(int ticks, boolean internal);

    boolean isCancelled();
    void cancel();
    void addCancelListener(CancelListener listener);
    void removeCancelListener(CancelListener listener);


    /**
     * Appends a message to the log managed by the progress monitor.
     * 
     * @param message the log message. Ignored if null or white space only.
     */
    void appendLogMessage(String message);
}
