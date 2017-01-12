package org.apache.commons.jcs.engine.behavior;

import java.io.IOException;

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

import org.apache.commons.jcs.engine.stats.behavior.IStats;

/**
 * Interface for a cache event queue. An event queue is used to propagate
 * ordered cache events to one and only one target listener.
 */
public interface ICacheEventQueue<K, V>
{
    enum QueueType
    {
        /** Does not use a thread pool. */
        SINGLE,

        /** Uses a thread pool. */
        POOLED
    }

    /**
     * Return the type of event queue we are using, either single or pooled.
     * <p>
     * @return the queue type: single or pooled
     */
    QueueType getQueueType();

    /**
     * Adds a feature to the PutEvent attribute of the ICacheEventQueue object
     * <p>
     * @param ce
     *            The feature to be added to the PutEvent attribute
     * @throws IOException
     */
    void addPutEvent( ICacheElement<K, V> ce )
        throws IOException;

    /**
     * Adds a feature to the RemoveEvent attribute of the ICacheEventQueue
     * object
     * <p>
     * @param key
     *            The feature to be added to the RemoveEvent attribute
     * @throws IOException
     */
    void addRemoveEvent( K key )
        throws IOException;

    /**
     * Adds a feature to the RemoveAllEvent attribute of the ICacheEventQueue
     * object
     * <p>
     * @throws IOException
     */
    void addRemoveAllEvent()
        throws IOException;

    /**
     * Adds a feature to the DisposeEvent attribute of the ICacheEventQueue
     * object
     * <p>
     * @throws IOException
     */
    void addDisposeEvent()
        throws IOException;

    /**
     * Gets the listenerId attribute of the ICacheEventQueue object
     *
     * @return The listenerId value
     */
    long getListenerId();

    /** Description of the Method */
    void destroy();

    /**
     * Gets the alive attribute of the ICacheEventQueue object. Alive just
     * indicates that there are active threads. This is less important that if
     * the queue is working.
     * <p>
     * @return The alive value
     */
    boolean isAlive();

    /**
     * A Queue is working unless it has reached its max failure count.
     * <p>
     * @return boolean
     */
    boolean isWorking();

    /**
     * Returns the number of elements in the queue.  If the queue cannot
     * determine the size accurately it will return 1.
     * <p>
     * @return number of items in the queue.
     */
    int size();

    /**
     * Are there elements in the queue.
     * <p>
     * @return true if there are stil elements.
     */
    boolean isEmpty();

    /**
     * Returns the historical and statistical data for an event queue cache.
     * <p>
     * @return IStats
     */
    IStats getStatistics();
}
