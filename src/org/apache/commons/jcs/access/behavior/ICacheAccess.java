package org.apache.commons.jcs.access.behavior;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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

import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;

/**
 * ICacheAccess defines the behavior for client access.
 */
public interface ICacheAccess<K, V>
    extends ICacheAccessManagement
{
    /**
     * Basic get method.
     * <p>
     * @param name
     * @return Object or null if not found.
     */
    V get(K name);

    /**
     * Basic get method. If the object cannot be found in the cache, it will be
     * retrieved by calling the supplier and subsequently storing it in the cache.
     * <p>
     * @param name
     * @param supplier supplier to be called if the value is not found
     * @return Object.
     */
    V get(K name, Supplier<V> supplier);

    /**
     * Retrieve matching objects from the cache region this instance provides access to.
     * <p>
     * @param pattern - a key pattern for the objects stored
     * @return A map of key to values. These are stripped from the wrapper.
     */
    Map<K, V> getMatching(String pattern);

    /**
     * Puts in cache if an item does not exist with the name in that region.
     * <p>
     * @param name
     * @param obj
     * @throws CacheException
     */
    void putSafe(K name, V obj)
        throws CacheException;

    /**
     * Puts and/or overrides an element with the name in that region.
     * <p>
     * @param name
     * @param obj
     * @throws CacheException
     */
    void put(K name, V obj)
        throws CacheException;

    /**
     * Description of the Method
     * <p>
     * @param name
     * @param obj
     * @param attr
     * @throws CacheException
     */
    void put(K name, V obj, IElementAttributes attr)
        throws CacheException;

    /**
     * This method returns the ICacheElement&lt;K, V&gt; wrapper which provides access to element info and other
     * attributes.
     * <p>
     * This returns a reference to the wrapper. Any modifications will be reflected in the cache. No
     * defensive copy is made.
     * <p>
     * This method is most useful if you want to determine things such as the how long the element
     * has been in the cache.
     * <p>
     * The last access time in the ElementAttributes should be current.
     * <p>
     * @param name Key the object is stored as
     * @return The ICacheElement&lt;K, V&gt; if the object is found or null
     */
    ICacheElement<K, V> getCacheElement(K name);

    /**
     * Get multiple elements from the cache based on a set of cache keys.
     * <p>
     * This method returns the ICacheElement&lt;K, V&gt; wrapper which provides access to element info and other
     * attributes.
     * <p>
     * This returns a reference to the wrapper. Any modifications will be reflected in the cache. No
     * defensive copy is made.
     * <p>
     * This method is most useful if you want to determine things such as the how long the element
     * has been in the cache.
     * <p>
     * The last access time in the ElementAttributes should be current.
     * <p>
     * @param names set of Object cache keys
     * @return a map of Object key to ICacheElement&lt;K, V&gt; element, or empty map if none of the keys are
     *         present
     */
    Map<K, ICacheElement<K, V>> getCacheElements(Set<K> names);

    /**
     * Get multiple elements from the cache based on a set of cache keys.
     * <p>
     * This method returns the ICacheElement&lt;K, V&gt; wrapper which provides access to element info and other
     * attributes.
     * <p>
     * This returns a reference to the wrapper. Any modifications will be reflected in the cache. No
     * defensive copy is made.
     * <p>
     * This method is most useful if you want to determine things such as the how long the element
     * has been in the cache.
     * <p>
     * The last access time in the ElementAttributes should be current.
     * <p>
     * @param pattern key search pattern
     * @return a map of Object key to ICacheElement&lt;K, V&gt; element, or empty map if no keys match the
     *         pattern
     */
    Map<K, ICacheElement<K, V>> getMatchingCacheElements(String pattern);

    /**
     * Remove an object for this key if one exists, else do nothing.
     * <p>
     * @param name
     * @throws CacheException
     */
    void remove(K name)
        throws CacheException;

    /**
     * Reset the attributes on the object matching this key name.
     * <p>
     * @param name
     * @param attributes
     * @throws CacheException
     */
    void resetElementAttributes(K name, IElementAttributes attributes)
        throws CacheException;

    /**
     * Gets the elementAttributes attribute of the ICacheAccess object
     * <p>
     * @param name
     * @return The elementAttributes value
     * @throws CacheException
     */
    IElementAttributes getElementAttributes(K name)
        throws CacheException;
}
