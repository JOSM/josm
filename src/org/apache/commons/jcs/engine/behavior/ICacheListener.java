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

import java.io.IOException;

/**
 * Used to receive a cache event notification.
 * <p>
 * Note: objects which implement this interface are local listeners to cache changes, whereas
 * objects which implement IRmiCacheListener are remote listeners to cache changes.
 */
public interface ICacheListener<K, V>
{
    /**
     * Notifies the subscribers for a cache entry update.
     * <p>
     * @param item
     * @throws IOException
     */
    void handlePut( ICacheElement<K, V> item )
        throws IOException;

    /**
     * Notifies the subscribers for a cache entry removal.
     * <p>
     * @param cacheName
     * @param key
     * @throws IOException
     */
    void handleRemove( String cacheName, K key )
        throws IOException;

    /**
     * Notifies the subscribers for a cache remove-all.
     * <p>
     * @param cacheName
     * @throws IOException
     */
    void handleRemoveAll( String cacheName )
        throws IOException;

    /**
     * Notifies the subscribers for freeing up the named cache.
     * <p>
     * @param cacheName
     * @throws IOException
     */
    void handleDispose( String cacheName )
        throws IOException;

    /**
     * sets unique identifier of listener home
     * <p>
     * @param id The new listenerId value
     * @throws IOException
     */
    void setListenerId( long id )
        throws IOException;

    /**
     * Gets the listenerId attribute of the ICacheListener object
     * <p>
     * @return The listenerId value
     * @throws IOException
     */
    long getListenerId()
        throws IOException;
}
