package org.apache.commons.jcs.engine;

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
import org.apache.commons.jcs.utils.threadpool.PoolConfiguration;
import org.apache.commons.jcs.utils.threadpool.PoolConfiguration.WhenBlockedPolicy;
import org.apache.commons.jcs.utils.threadpool.ThreadPoolManager;

/**
 * An event queue is used to propagate ordered cache events to one and only one target listener.
 */
public class CacheEventQueue<K, V>
    extends PooledCacheEventQueue<K, V>
{
    /** The type of queue -- there are pooled and single */
    private static final QueueType queueType = QueueType.SINGLE;

    /**
     * Constructs with the specified listener and the cache name.
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     */
    public CacheEventQueue( ICacheListener<K, V> listener, long listenerId, String cacheName )
    {
        this( listener, listenerId, cacheName, 10, 500 );
    }

    /**
     * Constructor for the CacheEventQueue object
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     * @param maxFailure
     * @param waitBeforeRetry
     */
    public CacheEventQueue( ICacheListener<K, V> listener, long listenerId, String cacheName, int maxFailure,
                            int waitBeforeRetry )
    {
        super( listener, listenerId, cacheName, maxFailure, waitBeforeRetry, null );
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
    @Override
    protected void initialize( ICacheListener<K, V> listener, long listenerId, String cacheName, int maxFailure,
                            int waitBeforeRetry, String threadPoolName )
    {
        super.initialize(listener, listenerId, cacheName, maxFailure, waitBeforeRetry);

        // create a default pool with one worker thread to mimic the SINGLE queue behavior
        pool = ThreadPoolManager.getInstance().createPool(
        		new PoolConfiguration(false, 0, 1, 0, getWaitToDieMillis(), WhenBlockedPolicy.RUN, 0),
        		"CacheEventQueue.QProcessor-" + getCacheName());
    }

    /**
     * What type of queue is this.
     * <p>
     * @return queueType
     */
    @Override
    public QueueType getQueueType()
    {
        return queueType;
    }
}
