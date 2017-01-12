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

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheService;
import org.apache.commons.jcs.engine.behavior.IZombie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Zombie adapter for any cache service. Balks at every call.
 */
public class ZombieCacheService<K, V>
    implements ICacheService<K, V>, IZombie
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( ZombieCacheService.class );

    /**
     * @param item
     */
    public void put( ICacheElement<K, V> item )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Zombie put for item " + item );
        }
        // zombies have no inner life
    }

    /**
     * Does nothing.
     * <p>
     * @param item
     */
    @Override
    public void update( ICacheElement<K, V> item )
    {
        // zombies have no inner life
    }

    /**
     * @param cacheName
     * @param key
     * @return null. zombies have no internal data
     */
    @Override
    public ICacheElement<K, V> get( String cacheName, K key )
    {
        return null;
    }

    /**
     * Returns an empty map. Zombies have no internal data.
     * <p>
     * @param cacheName
     * @param keys
     * @return Collections.EMPTY_MAP
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMultiple( String cacheName, Set<K> keys )
    {
        return Collections.emptyMap();
    }

    /**
     * Returns an empty map. Zombies have no internal data.
     * <p>
     * @param cacheName
     * @param pattern
     * @return Collections.EMPTY_MAP
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatching( String cacheName, String pattern )
    {
        return Collections.emptyMap();
    }

    /**
     * Logs the get to debug, but always balks.
     * <p>
     * @param cacheName
     * @param key
     * @param container
     * @return null always
     */
    public Serializable get( String cacheName, K key, boolean container )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Zombie get for key [" + key + "] cacheName [" + cacheName + "] container [" + container + "]" );
        }
        // zombies have no inner life
        return null;
    }

    /**
     * @param cacheName
     * @param key
     */
    @Override
    public void remove( String cacheName, K key )
    {
        // zombies have no inner life
    }

    /**
     * @param cacheName
     */
    @Override
    public void removeAll( String cacheName )
    {
        // zombies have no inner life
    }

    /**
     * @param cacheName
     */
    @Override
    public void dispose( String cacheName )
    {
        // zombies have no inner life
    }

    /**
     * Frees all caches.
     */
    @Override
    public void release()
    {
        // zombies have no inner life
    }
}
