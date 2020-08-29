// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.lifecycle;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * JOSM lifecycle.
 * @since 14125
 */
public final class Lifecycle {

    private static volatile InitStatusListener initStatusListener;

    private static volatile Runnable shutdownSequence;

    private Lifecycle() {
        // Hide constructor
    }

    /**
     * Gets initialization task listener.
     * @return initialization task listener
     */
    public static InitStatusListener getInitStatusListener() {
        return initStatusListener;
    }

    /**
     * Sets initialization task listener.
     * @param listener initialization task listener. Must not be null
     */
    public static void setInitStatusListener(InitStatusListener listener) {
        initStatusListener = Objects.requireNonNull(listener);
    }

    /**
     * Gets shutdown sequence.
     * @return shutdown sequence
     * @since 14140
     */
    public static Runnable getShutdownSequence() {
        return shutdownSequence;
    }

    /**
     * Sets shutdown sequence.
     * @param sequence shutdown sequence. Must not be null
     * @since 14140
     */
    public static void setShutdownSequence(Runnable sequence) {
        shutdownSequence = Objects.requireNonNull(sequence);
    }

    /**
     * Initializes the main object. A lot of global variables are initialized here.
     * @param initSequence Initialization sequence
     * @since 14139
     */
    public static void initialize(InitializationSequence initSequence) {
        // Initializes tasks that must be run before parallel tasks
        runInitializationTasks(initSequence.beforeInitializationTasks());

        // Initializes tasks to be executed (in parallel) by a ExecutorService
        try {
            ExecutorService service = ForkJoinPool.commonPool();
            for (Future<Void> i : service.invokeAll(initSequence.parallelInitializationTasks())) {
                i.get();
            }
            // asynchronous initializations to be completed eventually
            initSequence.asynchronousRunnableTasks().forEach(service::submit);
            initSequence.asynchronousCallableTasks().forEach(service::submit);
        } catch (InterruptedException | ExecutionException ex) {
            throw new JosmRuntimeException(ex);
        }

        // Initializes tasks that must be run after parallel tasks
        runInitializationTasks(initSequence.afterInitializationTasks());
    }

    private static void runInitializationTasks(List<InitializationTask> tasks) {
        for (InitializationTask task : tasks) {
            try {
                task.call();
            } catch (JosmRuntimeException e) {
                // Can happen if the current projection needs NTV2 grid which is not available
                // In this case we want the user be able to change his projection
                BugReport.intercept(e).warn();
            }
        }
    }

    /**
     * Closes JOSM and optionally terminates the Java Virtual Machine (JVM).
     * @param exit If {@code true}, the JVM is terminated by running {@link System#exit} with a given return code.
     * @param exitCode The return code
     * @return {@code true}
     * @since 14140
     */
    public static boolean exitJosm(boolean exit, int exitCode) {
        if (shutdownSequence != null) {
            shutdownSequence.run();
        }

        if (exit) {
            System.exit(exitCode);
        }
        return true;
    }
}
