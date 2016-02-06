package org.apache.commons.jcs.engine.memory.behavior;

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

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.stats.behavior.IStats;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/** For the framework. Insures methods a MemoryCache needs to access. */
public interface IMemoryCache<K, V>
{
    /**
     * Initialize the memory cache
     * <p>
     * @param cache The cache (region) this memory store is attached to.
     */
    void initialize( CompositeCache<K, V> cache );

    /**
     * Destroy the memory cache
     * <p>
     * @throws IOException
     */
    void dispose()
        throws IOException;

    /**
     * Get the number of elements contained in the memory store
     * <p>
     * @return Element count
     */
    int getSize();

    /**
     * Returns the historical and statistical data for a region's memory cache.
     * <p>
     * @return Statistics and Info for the Memory Cache.
     */
    IStats getStatistics();

    /**
     * Get a set of the keys for all elements in the memory cache.
     * <p>
     * @return a set of the key type
     * TODO This should probably be done in chunks with a range passed in. This
     *       will be a problem if someone puts a 1,000,000 or so items in a
     *       region.
     */
    Set<K> getKeySet();

    /**
     * Removes an item from the cache
     * <p>
     * @param key
     *            Identifies item to be removed
     * @return Description of the Return Value
     * @throws IOException
     *                Description of the Exception
     */
    boolean remove( K key )
        throws IOException;

    /**
     * Removes all cached items from the cache.
     * <p>
     * @throws IOException
     *                Description of the Exception
     */
    void removeAll()
        throws IOException;

    /**
     * This instructs the memory cache to remove the <i>numberToFree</i>
     * according to its eviction policy. For example, the LRUMemoryCache will
     * remove the <i>numberToFree</i> least recently used items. These will be
     * spooled to disk if a disk auxiliary is available.
     * <p>
     * @param numberToFree
     * @return the number that were removed. if you ask to free 5, but there are
     *         only 3, you will get 3.
     * @throws IOException
     */
    int freeElements( int numberToFree )
        throws IOException;

    /**
     * Get an item from the cache
     * <p>
     * @param key
     *            Description of the Parameter
     * @return Description of the Return Value
     * @throws IOException
     *                Description of the Exception
     */
    ICacheElement<K, V> get( K key )
        throws IOException;

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param keys
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map
     * if there is no data in cache for any of these keys
     * @throws IOException
     */
    Map<K, ICacheElement<K, V>> getMultiple( Set<K> keys )
        throws IOException;

    /**
     * Get an item from the cache without effecting its order or last access
     * time
     * <p>
     * @param key
     *            Description of the Parameter
     * @return The quiet value
     * @throws IOException
     *                Description of the Exception
     */
    ICacheElement<K, V> getQuiet( K key )
        throws IOException;

    /**
     * Spools the item contained in the provided element to disk
     * <p>
     * @param ce
     *            Description of the Parameter
     * @throws IOException
     *                Description of the Exception
     */
    void waterfal( ICacheElement<K, V> ce )
        throws IOException;

    /**
     * Puts an item to the cache.
     * <p>
     * @param ce
     *            Description of the Parameter
     * @throws IOException
     *                Description of the Exception
     */
    void update( ICacheElement<K, V> ce )
        throws IOException;

    /**
     * Returns the CacheAttributes for the region.
     * <p>
     * @return The cacheAttributes value
     */
    ICompositeCacheAttributes getCacheAttributes();

    /**
     * Sets the CacheAttributes of the region.
     * <p>
     * @param cattr
     *            The new cacheAttributes value
     */
    void setCacheAttributes( ICompositeCacheAttributes cattr );

    /**
     * Gets the cache hub / region that uses the MemoryCache.
     * <p>
     * @return The cache value
     */
    CompositeCache<K, V> getCompositeCache();
}
