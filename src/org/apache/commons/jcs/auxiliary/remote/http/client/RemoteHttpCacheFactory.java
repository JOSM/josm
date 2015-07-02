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
import org.apache.commons.jcs.auxiliary.remote.RemoteCacheNoWaitFacade;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The RemoteCacheFactory creates remote caches for the cache hub. It returns a no wait facade which
 * is a wrapper around a no wait. The no wait object is either an active connection to a remote
 * cache or a balking zombie if the remote cache is not accessible. It should be transparent to the
 * clients.
 */
public class RemoteHttpCacheFactory
    extends AbstractAuxiliaryCacheFactory
{
    /** store reference of facades to initiate failover */
    private static final HashMap<String, RemoteCacheNoWaitFacade<?, ?>> facades =
        new HashMap<String, RemoteCacheNoWaitFacade<?, ?>>();

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

        ArrayList<ICache<K, V>> noWaits = new ArrayList<ICache<K, V>>();

        RemoteHttpCacheManager rcm = RemoteHttpCacheManager.getInstance( cacheMgr, cacheEventLogger, elementSerializer );
        // TODO, use the configured value.
        rca.setRemoteType( RemoteType.LOCAL );
        ICache<K, V> ic = rcm.getCache( rca );
        noWaits.add( ic );

        @SuppressWarnings("unchecked") // No generic arrays in java
        RemoteCacheNoWait<K, V>[] rcnwArray = noWaits.toArray( new RemoteCacheNoWait[0] );
        RemoteCacheNoWaitFacade<K, V> rcnwf =
            new RemoteCacheNoWaitFacade<K, V>(rcnwArray, rca, cacheMgr, cacheEventLogger, elementSerializer );

        getFacades().put( rca.getCacheName(), rcnwf );

        return rcnwf;
    }

    /**
     * The facades are what the cache hub talks to.
     * @return Returns the facades.
     */
    public static HashMap<String, RemoteCacheNoWaitFacade<?, ?>> getFacades()
    {
        return facades;
    }
}
