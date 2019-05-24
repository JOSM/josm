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

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.RemoteCacheNoWait;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheClient;
import org.apache.commons.jcs.auxiliary.remote.http.client.behavior.IRemoteHttpCacheClient;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.config.OptionConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The RemoteCacheFactory creates remote caches for the cache hub. It returns a no wait facade which
 * is a wrapper around a no wait. The no wait object is either an active connection to a remote
 * cache or a balking zombie if the remote cache is not accessible. It should be transparent to the
 * clients.
 */
public class RemoteHttpCacheFactory
    extends AbstractAuxiliaryCacheFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteHttpCacheFactory.class );

    /** Monitor thread instance */
    private RemoteHttpCacheMonitor monitor;

    /**
     * For LOCAL clients we get a handle to all the failovers, but we do not register a listener
     * with them. We create the RemoteCacheManager, but we do not get a cache.
     * <p>
     * The failover runner will get a cache from the manager. When the primary is restored it will
     * tell the manager for the failover to deregister the listener.
     * <p>
     * @param iaca
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return AuxiliaryCache
     */
    @Override
    public <K, V> AuxiliaryCache<K, V> createCache( AuxiliaryCacheAttributes iaca, ICompositeCacheManager cacheMgr,
                                       ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        RemoteHttpCacheAttributes rca = (RemoteHttpCacheAttributes) iaca;

        // TODO, use the configured value.
        rca.setRemoteType( RemoteType.LOCAL );

        RemoteHttpClientListener<K, V> listener = new RemoteHttpClientListener<>( rca, cacheMgr, elementSerializer );

        IRemoteHttpCacheClient<K, V> remoteService = createRemoteHttpCacheClientForAttributes(rca);

        IRemoteCacheClient<K, V> remoteCacheClient =
                new RemoteHttpCache<>( rca, remoteService, listener, monitor );
        remoteCacheClient.setCacheEventLogger( cacheEventLogger );
        remoteCacheClient.setElementSerializer( elementSerializer );

        RemoteCacheNoWait<K, V> remoteCacheNoWait = new RemoteCacheNoWait<>( remoteCacheClient );
        remoteCacheNoWait.setCacheEventLogger( cacheEventLogger );
        remoteCacheNoWait.setElementSerializer( elementSerializer );

        return remoteCacheNoWait;
    }

    /**
     * This is an extension point. The manager and other classes will only create
     * RemoteHttpCacheClient through this method.

     * @param cattr the cache configuration
     * @return the client instance
     */
    protected <V, K> IRemoteHttpCacheClient<K, V> createRemoteHttpCacheClientForAttributes(RemoteHttpCacheAttributes cattr)
    {
        IRemoteHttpCacheClient<K, V> remoteService = OptionConverter.instantiateByClassName( cattr
                        .getRemoteHttpClientClassName(), null );

        if ( remoteService == null )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Creating the default client for " + cattr.getCacheName());
            }
            remoteService = new RemoteHttpCacheClient<>( );
        }

        remoteService.initialize( cattr );
        return remoteService;
    }

    /**
     * @see org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory#initialize()
     */
    @Override
    public void initialize()
    {
        super.initialize();
        monitor = new RemoteHttpCacheMonitor(this);
        monitor.setDaemon(true);
        monitor.start();
    }

    /**
     * @see org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory#dispose()
     */
    @Override
    public void dispose()
    {
        if (monitor != null)
        {
            monitor.notifyShutdown();
            try
            {
                monitor.join(5000);
            }
            catch (InterruptedException e)
            {
                // swallow
            }
            monitor = null;
        }

        super.dispose();
    }
}
