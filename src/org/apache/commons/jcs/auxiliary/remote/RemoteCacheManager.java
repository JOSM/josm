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
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheClient;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
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
    final ConcurrentMap<String, RemoteCacheNoWait<?, ?>> caches = new ConcurrentHashMap<String, RemoteCacheNoWait<?, ?>>();

    /** Lock for initialization of caches */
    private ReentrantLock cacheLock = new ReentrantLock();

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

    /** For error notification */
    private final RemoteCacheMonitor monitor;

    /** The service found through lookup */
    private final String registry;

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
        this.remoteCacheAttributes = cattr;
        this.cacheMgr = cacheMgr;
        this.monitor = monitor;
        this.cacheEventLogger = cacheEventLogger;
        this.elementSerializer = elementSerializer;

        this.registry = RemoteUtils.getNamingURL(cattr.getRemoteLocation(), cattr.getRemoteServiceName());
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
            monitor.notifyError();
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
     * @param cacheName the name of the cache
     * @param listener the listener to de-register
     * @throws IOException
     */
    public <K, V> void removeRemoteCacheListener( String cacheName, IRemoteCacheListener<K, V> listener )
        throws IOException
    {
        remoteWatch.removeCacheListener( cacheName, listener );
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
    	removeRemoteCacheListener(cattr.getCacheName(), listener);
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

    /**
     * Stops a listener. This is used to deregister a failover after primary reconnection.
     * <p>
     * @param cacheName
     * @throws IOException
     */
    public void removeRemoteCacheListener( String cacheName )
        throws IOException
    {
        RemoteCacheNoWait<?, ?> cache = caches.get( cacheName );
        if ( cache != null )
        {
            removeListenerFromCache(cache);
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
		removeRemoteCacheListener( cache.getCacheName(), listener );
	}

    /**
     * Returns a remote cache for the given cache name.
     * <p>
     * @param cacheName
     * @return The cache value
     */
    public <K, V> RemoteCacheNoWait<K, V> getCache( String cacheName )
    {
        IRemoteCacheAttributes ca = (IRemoteCacheAttributes) remoteCacheAttributes.clone();
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
    @SuppressWarnings("unchecked") // Need to cast because of common map for all caches
    public <K, V> RemoteCacheNoWait<K, V> getCache( IRemoteCacheAttributes cattr )
    {
        RemoteCacheNoWait<K, V> remoteCacheNoWait = (RemoteCacheNoWait<K, V>) caches.get( cattr.getCacheName() );

        if ( remoteCacheNoWait == null )
        {
            cacheLock.lock();

            try
            {
                remoteCacheNoWait = (RemoteCacheNoWait<K, V>) caches.get( cattr.getCacheName() );

                if (remoteCacheNoWait == null)
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
                        log.error( "Problem adding listener. Message: " + ioe.getMessage()
                            + " | RemoteCacheListener = " + listener, ioe );
                    }
                    catch ( Exception e )
                    {
                        log.error( "Problem adding listener. Message: " + e.getMessage() + " | RemoteCacheListener = "
                            + listener, e );
                    }

                    IRemoteCacheClient<K, V> remoteCacheClient =
                        new RemoteCache<K, V>( cattr, (ICacheServiceNonLocal<K, V>) remoteService, listener, monitor );
                    remoteCacheClient.setCacheEventLogger( cacheEventLogger );
                    remoteCacheClient.setElementSerializer( elementSerializer );

                    remoteCacheNoWait = new RemoteCacheNoWait<K, V>( remoteCacheClient );
                    remoteCacheNoWait.setCacheEventLogger( cacheEventLogger );
                    remoteCacheNoWait.setElementSerializer( elementSerializer );

                    caches.put( cattr.getCacheName(), remoteCacheNoWait );
                }
            }
            finally
            {
                cacheLock.unlock();
            }
        }

        // might want to do some listener sanity checking here.

        return remoteCacheNoWait;
    }

    /**
     * Releases the cache.
     * <p>
     * @param name the name of the cache
     * @throws IOException
     */
    public void freeCache( String name )
        throws IOException
    {
    	RemoteCacheNoWait<?, ?> c = caches.remove( name );
        freeCache(c);
    }

    /**
     * Releases the cache.
     * <p>
     * @param cache the cache instance
     * @throws IOException
     */
    public void freeCache( RemoteCacheNoWait<?, ?> cache )
        throws IOException
    {
        if ( cache != null )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "freeCache [" + cache.getCacheName() + "]" );
            }

            removeListenerFromCache(cache);
            cache.dispose();
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
        cacheLock.lock();

        try
        {
            for (RemoteCacheNoWait<?, ?> c : caches.values())
            {
                try
                {
                    freeCache( c );
                }
                catch ( IOException ex )
                {
                    log.error( "Problem releasing " + c.getCacheName(), ex );
                }
            }
        }
        finally
        {
            cacheLock.unlock();
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

        synchronized ( this )
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
     * Get the registry RMI URL
     * @return the registry URL
     */
    public String getRegistryURL()
    {
        return registry;
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
