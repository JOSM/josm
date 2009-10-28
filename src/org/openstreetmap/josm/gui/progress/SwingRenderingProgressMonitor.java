// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import javax.swing.SwingUtilities;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * SwingRenderingProgressMonitor is progress monitor which delegates the rendering
 * of progress information to a {@see ProgressRenderer}.
 * Methods of the progress renderer are always called on the Swing EDT.
 *
 */
public class SwingRenderingProgressMonitor extends AbstractProgressMonitor {
    private static final int PROGRESS_BAR_MAX = 100;
    private int currentProgressValue = 0;
    private ProgressRenderer delegate;

    /**
     * 
     * @param delegate the delegate which renders the progress information. Must not be null.
     * @throws IllegalArgumentException thrown if delegate is null
     * 
     */
    public SwingRenderingProgressMonitor(ProgressRenderer delegate) {
        super(new CancelHandler());
        if (delegate == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "delegate"));
        this.delegate = delegate;
    }

    private void doInEDT(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    @Override
    public void doBeginTask() {
        doInEDT(new Runnable() {
            public void run() {
                delegate.setCustomText("");
                delegate.setMaximum(PROGRESS_BAR_MAX);
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
                    delegate.setValue(currentProgressValue);
                }
            });
        }
    }

    @Override
    protected void doSetCustomText(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        doInEDT(new Runnable() {
            public void run() {
                delegate.setCustomText(title);
            }
        });
    }

    @Override
    protected void doSetTitle(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        doInEDT(new Runnable() {
            public void run() {
                delegate.setTaskTitle(title);
            }
        });
    }

    @Override
    protected void doSetIntermediate(final boolean value) {
        doInEDT(new Runnable() {
            public void run() {
                delegate.setIndeterminate(value);
            }
        });
    }
}
