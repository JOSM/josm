package org.apache.commons.jcs.auxiliary;

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

import java.io.Serializable;

import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;

/**
 * This is a nominal interface that auxiliary cache attributes should implement. This allows the
 * auxiliary mangers to share a common interface.
 */
public interface AuxiliaryCacheAttributes
    extends Serializable, Cloneable
{
    /**
     * Sets the name of the cache, referenced by the appropriate manager.
     * <p>
     * @param s The new cacheName value
     */
    void setCacheName( String s );

    /**
     * Gets the cacheName attribute of the AuxiliaryCacheAttributes object
     * <p>
     * @return The cacheName value
     */
    String getCacheName();

    /**
     * Name known by by configurator
     * <p>
     * @param s The new name value
     */
    void setName( String s );

    /**
     * Gets the name attribute of the AuxiliaryCacheAttributes object
     * <p>
     * @return The name value
     */
    String getName();

    /**
     * SINGLE is the default. If you choose POOLED, the value of EventQueuePoolName will be used
     * <p>
     * @param s SINGLE or POOLED
     */
    void setEventQueueType( ICacheEventQueue.QueueType s );

    /**
     * @return SINGLE or POOLED
     */
    ICacheEventQueue.QueueType getEventQueueType();

    /**
     * If you choose a POOLED event queue type, the value of EventQueuePoolName will be used. This
     * is ignored if the pool type is SINGLE
     * <p>
     * @param s SINGLE or POOLED
     */
    void setEventQueuePoolName( String s );

    /**
     * Sets the pool name to use. If a pool is not found by this name, the thread pool manager will
     * return a default configuration.
     * <p>
     * @return name of thread pool to use for this auxiliary
     */
    String getEventQueuePoolName();

    /**
     * Clone object
     */
    AuxiliaryCacheAttributes clone();
}
