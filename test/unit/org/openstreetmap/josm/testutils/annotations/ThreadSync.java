// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;

/**
 * Ensure threads are synced between tests. If you need to sync specific threads, use {@link ThreadSyncExtension}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(ThreadSync.ThreadSyncExtension.class)
public @interface ThreadSync {
    /**
     * The extension that actually performs thread syncs.
     */
    class ThreadSyncExtension implements AfterEachCallback {
        private final Duration defaultDuration = Durations.TEN_MINUTES;
        private final List<Consumer<Runnable>> threadExecutors = new ArrayList<>(Arrays.asList(GuiHelper::runInEDTAndWait,
                MainApplication.worker::execute));
        private final List<ForkJoinPool> forkJoinPools = new ArrayList<>(Collections.singletonList(ForkJoinPool.commonPool()));

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            final List<String> deadlockedMessages = threadSync();
            if (!deadlockedMessages.isEmpty()) {
                fail(String.join(System.lineSeparator(), deadlockedMessages));
            }
        }

        /**
         * Register a thread executor (single threaded)
         * @param executor The executor to register
         */
        public void registerThreadExecutor(Consumer<Runnable> executor) {
            this.threadExecutors.add(executor);
        }

        /**
         * Register a ForkJoin pool that should be quiescent on finish
         * @param forkJoinPool The pool
         */
        public void registerForkJoinPool(ForkJoinPool forkJoinPool) {
            this.forkJoinPools.add(forkJoinPool);
        }

        /**
         * Force a thread sync of all registered threads
         * @return A list of potentially deadlocked threads
         */
        public List<String> threadSync() {
            final List<String> deadlockedMessages = new ArrayList<>();
            for (Consumer<Runnable> workerThread : this.threadExecutors) {
                // Sync the thread
                final AtomicBoolean queueEmpty = new AtomicBoolean();
                workerThread.accept(() -> queueEmpty.set(true));
                try {
                    Awaitility.await().atMost(defaultDuration).untilTrue(queueEmpty);
                } catch (ConditionTimeoutException timeoutException) {
                    Logging.trace(timeoutException);
                    final long[] deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
                    Arrays.sort(deadlocked);
                    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                        if (Arrays.binarySearch(deadlocked, entry.getKey().getId()) >= 0 || entry.getKey().getName().contains("main-worker")) {
                            final StringBuilder builder = new StringBuilder();
                            for (StackTraceElement element : entry.getValue()) {
                                builder.append('\t').append(element);
                            }
                            deadlockedMessages.add(MessageFormat.format("Thread {0}-{1} may be deadlocked:\n{2}",
                                    entry.getKey().getId(), entry.getKey().getName(), builder.toString()));
                            entry.getKey().interrupt();
                        }
                    }
                }
            }
            for (ForkJoinPool pool : this.forkJoinPools) {
                assertTrue(pool.awaitQuiescence(defaultDuration.toMillis(), TimeUnit.MILLISECONDS));
            }
            this.forkJoinPools.removeIf(ForkJoinPool::isShutdown);
            return deadlockedMessages;
        }
    }
}
