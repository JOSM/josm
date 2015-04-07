package org.apache.commons.jcs.auxiliary.disk.block;

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

import org.apache.commons.jcs.auxiliary.disk.AbstractDiskCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache manager for BlockDiskCaches. This manages the instances of the disk cache.
 */
public class BlockDiskCacheManager
    extends AbstractDiskCacheManager
{
    /** The logger */
    private static final Log log = LogFactory.getLog( BlockDiskCacheManager.class );

    /** The singleton instance */
    private static BlockDiskCacheManager instance;

    /** block disks for a region. */
    private final Map<String, BlockDiskCache<?, ?>> caches =
        new ConcurrentHashMap<String, BlockDiskCache<?, ?>>();

    /** Attributes. */
    private final BlockDiskCacheAttributes defaultCacheAttributes;

    /**
     * Constructor for the BlockDiskCacheManager object
     * <p>
     * @param defaultCacheAttributes Default attributes for caches managed by the instance.
     * @param cacheEventLogger
     * @param elementSerializer
     */
    private BlockDiskCacheManager( BlockDiskCacheAttributes defaultCacheAttributes, ICacheEventLogger cacheEventLogger,
                                   IElementSerializer elementSerializer )
    {
        this.defaultCacheAttributes = defaultCacheAttributes;
        setElementSerializer( elementSerializer );
        setCacheEventLogger( cacheEventLogger );
    }

    /**
     * Gets the singleton instance of the manager
     * <p>
     * @param defaultCacheAttributes If the instance has not yet been created, it will be
     *            initialized with this set of default attributes.
     * @param cacheEventLogger
     * @param elementSerializer
     * @return The instance value
     */
    public static BlockDiskCacheManager getInstance( BlockDiskCacheAttributes defaultCacheAttributes,
                                                     ICacheEventLogger cacheEventLogger,
                                                     IElementSerializer elementSerializer )
    {
        synchronized ( BlockDiskCacheManager.class )
        {
            if ( instance == null )
            {
                instance = new BlockDiskCacheManager( defaultCacheAttributes, cacheEventLogger, elementSerializer );
            }
        }
        return instance;
    }

    /**
     * Gets an BlockDiskCache for the supplied name using the default attributes.
     * <p>
     * @param cacheName Name that will be used when creating attributes.
     * @return A cache.
     */
    @Override
    public <K, V> BlockDiskCache<K, V> getCache( String cacheName )
    {
        BlockDiskCacheAttributes cacheAttributes = (BlockDiskCacheAttributes) defaultCacheAttributes.copy();

        cacheAttributes.setCacheName( cacheName );

        return getCache( cacheAttributes );
    }

    /**
     * Get an BlockDiskCache for the supplied attributes. Will provide an existing cache for the
     * name attribute if one has been created, or will create a new cache.
     * <p>
     * @param cacheAttributes Attributes the cache should have.
     * @return A cache, either from the existing set or newly created.
     */
    public <K, V> BlockDiskCache<K, V> getCache( BlockDiskCacheAttributes cacheAttributes )
    {
        BlockDiskCache<K, V> cache = null;

        String cacheName = cacheAttributes.getCacheName();

        log.debug( "Getting cache named: " + cacheName );

        synchronized ( caches )
        {
            // Try to load the cache from the set that have already been
            // created. This only looks at the name attribute.

            @SuppressWarnings("unchecked") // Need to cast because of common map for all caches
            BlockDiskCache<K, V> blockDiskCache = (BlockDiskCache<K, V>) caches.get( cacheName );
            cache = blockDiskCache;

            // If it was not found, create a new one using the supplied
            // attributes
            if ( cache == null )
            {
                cache = new BlockDiskCache<K, V>( cacheAttributes );
                cache.setCacheEventLogger( getCacheEventLogger() );
                cache.setElementSerializer( getElementSerializer() );
                caches.put( cacheName, cache );
            }
        }

        return cache;
    }
}
