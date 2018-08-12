// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.lifecycle;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Defines the initialization sequence.
 * @since 14139
 */
public interface InitializationSequence {

    /**
     * Returns tasks that must be run before parallel tasks.
     * @return tasks that must be run before parallel tasks
     * @see #afterInitializationTasks
     * @see #parallelInitializationTasks
     */
    default List<InitializationTask> beforeInitializationTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns tasks to be executed (in parallel) by a ExecutorService.
     * @return tasks to be executed (in parallel) by a ExecutorService
     */
    default Collection<InitializationTask> parallelInitializationTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns asynchronous callable initializations to be completed eventually
     * @return asynchronous callable initializations to be completed eventually
     */
    default List<Callable<?>> asynchronousCallableTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns asynchronous runnable initializations to be completed eventually
     * @return asynchronous runnable initializations to be completed eventually
     */
    default List<Runnable> asynchronousRunnableTasks() {
        return Collections.emptyList();
    }

    /**
     * Returns tasks that must be run after parallel tasks.
     * @return tasks that must be run after parallel tasks
     * @see #beforeInitializationTasks
     * @see #parallelInitializationTasks
     */
    default List<InitializationTask> afterInitializationTasks() {
        return Collections.emptyList();
    }
}
