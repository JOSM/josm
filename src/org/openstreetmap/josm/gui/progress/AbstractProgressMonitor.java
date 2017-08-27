// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class contains the progress logic required to implement a {@link ProgressMonitor}.
 */
public abstract class AbstractProgressMonitor implements ProgressMonitor {

    private static class Request {
        private AbstractProgressMonitor originator;
        private int childTicks;
        private double currentValue;

        private String title;
        private String customText;
        private String extraText;
        private Boolean intermediate;

        private boolean finishRequested;
    }

    private final CancelHandler cancelHandler;

    /**
     * Progress monitor state
     * @since 12675 (visibility)
     */
    public enum State {
        /** Initialization. Next valid states are {@link #IN_TASK} or {@link #FINISHED} */
        INIT,
        /** In task. Next valid states are {@link #IN_SUBTASK} or {@link #FINISHED} */
        IN_TASK,
        /** In subtask. Next valid states is {@link #IN_TASK} */
        IN_SUBTASK,
        /** Finished. Can't change state after that */
        FINISHED
    }

    protected State state = State.INIT;

    private int ticksCount;
    private int ticks;
    private int childTicks;

    private String taskTitle;
    private String customText;
    private String extraText;
    private String shownTitle;
    private String shownCustomText;
    private boolean intermediateTask;

    private final Queue<Request> requests = new LinkedList<>();
    private AbstractProgressMonitor currentChild;
    private Request requestedState = new Request();

    protected abstract void doBeginTask();

    protected abstract void doFinishTask();

    protected abstract void doSetIntermediate(boolean value);

    protected abstract void doSetTitle(String title);

    protected abstract void doSetCustomText(String title);

    /**
     * Create a new {@link AbstractProgressMonitor}
     * @param cancelHandler The handler that gets notified when the process is canceled.
     */
    protected AbstractProgressMonitor(CancelHandler cancelHandler) {
        this.cancelHandler = cancelHandler;
    }

    protected void checkState(State... expectedStates) {
        for (State s:expectedStates) {
            if (s == state)
                return;
        }
        throw new ProgressException("Expected states are %s but current state is %s", Arrays.asList(expectedStates).toString(), state);
    }

    /*=======
     * Tasks
     =======*/

    @Override
    public void beginTask(String title) {
        beginTask(title, DEFAULT_TICKS);
    }

    @Override
    public synchronized void beginTask(String title, int ticks) {
        this.taskTitle = title;
        checkState(State.INIT);
        state = State.IN_TASK;
        doBeginTask();
        setTicksCount(ticks);
        resetState();
    }

    @Override
    public synchronized void finishTask() {
        if (state != State.FINISHED) {

            if (state == State.IN_SUBTASK) {
                requestedState.finishRequested = true;
            } else {
                checkState(State.IN_TASK);
                state = State.FINISHED;
                doFinishTask();
            }
        }
    }

    @Override
    public synchronized void invalidate() {
        if (state == State.INIT) {
            state = State.FINISHED;
            doFinishTask();
        }
    }

    @Override
    public synchronized void subTask(final String title) {
        if (state == State.IN_SUBTASK) {
            if (title != null) {
                requestedState.title = title;
            }
            requestedState.intermediate = Boolean.FALSE;
        } else {
            checkState(State.IN_TASK);
            if (title != null) {
                this.taskTitle = title;
                resetState();
            }
            this.intermediateTask = false;
            doSetIntermediate(false);
        }
    }

    @Override
    public synchronized void indeterminateSubTask(String title) {
        if (state == State.IN_SUBTASK) {
            if (title != null) {
                requestedState.title = title;
            }
            requestedState.intermediate = Boolean.TRUE;
        } else {
            checkState(State.IN_TASK);
            if (title != null) {
                this.taskTitle = title;
                resetState();
            }
            this.intermediateTask = true;
            doSetIntermediate(true);
        }
    }

    @Override
    public synchronized void setCustomText(String text) {
        if (state == State.IN_SUBTASK) {
            requestedState.customText = text;
        } else {
            this.customText = text;
            resetState();
        }
    }

    @Override
    public synchronized void setExtraText(String text) {
        if (state == State.IN_SUBTASK) {
            requestedState.extraText = text;
        } else {
            this.extraText = text;
            resetState();
        }
    }

    /**
     * Default implementation is empty. Override in subclasses to display the log messages.
     */
    @Override
    public void appendLogMessage(String message) {
        // do nothing
    }

    private void resetState() {
        String newTitle;
        if (extraText != null) {
            newTitle = taskTitle + ' ' + extraText;
        } else {
            newTitle = taskTitle;
        }

        if (newTitle == null ? shownTitle != null : !newTitle.equals(shownTitle)) {
            shownTitle = newTitle;
            doSetTitle(shownTitle);
        }

        if (customText == null ? shownCustomText != null : !customText.equals(shownCustomText)) {
            shownCustomText = customText;
            doSetCustomText(shownCustomText);
        }
        doSetIntermediate(intermediateTask);
    }

    @Override
    public void cancel() {
        cancelHandler.cancel();
    }

    @Override
    public boolean isCanceled() {
        return cancelHandler.isCanceled();
    }

    @Override
    public void addCancelListener(CancelListener listener) {
        cancelHandler.addCancelListener(listener);
    }

    @Override
    public void removeCancelListener(CancelListener listener) {
        cancelHandler.removeCancelListener(listener);
    }

    /*=================
     * Ticks handling
    ==================*/

    protected abstract void updateProgress(double value);

    @Override
    public synchronized void setTicks(int ticks) {
        if (ticks >= ticksCount) {
            ticks = ticksCount - 1;
        }
        this.ticks = ticks;
        internalUpdateProgress(0);
    }

    @Override
    public synchronized void setTicksCount(int ticks) {
        this.ticksCount = ticks;
        internalUpdateProgress(0);
    }

    @Override
    public void worked(int ticks) {
        if (ticks == ALL_TICKS) {
            setTicks(this.ticksCount - 1);
        } else {
            setTicks(this.ticks + ticks);
        }
    }

    private void internalUpdateProgress(double childProgress) {
        if (childProgress > 1) {
            childProgress = 1;
        }
        checkState(State.IN_TASK, State.IN_SUBTASK);
        updateProgress(ticksCount == 0 ? 0 : (ticks + childProgress * childTicks) / ticksCount);
    }

    @Override
    public synchronized int getTicks() {
        return ticks;
    }

    @Override
    public synchronized int getTicksCount() {
        return ticksCount;
    }

    /*==========
     * Subtasks
     ==========*/

    @Override
    public synchronized ProgressMonitor createSubTaskMonitor(int ticks, boolean internal) {
        if (ticks == ALL_TICKS) {
            ticks = ticksCount - this.ticks;
        }

        if (state == State.IN_SUBTASK) {
            Request request = new Request();
            request.originator = new ChildProgress(this, cancelHandler, internal);
            request.childTicks = ticks;
            requests.add(request);
            return request.originator;
        } else {
            checkState(State.IN_TASK);
            state = State.IN_SUBTASK;
            this.childTicks = ticks;
            currentChild = new ChildProgress(this, cancelHandler, internal);
            return currentChild;
        }
    }

    private void applyChildRequest(Request request) {
        if (request.customText != null) {
            doSetCustomText(request.customText);
        }

        if (request.title != null) {
            doSetTitle(request.title);
        }

        if (request.intermediate != null) {
            doSetIntermediate(request.intermediate);
        }

        internalUpdateProgress(request.currentValue);
    }

    private void applyThisRequest(Request request) {
        if (request.finishRequested) {
            finishTask();
        } else {
            if (request.customText != null) {
                this.customText = request.customText;
            }

            if (request.title != null) {
                this.taskTitle = request.title;
            }

            if (request.intermediate != null) {
                this.intermediateTask = request.intermediate;
            }

            if (request.extraText != null) {
                this.extraText = request.extraText;
            }

            resetState();
        }
    }

    protected synchronized void childFinished(AbstractProgressMonitor child) {
        checkState(State.IN_SUBTASK);
        if (currentChild == child) {
            setTicks(ticks + childTicks);
            if (requests.isEmpty()) {
                state = State.IN_TASK;
                applyThisRequest(requestedState);
                requestedState = new Request();
            } else {
                Request newChild = requests.poll();
                currentChild = newChild.originator;
                childTicks = newChild.childTicks;
                applyChildRequest(newChild);
            }
        } else {
            Iterator<Request> it = requests.iterator();
            while (it.hasNext()) {
                if (it.next().originator == child) {
                    it.remove();
                    return;
                }
            }
            throw new ProgressException("Subtask %s not found", child);
        }
    }

    private Request getRequest(AbstractProgressMonitor child) {
        for (Request request:requests) {
            if (request.originator == child)
                return request;
        }
        throw new ProgressException("Subtask %s not found", child);
    }

    protected synchronized void childSetProgress(AbstractProgressMonitor child, double value) {
        checkState(State.IN_SUBTASK);
        if (currentChild == child) {
            internalUpdateProgress(value);
        } else {
            getRequest(child).currentValue = value;
        }
    }

    protected synchronized void childSetTitle(AbstractProgressMonitor child, String title) {
        checkState(State.IN_SUBTASK);
        if (currentChild == child) {
            doSetTitle(title);
        } else {
            getRequest(child).title = title;
        }
    }

    protected synchronized void childSetCustomText(AbstractProgressMonitor child, String customText) {
        checkState(State.IN_SUBTASK);
        if (currentChild == child) {
            doSetCustomText(customText);
        } else {
            getRequest(child).customText = customText;
        }
    }

    protected synchronized void childSetIntermediate(AbstractProgressMonitor child, boolean value) {
        checkState(State.IN_SUBTASK);
        if (currentChild == child) {
            doSetIntermediate(value);
        } else {
            getRequest(child).intermediate = value;
        }
    }
}
