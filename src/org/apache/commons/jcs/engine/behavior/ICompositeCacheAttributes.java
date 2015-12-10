package org.apache.commons.jcs.engine.behavior;

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

/**
 * This defines the minimal behavior for the Cache Configuration settings.
 */
public interface ICompositeCacheAttributes
    extends Serializable, Cloneable
{
    enum DiskUsagePattern
    {
        /** Items will only go to disk when the memory limit is reached. This is the default. */
        SWAP,

        /**
         * Items will go to disk on a normal put. If The disk usage pattern is UPDATE, the swap will be
         * disabled.
         */
        UPDATE
    }

    /**
     * SetMaxObjects is used to set the attribute to determine the maximum
     * number of objects allowed in the memory cache. If the max number of
     * objects or the cache size is set, the default for the one not set is
     * ignored. If both are set, both are used to determine the capacity of the
     * cache, i.e., object will be removed from the cache if either limit is
     * reached. TODO: move to MemoryCache config file.
     * <p>
     * @param size
     *            The new maxObjects value
     */
    void setMaxObjects( int size );

    /**
     * Gets the maxObjects attribute of the ICompositeCacheAttributes object
     * <p>
     * @return The maxObjects value
     */
    int getMaxObjects();

    /**
     * Sets the useDisk attribute of the ICompositeCacheAttributes object
     * <p>
     * @param useDisk
     *            The new useDisk value
     */
    void setUseDisk( boolean useDisk );

    /**
     * Gets the useDisk attribute of the ICompositeCacheAttributes object
     * <p>
     * @return The useDisk value
     */
    boolean isUseDisk();

    /**
     * set whether the cache should use a lateral cache
     * <p>
     * @param d
     *            The new useLateral value
     */
    void setUseLateral( boolean d );

    /**
     * Gets the useLateral attribute of the ICompositeCacheAttributes object
     * <p>
     * @return The useLateral value
     */
    boolean isUseLateral();

    /**
     * Sets whether the cache is remote enabled
     * <p>
     * @param isRemote
     *            The new useRemote value
     */
    void setUseRemote( boolean isRemote );

    /**
     * returns whether the cache is remote enabled
     * <p>
     * @return The useRemote value
     */
    boolean isUseRemote();

    /**
     * Sets the name of the cache, referenced by the appropriate manager.
     * <p>
     * @param s
     *            The new cacheName value
     */
    void setCacheName( String s );

    /**
     * Gets the cacheName attribute of the ICompositeCacheAttributes object
     * <p>
     * @return The cacheName value
     */
    String getCacheName();

    /**
     * Sets the name of the MemoryCache, referenced by the appropriate manager.
     * TODO: create a separate memory cache attribute class.
     * <p>
     * @param s
     *            The new memoryCacheName value
     */
    void setMemoryCacheName( String s );

    /**
     * Gets the memoryCacheName attribute of the ICompositeCacheAttributes
     * object
     * <p>
     * @return The memoryCacheName value
     */
    String getMemoryCacheName();

    /**
     * Whether the memory cache should perform background memory shrinkage.
     * <p>
     * @param useShrinker
     *            The new UseMemoryShrinker value
     */
    void setUseMemoryShrinker( boolean useShrinker );

    /**
     * Whether the memory cache should perform background memory shrinkage.
     * <p>
     * @return The UseMemoryShrinker value
     */
    boolean isUseMemoryShrinker();

    /**
     * If UseMemoryShrinker is true the memory cache should auto-expire elements
     * to reclaim space.
     * <p>
     * @param seconds
     *            The new MaxMemoryIdleTimeSeconds value
     */
    void setMaxMemoryIdleTimeSeconds( long seconds );

    /**
     * If UseMemoryShrinker is true the memory cache should auto-expire elements
     * to reclaim space.
     * <p>
     * @return The MaxMemoryIdleTimeSeconds value
     */
    long getMaxMemoryIdleTimeSeconds();

    /**
     * If UseMemoryShrinker is true the memory cache should auto-expire elements
     * to reclaim space. This sets the shrinker interval.
     * <p>
     * @param seconds
     *            The new ShrinkerIntervalSeconds value
     */
    void setShrinkerIntervalSeconds( long seconds );

    /**
     * If UseMemoryShrinker is true the memory cache should auto-expire elements
     * to reclaim space. This gets the shrinker interval.
     * <p>
     * @return The ShrinkerIntervalSeconds value
     */
    long getShrinkerIntervalSeconds();

    /**
     * If UseMemoryShrinker is true the memory cache should auto-expire elements
     * to reclaim space. This sets the maximum number of items to spool per run.
     * <p>
     * @param maxSpoolPerRun
     *            The new maxSpoolPerRun value
     */
    void setMaxSpoolPerRun( int maxSpoolPerRun );

    /**
     * If UseMemoryShrinker is true the memory cache should auto-expire elements
     * to reclaim space. This gets the maximum number of items to spool per run.
     * <p>
     * @return The maxSpoolPerRun value
     */
    int getMaxSpoolPerRun();

    /**
     * By default this is SWAP_ONLY.
     * <p>
     * @param diskUsagePattern The diskUsagePattern to set.
     */
    void setDiskUsagePattern( DiskUsagePattern diskUsagePattern );

    /**
     * Translates the name to the disk usage pattern short value.
     * <p>
     * The allowed values are SWAP and UPDATE.
     * <p>
     * @param diskUsagePatternName The diskUsagePattern to set.
     */
    void setDiskUsagePatternName( String diskUsagePatternName );

    /**
     * @return Returns the diskUsagePattern.
     */
    DiskUsagePattern getDiskUsagePattern();

    /**
     * Number to send to disk at at time when memory is full.
     * <p>
     * @return int
     */
    int getSpoolChunkSize();

    /**
     * Number to send to disk at a time.
     * <p>
     * @param spoolChunkSize
     */
    void setSpoolChunkSize( int spoolChunkSize );

    /**
     * Clone object
     */
    ICompositeCacheAttributes clone();
}
