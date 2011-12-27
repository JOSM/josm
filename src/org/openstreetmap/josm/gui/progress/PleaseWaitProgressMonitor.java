// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapStatus.BackgroundProgressMonitor;
import org.openstreetmap.josm.gui.PleaseWaitDialog;

public class PleaseWaitProgressMonitor extends AbstractProgressMonitor {

	/**
	 * Implemented by both foreground dialog and background progress dialog (in status bar)
	 */
    public interface ProgressMonitorDialog {
        void setVisible(boolean visible);
        void updateProgress(int progress);
        void setCustomText(String text);
        void setTitle(String text);
        void setIndeterminate(boolean newValue);
        void appendLogMessage(String message);
    }

    public static final int PROGRESS_BAR_MAX = 100;
    private final Window dialogParent;
    private int currentProgressValue = 0;

    private boolean isInBackground;
    private PleaseWaitDialog dialog;
    private String windowTitle;
    protected ProgressTaskId taskId;

    private boolean cancelable;

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
            dialog.setVisible(!isInBackground || backgroundMonitor == null);
        }

        if (isInBackground && backgroundMonitor != null) {
            backgroundMonitor.setVisible(true);
            if (dialog != null) {
                dialog.setVisible(false);
            }
            return backgroundMonitor;
        } else if (backgroundMonitor != null) {
            backgroundMonitor.setVisible(false);
            if (dialog != null) {
                dialog.setVisible(true);
            }
            return dialog;
        } else if (dialog != null) {
            dialog.setVisible(true);
            return dialog;
        } else
            return null;
    }

    public PleaseWaitProgressMonitor() {
        this("");
    }

    public PleaseWaitProgressMonitor(String windowTitle) {
        this(Main.parent);
        this.windowTitle = windowTitle;
    }

    public PleaseWaitProgressMonitor(Component dialogParent) {
        super(new CancelHandler());
        this.dialogParent = JOptionPane.getFrameForComponent(dialogParent);
        this.cancelable = true;
    }

    public PleaseWaitProgressMonitor(Component dialogParent, String windowTitle) {
        this(JOptionPane.getFrameForComponent(dialogParent));
        this.windowTitle = windowTitle;
    }

    private ActionListener cancelListener = new ActionListener(){
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    };

    private ActionListener inBackgroundListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            isInBackground = true;
            ProgressMonitorDialog dialog = getDialog();
            if (dialog != null) {
                dialog.setVisible(true);
            }
        }
    };

    private WindowListener windowListener = new WindowAdapter(){
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
        doInEDT(new Runnable() {
            public void run() {
                Main.currentProgressMonitor = PleaseWaitProgressMonitor.this;
                if (dialogParent instanceof Frame && dialog == null) {
                    dialog = new PleaseWaitDialog(dialogParent);
                } else if (dialogParent instanceof Dialog && dialog == null) {
                    dialog = new PleaseWaitDialog(dialogParent);
                } else
                    throw new ProgressException("PleaseWaitDialog parent must be either Frame or Dialog");

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
            }
        });
    }

    @Override
    public void doFinishTask() {
        // do nothing
    }

    @Override
    protected void updateProgress(double progressValue) {
        final int newValue = (int)(progressValue * PROGRESS_BAR_MAX);
        if (newValue != currentProgressValue) {
            currentProgressValue = newValue;
            doInEDT(new Runnable() {
                public void run() {
                    ProgressMonitorDialog dialog = getDialog();
                    if (dialog != null) {
                        dialog.updateProgress(currentProgressValue);
                    }
                }
            });
        }
    }

    @Override
    protected void doSetCustomText(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        doInEDT(new Runnable() {
            public void run() {
                ProgressMonitorDialog dialog = getDialog();
                if (dialog != null) {
                    dialog.setCustomText(title);
                }
            }
        });
    }

    @Override
    protected void doSetTitle(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        doInEDT(new Runnable() {
            public void run() {
                ProgressMonitorDialog dialog = getDialog();
                if (dialog != null) {
                    dialog.setTitle(title);
                }
            }
        });
    }

    @Override
    protected void doSetIntermediate(final boolean value) {
        doInEDT(new Runnable() {
            public void run() {
                // Enable only if progress is at the beginning. Doing intermediate progress in the middle
                // will hide already reached progress
                ProgressMonitorDialog dialog = getDialog();
                if (dialog != null) {
                    dialog.setIndeterminate(value && PleaseWaitProgressMonitor.this.dialog.progress.getValue() == 0);
                }
            }
        });
    }

    @Override
    public void appendLogMessage(final String message) {
        doInEDT(new Runnable() {
            public void run() {
                ProgressMonitorDialog dialog = getDialog();
                if (dialog != null) {
                    dialog.appendLogMessage(message);
                }
            }
        });
    }

    public void close() {
        doInEDT(new Runnable() {
            @Override
            public void run() {
                dialog.setVisible(false);
                dialog.setCancelCallback(null);
                dialog.setInBackgroundCallback(null);
                dialog.removeWindowListener(windowListener);
                dialog.dispose();
                dialog = null;
                MapFrame map = Main.map;
                if (map != null) {
                    map.statusLine.progressMonitor.setVisible(false);
                }
                Main.currentProgressMonitor = null;
            }
        });
    }

    public void showForegroundDialog() {
        isInBackground = false;
        doInEDT(new Runnable() {
            @Override
            public void run() {
                dialog.setInBackgroundPossible(PleaseWaitProgressMonitor.this.taskId != null && Main.isDisplayingMapView());
                getDialog();
            }
        });

    }

    @Override
    public void setProgressTaskId(ProgressTaskId taskId) {
        this.taskId = taskId;
        doInEDT(new Runnable() {
            @Override
            public void run() {
                dialog.setInBackgroundPossible(PleaseWaitProgressMonitor.this.taskId != null && Main.isDisplayingMapView());
            }
        });
    }

    @Override
    public ProgressTaskId getProgressTaskId() {
        return taskId;
    }

}
