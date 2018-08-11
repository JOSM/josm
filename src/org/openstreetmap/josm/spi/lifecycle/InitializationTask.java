// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.lifecycle;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Initialization task notifying the current lifecycle {@link InitStatusListener}.
 * @since 14125
 */
public final class InitializationTask implements Callable<Void> {

    private final String name;
    private final Runnable task;

    /**
     * Constructs a new {@code InitializationTask}.
     * @param name translated name to be displayed to user
     * @param task runnable initialization task
     */
    public InitializationTask(String name, Runnable task) {
        this.name = Objects.requireNonNull(name);
        this.task = Objects.requireNonNull(task);
    }

    @Override
    public Void call() {
        Object status = null;
        InitStatusListener initListener = Lifecycle.getInitStatusListener();
        if (initListener != null) {
            status = initListener.updateStatus(name);
        }
        task.run();
        if (initListener != null) {
            initListener.finish(status);
        }
        return null;
    }
}
