package org.apache.commons.jcs.engine;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.jcs.engine.behavior.ICacheListener;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.jcs.utils.threadpool.ThreadPoolManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An event queue is used to propagate ordered cache events to one and only one target listener.
 * <p>
 * This is a modified version of the experimental version. It uses a PooledExecutor and a
 * BoundedBuffer to queue up events and execute them as threads become available.
 * <p>
 * The PooledExecutor is static, because presumably these processes will be IO bound, so throwing
 * more than a few threads at them will serve no purpose other than to saturate the IO interface. In
 * light of this, having one thread per region seems unnecessary. This may prove to be false.
 */
public class PooledCacheEventQueue<K, V>
    extends AbstractCacheEventQueue<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( PooledCacheEventQueue.class );

    /** The type of event queue */
    private static final QueueType queueType = QueueType.POOLED;

    /** The Thread Pool to execute events with. */
    protected ExecutorService pool = null;

    /** The Thread Pool queue */
    protected BlockingQueue<Runnable> queue = null;

    /**
     * Constructor for the CacheEventQueue object
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     * @param maxFailure
     * @param waitBeforeRetry
     * @param threadPoolName
     */
    public PooledCacheEventQueue( ICacheListener<K, V> listener, long listenerId, String cacheName, int maxFailure,
                                  int waitBeforeRetry, String threadPoolName )
    {
        initialize( listener, listenerId, cacheName, maxFailure, waitBeforeRetry, threadPoolName );
    }

    /**
     * Initializes the queue.
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     * @param maxFailure
     * @param waitBeforeRetry
     * @param threadPoolName
     */
    protected void initialize( ICacheListener<K, V> listener, long listenerId, String cacheName, int maxFailure,
                            int waitBeforeRetry, String threadPoolName )
    {
        super.initialize(listener, listenerId, cacheName, maxFailure, waitBeforeRetry);

        // this will share the same pool with other event queues by default.
        pool = ThreadPoolManager.getInstance().getExecutorService(
                (threadPoolName == null) ? "cache_event_queue" : threadPoolName );
        
        if (pool instanceof ThreadPoolExecutor)
        {
        	queue = ((ThreadPoolExecutor) pool).getQueue();
        }
    }

    /**
     * @return the queue type
     */
    @Override
    public QueueType getQueueType()
    {
        return queueType;
    }

    /**
     * Destroy the queue. Interrupt all threads.
     */
    @Override
    public synchronized void destroy()
    {
        if ( isWorking() )
        {
            setWorking(false);
            pool.shutdownNow();
            if ( log.isInfoEnabled() )
            {
                log.info( "Cache event queue destroyed: " + this );
            }
        }
    }

    /**
     * Adds an event to the queue.
     * <p>
     * @param event
     */
    @Override
    protected void put( AbstractCacheEvent event )
    {
        pool.execute( event );
    }

    /**
     * @return IStats
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "Pooled Cache Event Queue" );

        ArrayList<IStatElement<?>> elems = new ArrayList<>();

        elems.add(new StatElement<>( "Working", Boolean.valueOf(isWorking()) ) );
        elems.add(new StatElement<>( "Empty", Boolean.valueOf(this.isEmpty()) ) );

        if ( queue != null )
        {
            elems.add(new StatElement<>( "Queue Size", Integer.valueOf(queue.size()) ) );
            elems.add(new StatElement<>( "Queue Capacity", Integer.valueOf(queue.remainingCapacity()) ) );
        }

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * If the Queue is using a bounded channel we can determine the size. If it is zero or we can't
     * determine the size, we return true.
     * <p>
     * @return whether or not there are items in the queue
     */
    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * Returns the number of elements in the queue. If the queue cannot determine the size
     * accurately it will return 0.
     * <p>
     * @return number of items in the queue.
     */
    @Override
    public int size()
    {
        if ( queue == null )
        {
            return 0;
        }
        else
        {
            return queue.size();
        }
    }
}
