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

import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;
import org.apache.commons.jcs.engine.behavior.ICacheListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class hands out event Queues. This allows us to change the implementation more easily. You
 * can confugure the cache to use a custom type.
 * <p>
 * @author aaronsm
 */
public class CacheEventQueueFactory<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( CacheEventQueueFactory.class );

    /**
     * The most commonly used factory method.
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     * @param threadPoolName
     * @param poolType - SINGLE, POOLED
     * @return ICacheEventQueue
     */
    public ICacheEventQueue<K, V> createCacheEventQueue( ICacheListener<K, V> listener, long listenerId, String cacheName,
                                                   String threadPoolName, ICacheEventQueue.QueueType poolType )
    {
        return createCacheEventQueue( listener, listenerId, cacheName, 10, 500, threadPoolName, poolType );
    }

    /**
     * Fully configured event queue.
     * <p>
     * @param listener
     * @param listenerId
     * @param cacheName
     * @param maxFailure
     * @param waitBeforeRetry
     * @param threadPoolName null is OK, if not a pooled event queue this is ignored
     * @param poolType single or pooled
     * @return ICacheEventQueue
     */
    public ICacheEventQueue<K, V> createCacheEventQueue( ICacheListener<K, V> listener, long listenerId, String cacheName,
                                                   int maxFailure, int waitBeforeRetry, String threadPoolName,
                                                   ICacheEventQueue.QueueType poolType )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "threadPoolName = [" + threadPoolName + "] poolType = " + poolType + " " );
        }

        ICacheEventQueue<K, V> eventQueue = null;
        if ( poolType == null || ICacheEventQueue.QueueType.SINGLE == poolType )
        {
            eventQueue = new CacheEventQueue<>( listener, listenerId, cacheName, maxFailure, waitBeforeRetry );
        }
        else if ( ICacheEventQueue.QueueType.POOLED == poolType )
        {
            eventQueue = new PooledCacheEventQueue<>( listener, listenerId, cacheName, maxFailure, waitBeforeRetry,
                                                    threadPoolName );
        }

        return eventQueue;
    }
}
