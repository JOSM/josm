// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * SwingRenderingProgressMonitor is progress monitor which delegates the rendering
 * of progress information to a {@link ProgressRenderer}.
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
        CheckParameterUtil.ensureParameterNotNull(delegate, "delegate");
        this.delegate = delegate;
    }

    @Override
    public void doBeginTask() {
        GuiHelper.runInEDT(new Runnable() {
            @Override
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
            GuiHelper.runInEDT(new Runnable() {
                @Override
                public void run() {
                    delegate.setValue(currentProgressValue);
                }
            });
        }
    }

    @Override
    protected void doSetCustomText(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                delegate.setCustomText(title);
            }
        });
    }

    @Override
    protected void doSetTitle(final String title) {
        checkState(State.IN_TASK, State.IN_SUBTASK);
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                delegate.setTaskTitle(title);
            }
        });
    }

    @Override
    protected void doSetIntermediate(final boolean value) {
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                delegate.setIndeterminate(value);
            }
        });
    }

    @Override
    public void setProgressTaskId(ProgressTaskId taskId) {
    }

    @Override
    public ProgressTaskId getProgressTaskId() {
        return null;
    }

    @Override
    public Component getWindowParent() {
        return Main.parent;
    }
}
