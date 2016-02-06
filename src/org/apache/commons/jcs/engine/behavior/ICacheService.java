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

import org.apache.commons.jcs.access.exception.ObjectExistsException;
import org.apache.commons.jcs.access.exception.ObjectNotFoundException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Used to retrieve and update the cache.
 * <p>
 * Note: server which implements this interface provides a local cache service, whereas server which
 * implements IRmiCacheService provides a remote cache service.
 */
public interface ICacheService<K, V>
{
    /**
     * Puts a cache item to the cache.
     * <p>
     * @param item
     * @throws ObjectExistsException
     * @throws IOException
     */
    void update( ICacheElement<K, V> item )
        throws ObjectExistsException, IOException;

    /**
     * Returns a cache bean from the specified cache; or null if the key does not exist.
     * <p>
     * @param cacheName
     * @param key
     * @return the ICacheElement&lt;K, V&gt; or null if not found
     * @throws ObjectNotFoundException
     * @throws IOException
     */
    ICacheElement<K, V> get( String cacheName, K key )
        throws ObjectNotFoundException, IOException;

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param cacheName
     * @param keys
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     * @throws ObjectNotFoundException
     * @throws IOException
     */
    Map<K, ICacheElement<K, V>> getMultiple( String cacheName, Set<K> keys )
        throws ObjectNotFoundException, IOException;

    /**
     * Gets multiple items from the cache matching the pattern.
     * <p>
     * @param cacheName
     * @param pattern
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache matching the pattern.
     * @throws IOException
     */
    Map<K, ICacheElement<K, V>> getMatching( String cacheName, String pattern )
        throws IOException;

    /**
     * Removes the given key from the specified cache.
     * <p>
     * @param cacheName
     * @param key
     * @throws IOException
     */
    void remove( String cacheName, K key )
        throws IOException;

    /**
     * Remove all keys from the specified cache.
     * @param cacheName
     * @throws IOException
     */
    void removeAll( String cacheName )
        throws IOException;

    /**
     * Frees the specified cache.
     * <p>
     * @param cacheName
     * @throws IOException
     */
    void dispose( String cacheName )
        throws IOException;

    /**
     * Frees all caches.
     * @throws IOException
     */
    void release()
        throws IOException;
}
