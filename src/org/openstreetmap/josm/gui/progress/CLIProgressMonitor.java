// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Utils;

/**
 * CLI implementation of a {@link ProgressMonitor}
 * @author Taylor Smock
 * @since 18365
 */
public class CLIProgressMonitor extends AbstractProgressMonitor {
    /** The current task id */
    private ProgressTaskId taskId;
    /** The current task title */
    private String title = "";
    /** The custom text (prepended with '/') */
    private String customText = "";
    /** The last time we updated the progress information */
    private Stopwatch lastUpdateTime;
    /** The start time of the monitor */
    private Stopwatch startTime;

    /**
     * Create a new {@link CLIProgressMonitor}
     */
    public CLIProgressMonitor() {
        super(new CancelHandler());
    }

    @Override
    protected void doBeginTask() {
        if (!Utils.isBlank(this.title)) {
            Logging.info(tr("Beginning task{2}: {0}{1}", this.title, this.customText,
                    Optional.ofNullable(this.taskId).map(ProgressTaskId::getId).map(id -> ' ' + id).orElse("")));
        }
        this.startTime = Stopwatch.createStarted();
        this.lastUpdateTime = this.startTime;
    }

    @Override
    protected void doFinishTask() {
        Logging.info(tr("Finishing task{2}: {0}{1} ({3})", this.title, this.customText,
                Optional.ofNullable(this.taskId).map(ProgressTaskId::getId).map(id -> ' ' + id).orElse(""), this.startTime));
        this.lastUpdateTime = null;
    }

    @Override
    protected void doSetIntermediate(boolean value) {
        // Do nothing for now
    }

    @Override
    protected void doSetTitle(String title) {
        this.title = Optional.ofNullable(title).orElse("");
    }

    @Override
    protected void doSetCustomText(String customText) {
        this.customText = Optional.ofNullable(customText).map(str -> '/' + str).orElse("");
    }

    @Override
    protected void updateProgress(double value) {
        if (this.lastUpdateTime == null || this.lastUpdateTime.elapsed() > TimeUnit.SECONDS.toMillis(10)) {
            this.lastUpdateTime = Stopwatch.createStarted();
            Logging.info(tr("Progress of task{2}: {0}{1} is {3}% ({4})", this.title, this.customText,
                    Optional.ofNullable(this.taskId).map(ProgressTaskId::getId).map(id -> ' ' + id).orElse(""), value * 100, this.startTime));
        }
    }

    @Override
    public void setProgressTaskId(ProgressTaskId taskId) {
        this.taskId = taskId;
    }

    @Override
    public ProgressTaskId getProgressTaskId() {
        return this.taskId;
    }

    @Override
    public Component getWindowParent() {
        return null;
    }
}
