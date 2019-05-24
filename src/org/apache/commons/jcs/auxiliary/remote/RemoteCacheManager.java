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

import java.io.IOException;
import java.rmi.Naming;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheClient;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.CacheWatchRepairable;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.ZombieCacheWatch;
import org.apache.commons.jcs.engine.behavior.ICacheObserver;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An instance of RemoteCacheManager corresponds to one remote connection of a specific host and
 * port. All RemoteCacheManager instances are monitored by the singleton RemoteCacheMonitor
 * monitoring daemon for error detection and recovery.
 * <p>
 * Getting an instance of the remote cache has the effect of getting a handle on the remote server.
 * Listeners are not registered with the server until a cache is requested from the manager.
 */
public class RemoteCacheManager
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteCacheManager.class );

    /** Contains instances of RemoteCacheNoWait managed by a RemoteCacheManager instance. */
    private final ConcurrentMap<String, RemoteCacheNoWait<?, ?>> caches =
            new ConcurrentHashMap<>();

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

    /** For error notification */
    private final RemoteCacheMonitor monitor;

    /** The service found through lookup */
    private final String registry;

    /** can it be restored */
    private boolean canFix = true;

    /**
     * Constructs an instance to with the given remote connection parameters. If the connection
     * cannot be made, "zombie" services will be temporarily used until a successful re-connection
     * is made by the monitoring daemon.
     * <p>
     * @param cattr cache attributes
     * @param cacheMgr the cache hub
     * @param monitor the cache monitor thread for error notifications
     * @param cacheEventLogger
     * @param elementSerializer
     */
    protected RemoteCacheManager( IRemoteCacheAttributes cattr, ICompositeCacheManager cacheMgr,
                                RemoteCacheMonitor monitor,
                                ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer)
    {
        this.cacheMgr = cacheMgr;
        this.monitor = monitor;
        this.cacheEventLogger = cacheEventLogger;
        this.elementSerializer = elementSerializer;
        this.remoteWatch = new CacheWatchRepairable();

        this.registry = RemoteUtils.getNamingURL(cattr.getRemoteLocation(), cattr.getRemoteServiceName());

        try
        {
            lookupRemoteService();
        }
        catch (IOException e)
        {
            log.error("Could not find server", e);
            // Notify the cache monitor about the error, and kick off the
            // recovery process.
            monitor.notifyError();
        }
    }

    /**
     * Lookup remote service from registry
     * @throws IOException if the remote service could not be found
     *
     */
    protected void lookupRemoteService() throws IOException
    {
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
            this.remoteService = (ICacheServiceNonLocal<?, ?>) obj;
            if ( log.isDebugEnabled() )
            {
                log.debug( "Remote Service = " + remoteService );
            }
            remoteWatch.setCacheWatch( (ICacheObserver) remoteService );
        }
        catch ( Exception ex )
        {
            // Failed to connect to the remote server.
            // Configure this RemoteCacheManager instance to use the "zombie"
            // services.
            this.remoteService = new ZombieCacheServiceNonLocal<>();
            remoteWatch.setCacheWatch( new ZombieCacheWatch() );
            throw new IOException( "Problem finding server at [" + registry + "]", ex );
        }
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

            remoteWatch.addCacheListener( cattr.getCacheName(), listener );
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
     * @throws IOException
     */
    public void removeRemoteCacheListener( IRemoteCacheAttributes cattr )
        throws IOException
    {
        RemoteCacheNoWait<?, ?> cache = caches.get( cattr.getCacheName() );
        if ( cache != null )
        {
        	removeListenerFromCache(cache);
        }
        else
        {
            if ( cattr.isReceive() )
            {
                log.warn( "Trying to deregister Cache Listener that was never registered." );
            }
            else if ( log.isDebugEnabled() )
            {
                log.debug( "Since the remote cache is configured to not receive, "
                    + "there is no listener to deregister." );
            }
        }
    }

    // common helper method
	private void removeListenerFromCache(RemoteCacheNoWait<?, ?> cache) throws IOException
	{
		IRemoteCacheClient<?, ?> rc = cache.getRemoteCache();
		if ( log.isDebugEnabled() )
		{
		    log.debug( "Found cache for [" + cache.getCacheName() + "], deregistering listener." );
		}
		// could also store the listener for a server in the manager.
		IRemoteCacheListener<?, ?> listener = rc.getListener();
        remoteWatch.removeCacheListener( cache.getCacheName(), listener );
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
    @SuppressWarnings("unchecked") // Need to cast because of common map for all caches
    public <K, V> RemoteCacheNoWait<K, V> getCache( IRemoteCacheAttributes cattr )
    {
        RemoteCacheNoWait<K, V> remoteCacheNoWait =
                (RemoteCacheNoWait<K, V>) caches.computeIfAbsent(cattr.getCacheName(), key -> {
                    return newRemoteCacheNoWait(cattr);
                });

        // might want to do some listener sanity checking here.
        return remoteCacheNoWait;
    }

    /**
     * Create new RemoteCacheNoWait instance
     *
     * @param cattr the cache configuration
     * @return the instance
     */
    protected <K, V> RemoteCacheNoWait<K, V> newRemoteCacheNoWait(IRemoteCacheAttributes cattr)
    {
        RemoteCacheNoWait<K, V> remoteCacheNoWait;
        // create a listener first and pass it to the remotecache
        // sender.
        RemoteCacheListener<K, V> listener = null;
        try
        {
            listener = new RemoteCacheListener<>( cattr, cacheMgr, elementSerializer );
            addRemoteCacheListener( cattr, listener );
        }
        catch ( IOException ioe )
        {
            log.error( "Problem adding listener. Message: " + ioe.getMessage()
                + " | RemoteCacheListener = " + listener, ioe );
        }
        catch ( Exception e )
        {
            log.error( "Problem adding listener. Message: " + e.getMessage() + " | RemoteCacheListener = "
                + listener, e );
        }

        IRemoteCacheClient<K, V> remoteCacheClient =
            new RemoteCache<>( cattr, (ICacheServiceNonLocal<K, V>) remoteService, listener, monitor );
        remoteCacheClient.setCacheEventLogger( cacheEventLogger );
        remoteCacheClient.setElementSerializer( elementSerializer );

        remoteCacheNoWait = new RemoteCacheNoWait<>( remoteCacheClient );
        remoteCacheNoWait.setCacheEventLogger( cacheEventLogger );
        remoteCacheNoWait.setElementSerializer( elementSerializer );

        return remoteCacheNoWait;
    }

    /** Shutdown all. */
    public void release()
    {
        for (RemoteCacheNoWait<?, ?> c : caches.values())
        {
            try
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "freeCache [" + c.getCacheName() + "]" );
                }

                removeListenerFromCache(c);
                c.dispose();
            }
            catch ( IOException ex )
            {
                log.error( "Problem releasing " + c.getCacheName(), ex );
            }
        }

        caches.clear();
    }

    /**
     * Fixes up all the caches managed by this cache manager.
     */
    public void fixCaches()
    {
        if ( !canFix )
        {
            return;
        }

        if ( log.isInfoEnabled() )
        {
            log.info( "Fixing caches. ICacheServiceNonLocal " + remoteService + " | IRemoteCacheObserver " + remoteWatch );
        }

        for (RemoteCacheNoWait<?, ?> c : caches.values())
        {
            if (c.getStatus() == CacheStatus.ERROR)
            {
                c.fixCache( remoteService );
            }
        }

        if ( log.isInfoEnabled() )
        {
            String msg = "Remote connection to " + registry + " resumed.";
            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logApplicationEvent( "RemoteCacheManager", "fix", msg );
            }
            log.info( msg );
        }
    }

    /**
     * Returns true if the connection to the remote host can be
     * successfully re-established.
     * <p>
     * @return true if we found a failover server
     */
    public boolean canFixCaches()
    {
        try
        {
            lookupRemoteService();
        }
        catch (IOException e)
        {
            log.error("Could not find server", e);
            canFix = false;
        }

        return canFix;
    }
}
