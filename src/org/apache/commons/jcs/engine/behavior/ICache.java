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

import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.match.behavior.IKeyMatcher;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * This is the top level interface for all cache like structures. It defines the methods used
 * internally by JCS to access, modify, and instrument such structures.
 * <p>
 * This allows for a suite of reusable components for accessing such structures, for example
 * asynchronous access via an event queue.
 */
public interface ICache<K, V>
    extends ICacheType
{
    /**
     * Puts an item to the cache.
     * <p>
     * @param element
     * @throws IOException
     */
    void update( ICacheElement<K, V> element )
        throws IOException;

    /**
     * Gets an item from the cache.
     * <p>
     * @param key
     * @return a cache element, or null if there is no data in cache for this key
     * @throws IOException
     */
    ICacheElement<K, V> get( K key )
        throws IOException;

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param keys
     * @return a map of K key to ICacheElement<K, V> element, or an empty map if there is no data in cache for any of these keys
     * @throws IOException
     */
    Map<K, ICacheElement<K, V>> getMultiple(Set<K> keys)
        throws IOException;

    /**
     * Gets items from the cache matching the given pattern.  Items from memory will replace those from remote sources.
     * <p>
     * This only works with string keys.  It's too expensive to do a toString on every key.
     * <p>
     * Auxiliaries will do their best to handle simple expressions.  For instance, the JDBC disk cache will convert * to % and . to _
     * <p>
     * @param pattern
     * @return a map of K key to ICacheElement<K, V> element, or an empty map if there is no data matching the pattern.
     * @throws IOException
     */
    Map<K, ICacheElement<K, V>> getMatching(String pattern)
        throws IOException;

    /**
     * Removes an item from the cache.
     * <p>
     * @param key
     * @return false if there was an error in removal
     * @throws IOException
     */
    boolean remove( K key )
        throws IOException;

    /**
     * Removes all cached items from the cache.
     * <p>
     * @throws IOException
     */
    void removeAll()
        throws IOException;

    /**
     * Prepares for shutdown.
     * @throws IOException
     */
    void dispose()
        throws IOException;

    /**
     * Returns the current cache size in number of elements.
     * <p>
     * @return number of elements
     */
    int getSize();

    /**
     * Returns the cache status.
     * <p>
     * @return Alive or Error
     */
    CacheStatus getStatus();

    /**
     * Returns the cache stats.
     * <p>
     * @return String of important historical information.
     */
    String getStats();

    /**
     * Returns the cache name.
     * <p>
     * @return usually the region name.
     */
    String getCacheName();

    /**
     * Sets the key matcher used by get matching.
     * <p>
     * @param keyMatcher
     */
    void setKeyMatcher( IKeyMatcher<K> keyMatcher );
}
