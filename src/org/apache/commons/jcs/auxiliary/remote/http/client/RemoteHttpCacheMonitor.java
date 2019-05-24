package org.apache.commons.jcs.auxiliary.remote.http.client;

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
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheMonitor;
import org.apache.commons.jcs.auxiliary.remote.http.client.behavior.IRemoteHttpCacheClient;
import org.apache.commons.jcs.engine.CacheStatus;

/**
 * Upon the notification of a connection error, the monitor changes to operate in a time driven
 * mode. That is, it attempts to recover the connections on a periodic basis. When all failed
 * connections are restored, it changes back to the failure driven mode.
 */
public class RemoteHttpCacheMonitor extends AbstractAuxiliaryCacheMonitor
{
    /** Set of remote caches to monitor. This are added on error, if not before. */
    private final ConcurrentHashMap<RemoteHttpCache<?, ?>, RemoteHttpCache<?, ?>> remoteHttpCaches;

    /** Factory instance */
    private RemoteHttpCacheFactory factory = null;

    /**
     * Constructor for the RemoteCacheMonitor object
     *
     * @param factory the factory to set
     */
    public RemoteHttpCacheMonitor(RemoteHttpCacheFactory factory)
    {
        super("JCS-RemoteHttpCacheMonitor");
        this.factory = factory;
        this.remoteHttpCaches = new ConcurrentHashMap<>();
        setIdlePeriod(3000L);
    }

    /**
     * Notifies the cache monitor that an error occurred, and kicks off the error recovery process.
     * <p>
     * @param remoteCache
     */
    public void notifyError( RemoteHttpCache<?, ?> remoteCache )
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Notified of an error. " + remoteCache );
        }

        remoteHttpCaches.put( remoteCache, remoteCache );
        notifyError();
    }

    /**
     * Clean up all resources before shutdown
     */
    @Override
    protected void dispose()
    {
        this.remoteHttpCaches.clear();
    }

    // Avoid the use of any synchronization in the process of monitoring for
    // performance reasons.
    // If exception is thrown owing to synchronization,
    // just skip the monitoring until the next round.
    /** Main processing method for the RemoteHttpCacheMonitor object */
    @Override
    protected void doWork()
    {
        // If no factory has been set, skip
        if (factory == null)
        {
            return;
        }

        // If any cache is in error, it strongly suggests all caches
        // managed by the same RmicCacheManager instance are in error. So we fix
        // them once and for all.
        for (RemoteHttpCache<?, ?> remoteCache : this.remoteHttpCaches.values())
        {
            try
            {
                if ( remoteCache.getStatus() == CacheStatus.ERROR )
                {
                    RemoteHttpCacheAttributes attributes = remoteCache.getRemoteHttpCacheAttributes();

                    IRemoteHttpCacheClient<Serializable, Serializable> remoteService =
                            factory.createRemoteHttpCacheClientForAttributes( attributes );

                    if ( log.isInfoEnabled() )
                    {
                        log.info( "Performing Alive check on service " + remoteService );
                    }
                    // If we can't fix them, just skip and re-try in
                    // the next round.
                    if ( remoteService.isAlive() )
                    {
                        remoteCache.fixCache( remoteService );
                    }
                    else
                    {
                        allright.set(false);
                    }
                    break;
                }
            }
            catch ( IOException ex )
            {
                allright.set(false);
                // Problem encountered in fixing the caches managed by a
                // RemoteCacheManager instance.
                // Soldier on to the next RemoteHttpCache.
                log.error( ex );
            }
        }
    }
}
