// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapStatus.BackgroundProgressMonitor;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * A progress monitor used in {@link org.openstreetmap.josm.gui.PleaseWaitRunnable}.
 * <p>
 * Progress is displayed in a dialog window ({@link PleaseWaitDialog}).
 */
public class PleaseWaitProgressMonitor extends AbstractProgressMonitor {

    /**
     * Implemented by both foreground dialog and background progress dialog (in status bar)
     */
    public interface ProgressMonitorDialog {
        /**
         * Sets the visibility of this dialog
         * @param visible The visibility, <code>true</code> to show it, <code>false</code> to hide it
         */
        void setVisible(boolean visible);

        /**
         * Updates the progress value to the specified progress.
         * @param progress The progress as integer. Between 0 and {@link PleaseWaitProgressMonitor#PROGRESS_BAR_MAX}
         */
        void updateProgress(int progress);

        /**
         * Sets the description of what is done
         * @param text The description of the task
         */
        void setCustomText(String text);

        /**
         * Sets the current action that is done
         * @param text The current action
         */
        void setCurrentAction(String text);

        /**
         * Display that the current progress cannot be determined
         * @param newValue wether the progress cannot be determined
         */
        void setIndeterminate(boolean newValue);

        /**
         * Append a message to the progress log
         * <p>
         * TODO Not implemented properly in background monitor, log message will get lost if progress runs in background
         * @param message The message
         */
        void appendLogMessage(String message);
    }

    /**
     * The maximum value the progress bar that displays the current progress should have.
     */
    public static final int PROGRESS_BAR_MAX = 10_000;

    /**
     * The progress monitor being currently displayed.
     */
    static PleaseWaitProgressMonitor currentProgressMonitor;

    private final Component dialogParent;

    private int currentProgressValue;
    private String customText;
    private String title;
    private boolean indeterminate;

    private boolean isInBackground;
    private PleaseWaitDialog dialog;
    private String windowTitle;
    protected ProgressTaskId taskId;

    private boolean cancelable;

    /**
     * Returns the progress monitor being currently displayed.
     * @return the progress monitor being currently displayed
     * @since 12638
     */
    public static PleaseWaitProgressMonitor getCurrent() {
        return currentProgressMonitor;
    }

    private void doInEDT(Runnable runnable) {
        // This must be invoke later even if current thread is EDT because inside there is dialog.setVisible
        // which freeze current code flow until modal dialog is closed
        SwingUtilities.invokeLater(() -> {
            try {
                runnable.run();
            } catch (RuntimeException e) { // NOPMD
                throw BugReport.intercept(e).put("monitor", this);
            }
        });
    }

    private void setDialogVisible(boolean visible) {
        if (dialog.isVisible() != visible) {
            dialog.setVisible(visible);
        }
    }

    private ProgressMonitorDialog getDialog() {

        BackgroundProgressMonitor backgroundMonitor = null;
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            backgroundMonitor = map.statusLine.progressMonitor;
        }

        if (backgroundMonitor != null) {
            backgroundMonitor.setVisible(isInBackground);
        }
        if (dialog != null) {
            setDialogVisible(!isInBackground || backgroundMonitor == null);
        }

        if (isInBackground && backgroundMonitor != null) {
            backgroundMonitor.setVisible(true);
            if (dialog != null) {
                setDialogVisible(false);
            }
            return backgroundMonitor;
        } else if (backgroundMonitor != null) {
            backgroundMonitor.setVisible(false);
            if (dialog != null) {
                setDialogVisible(true);
            }
            return dialog;
        } else if (dialog != null) {
            setDialogVisible(true);
            return dialog;
        } else
            return null;
    }

    /**
     * Constructs a new {@code PleaseWaitProgressMonitor}.
     */
    public PleaseWaitProgressMonitor() {
        this("");
    }

    /**
     * Constructs a new {@code PleaseWaitProgressMonitor}.
     * @param windowTitle window title
     */
    public PleaseWaitProgressMonitor(String windowTitle) {
        this(Main.parent);
        this.windowTitle = windowTitle;
    }

    /**
     * Constructs a new {@code PleaseWaitProgressMonitor}.
     * @param dialogParent component to get parent frame from
     */
    public PleaseWaitProgressMonitor(Component dialogParent) {
        super(new CancelHandler());
        if (GraphicsEnvironment.isHeadless()) {
            this.dialogParent = dialogParent;
        } else {
            this.dialogParent = GuiHelper.getFrameForComponent(dialogParent);
        }
        this.cancelable = true;
    }

    /**
     * Constructs a new {@code PleaseWaitProgressMonitor}.
     * @param dialogParent component to get parent frame from
     * @param windowTitle window title
     */
    public PleaseWaitProgressMonitor(Component dialogParent, String windowTitle) {
        this(GuiHelper.getFrameForComponent(dialogParent));
        this.windowTitle = windowTitle;
    }

    private final ActionListener cancelListener = e -> cancel();

    private final ActionListener inBackgroundListener = e -> {
        isInBackground = true;
        ProgressMonitorDialog dlg = getDialog();
        if (dlg != null) {
            reset();
            dlg.setVisible(true);
        }
    };

    private final WindowListener windowListener = new WindowAdapter() {
        @Override public void windowClosing(WindowEvent e) {
            cancel();
        }
    };

    /**
     * See if this task is canceleable
     * @return <code>true</code> if it can be canceled
     */
    public final boolean isCancelable() {
        return cancelable;
    }

    /**
     * Sets this task to be cancelable
     * @param cancelable Whether it can be canceled
     */
    public final void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    @Override
    public void doBeginTask() {
        doInEDT(() -> {
            currentProgressMonitor = this;
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            if (dialogParent != null && dialog == null) {
                dialog = new PleaseWaitDialog(dialogParent);
            } else {
                throw new ProgressException("PleaseWaitDialog parent must be set");
            }

            if (windowTitle != null) {
                dialog.setTitle(windowTitle);
            }
            dialog.setCancelEnabled(cancelable);
            dialog.setCancelCallback(cancelListener);
            dialog.setInBackgroundCallback(inBackgroundListener);
            dialog.setCustomText("");
            dialog.addWindowListener(windowListener);
            dialog.setMaximumProgress(PROGRESS_BAR_MAX);
            dialog.setVisible(true);
        });
    }

    @Override
    public void doFinishTask() {
        // do nothing
    }

    @Override
    protected void updateProgress(double progressValue) {
        final int newValue = (int) (progressValue * PROGRESS_BAR_MAX);
        if (newValue != currentProgressValue) {
            currentProgressValue = newValue;
            doInEDT(() -> {
                ProgressMonitorDialog dlg = getDialog();
                if (dlg != null) {
                    dlg.updateProgress(currentProgressValue);
                }
            });
        }
    }

    @Override
    protected void doSetCustomText(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        this.customText = title;
        doInEDT(() -> {
            ProgressMonitorDialog dlg = getDialog();
            if (dlg != null) {
                dlg.setCustomText(title);
            }
        });
    }

    @Override
    protected void doSetTitle(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        this.title = title;
        doInEDT(() -> {
            ProgressMonitorDialog dlg = getDialog();
            if (dlg != null) {
                dlg.setCurrentAction(title);
            }
        });
    }

    @Override
    protected void doSetIntermediate(final boolean value) {
        this.indeterminate = value;
        doInEDT(() -> {
            // Enable only if progress is at the beginning. Doing intermediate progress in the middle
            // will hide already reached progress
            ProgressMonitorDialog dlg = getDialog();
            if (dlg != null) {
                dlg.setIndeterminate(value && currentProgressValue == 0);
            }
        });
    }

    @Override
    public void appendLogMessage(final String message) {
        doInEDT(() -> {
            ProgressMonitorDialog dlg = getDialog();
            if (dlg != null) {
                dlg.appendLogMessage(message);
            }
        });
    }

    /**
     * Update the dialog values
     */
    public void reset() {
        if (dialog != null) {
            dialog.setTitle(title);
            dialog.setCustomText(customText);
            dialog.updateProgress(currentProgressValue);
            dialog.setIndeterminate(indeterminate && currentProgressValue == 0);
        }
        BackgroundProgressMonitor backgroundMonitor = null;
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            backgroundMonitor = map.statusLine.progressMonitor;
        }
        if (backgroundMonitor != null) {
            backgroundMonitor.setCurrentAction(title);
            backgroundMonitor.setCustomText(customText);
            backgroundMonitor.updateProgress(currentProgressValue);
            backgroundMonitor.setIndeterminate(indeterminate && currentProgressValue == 0);
        }
    }

    /**
     * Close the progress dialog window.
     */
    public void close() {
        doInEDT(() -> {
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.setCancelCallback(null);
                dialog.setInBackgroundCallback(null);
                dialog.removeWindowListener(windowListener);
                dialog.dispose();
                dialog = null;
                currentProgressMonitor = null;
                MapFrame map = MainApplication.getMap();
                if (map != null) {
                    map.statusLine.progressMonitor.setVisible(false);
                }
            }
        });
    }

    /**
     * Show the progress dialog in foreground
     */
    public void showForegroundDialog() {
        isInBackground = false;
        doInEDT(() -> {
            if (dialog != null) {
                dialog.setInBackgroundPossible(taskId != null && MainApplication.isDisplayingMapView());
                reset();
                getDialog();
            }
        });
    }

    @Override
    public void setProgressTaskId(ProgressTaskId taskId) {
        this.taskId = taskId;
        doInEDT(() -> {
            if (dialog != null) {
                dialog.setInBackgroundPossible(taskId != null && MainApplication.isDisplayingMapView());
            }
        });
    }

    @Override
    public ProgressTaskId getProgressTaskId() {
        return taskId;
    }

    @Override
    public Component getWindowParent() {
        Component parent = dialog;
        if (isInBackground || parent == null)
            return Main.parent;
        else
            return parent;
    }

    @Override
    public String toString() {
        return "PleaseWaitProgressMonitor [currentProgressValue=" + currentProgressValue + ", customText=" + customText
                + ", title=" + title + ", indeterminate=" + indeterminate + ", isInBackground=" + isInBackground
                + ", windowTitle=" + windowTitle + ", taskId=" + taskId + ", cancelable=" + cancelable + ", state="
                + state + "]";
    }
}
