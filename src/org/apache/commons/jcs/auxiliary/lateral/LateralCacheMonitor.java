package org.apache.commons.jcs.auxiliary.lateral;

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheMonitor;
import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.LateralTCPCacheFactory;
import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.behavior.ITCPLateralCacheAttributes;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;

/**
 * Used to monitor and repair any failed connection for the lateral cache service. By default the
 * monitor operates in a failure driven mode. That is, it goes into a wait state until there is an
 * error. Upon the notification of a connection error, the monitor changes to operate in a time
 * driven mode. That is, it attempts to recover the connections on a periodic basis. When all failed
 * connections are restored, it changes back to the failure driven mode.
 */
public class LateralCacheMonitor extends AbstractAuxiliaryCacheMonitor
{
    /**
     * Map of caches to monitor
     */
    private ConcurrentHashMap<String, LateralCacheNoWait<?, ?>> caches;

    /**
     * Reference to the factory
     */
    private LateralTCPCacheFactory factory;

    /**
     * Allows close classes, ie testers to set the idle period to something testable.
     * <p>
     * @param idlePeriod
     */
    protected static void forceShortIdlePeriod( long idlePeriod )
    {
        LateralCacheMonitor.idlePeriod = idlePeriod;
    }

    /**
     * Constructor for the LateralCacheMonitor object
     * <p>
     * It's the clients responsibility to decide how many of these there will be.
     *
     * @param factory a reference to the factory that manages the service instances
     */
    public LateralCacheMonitor(LateralTCPCacheFactory factory)
    {
        super("JCS-LateralCacheMonitor");
        this.factory = factory;
        this.caches = new ConcurrentHashMap<>();
        setIdlePeriod(20000L);
    }

    /**
     * Add a cache to be monitored
     *
     * @param cache the cache
     */
    public void addCache(LateralCacheNoWait<?, ?> cache)
    {
        this.caches.put(cache.getCacheName(), cache);

        // if not yet started, go ahead
        if (this.getState() == Thread.State.NEW)
        {
            this.start();
        }
    }

    /**
     * Clean up all resources before shutdown
     */
    @Override
    public void dispose()
    {
        this.caches.clear();
    }

    /**
     * Main processing method for the LateralCacheMonitor object
     */
    @Override
    public void doWork()
    {
        // Monitor each cache instance one after the other.
        log.info( "Number of caches to monitor = " + caches.size() );
        //for
        for (Map.Entry<String, LateralCacheNoWait<?, ?>> entry : caches.entrySet())
        {
            String cacheName = entry.getKey();

            @SuppressWarnings("unchecked") // Downcast to match service
            LateralCacheNoWait<Object, Object> c = (LateralCacheNoWait<Object, Object>) entry.getValue();
            if ( c.getStatus() == CacheStatus.ERROR )
            {
                log.info( "Found LateralCacheNoWait in error, " + cacheName );

                ITCPLateralCacheAttributes lca = (ITCPLateralCacheAttributes)c.getAuxiliaryCacheAttributes();

                // Get service instance
                ICacheServiceNonLocal<Object, Object> cacheService = factory.getCSNLInstance(lca);

                // If we can't fix them, just skip and re-try in the
                // next round.
                if (cacheService instanceof ZombieCacheServiceNonLocal)
                {
                    continue;
                }

                c.fixCache(cacheService);
            }
        }
    }
}
