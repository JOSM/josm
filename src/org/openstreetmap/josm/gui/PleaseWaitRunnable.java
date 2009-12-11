// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor.CancelListener;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * Instanced of this thread will display a "Please Wait" message in middle of JOSM
 * to indicate a progress being executed.
 *
 * @author Imi
 */
public abstract class PleaseWaitRunnable implements Runnable, CancelListener {
    private final static Logger logger = Logger.getLogger(PleaseWaitRunnable.class.getName());

    private boolean cancelled = false;
    private boolean ignoreException;
    private final String title;

    protected final ProgressMonitor progressMonitor;

    /**
     * Create the runnable object with a given message for the user.
     */
    public PleaseWaitRunnable(String title) {
        this(title, false);
    }

    /**
     * Create the runnable object with a given message for the user.
     *
     * @param title message for the user
     * @param ignoreException If true, exception will be propagated to calling code. If false then
     * exception will be thrown directly in EDT. When this runnable is executed using executor framework
     * then use false unless you read result of task (because exception will get lost if you don't)
     */
    public PleaseWaitRunnable(String title, boolean ignoreException) {
        this(title, new PleaseWaitProgressMonitor(title), ignoreException);
    }

    public PleaseWaitRunnable(String title, ProgressMonitor progressMonitor, boolean ignoreException) {
        this.title = title;
        this.progressMonitor = progressMonitor == null?new PleaseWaitProgressMonitor(title):progressMonitor;
        this.ignoreException = ignoreException;
    }

    private void doRealRun() {
        try {
            try {
                progressMonitor.addCancelListener(this);
                progressMonitor.beginTask(title);
                try {
                    realRun();
                } finally {
                    if (EventQueue.isDispatchThread()) {
                        finish();
                    } else {
                        EventQueue.invokeAndWait(new Runnable() {
                            public void run() {
                                finish();
                            }
                        });
                    }
                }
            } finally {
                progressMonitor.finishTask();
                progressMonitor.removeCancelListener(this);
                if (progressMonitor instanceof PleaseWaitProgressMonitor) {
                    ((PleaseWaitProgressMonitor)progressMonitor).close();
                }
            }
        } catch (final Exception e) {
            if (!ignoreException) {
                // Exception has to thrown in EDT to be shown to user
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ExceptionDialogUtil.explainException(e);
                    }
                });
            }
        }
    }

    public final void run() {
        if (cancelled)
            return; // since realRun isn't executed, do not call to finish

        if (EventQueue.isDispatchThread()) {
            new Thread(new Runnable() {
                public void run() {
                    doRealRun();
                }
            }).start();
        } else {
            doRealRun();
        }
    }

    public void operationCanceled() {
        cancel();
    }

    /**
     * User pressed cancel button.
     */
    protected abstract void cancel();

    /**
     * Called in the worker thread to do the actual work. When any of the
     * exception is thrown, a message box will be displayed and closeDialog
     * is called. finish() is called in any case.
     */
    protected abstract void realRun() throws SAXException, IOException, OsmTransferException;

    /**
     * Finish up the data work. Is guaranteed to be called if realRun is called.
     * Finish is called in the gui thread just after the dialog disappeared.
     */
    protected abstract void finish();

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }
}
