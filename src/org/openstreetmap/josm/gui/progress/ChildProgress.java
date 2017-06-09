// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;

/**
 * The progress of a sub task
 */
public class ChildProgress extends AbstractProgressMonitor {

    private final AbstractProgressMonitor parent;
    private final boolean internal;

    /**
     * Creates a new {@link ChildProgress}
     * @param parent The parent task that creates this progress
     * @param cancelHandler The cancel handler to notify when this task is canceled
     * @param internal this is an internal task that will not modify the text that is displayed to the user
     */
    public ChildProgress(AbstractProgressMonitor parent, CancelHandler cancelHandler, boolean internal) {
        super(cancelHandler);
        this.parent = parent;
        this.internal = internal;
    }

    /**
     * Gets the parent task
     * @return The parent task
     */
    public final AbstractProgressMonitor getParent() {
        return parent;
    }

    /**
     * See if this is an internal task
     * @return True if this task should not modify the text that is displayed to the user
     */
    public final boolean isInternal() {
        return internal;
    }

    @Override
    protected void updateProgress(double value) {
        parent.childSetProgress(this, value);
    }

    @Override
    protected void doBeginTask() {
        // Do nothing
    }

    @Override
    protected void doSetCustomText(String title) {
        if (!internal) {
            parent.childSetCustomText(this, title);
        }
    }

    @Override
    protected void doSetTitle(String title) {
        if (!internal) {
            parent.childSetTitle(this, title);
        }
    }

    @Override
    protected void doSetIntermediate(boolean value) {
        if (!internal) {
            parent.childSetIntermediate(this, value);
        }
    }

    @Override
    protected void doFinishTask() {
        parent.childFinished(this);
    }

    @Override
    public void setProgressTaskId(ProgressTaskId taskId) {
        parent.setProgressTaskId(taskId);
    }

    @Override
    public ProgressTaskId getProgressTaskId() {
        return parent.getProgressTaskId();
    }

    @Override
    public Component getWindowParent() {
        return parent.getWindowParent();
    }
}
