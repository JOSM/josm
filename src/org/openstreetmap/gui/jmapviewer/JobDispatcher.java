package org.openstreetmap.gui.jmapviewer;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A generic class that processes a list of {@link Runnable} one-by-one using
 * one or more {@link Thread}-instances. The number of instances varies between
 * 1 and {@link #WORKER_THREAD_MAX_COUNT} (default: 8). If an instance is idle
 * more than {@link #WORKER_THREAD_TIMEOUT} seconds (default: 30), the instance
 * ends itself.
 *
 * @author Jan Peter Stotz
 */
public class JobDispatcher {

    private static final JobDispatcher instance = new JobDispatcher();

    /**
     * @return the singelton instance of the {@link JobDispatcher}
     */
    public static JobDispatcher getInstance() {
        return instance;
    }

    private JobDispatcher() {
        addWorkerThread().firstThread = true;
    }

    protected BlockingQueue<Runnable> jobQueue = new LinkedBlockingQueue<Runnable>();

    public static int WORKER_THREAD_MAX_COUNT = 8;

    /**
     * Specifies the time span in seconds that a worker thread waits for new
     * jobs to perform. If the time span has elapsed the worker thread
     * terminates itself. Only the first worker thread works differently, it
     * ignores the timeout and will never terminate itself.
     */
    public static int WORKER_THREAD_TIMEOUT = 30;

    /**
     * Total number of worker threads currently idle or active
     */
    protected int workerThreadCount = 0;

    /**
     * Number of worker threads currently idle
     */
    protected int workerThreadIdleCount = 0;

    /**
     * Just an id for identifying an worker thread instance
     */
    protected int workerThreadId = 0;

    /**
     * Removes all jobs from the queue that are currently not being processed.
     */
    public void cancelOutstandingJobs() {
        jobQueue.clear();
    }

    public void addJob(Runnable job) {
        try {
            jobQueue.put(job);
            if (workerThreadIdleCount == 0 && workerThreadCount < WORKER_THREAD_MAX_COUNT)
                addWorkerThread();
        } catch (InterruptedException e) {
        }
    }

    protected JobThread addWorkerThread() {
        JobThread jobThread = new JobThread(++workerThreadId);
        synchronized (this) {
            workerThreadCount++;
        }
        jobThread.start();
        return jobThread;
    }

    public class JobThread extends Thread {

        Runnable job;
        boolean firstThread = false;

        public JobThread(int threadId) {
            super("OSMJobThread " + threadId);
            setDaemon(true);
            job = null;
        }

        @Override
        public void run() {
            executeJobs();
            synchronized (instance) {
                workerThreadCount--;
            }
        }

        protected void executeJobs() {
            while (!isInterrupted()) {
                try {
                    synchronized (instance) {
                        workerThreadIdleCount++;
                    }
                    if (firstThread)
                        job = jobQueue.take();
                    else
                        job = jobQueue.poll(WORKER_THREAD_TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e1) {
                    return;
                } finally {
                    synchronized (instance) {
                        workerThreadIdleCount--;
                    }
                }
                if (job == null)
                    return;
                try {
                    job.run();
                    job = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
