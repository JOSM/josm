// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.awt.Component;

public class ChildProgress extends AbstractProgressMonitor {

    private final AbstractProgressMonitor parent;
    private final boolean internal;

    public ChildProgress(AbstractProgressMonitor parent, CancelHandler cancelHandler, boolean internal) {
        super(cancelHandler);
        this.parent = parent;
        this.internal = internal;
    }

    public final AbstractProgressMonitor getParent() {
        return parent;
    }

    public final boolean isInternal() {
        return internal;
    }

    @Override
    void updateProgress(double value) {
        parent.childSetProgress(this, value);
    }

    @Override
    protected void doBeginTask() {
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
