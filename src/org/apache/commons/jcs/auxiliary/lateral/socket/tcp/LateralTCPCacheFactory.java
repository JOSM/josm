package org.apache.commons.jcs.auxiliary.lateral.socket.tcp;

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

import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheAbstractFactory;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheNoWait;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheNoWaitFacade;
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheListener;
import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.behavior.ITCPLateralCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.behavior.IShutdownObserver;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.discovery.UDPDiscoveryManager;
import org.apache.commons.jcs.utils.discovery.UDPDiscoveryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Constructs a LateralCacheNoWaitFacade for the given configuration. Each lateral service / local
 * relationship is managed by one manager. This manager can have multiple caches. The remote
 * relationships are consolidated and restored via these managers.
 * <p>
 * The facade provides a front to the composite cache so the implementation is transparent.
 */
public class LateralTCPCacheFactory
    extends LateralCacheAbstractFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( LateralTCPCacheFactory.class );

    /** Non singleton manager. Used by this instance of the factory. */
    private LateralTCPDiscoveryListenerManager lateralTCPDiscoveryListenerManager;

    /**
     * Creates a TCP lateral.
     * <p>
     * @param iaca
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return AuxiliaryCache
     */
    @Override
    public <K, V> AuxiliaryCache<K, V> createCache(
            AuxiliaryCacheAttributes iaca, ICompositeCacheManager cacheMgr,
           ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        ITCPLateralCacheAttributes lac = (ITCPLateralCacheAttributes) iaca;
        ArrayList<ICache<K, V>> noWaits = new ArrayList<ICache<K, V>>();

        // pairs up the tcp servers and set the tcpServer value and
        // get the manager and then get the cache
        // no servers are required.
        if ( lac.getTcpServers() != null )
        {
            StringTokenizer it = new StringTokenizer( lac.getTcpServers(), "," );
            if ( log.isDebugEnabled() )
            {
                log.debug( "Configured for [" + it.countTokens() + "]  servers." );
            }
            while ( it.hasMoreElements() )
            {
                String server = (String) it.nextElement();
                if ( log.isDebugEnabled() )
                {
                    log.debug( "tcp server = " + server );
                }
                ITCPLateralCacheAttributes lacC = (ITCPLateralCacheAttributes) lac.copy();
                lacC.setTcpServer( server );
                LateralTCPCacheManager lcm = LateralTCPCacheManager.getInstance( lacC, cacheMgr, cacheEventLogger,
                                                                                 elementSerializer );

                // register for shutdown notification
                cacheMgr.registerShutdownObserver( lcm );

                ICache<K, V> ic = lcm.getCache( lacC.getCacheName() );
                noWaits.add( ic );
            }
        }

        ILateralCacheListener<K, V> listener = createListener( (ILateralCacheAttributes) iaca, cacheMgr );

        // create the no wait facade.
        @SuppressWarnings("unchecked") // No generic arrays in java
        LateralCacheNoWait<K, V>[] lcnwArray = noWaits.toArray( new LateralCacheNoWait[0] );
        LateralCacheNoWaitFacade<K, V> lcnwf =
            new LateralCacheNoWaitFacade<K, V>(listener, lcnwArray, (ILateralCacheAttributes) iaca );

        // create udp discovery if available.
        createDiscoveryService( lac, lcnwf, cacheMgr, cacheEventLogger, elementSerializer );

        return lcnwf;
    }

    /**
     * Makes sure a listener gets created. It will get monitored as soon as it
     * is used.
     * <p>
     * This should be called by create cache.
     * <p>
     * @param lac  ILateralCacheAttributes
     * @param cacheMgr
     *
     * @return the listener if created, else null
     */
    @Override
    public <K, V>
        ILateralCacheListener<K, V> createListener( ILateralCacheAttributes lac, ICompositeCacheManager cacheMgr )
    {
        ITCPLateralCacheAttributes attr = (ITCPLateralCacheAttributes) lac;
        ILateralCacheListener<K, V> listener = null;

        // don't create a listener if we are not receiving.
        if ( attr.isReceive() )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Getting listener for " + lac );
            }

            try
            {
                // make a listener. if one doesn't exist
                listener = LateralTCPListener.getInstance( attr, cacheMgr );

                // register for shutdown notification
                cacheMgr.registerShutdownObserver( (IShutdownObserver) listener );
            }
            catch ( Exception e )
            {
                log.error( "Problem creating lateral listener", e );
            }
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Not creating a listener since we are not receiving." );
            }
        }

        return listener;
    }

    /**
     * Creates the discovery service. Only creates this for tcp laterals right now.
     * <p>
     * @param lac ITCPLateralCacheAttributes
     * @param lcnwf
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return null if none is created.
     */
    private synchronized <K, V> UDPDiscoveryService createDiscoveryService(
            ITCPLateralCacheAttributes lac,
            LateralCacheNoWaitFacade<K, V> lcnwf,
            ICompositeCacheManager cacheMgr,
            ICacheEventLogger cacheEventLogger,
            IElementSerializer elementSerializer )
    {
        UDPDiscoveryService discovery = null;

        // create the UDP discovery for the TCP lateral
        if ( lac.isUdpDiscoveryEnabled() )
        {
            if ( lateralTCPDiscoveryListenerManager == null )
            {
                lateralTCPDiscoveryListenerManager = new LateralTCPDiscoveryListenerManager();
            }

            // One can be used for all regions
            LateralTCPDiscoveryListener discoveryListener = lateralTCPDiscoveryListenerManager
                .getDiscoveryListener( lac, cacheMgr, cacheEventLogger, elementSerializer );

            discoveryListener.addNoWaitFacade( lac.getCacheName(), lcnwf );

            // need a factory for this so it doesn't
            // get dereferenced, also we don't want one for every region.
            discovery = UDPDiscoveryManager.getInstance().getService( lac.getUdpDiscoveryAddr(),
                                                                      lac.getUdpDiscoveryPort(),
                                                                      lac.getTcpListenerPort(), cacheMgr);

            discovery.addParticipatingCacheName( lac.getCacheName() );
            discovery.addDiscoveryListener( discoveryListener );

            if ( log.isInfoEnabled() )
            {
                log.info( "Registered TCP lateral cache [" + lac.getCacheName() + "] with UDPDiscoveryService." );
            }
        }
        return discovery;
    }
}
