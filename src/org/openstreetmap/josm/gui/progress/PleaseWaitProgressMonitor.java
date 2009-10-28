// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
import static org.openstreetmap.josm.tools.I18n.tr;


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
        this(JOptionPane.getFrameForComponent(Main.parent));
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
        }
    };

    private void doInEDT(Runnable runnable) {
        EventQueue.invokeLater(runnable);
    }

    @Override
    public void doBeginTask() {
        doInEDT(new Runnable() {
            public void run() {
                if (dialogParent instanceof Frame && dialog == null) {
                    dialog = new PleaseWaitDialog(dialogParent);
                } else if (dialogParent instanceof Dialog && dialog == null) {
                    dialog = new PleaseWaitDialog(dialogParent);
                } else
                    throw new ProgressException("PleaseWaitDialog parent must be either Frame or Dialog");

                if (windowTitle != null) {
                    dialog.setTitle(windowTitle);
                }
                dialog.setCancelEnabled(true);
                dialog.setCancelCallback(cancelListener);
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
                    // Enable only if progress is at the beginning. Doing intermediate progress in the middle
                    // will hide already reached progress
                    dialog.setIndeterminate(true);
                } else {
                    dialog.setIndeterminate(false);
                }
            }
        });
    }

    @Override
    public void appendLogMessage(final String message) {
        doInEDT(new Runnable() {
            public void run() {
                dialog.appendLogMessage(message);
            }
        });
    }

    public void close() {
        dialog.setVisible(false);
        dialog.setCancelCallback(null);
        dialog.removeWindowListener(windowListener);
        dialog.dispose();
        dialog = null;
    }
}
