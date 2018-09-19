// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.tools.Logging;

/**
 * Queue for ThreadPoolExecutor that implements per-host limit. It will acquire a semaphore for each task
 * and it will set a runnable task with semaphore release, when job has finished.
 * <p>
 * This implementation doesn't guarantee to have at most hostLimit connections per host[1], and it doesn't
 * guarantee that all threads will be busy, when there is work for them[2]. <br>
 * [1] More connection per host may happen, when ThreadPoolExecutor is growing its pool, and thus
 *     tasks do not go through the Queue <br>
 * [2] If we have a queue, and for all hosts in queue we will fail to acquire semaphore, the thread
 *     take the first available job and wait for semaphore. It might be the case, that semaphore was released
 *     for some task further in queue, but this implementation doesn't try to detect such situation
 *
 * @author Wiktor Niesiobędzki
 */
public class HostLimitQueue extends LinkedBlockingDeque<Runnable> {
    private static final long serialVersionUID = 1L;

    private final Map<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();
    private final int hostLimit;

    /**
     * Creates an unbounded queue
     * @param hostLimit how many parallel calls to host to allow
     */
    public HostLimitQueue(int hostLimit) {
        super(); // create unbounded queue
        this.hostLimit = hostLimit;
    }

    /**
     * Creates bounded queue
     * @param hostLimit how many parallel calls to host to allow
     * @param queueLimit how deep the queue should be
     */
    public HostLimitQueue(int hostLimit, int queueLimit) {
        super(queueLimit); // create bounded queue
        this.hostLimit = hostLimit;
    }

    private JCSCachedTileLoaderJob<?, ?> findJob() {
        for (Iterator<Runnable> it = iterator(); it.hasNext();) {
            Runnable r = it.next();
            if (r instanceof JCSCachedTileLoaderJob) {
                JCSCachedTileLoaderJob<?, ?> job = (JCSCachedTileLoaderJob<?, ?>) r;
                if (tryAcquireSemaphore(job)) {
                    if (remove(job)) {
                        return job;
                    } else {
                        // we have acquired the semaphore, but we didn't manage to remove job, as someone else did
                        // release the semaphore and look for another candidate
                        releaseSemaphore(job);
                    }
                } else {
                    URL url = null;
                    try {
                        url = job.getUrl();
                    } catch (IOException e) {
                        Logging.debug(e);
                    }
                    Logging.debug("TMS - Skipping job {0} because host limit reached", url);
                }
            }
        }
        return null;
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        Runnable job = findJob();
        if (job != null) {
            return job;
        }
        job = pollFirst(timeout, unit);
        if (job != null) {
            try {
                boolean gotLock = tryAcquireSemaphore(job, timeout, unit);
                return gotLock ? job : null;
            } catch (InterruptedException e) {
                // acquire my got interrupted, first offer back what was taken
                if (!offer(job)) {
                    Logging.warn("Unable to offer back " + job);
                }
                throw e;
            }
        }
        return job;
    }

    @Override
    public Runnable take() throws InterruptedException {
        Runnable job = findJob();
        if (job != null) {
            return job;
        }
        job = takeFirst();
        try {
            acquireSemaphore(job);
        } catch (InterruptedException e) {
            // acquire my got interrupted, first offer back what was taken
            if (!offer(job)) {
                Logging.warn("Unable to offer back " + job);
            }
            throw e;
        }
        return job;
    }

    private Semaphore getSemaphore(JCSCachedTileLoaderJob<?, ?> job) {
        String host;
        try {
            host = job.getUrl().getHost();
        } catch (IOException e) {
            // do not pass me illegal URL's
            throw new IllegalArgumentException(e);
        }
        Semaphore limit = hostSemaphores.get(host);
        if (limit == null) {
            synchronized (hostSemaphores) {
                limit = hostSemaphores.computeIfAbsent(host, k -> new Semaphore(hostLimit));
            }
        }
        return limit;
    }

    private void acquireSemaphore(Runnable job) throws InterruptedException {
        if (job instanceof JCSCachedTileLoaderJob) {
            final JCSCachedTileLoaderJob<?, ?> jcsJob = (JCSCachedTileLoaderJob<?, ?>) job;
            getSemaphore(jcsJob).acquire();
            jcsJob.setFinishedTask(() -> releaseSemaphore(jcsJob));
        }
    }

    private boolean tryAcquireSemaphore(final JCSCachedTileLoaderJob<?, ?> job) {
        boolean ret = true;
        Semaphore limit = getSemaphore(job);
        if (limit != null) {
            ret = limit.tryAcquire();
            if (ret) {
                job.setFinishedTask(() -> releaseSemaphore(job));
            }
        }
        return ret;
    }

    private boolean tryAcquireSemaphore(Runnable job, long timeout, TimeUnit unit) throws InterruptedException {
        boolean ret = true;
        if (job instanceof JCSCachedTileLoaderJob) {
            final JCSCachedTileLoaderJob<?, ?> jcsJob = (JCSCachedTileLoaderJob<?, ?>) job;
            Semaphore limit = getSemaphore(jcsJob);
            if (limit != null) {
                ret = limit.tryAcquire(timeout, unit);
                if (ret) {
                    jcsJob.setFinishedTask(() -> releaseSemaphore(jcsJob));
                }
            }
        }
        return ret;
    }

    private void releaseSemaphore(JCSCachedTileLoaderJob<?, ?> job) {
        Semaphore limit = getSemaphore(job);
        if (limit != null) {
            limit.release();
            if (limit.availablePermits() > hostLimit) {
                Logging.warn("More permits than it should be");
            }
        }
    }
}
