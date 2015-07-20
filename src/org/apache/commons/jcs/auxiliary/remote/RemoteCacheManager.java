package org.apache.commons.jcs.auxiliary.remote;

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

import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheClient;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
import org.apache.commons.jcs.engine.CacheWatchRepairable;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.ZombieCacheWatch;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICacheObserver;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.behavior.IShutdownObserver;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

/**
 * An instance of RemoteCacheManager corresponds to one remote connection of a specific host and
 * port. All RemoteCacheManager instances are monitored by the singleton RemoteCacheMonitor
 * monitoring daemon for error detection and recovery.
 * <p>
 * Getting an instance of the remote cache has the effect of getting a handle on the remote server.
 * Listeners are not registered with the server until a cache is requested from the manager.
 */
public class RemoteCacheManager
    implements IShutdownObserver
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteCacheManager.class );

    /** Contains mappings of Location instance to RemoteCacheManager instance. */
    static final Map<Location, RemoteCacheManager> instances = new HashMap<Location, RemoteCacheManager>();

    /** Monitors connections. */
    private static RemoteCacheMonitor monitor;

    /** Not so useful. How many getCaches over releases were called. */
    private int clients;

    /** Contains instances of RemoteCacheNoWait managed by a RemoteCacheManager instance. */
    final Map<String, RemoteCacheNoWait<?, ?>> caches =
        new HashMap<String, RemoteCacheNoWait<?, ?>>();

    /** The remote host */
    final String host;

    /** The remote port */
    final int port;

    /** The service name */
    final String service;

    /** The configuration attributes. */
    private IRemoteCacheAttributes remoteCacheAttributes;

    /** The event logger. */
    private final ICacheEventLogger cacheEventLogger;

    /** The serializer. */
    private final IElementSerializer elementSerializer;

    /** Handle to the remote cache service; or a zombie handle if failed to connect. */
    private ICacheServiceNonLocal<?, ?> remoteService;

    /**
     * Wrapper of the remote cache watch service; or wrapper of a zombie service if failed to
     * connect.
     */
    private CacheWatchRepairable remoteWatch;

    /** The cache manager listeners will need to use to get a cache. */
    private final ICompositeCacheManager cacheMgr;

    /** The service found through lookup */
    //private String registry;

    /**
     * Constructs an instance to with the given remote connection parameters. If the connection
     * cannot be made, "zombie" services will be temporarily used until a successful re-connection
     * is made by the monitoring daemon.
     * <p>
     * @param host
     * @param port
     * @param service
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     */
    private RemoteCacheManager( String host, int port, String service, ICompositeCacheManager cacheMgr,
                                ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        this.host = host;
        this.port = port;
        this.service = service;
        this.cacheMgr = cacheMgr;
        this.cacheEventLogger = cacheEventLogger;
        this.elementSerializer = elementSerializer;

        // register shutdown observer
        this.cacheMgr.registerShutdownObserver( this );

        String registry = RemoteUtils.getNamingURL(host, port, service);
        if ( log.isInfoEnabled() )
        {
            log.info( "Looking up server [" + registry + "]" );
        }
        try
        {
            Object obj = Naming.lookup( registry );
            if ( log.isInfoEnabled() )
            {
                log.info( "Server found: " + obj );
            }

            // Successful connection to the remote server.
            remoteService = (ICacheServiceNonLocal<?, ?>) obj;
            if ( log.isDebugEnabled() )
            {
                log.debug( "remoteService = " + remoteService );
            }

            ICacheObserver remoteObserver = (ICacheObserver) obj;
            remoteWatch = new CacheWatchRepairable();
            remoteWatch.setCacheWatch( remoteObserver );
        }
        catch ( Exception ex )
        {
            // Failed to connect to the remote server.
            // Configure this RemoteCacheManager instance to use the "zombie"
            // services.
            log.error( "Problem finding server at [" + registry + "]", ex );
            remoteService = new ZombieCacheServiceNonLocal<String, String>();
            remoteWatch = new CacheWatchRepairable();
            remoteWatch.setCacheWatch( new ZombieCacheWatch() );

            // Notify the cache monitor about the error, and kick off the
            // recovery process.
            RemoteCacheMonitor.getInstance().notifyError();
        }
    }

    /**
     * Gets the defaultCattr attribute of the RemoteCacheManager object.
     * <p>
     * @return The defaultCattr value
     */
    public IRemoteCacheAttributes getDefaultCattr()
    {
        return this.remoteCacheAttributes;
    }

    /**
     * Adds the remote cache listener to the underlying cache-watch service.
     * <p>
     * @param cattr The feature to be added to the RemoteCacheListener attribute
     * @param listener The feature to be added to the RemoteCacheListener attribute
     * @throws IOException
     */
    public <K, V> void addRemoteCacheListener( IRemoteCacheAttributes cattr, IRemoteCacheListener<K, V> listener )
        throws IOException
    {
        if ( cattr.isReceive() )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "The remote cache is configured to receive events from the remote server.  "
                    + "We will register a listener. remoteWatch = " + remoteWatch + " | IRemoteCacheListener = "
                    + listener + " | cacheName " + cattr.getCacheName() );
            }

            synchronized ( caches )
            {
                remoteWatch.addCacheListener( cattr.getCacheName(), listener );
            }
        }
        else
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "The remote cache is configured to NOT receive events from the remote server.  "
                    + "We will NOT register a listener." );
            }
        }
    }

    /**
     * Removes a listener. When the primary recovers the failover must deregister itself for a
     * region. The failover runner will call this method to de-register. We do not want to deregister
     * all listeners to a remote server, in case a failover is a primary of another region. Having
     * one regions failover act as another servers primary is not currently supported.
     * <p>
     * @param cattr
     * @param listener
     * @throws IOException
     */
    public <K, V> void removeRemoteCacheListener( IRemoteCacheAttributes cattr, IRemoteCacheListener<K, V> listener )
        throws IOException
    {
        synchronized ( caches )
        {
            remoteWatch.removeCacheListener( cattr.getCacheName(), listener );
        }
    }

    /**
     * Stops a listener. This is used to deregister a failover after primary reconnection.
     * <p>
     * @param cattr
     * @throws IOException
     */
    public void removeRemoteCacheListener( IRemoteCacheAttributes cattr )
        throws IOException
    {
        synchronized ( caches )
        {
            RemoteCacheNoWait<?, ?> cache = caches.get( cattr.getCacheName() );
            if ( cache != null )
            {
                IRemoteCacheClient<?, ?> rc = cache.getRemoteCache();
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Found cache for[ " + cattr.getCacheName() + "], deregistering listener." );
                }
                // could also store the listener for a server in the manager.
                IRemoteCacheListener<?, ?> listener = rc.getListener();
                remoteWatch.removeCacheListener( cattr.getCacheName(), listener );
            }
            else
            {
                if ( cattr.isReceive() )
                {
                    log.warn( "Trying to deregister Cache Listener that was never registered." );
                }
                else
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Since the remote cache is configured to not receive, "
                            + "there is no listener to deregister." );
                    }
                }
            }
        }
    }

    /**
     * Stops a listener. This is used to deregister a failover after primary reconnection.
     * <p>
     * @param cacheName
     * @throws IOException
     */
    public void removeRemoteCacheListener( String cacheName )
        throws IOException
    {
        synchronized ( caches )
        {
            RemoteCacheNoWait<?, ?> cache = caches.get( cacheName );
            if ( cache != null )
            {
                IRemoteCacheClient<?, ?> rc = cache.getRemoteCache();
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Found cache for [" + cacheName + "], deregistering listener." );
                }
                // could also store the listener for a server in the manager.
                IRemoteCacheListener<?, ?> listener = rc.getListener();
                remoteWatch.removeCacheListener( cacheName, listener );
            }
        }
    }

    /**
     * Returns an instance of RemoteCacheManager for the given connection parameters.
     * <p>
     * Host and Port uniquely identify a manager instance.
     * <p>
     * Also starts up the monitoring daemon, if not already started.
     * <p>
     * If the connection cannot be established, zombie objects will be used for future recovery
     * purposes.
     * <p>
     * @param cattr
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return The instance value
     */
    public static RemoteCacheManager getInstance( IRemoteCacheAttributes cattr, ICompositeCacheManager cacheMgr,
                                                  ICacheEventLogger cacheEventLogger,
                                                  IElementSerializer elementSerializer )
    {
        String host = cattr.getRemoteHost();
        int port = cattr.getRemotePort();
        String service = cattr.getRemoteServiceName();
        if ( host == null )
        {
            host = "";
        }
        if ( port < 1024 )
        {
            port = Registry.REGISTRY_PORT;
        }
        Location loc = new Location( host, port );

        RemoteCacheManager ins = null;
        synchronized ( instances )
        {
            ins = instances.get( loc );
            if ( ins == null )
            {
                // Change to use cattr and to set defaults
                ins = new RemoteCacheManager( host, port, service, cacheMgr, cacheEventLogger, elementSerializer );
                ins.remoteCacheAttributes = cattr;
                instances.put( loc, ins );
            }
        }

        ins.clients++;
        // Fires up the monitoring daemon.
        if ( monitor == null )
        {
            monitor = RemoteCacheMonitor.getInstance();
            // If the returned monitor is null, it means it's already started
            // elsewhere.
            if ( monitor != null )
            {
                Thread t = new Thread( monitor );
                t.setDaemon( true );
                t.start();
            }
        }
        return ins;
    }

    /**
     * Returns a remote cache for the given cache name.
     * <p>
     * @param cacheName
     * @return The cache value
     */
    public <K, V> RemoteCacheNoWait<K, V> getCache( String cacheName )
    {
        IRemoteCacheAttributes ca = (IRemoteCacheAttributes) remoteCacheAttributes.copy();
        ca.setCacheName( cacheName );
        return getCache( ca );
    }

    /**
     * Gets a RemoteCacheNoWait from the RemoteCacheManager. The RemoteCacheNoWait objects are
     * identified by the cache name value of the RemoteCacheAttributes object.
     * <p>
     * If the client is configured to register a listener, this call results on a listener being
     * created if one isn't already registered with the remote cache for this region.
     * <p>
     * @param cattr
     * @return The cache value
     */
    public <K, V> RemoteCacheNoWait<K, V> getCache( IRemoteCacheAttributes cattr )
    {
        RemoteCacheNoWait<K, V> remoteCacheNoWait = null;

        synchronized ( caches )
        {
            @SuppressWarnings("unchecked") // Need to cast because of common map for all caches
            RemoteCacheNoWait<K, V> remoteCacheNoWait2 = (RemoteCacheNoWait<K, V>) caches.get( cattr.getCacheName() );
            remoteCacheNoWait = remoteCacheNoWait2;
            if ( remoteCacheNoWait == null )
            {
                // create a listener first and pass it to the remotecache
                // sender.
                RemoteCacheListener<K, V> listener = null;
                try
                {
                    listener = new RemoteCacheListener<K, V>( cattr, cacheMgr );
                    addRemoteCacheListener( cattr, listener );
                }
                catch ( IOException ioe )
                {
                    log.error( "IOException. Problem adding listener. Message: " + ioe.getMessage()
                        + " | RemoteCacheListener = " + listener, ioe );
                }
                catch ( Exception e )
                {
                    log.error( "Problem adding listener. Message: " + e.getMessage() + " | RemoteCacheListener = "
                        + listener, e );
                }

                @SuppressWarnings("unchecked") // Need to cast for specialized type
                IRemoteCacheClient<K, V> remoteCacheClient = new RemoteCache<K, V>( cattr, (ICacheServiceNonLocal<K, V>) remoteService, listener );
                remoteCacheClient.setCacheEventLogger( cacheEventLogger );
                remoteCacheClient.setElementSerializer( elementSerializer );

                remoteCacheNoWait = new RemoteCacheNoWait<K, V>( remoteCacheClient );
                remoteCacheNoWait.setCacheEventLogger( cacheEventLogger );
                remoteCacheNoWait.setElementSerializer( elementSerializer );

                caches.put( cattr.getCacheName(), remoteCacheNoWait );
            }

            // might want to do some listener sanity checking here.
        }

        return remoteCacheNoWait;
    }

    /**
     * Releases.
     * <p>
     * @param name
     * @throws IOException
     */
    public void freeCache( String name )
        throws IOException
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "freeCache [" + name + "]" );
        }
        ICache<?, ?> c = null;
        synchronized ( caches )
        {
            c = caches.get( name );
        }
        if ( c != null )
        {
            this.removeRemoteCacheListener( name );
            c.dispose();
        }
    }

    /**
     * Gets the stats attribute of the RemoteCacheManager object
     * <p>
     * @return The stats value
     */
    public String getStats()
    {
        StringBuilder stats = new StringBuilder();
        for (RemoteCacheNoWait<?, ?> c : caches.values())
        {
            if ( c != null )
            {
                stats.append( c.getCacheName() );
            }
        }
        return stats.toString();
    }

    /** Shutdown all. */
    public void release()
    {
        // Wait until called by the last client
        if ( --clients != 0 )
        {
            return;
        }
        synchronized ( caches )
        {
            for (RemoteCacheNoWait<?, ?> c : caches.values())
            {
                if ( c != null )
                {
                    try
                    {
                        // c.dispose();
                        freeCache( c.getCacheName() );
                    }
                    catch ( IOException ex )
                    {
                        log.error( "Problem in release.", ex );
                    }
                }
            }
        }
    }

    /**
     * Fixes up all the caches managed by this cache manager.
     * <p>
     * @param remoteService
     * @param remoteWatch
     */
    public void fixCaches( ICacheServiceNonLocal<?, ?> remoteService, ICacheObserver remoteWatch )
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Fixing caches. ICacheServiceNonLocal " + remoteService + " | IRemoteCacheObserver " + remoteWatch );
        }
        synchronized ( caches )
        {
            this.remoteService = remoteService;
            this.remoteWatch.setCacheWatch( remoteWatch );
            for (RemoteCacheNoWait<?, ?> c : caches.values())
            {
                c.fixCache( remoteService );
            }
        }
    }

    /**
     * Location of the RMI registry.
     */
    private static final class Location
    {
        /** Description of the Field */
        public final String host;

        /** Description of the Field */
        public final int port;

        /**
         * Constructor for the Location object
         * <p>
         * @param host
         * @param port
         */
        public Location( String host, int port )
        {
            this.host = host;
            this.port = port;
        }

        /**
         * @param obj
         * @return true if the host and port are equal
         */
        @Override
        public boolean equals( Object obj )
        {
            if ( obj == this )
            {
                return true;
            }
            if ( obj == null || !( obj instanceof Location ) )
            {
                return false;
            }
            Location l = (Location) obj;
            if ( this.host == null )
            {
                return l.host == null && port == l.port;
            }
            return host.equals( l.host ) && port == l.port;
        }

        /**
         * @return int
         */
        @Override
        public int hashCode()
        {
            return host == null ? port : host.hashCode() ^ port;
        }
    }

    /**
     * Shutdown callback from composite cache manager.
     * <p>
     * @see org.apache.commons.jcs.engine.behavior.IShutdownObserver#shutdown()
     */
    @Override
    public void shutdown()
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Observed shutdown request." );
        }
        release();
    }

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param source
     * @param eventName
     * @param optionalDetails
     */
    protected void logApplicationEvent( String source, String eventName, String optionalDetails )
    {
        if ( cacheEventLogger != null )
        {
            cacheEventLogger.logApplicationEvent( source, eventName, optionalDetails );
        }
    }
}
