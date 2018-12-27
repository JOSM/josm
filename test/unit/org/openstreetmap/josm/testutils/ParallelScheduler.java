/**
 * Copyright 2012-2017 Michael Tamm and other junit-toolbox contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openstreetmap.josm.testutils;

import static java.util.concurrent.ForkJoinTask.inForkJoinPool;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;

import org.junit.runners.model.RunnerScheduler;

/**
 * Encapsulates the singleton {@link ForkJoinPool} used by {@link ParallelParameterized}
 * to execute test classes and test methods concurrently.
 *
 * @author Michael Tamm (junit-toolbox)
 */
class ParallelScheduler implements RunnerScheduler {

    static ForkJoinPool forkJoinPool = setUpForkJoinPool();

    static ForkJoinPool setUpForkJoinPool() {
        Runtime runtime = Runtime.getRuntime();
        int numThreads = Math.max(2, runtime.availableProcessors());
        ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = pool -> {
            if (pool.getPoolSize() >= pool.getParallelism()) {
                return null;
            } else {
                ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                thread.setName("JUnit-" + thread.getName());
                return thread;
            }
        };
        return new ForkJoinPool(numThreads, threadFactory, null, false);
    }

    private final Deque<ForkJoinTask<?>> _asyncTasks = new LinkedList<>();
    private Runnable _lastScheduledChild;

    @Override
    public void schedule(Runnable childStatement) {
        if (_lastScheduledChild != null) {
            // Execute previously scheduled child asynchronously ...
            if (inForkJoinPool()) {
                _asyncTasks.addFirst(ForkJoinTask.adapt(_lastScheduledChild).fork());
            } else {
                _asyncTasks.addFirst(forkJoinPool.submit(_lastScheduledChild));
            }
        }
        // Note: We don't schedule the childStatement immediately here,
        // but remember it, so that we can synchronously execute the
        // last scheduled child in the finished method() -- this way,
        // the current thread does not immediately call join() in the
        // finished() method, which might block it ...
        _lastScheduledChild = childStatement;
    }

    @Override
    public void finished() {
        RuntimeException me = new RuntimeException();
        if (_lastScheduledChild != null) {
            if (inForkJoinPool()) {
                // Execute the last scheduled child in the current thread ...
                try {
                    _lastScheduledChild.run();
                } catch (Throwable t) {
                    me.addSuppressed(t);
                }
            } else {
                // Submit the last scheduled child to the ForkJoinPool too,
                // because all tests should run in the worker threads ...
                _asyncTasks.addFirst(forkJoinPool.submit(_lastScheduledChild));
            }
            // Make sure all asynchronously executed children are done, before we return ...
            for (ForkJoinTask<?> task : _asyncTasks) {
                // Note: Because we have added all tasks via addFirst into _asyncTasks,
                // task.join() is able to steal tasks from other worker threads,
                // if there are tasks, which have not been started yet ...
                // from other worker threads ...
                try {
                    task.join();
                } catch (Throwable t) {
                    me.addSuppressed(t);
                }
            }
            if (me.getSuppressed().length > 0) {
                throw me;
            }
        }
    }
}
