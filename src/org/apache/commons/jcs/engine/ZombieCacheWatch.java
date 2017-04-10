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

import org.apache.commons.jcs.engine.behavior.ICacheListener;
import org.apache.commons.jcs.engine.behavior.ICacheObserver;
import org.apache.commons.jcs.engine.behavior.IZombie;

/**
 * Zombie Observer.
 */
public class ZombieCacheWatch
    implements ICacheObserver, IZombie
{
    /**
     * Adds a feature to the CacheListener attribute of the ZombieCacheWatch object
     * <p>
     * @param cacheName The feature to be added to the CacheListener attribute
     * @param obj The feature to be added to the CacheListener attribute
     */
    @Override
    public <K, V> void addCacheListener( String cacheName, ICacheListener<K, V> obj )
    {
        // empty
    }

    /**
     * Adds a feature to the CacheListener attribute of the ZombieCacheWatch object
     * <p>
     * @param obj The feature to be added to the CacheListener attribute
     */
    @Override
    public <K, V> void addCacheListener( ICacheListener<K, V> obj )
    {
        // empty
    }

    /**
     * @param cacheName
     * @param obj
     */
    @Override
    public <K, V> void removeCacheListener( String cacheName, ICacheListener<K, V> obj )
    {
        // empty
    }

    /**
     * @param obj
     */
    @Override
    public <K, V> void removeCacheListener( ICacheListener<K, V> obj )
    {
        // empty
    }
}
