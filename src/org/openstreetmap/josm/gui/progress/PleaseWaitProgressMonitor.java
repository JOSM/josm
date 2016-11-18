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
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapStatus.BackgroundProgressMonitor;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.bugreport.BugReport;

public class PleaseWaitProgressMonitor extends AbstractProgressMonitor {

    /**
     * Implemented by both foreground dialog and background progress dialog (in status bar)
     */
    public interface ProgressMonitorDialog {
        void setVisible(boolean visible);

        void updateProgress(int progress);

        void setCustomText(String text);

        void setCurrentAction(String text);

        void setIndeterminate(boolean newValue);

        // TODO Not implemented properly in background monitor, log message will get lost if progress runs in background
        void appendLogMessage(String message);
    }

    public static final int PROGRESS_BAR_MAX = 10_000;
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

    private void doInEDT(Runnable runnable) {
        // This must be invoke later even if current thread is EDT because inside there is dialog.setVisible
        // which freeze current code flow until modal dialog is closed
        SwingUtilities.invokeLater(() -> {
            try {
                runnable.run();
            } catch (RuntimeException e) {
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
        MapFrame map = Main.map;
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

    public final boolean isCancelable() {
        return cancelable;
    }

    public final void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    @Override
    public void doBeginTask() {
        doInEDT(() -> {
            Main.currentProgressMonitor = this;
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
            dialog.progress.setMaximum(PROGRESS_BAR_MAX);
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

    public void reset() {
        if (dialog != null) {
            dialog.setTitle(title);
            dialog.setCustomText(customText);
            dialog.updateProgress(currentProgressValue);
            dialog.setIndeterminate(indeterminate && currentProgressValue == 0);
        }
        BackgroundProgressMonitor backgroundMonitor = null;
        MapFrame map = Main.map;
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

    public void close() {
        doInEDT(() -> {
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.setCancelCallback(null);
                dialog.setInBackgroundCallback(null);
                dialog.removeWindowListener(windowListener);
                dialog.dispose();
                dialog = null;
                Main.currentProgressMonitor = null;
                MapFrame map = Main.map;
                if (map != null) {
                    map.statusLine.progressMonitor.setVisible(false);
                }
            }
        });
    }

    public void showForegroundDialog() {
        isInBackground = false;
        doInEDT(() -> {
            if (dialog != null) {
                dialog.setInBackgroundPossible(taskId != null && Main.isDisplayingMapView());
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
                dialog.setInBackgroundPossible(taskId != null && Main.isDisplayingMapView());
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
