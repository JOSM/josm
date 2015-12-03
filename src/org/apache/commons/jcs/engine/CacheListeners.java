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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;

/**
 * Used to associates a set of [cache listener to cache event queue] for a
 * cache.
 */
public class CacheListeners<K, V>
{
    /** The cache using the queue. */
    public final ICache<K, V> cache;

    /** Map ICacheListener to ICacheEventQueue */
    public final ConcurrentMap<Long, ICacheEventQueue<K, V>> eventQMap =
        new ConcurrentHashMap<Long, ICacheEventQueue<K, V>>();

    /**
     * Constructs with the given cache.
     * <p>
     * @param cache
     */
    public CacheListeners( ICache<K, V> cache )
    {
        if ( cache == null )
        {
            throw new IllegalArgumentException( "cache must not be null" );
        }
        this.cache = cache;
    }

    /** @return info on the listeners */
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "\n CacheListeners" );
        if ( cache != null )
        {
            buffer.append( "\n Region = " + cache.getCacheName() );
        }
        if ( eventQMap != null )
        {
            buffer.append( "\n Event Queue Map " );
            buffer.append( "\n size = " + eventQMap.size() );
            Iterator<Map.Entry<Long, ICacheEventQueue<K, V>>> it = eventQMap.entrySet().iterator();
            while ( it.hasNext() )
            {
                buffer.append( "\n Entry: " + it.next() );
            }
        }
        else
        {
            buffer.append( "\n No Listeners. " );
        }
        return buffer.toString();
    }
}
