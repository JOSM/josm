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

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.LateralCache;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheMonitor;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheNoWait;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheNoWaitFacade;
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheListener;
import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.behavior.ITCPLateralCacheAttributes;
import org.apache.commons.jcs.engine.CacheWatchRepairable;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.ZombieCacheWatch;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.behavior.IShutdownObserver;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.discovery.UDPDiscoveryManager;
import org.apache.commons.jcs.utils.discovery.UDPDiscoveryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Constructs a LateralCacheNoWaitFacade for the given configuration. Each lateral service / local
 * relationship is managed by one manager. This manager can have multiple caches. The remote
 * relationships are consolidated and restored via these managers.
 * <p>
 * The facade provides a front to the composite cache so the implementation is transparent.
 */
public class LateralTCPCacheFactory
    extends AbstractAuxiliaryCacheFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( LateralTCPCacheFactory.class );

    /** Address to service map. */
    private ConcurrentHashMap<String, ICacheServiceNonLocal<?, ?>> csnlInstances;

    /** Map of available discovery listener instances, keyed by port. */
    private ConcurrentHashMap<String, LateralTCPDiscoveryListener> lTCPDLInstances;

    /** Monitor thread */
    private LateralCacheMonitor monitor;

    /**
     * Wrapper of the lateral cache watch service; or wrapper of a zombie
     * service if failed to connect.
     */
    private CacheWatchRepairable lateralWatch;

    /**
     * Creates a TCP lateral.
     * <p>
     * @param iaca
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return LateralCacheNoWaitFacade
     */
    @Override
    public <K, V> LateralCacheNoWaitFacade<K, V> createCache(
            AuxiliaryCacheAttributes iaca, ICompositeCacheManager cacheMgr,
           ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        ITCPLateralCacheAttributes lac = (ITCPLateralCacheAttributes) iaca;
        ArrayList<ICache<K, V>> noWaits = new ArrayList<>();

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
                ITCPLateralCacheAttributes lacC = (ITCPLateralCacheAttributes) lac.clone();
                lacC.setTcpServer( server );

                LateralCacheNoWait<K, V> lateralNoWait = createCacheNoWait(lacC, cacheEventLogger, elementSerializer);

                addListenerIfNeeded( lacC, cacheMgr );
                monitor.addCache(lateralNoWait);
                noWaits.add( lateralNoWait );
            }
        }

        ILateralCacheListener<K, V> listener = createListener( lac, cacheMgr );

        // create the no wait facade.
        @SuppressWarnings("unchecked") // No generic arrays in java
        LateralCacheNoWait<K, V>[] lcnwArray = noWaits.toArray( new LateralCacheNoWait[0] );
        LateralCacheNoWaitFacade<K, V> lcnwf =
            new LateralCacheNoWaitFacade<>(listener, lcnwArray, lac );

        // create udp discovery if available.
        createDiscoveryService( lac, lcnwf, cacheMgr, cacheEventLogger, elementSerializer );

        return lcnwf;
    }

    protected <K, V> LateralCacheNoWait<K, V> createCacheNoWait( ITCPLateralCacheAttributes lca,
            ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        ICacheServiceNonLocal<K, V> lateralService = getCSNLInstance(lca);

        LateralCache<K, V> cache = new LateralCache<>( lca, lateralService, this.monitor );
        cache.setCacheEventLogger( cacheEventLogger );
        cache.setElementSerializer( elementSerializer );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Created cache for noWait, cache [" + cache + "]" );
        }

        LateralCacheNoWait<K, V> lateralNoWait = new LateralCacheNoWait<>( cache );
        lateralNoWait.setCacheEventLogger( cacheEventLogger );
        lateralNoWait.setElementSerializer( elementSerializer );

        if ( log.isInfoEnabled() )
        {
            log.info( "Created LateralCacheNoWait for [" + lca + "] LateralCacheNoWait = [" + lateralNoWait
                + "]" );
        }

        return lateralNoWait;
    }

    /**
     * Initialize this factory
     */
    @Override
    public void initialize()
    {
        this.csnlInstances = new ConcurrentHashMap<>();
        this.lTCPDLInstances = new ConcurrentHashMap<>();

        // Create the monitoring daemon thread
        this.monitor = new LateralCacheMonitor(this);
        this.monitor.setDaemon( true );
        this.monitor.start();

        this.lateralWatch = new CacheWatchRepairable();
        this.lateralWatch.setCacheWatch( new ZombieCacheWatch() );
    }

    /**
     * Dispose of this factory, clean up shared resources
     */
    @Override
    public void dispose()
    {
        for (ICacheServiceNonLocal<?, ?> service : this.csnlInstances.values())
        {
            try
            {
                service.dispose("");
            }
            catch (IOException e)
            {
                log.error("Could not dispose service " + service, e);
            }
        }

        this.csnlInstances.clear();

        // TODO: shut down discovery listeners
        this.lTCPDLInstances.clear();

        if (this.monitor != null)
        {
            this.monitor.notifyShutdown();
            try
            {
                this.monitor.join(5000);
            }
            catch (InterruptedException e)
            {
                // swallow
            }
            this.monitor = null;
        }
    }

    /**
     * Returns an instance of the cache service.
     * <p>
     * @param lca configuration for the creation of a new service instance
     *
     * @return ICacheServiceNonLocal&lt;K, V&gt;
     */
    // Need to cast because of common map for all cache services
    @SuppressWarnings("unchecked")
    public <K, V> ICacheServiceNonLocal<K, V> getCSNLInstance( ITCPLateralCacheAttributes lca )
    {
        String key = lca.getTcpServer();

        csnlInstances.computeIfPresent(key, (name, service) -> {
            // If service creation did not succeed last time, force retry
            if (service instanceof ZombieCacheServiceNonLocal)
            {
                log.info("Disposing of zombie service instance for [" + name + "]");
                return null;
            }

            return service;
        });

        ICacheServiceNonLocal<K, V> service =
                (ICacheServiceNonLocal<K, V>) csnlInstances.computeIfAbsent(key, name -> {

                    log.info( "Instance for [" + name + "] is null, creating" );

                    // Create the service
                    try
                    {
                        if ( log.isInfoEnabled() )
                        {
                            log.info( "Creating TCP service, lca = " + lca );
                        }

                        return new LateralTCPService<>( lca );
                    }
                    catch ( IOException ex )
                    {
                        // Failed to connect to the lateral server.
                        // Configure this LateralCacheManager instance to use the
                        // "zombie" services.
                        log.error( "Failure, lateral instance will use zombie service", ex );

                        ICacheServiceNonLocal<K, V> zombieService =
                                new ZombieCacheServiceNonLocal<>( lca.getZombieQueueMaxSize() );

                        // Notify the cache monitor about the error, and kick off
                        // the recovery process.
                        monitor.notifyError();

                        return zombieService;
                    }
                });

        return service;
    }

    /**
     * Gets the instance attribute of the LateralCacheTCPListener class.
     * <p>
     * @param ilca ITCPLateralCacheAttributes
     * @param cacheManager a reference to the global cache manager
     *
     * @return The instance value
     */
    private LateralTCPDiscoveryListener getDiscoveryListener(ITCPLateralCacheAttributes ilca, ICompositeCacheManager cacheManager)
    {
        String key = ilca.getUdpDiscoveryAddr() + ":" + ilca.getUdpDiscoveryPort();

        LateralTCPDiscoveryListener ins = lTCPDLInstances.computeIfAbsent(key, key1 -> {
            if ( log.isInfoEnabled() )
            {
                log.info("Created new discovery listener for " + key1 + " cacheName for request " + ilca.getCacheName());
            }
            return new LateralTCPDiscoveryListener( this.getName(),  cacheManager);
        });

        return ins;
    }

    /**
     * Add listener for receivers
     * <p>
     * @param iaca cache configuration attributes
     * @param cacheMgr the composite cache manager
     */
    private void addListenerIfNeeded( ITCPLateralCacheAttributes iaca, ICompositeCacheManager cacheMgr )
    {
        // don't create a listener if we are not receiving.
        if ( iaca.isReceive() )
        {
            try
            {
                addLateralCacheListener( iaca.getCacheName(), LateralTCPListener.getInstance( iaca, cacheMgr ) );
            }
            catch ( IOException ioe )
            {
                log.error("Problem creating lateral listener", ioe);
            }
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Not creating a listener since we are not receiving." );
            }
        }
    }

    /**
     * Adds the lateral cache listener to the underlying cache-watch service.
     * <p>
     * @param cacheName The feature to be added to the LateralCacheListener attribute
     * @param listener The feature to be added to the LateralCacheListener attribute
     * @throws IOException
     */
    private <K, V> void addLateralCacheListener( String cacheName, ILateralCacheListener<K, V> listener )
        throws IOException
    {
        synchronized ( this.lateralWatch )
        {
            lateralWatch.addCacheListener( cacheName, listener );
        }
    }

    /**
     * Makes sure a listener gets created. It will get monitored as soon as it
     * is used.
     * <p>
     * This should be called by create cache.
     * <p>
     * @param attr  ITCPLateralCacheAttributes
     * @param cacheMgr
     *
     * @return the listener if created, else null
     */
    private <K, V> ILateralCacheListener<K, V> createListener( ITCPLateralCacheAttributes attr,
            ICompositeCacheManager cacheMgr )
    {
        ILateralCacheListener<K, V> listener = null;

        // don't create a listener if we are not receiving.
        if ( attr.isReceive() )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Getting listener for " + attr );
            }

            // make a listener. if one doesn't exist
            listener = LateralTCPListener.getInstance( attr, cacheMgr );

            // register for shutdown notification
            cacheMgr.registerShutdownObserver( (IShutdownObserver) listener );
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
            // One can be used for all regions
            LateralTCPDiscoveryListener discoveryListener = getDiscoveryListener( lac, cacheMgr );
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
