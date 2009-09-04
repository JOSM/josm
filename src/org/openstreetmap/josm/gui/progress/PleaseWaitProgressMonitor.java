// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitDialog;


public class PleaseWaitProgressMonitor extends AbstractProgressMonitor {

    private static final int PROGRESS_BAR_MAX = 100;
    private final Window dialogParent;
    private int currentProgressValue = 0;

    private PleaseWaitDialog dialog;
    private String windowTitle;

    public PleaseWaitProgressMonitor() {
        this("");
    }

    public PleaseWaitProgressMonitor(String windowTitle) {
        this(JOptionPane.getFrameForComponent(Main.map));
        this.windowTitle = windowTitle;
    }

    public PleaseWaitProgressMonitor(Window dialogParent) {
        super(new CancelHandler());
        this.dialogParent = dialogParent;
    }

    private ActionListener cancelListener = new ActionListener(){
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    };

    private WindowListener windowListener = new WindowAdapter(){
        @Override public void windowClosing(WindowEvent e) {
            cancel();
            closeDialog();
        }
    };

    private void closeDialog() {
        try {
            Runnable runnable = new Runnable(){
                public void run() {
                }
            };

            // make sure, this is called in the dispatcher thread ASAP
            if (EventQueue.isDispatchThread()) {
                runnable.run();
            } else {
                EventQueue.invokeAndWait(runnable);
            }

        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void doInEDT(Runnable runnable) {
        EventQueue.invokeLater(runnable);
    }

    @Override
    public void doBeginTask() {
        doInEDT(new Runnable() {
            public void run() {
                if (dialogParent instanceof Frame) {
                    dialog = new PleaseWaitDialog((Frame)dialogParent);
                } else if (dialogParent instanceof Dialog) {
                    dialog = new PleaseWaitDialog((Dialog)dialogParent);
                } else
                    throw new ProgressException("PleaseWaitDialog parent must be either Frame or Dialog");

                if (windowTitle != null) {
                    dialog.setTitle(windowTitle);
                }
                dialog.cancel.setEnabled(true);
                dialog.setCustomText("");
                dialog.cancel.addActionListener(cancelListener);
                dialog.addWindowListener(windowListener);
                dialog.progress.setMaximum(PROGRESS_BAR_MAX);
                dialog.setVisible(true);
            }
        });
    }

    @Override
    public void doFinishTask() {
        doInEDT(new Runnable() {
            public void run() {
                if (dialog != null) {
                    dialog.setVisible(false);
                    dialog.dispose();
                    dialog.removeWindowListener(windowListener);
                    dialog.cancel.removeActionListener(cancelListener);
                    if (getErrorMessage() != null) {
                        JOptionPane.showMessageDialog(
                                Main.parent, getErrorMessage(),
                                tr("Error"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                    dialog = null;
                }
            }
        });
    }

    @Override
    protected void updateProgress(double progressValue) {
        final int newValue = (int)(progressValue * PROGRESS_BAR_MAX);
        if (newValue != currentProgressValue) {
            currentProgressValue = newValue;
            doInEDT(new Runnable() {
                public void run() {
                    dialog.progress.setValue(currentProgressValue);
                }
            });
        }
    }

    @Override
    protected void doSetCustomText(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        doInEDT(new Runnable() {
            public void run() {
                dialog.setCustomText(title);
            }
        });
    }

    @Override
    protected void doSetTitle(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        doInEDT(new Runnable() {
            public void run() {
                dialog.currentAction.setText(title);
            }
        });
    }

    @Override
    protected void doSetIntermediate(final boolean value) {
        doInEDT(new Runnable() {
            public void run() {
                if (value && dialog.progress.getValue() == 0) {
                    // Enable only if progress is at the begging. Doing intermediate progress in the middle
                    // will hide already reached progress
                    dialog.setIndeterminate(true);
                } else {
                    dialog.setIndeterminate(false);
                }
            }
        });
    }

    @Override
    protected void doSetErrorMessage(String message) {
        // Do nothing
    }

}
