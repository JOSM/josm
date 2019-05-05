package org.apache.commons.jcs.auxiliary.remote.server;

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
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.IRemoteCacheServer;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.IRemoteCacheServerAttributes;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.CacheEventQueueFactory;
import org.apache.commons.jcs.engine.CacheListeners;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;
import org.apache.commons.jcs.engine.behavior.ICacheListener;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;
import org.apache.commons.jcs.engine.logging.CacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides remote cache services. The remote cache server propagates events from local
 * caches to other local caches. It can also store cached data, making it available to new clients.
 * <p>
 * Remote cache servers can be clustered. If the cache used by this remote cache is configured to
 * use a remote cache of type cluster, the two remote caches will communicate with each other.
 * Remote and put requests can be sent from one remote to another. If they are configured to
 * broadcast such event to their client, then remove an puts can be sent to all locals in the
 * cluster.
 * <p>
 * Get requests are made between clustered servers if AllowClusterGet is true. You can setup several
 * clients to use one remote server and several to use another. The get local will be distributed
 * between the two servers. Since caches are usually high get and low put, this should allow you to
 * scale.
 */
public class RemoteCacheServer<K, V>
    extends UnicastRemoteObject
    implements IRemoteCacheServer<K, V>, Unreferenced
{
    public static final String DFEAULT_REMOTE_CONFIGURATION_FILE = "/remote.cache.ccf";

    /** For serialization. Don't change. */
    private static final long serialVersionUID = -8072345435941473116L;

    /** log instance */
    private static final Log log = LogFactory.getLog( RemoteCacheServer.class );

    /** timing -- if we should record operation times. */
    private static final boolean timing = true;

    /** Number of puts into the cache. */
    private int puts = 0;

    /** Maps cache name to CacheListeners object. association of listeners (regions). */
    private final transient ConcurrentMap<String, CacheListeners<K, V>> cacheListenersMap =
        new ConcurrentHashMap<String, CacheListeners<K, V>>();

    /** maps cluster listeners to regions. */
    private final transient ConcurrentMap<String, CacheListeners<K, V>> clusterListenersMap =
        new ConcurrentHashMap<String, CacheListeners<K, V>>();

    /** The central hub */
    private transient CompositeCacheManager cacheManager;

    /** relates listener id with a type */
    private final ConcurrentMap<Long, RemoteType> idTypeMap = new ConcurrentHashMap<Long, RemoteType>();

    /** relates listener id with an ip address */
    private final ConcurrentMap<Long, String> idIPMap = new ConcurrentHashMap<Long, String>();

    /** Used to get the next listener id. */
    private final int[] listenerId = new int[1];

    /** Configuration settings. */
    // package protected for access by unit test code
    final IRemoteCacheServerAttributes remoteCacheServerAttributes;

    /** The interval at which we will log updates. */
    private final int logInterval = 100;

    /** An optional event logger */
    private transient ICacheEventLogger cacheEventLogger;

    /**
     * Constructor for the RemoteCacheServer object. This initializes the server with the values
     * from the properties object.
     * <p>
     * @param rcsa
     * @param config cache hub configuration
     * @throws RemoteException
     */
    protected RemoteCacheServer( IRemoteCacheServerAttributes rcsa, Properties config )
        throws RemoteException
    {
        super( rcsa.getServicePort() );
        this.remoteCacheServerAttributes = rcsa;
        init( config );
    }

    /**
     * Constructor for the RemoteCacheServer object. This initializes the server with the values
     * from the properties object.
     * <p>
     * @param rcsa
     * @param config cache hub configuration
     * @param customRMISocketFactory
     * @throws RemoteException
     */
    protected RemoteCacheServer( IRemoteCacheServerAttributes rcsa, Properties config, RMISocketFactory customRMISocketFactory )
        throws RemoteException
    {
        super( rcsa.getServicePort(), customRMISocketFactory, customRMISocketFactory );
        this.remoteCacheServerAttributes = rcsa;
        init( config );
    }

    /**
     * Initialize the RMI Cache Server from a properties object.
     * <p>
     * @param prop the configuration properties
     * @throws RemoteException if the configuration of the cache manager instance fails
     */
    private void init( Properties prop ) throws RemoteException
    {
        try
        {
            cacheManager = createCacheManager( prop );
        }
        catch (CacheException e)
        {
            throw new RemoteException(e.getMessage(), e);
        }

        // cacheManager would have created a number of ICache objects.
        // Use these objects to set up the cacheListenersMap.
        String[] list = cacheManager.getCacheNames();
        for ( int i = 0; i < list.length; i++ )
        {
            String name = list[i];
            CompositeCache<K, V> cache = cacheManager.getCache( name );
            cacheListenersMap.put( name, new CacheListeners<K, V>( cache ) );
        }
    }

    /**
     * Subclass can override this method to create the specific cache manager.
     * <p>
     * @param prop the configuration object.
     * @return The cache hub configured with this configuration.
     *
     * @throws CacheException if the configuration cannot be loaded
     */
    private CompositeCacheManager createCacheManager( Properties prop ) throws CacheException
    {
        CompositeCacheManager hub = CompositeCacheManager.getUnconfiguredInstance();
        hub.configure( prop );
        return hub;
    }

    /**
     * Puts a cache bean to the remote cache and notifies all listeners which <br>
     * <ol>
     * <li>have a different listener id than the originating host;</li>
     * <li>are currently subscribed to the related cache.</li>
     * </ol>
     * <p>
     * @param item
     * @throws IOException
     */
    public void put( ICacheElement<K, V> item )
        throws IOException
    {
        update( item );
    }

    /**
     * @param item
     * @throws IOException
     */
    @Override
    public void update( ICacheElement<K, V> item )
        throws IOException
    {
        update( item, 0 );
    }

    /**
     * The internal processing is wrapped in event logging calls.
     * <p>
     * @param item
     * @param requesterId
     * @throws IOException
     */
    @Override
    public void update( ICacheElement<K, V> item, long requesterId )
        throws IOException
    {
        ICacheEvent<ICacheElement<K, V>> cacheEvent = createICacheEvent( item, requesterId, ICacheEventLogger.UPDATE_EVENT );
        try
        {
            processUpdate( item, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * An update can come from either a local cache's remote auxiliary, or it can come from a remote
     * server. A remote server is considered a a source of type cluster.
     * <p>
     * If the update came from a cluster, then we should tell the cache manager that this was a
     * remote put. This way, any lateral and remote auxiliaries configured for the region will not
     * be updated. This is basically how a remote listener works when plugged into a local cache.
     * <p>
     * If the cluster is configured to keep local cluster consistency, then all listeners will be
     * updated. This allows cluster server A to update cluster server B and then B to update its
     * clients if it is told to keep local cluster consistency. Otherwise, server A will update
     * server B and B will not tell its clients. If you cluster using lateral caches for instance,
     * this is how it will work. Updates to a cluster node, will never get to the leaves. The remote
     * cluster, with local cluster consistency, allows you to update leaves. This basically allows
     * you to have a failover remote server.
     * <p>
     * Since currently a cluster will not try to get from other cluster servers, you can scale a bit
     * with a cluster configuration. Puts and removes will be broadcasted to all clients, but the
     * get load on a remote server can be reduced.
     * <p>
     * @param item
     * @param requesterId
     */
    private void processUpdate( ICacheElement<K, V> item, long requesterId )
    {
        long start = 0;
        if ( timing )
        {
            start = System.currentTimeMillis();
        }

        logUpdateInfo( item );

        try
        {
            CacheListeners<K, V> cacheDesc = getCacheListeners( item.getCacheName() );
            /* Object val = */item.getVal();

            boolean fromCluster = isRequestFromCluster( requesterId );

            if ( log.isDebugEnabled() )
            {
                log.debug( "In update, requesterId = [" + requesterId + "] fromCluster = " + fromCluster );
            }

            // ordered cache item update and notification.
            synchronized ( cacheDesc )
            {
                try
                {
                    CompositeCache<K, V> c = (CompositeCache<K, V>) cacheDesc.cache;

                    // If the source of this request was not from a cluster,
                    // then consider it a local update. The cache manager will
                    // try to
                    // update all auxiliaries.
                    //
                    // This requires that two local caches not be connected to
                    // two clustered remote caches. The failover runner will
                    // have to make sure of this. ALos, the local cache needs
                    // avoid updating this source. Will need to pass the source
                    // id somehow. The remote cache should update all local
                    // caches
                    // but not update the cluster source. Cluster remote caches
                    // should only be updated by the server and not the
                    // RemoteCache.
                    if ( fromCluster )
                    {
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Put FROM cluster, NOT updating other auxiliaries for region. "
                                + " requesterId [" + requesterId + "]" );
                        }
                        c.localUpdate( item );
                    }
                    else
                    {
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Put NOT from cluster, updating other auxiliaries for region. "
                                + " requesterId [" + requesterId + "]" );
                        }
                        c.update( item );
                    }
                }
                catch ( Exception ce )
                {
                    // swallow
                    if ( log.isInfoEnabled() )
                    {
                        log.info( "Exception caught updating item. requesterId [" + requesterId + "] "
                            + ce.getMessage() );
                    }
                }

                // UPDATE LOCALS IF A REQUEST COMES FROM A CLUSTER
                // IF LOCAL CLUSTER CONSISTENCY IS CONFIGURED
                if ( !fromCluster || ( fromCluster && remoteCacheServerAttributes.isLocalClusterConsistency() ) )
                {
                    ICacheEventQueue<K, V>[] qlist = getEventQList( cacheDesc, requesterId );
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "qlist.length = " + qlist.length );
                    }
                    for ( int i = 0; i < qlist.length; i++ )
                    {
                        qlist[i].addPutEvent( item );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logError( "RemoteCacheServer", ICacheEventLogger.UPDATE_EVENT, e.getMessage()
                    + " REGION: " + item.getCacheName() + " ITEM: " + item );
            }

            log.error( "Trouble in Update. requesterId [" + requesterId + "]", e );
        }

        // TODO use JAMON for timing
        if ( timing )
        {
            long end = System.currentTimeMillis();
            if ( log.isDebugEnabled() )
            {
                log.debug( "put took " + String.valueOf( end - start ) + " ms." );
            }
        }
    }

    /**
     * Log some details.
     * <p>
     * @param item
     */
    private void logUpdateInfo( ICacheElement<K, V> item )
    {
        // not thread safe, but it doesn't have to be 100% accurate
        puts++;

        if ( log.isInfoEnabled() )
        {
            if ( puts % logInterval == 0 )
            {
                log.info( "puts = " + puts );
            }
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "In update, put [" + item.getKey() + "] in [" + item.getCacheName() + "]" );
        }
    }

    /**
     * Returns a cache value from the specified remote cache; or null if the cache or key does not
     * exist.
     * <p>
     * @param cacheName
     * @param key
     * @return ICacheElement
     * @throws IOException
     */
    @Override
    public ICacheElement<K, V> get( String cacheName, K key )
        throws IOException
    {
        return this.get( cacheName, key, 0 );
    }

    /**
     * Returns a cache bean from the specified cache; or null if the key does not exist.
     * <p>
     * Adding the requestor id, allows the cache to determine the source of the get.
     * <p>
     * The internal processing is wrapped in event logging calls.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @return ICacheElement
     * @throws IOException
     */
    @Override
    public ICacheElement<K, V> get( String cacheName, K key, long requesterId )
        throws IOException
    {
        ICacheElement<K, V> element = null;
        ICacheEvent<K> cacheEvent = createICacheEvent( cacheName, key, requesterId, ICacheEventLogger.GET_EVENT );
        try
        {
            element = processGet( cacheName, key, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
        return element;
    }

    /**
     * Returns a cache bean from the specified cache; or null if the key does not exist.
     * <p>
     * Adding the requester id, allows the cache to determine the source of the get.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @return ICacheElement
     */
    private ICacheElement<K, V> processGet( String cacheName, K key, long requesterId )
    {
        boolean fromCluster = isRequestFromCluster( requesterId );

        if ( log.isDebugEnabled() )
        {
            log.debug( "get [" + key + "] from cache [" + cacheName + "] requesterId = [" + requesterId
                + "] fromCluster = " + fromCluster );
        }

        CacheListeners<K, V> cacheDesc = null;
        try
        {
            cacheDesc = getCacheListeners( cacheName );
        }
        catch ( Exception e )
        {
            log.error( "Problem getting listeners.", e );

            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logError( "RemoteCacheServer", ICacheEventLogger.GET_EVENT, e.getMessage() + cacheName
                    + " KEY: " + key );
            }
        }

        ICacheElement<K, V> element = getFromCacheListeners( key, fromCluster, cacheDesc, null );
        return element;
    }

    /**
     * Gets the item from the associated cache listeners.
     * <p>
     * @param key
     * @param fromCluster
     * @param cacheDesc
     * @param element
     * @return ICacheElement
     */
    private ICacheElement<K, V> getFromCacheListeners( K key, boolean fromCluster, CacheListeners<K, V> cacheDesc,
                                                 ICacheElement<K, V> element )
    {
        ICacheElement<K, V> returnElement = element;

        if ( cacheDesc != null )
        {
            CompositeCache<K, V> c = (CompositeCache<K, V>) cacheDesc.cache;

            // If we have a get come in from a client and we don't have the item
            // locally, we will allow the cache to look in other non local sources,
            // such as a remote cache or a lateral.
            //
            // Since remote servers never get from clients and clients never go
            // remote from a remote call, this
            // will not result in any loops.
            //
            // This is the only instance I can think of where we allow a remote get
            // from a remote call. The purpose is to allow remote cache servers to
            // talk to each other. If one goes down, you want it to be able to get
            // data from those that were up when the failed server comes back o
            // line.

            if ( !fromCluster && this.remoteCacheServerAttributes.isAllowClusterGet() )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "NonLocalGet. fromCluster [" + fromCluster + "] AllowClusterGet ["
                        + this.remoteCacheServerAttributes.isAllowClusterGet() + "]" );
                }
                returnElement = c.get( key );
            }
            else
            {
                // Gets from cluster type remote will end up here.
                // Gets from all clients will end up here if allow cluster get is
                // false.

                if ( log.isDebugEnabled() )
                {
                    log.debug( "LocalGet.  fromCluster [" + fromCluster + "] AllowClusterGet ["
                        + this.remoteCacheServerAttributes.isAllowClusterGet() + "]" );
                }
                returnElement = c.localGet( key );
            }
        }

        return returnElement;
    }

    /**
     * Gets all matching items.
     * <p>
     * @param cacheName
     * @param pattern
     * @return Map of keys and wrapped objects
     * @throws IOException
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatching( String cacheName, String pattern )
        throws IOException
    {
        return getMatching( cacheName, pattern, 0 );
    }

    /**
     * Retrieves all matching keys.
     * <p>
     * @param cacheName
     * @param pattern
     * @param requesterId
     * @return Map of keys and wrapped objects
     * @throws IOException
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatching( String cacheName, String pattern, long requesterId )
        throws IOException
    {
        ICacheEvent<String> cacheEvent = createICacheEvent( cacheName, pattern, requesterId,
                                                    ICacheEventLogger.GETMATCHING_EVENT );
        try
        {
            return processGetMatching( cacheName, pattern, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * Retrieves all matching keys.
     * <p>
     * @param cacheName
     * @param pattern
     * @param requesterId
     * @return Map of keys and wrapped objects
     */
    protected Map<K, ICacheElement<K, V>> processGetMatching( String cacheName, String pattern, long requesterId )
    {
        boolean fromCluster = isRequestFromCluster( requesterId );

        if ( log.isDebugEnabled() )
        {
            log.debug( "getMatching [" + pattern + "] from cache [" + cacheName + "] requesterId = [" + requesterId
                + "] fromCluster = " + fromCluster );
        }

        CacheListeners<K, V> cacheDesc = null;
        try
        {
            cacheDesc = getCacheListeners( cacheName );
        }
        catch ( Exception e )
        {
            log.error( "Problem getting listeners.", e );

            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logError( "RemoteCacheServer", ICacheEventLogger.GETMATCHING_EVENT, e.getMessage()
                    + cacheName + " pattern: " + pattern );
            }
        }

        return getMatchingFromCacheListeners( pattern, fromCluster, cacheDesc );
    }

    /**
     * Gets the item from the associated cache listeners.
     * <p>
     * @param pattern
     * @param fromCluster
     * @param cacheDesc
     * @return Map of keys to results
     */
    private Map<K, ICacheElement<K, V>> getMatchingFromCacheListeners( String pattern, boolean fromCluster, CacheListeners<K, V> cacheDesc )
    {
        Map<K, ICacheElement<K, V>> elements = null;
        if ( cacheDesc != null )
        {
            CompositeCache<K, V> c = (CompositeCache<K, V>) cacheDesc.cache;

            // We always want to go remote and then merge the items.  But this can lead to inconsistencies after
            // failover recovery.  Removed items may show up.  There is no good way to prevent this.
            // We should make it configurable.

            if ( !fromCluster && this.remoteCacheServerAttributes.isAllowClusterGet() )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "NonLocalGetMatching. fromCluster [" + fromCluster + "] AllowClusterGet ["
                        + this.remoteCacheServerAttributes.isAllowClusterGet() + "]" );
                }
                elements = c.getMatching( pattern );
            }
            else
            {
                // Gets from cluster type remote will end up here.
                // Gets from all clients will end up here if allow cluster get is
                // false.

                if ( log.isDebugEnabled() )
                {
                    log.debug( "LocalGetMatching.  fromCluster [" + fromCluster + "] AllowClusterGet ["
                        + this.remoteCacheServerAttributes.isAllowClusterGet() + "]" );
                }
                elements = c.localGetMatching( pattern );
            }
        }
        return elements;
    }

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param cacheName
     * @param keys
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     * @throws IOException
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMultiple( String cacheName, Set<K> keys )
        throws IOException
    {
        return this.getMultiple( cacheName, keys, 0 );
    }

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * The internal processing is wrapped in event logging calls.
     * <p>
     * @param cacheName
     * @param keys
     * @param requesterId
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     * @throws IOException
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMultiple( String cacheName, Set<K> keys, long requesterId )
        throws IOException
    {
        ICacheEvent<Serializable> cacheEvent = createICacheEvent( cacheName, (Serializable) keys, requesterId,
                                                    ICacheEventLogger.GETMULTIPLE_EVENT );
        try
        {
            return processGetMultiple( cacheName, keys, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param cacheName
     * @param keys
     * @param requesterId
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     */
    private Map<K, ICacheElement<K, V>> processGetMultiple( String cacheName, Set<K> keys, long requesterId )
    {
        boolean fromCluster = isRequestFromCluster( requesterId );

        if ( log.isDebugEnabled() )
        {
            log.debug( "getMultiple [" + keys + "] from cache [" + cacheName + "] requesterId = [" + requesterId
                + "] fromCluster = " + fromCluster );
        }

        CacheListeners<K, V> cacheDesc = getCacheListeners( cacheName );
        Map<K, ICacheElement<K, V>> elements = getMultipleFromCacheListeners( keys, null, fromCluster, cacheDesc );
        return elements;
    }

    /**
     * Since a non-receiving remote cache client will not register a listener, it will not have a
     * listener id assigned from the server. As such the remote server cannot determine if it is a
     * cluster or a normal client. It will assume that it is a normal client.
     * <p>
     * @param requesterId
     * @return true is from a cluster.
     */
    private boolean isRequestFromCluster( long requesterId )
    {
        RemoteType remoteTypeL = idTypeMap.get( Long.valueOf( requesterId ) );
        return remoteTypeL == RemoteType.CLUSTER;
    }

    /**
     * Gets the items from the associated cache listeners.
     * <p>
     * @param keys
     * @param elements
     * @param fromCluster
     * @param cacheDesc
     * @return Map
     */
    private Map<K, ICacheElement<K, V>> getMultipleFromCacheListeners( Set<K> keys, Map<K, ICacheElement<K, V>> elements, boolean fromCluster, CacheListeners<K, V> cacheDesc )
    {
        Map<K, ICacheElement<K, V>> returnElements = elements;

        if ( cacheDesc != null )
        {
            CompositeCache<K, V> c = (CompositeCache<K, V>) cacheDesc.cache;

            // If we have a getMultiple come in from a client and we don't have the item
            // locally, we will allow the cache to look in other non local sources,
            // such as a remote cache or a lateral.
            //
            // Since remote servers never get from clients and clients never go
            // remote from a remote call, this
            // will not result in any loops.
            //
            // This is the only instance I can think of where we allow a remote get
            // from a remote call. The purpose is to allow remote cache servers to
            // talk to each other. If one goes down, you want it to be able to get
            // data from those that were up when the failed server comes back on
            // line.

            if ( !fromCluster && this.remoteCacheServerAttributes.isAllowClusterGet() )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "NonLocalGetMultiple. fromCluster [" + fromCluster + "] AllowClusterGet ["
                        + this.remoteCacheServerAttributes.isAllowClusterGet() + "]" );
                }

                returnElements = c.getMultiple( keys );
            }
            else
            {
                // Gets from cluster type remote will end up here.
                // Gets from all clients will end up here if allow cluster get is
                // false.

                if ( log.isDebugEnabled() )
                {
                    log.debug( "LocalGetMultiple.  fromCluster [" + fromCluster + "] AllowClusterGet ["
                        + this.remoteCacheServerAttributes.isAllowClusterGet() + "]" );
                }

                returnElements = c.localGetMultiple( keys );
            }
        }

        return returnElements;
    }

    /**
     * Return the keys in the cache.
     * <p>
     * @param cacheName the name of the cache region
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet(String cacheName) throws IOException
    {
        return processGetKeySet( cacheName );
    }

    /**
     * Gets the set of keys of objects currently in the cache.
     * <p>
     * @param cacheName
     * @return Set
     */
    protected Set<K> processGetKeySet( String cacheName )
    {
        CacheListeners<K, V> cacheDesc = null;
        try
        {
            cacheDesc = getCacheListeners( cacheName );
        }
        catch ( Exception e )
        {
            log.error( "Problem getting listeners.", e );
        }

        if ( cacheDesc == null )
        {
            return Collections.emptySet();
        }

        CompositeCache<K, V> c = (CompositeCache<K, V>) cacheDesc.cache;
        return c.getKeySet();
    }

    /**
     * Removes the given key from the specified remote cache. Defaults the listener id to 0.
     * <p>
     * @param cacheName
     * @param key
     * @throws IOException
     */
    @Override
    public void remove( String cacheName, K key )
        throws IOException
    {
        remove( cacheName, key, 0 );
    }

    /**
     * Remove the key from the cache region and don't tell the source listener about it.
     * <p>
     * The internal processing is wrapped in event logging calls.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @throws IOException
     */
    @Override
    public void remove( String cacheName, K key, long requesterId )
        throws IOException
    {
        ICacheEvent<K> cacheEvent = createICacheEvent( cacheName, key, requesterId, ICacheEventLogger.REMOVE_EVENT );
        try
        {
            processRemove( cacheName, key, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * Remove the key from the cache region and don't tell the source listener about it.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @throws IOException
     */
    private void processRemove( String cacheName, K key, long requesterId )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "remove [" + key + "] from cache [" + cacheName + "]" );
        }

        CacheListeners<K, V> cacheDesc = cacheListenersMap.get( cacheName );

        boolean fromCluster = isRequestFromCluster( requesterId );

        if ( cacheDesc != null )
        {
            // best attempt to achieve ordered cache item removal and
            // notification.
            synchronized ( cacheDesc )
            {
                boolean removeSuccess = false;

                // No need to notify if it was not cached.
                CompositeCache<K, V> c = (CompositeCache<K, V>) cacheDesc.cache;

                if ( fromCluster )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Remove FROM cluster, NOT updating other auxiliaries for region" );
                    }
                    removeSuccess = c.localRemove( key );
                }
                else
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Remove NOT from cluster, updating other auxiliaries for region" );
                    }
                    removeSuccess = c.remove( key );
                }

                if ( log.isDebugEnabled() )
                {
                    log.debug( "remove [" + key + "] from cache [" + cacheName + "] success (was it found) = "
                        + removeSuccess );
                }

                // UPDATE LOCALS IF A REQUEST COMES FROM A CLUSTER
                // IF LOCAL CLUSTER CONSISTENCY IS CONFIGURED
                if ( !fromCluster || ( fromCluster && remoteCacheServerAttributes.isLocalClusterConsistency() ) )
                {
                    ICacheEventQueue<K, V>[] qlist = getEventQList( cacheDesc, requesterId );

                    for ( int i = 0; i < qlist.length; i++ )
                    {
                        qlist[i].addRemoveEvent( key );
                    }
                }
            }
        }
    }

    /**
     * Remove all keys from the specified remote cache.
     * <p>
     * @param cacheName
     * @throws IOException
     */
    @Override
    public void removeAll( String cacheName )
        throws IOException
    {
        removeAll( cacheName, 0 );
    }

    /**
     * Remove all keys from the specified remote cache.
     * <p>
     * The internal processing is wrapped in event logging calls.
     * <p>
     * @param cacheName
     * @param requesterId
     * @throws IOException
     */
    @Override
    public void removeAll( String cacheName, long requesterId )
        throws IOException
    {
        ICacheEvent<String> cacheEvent = createICacheEvent( cacheName, "all", requesterId, ICacheEventLogger.REMOVEALL_EVENT );
        try
        {
            processRemoveAll( cacheName, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * Remove all keys from the specified remote cache.
     * <p>
     * @param cacheName
     * @param requesterId
     * @throws IOException
     */
    private void processRemoveAll( String cacheName, long requesterId )
        throws IOException
    {
        CacheListeners<K, V> cacheDesc = cacheListenersMap.get( cacheName );

        boolean fromCluster = isRequestFromCluster( requesterId );

        if ( cacheDesc != null )
        {
            // best attempt to achieve ordered cache item removal and
            // notification.
            synchronized ( cacheDesc )
            {
                // No need to broadcast, or notify if it was not cached.
                CompositeCache<K, V> c = (CompositeCache<K, V>) cacheDesc.cache;

                if ( fromCluster )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "RemoveALL FROM cluster, NOT updating other auxiliaries for region" );
                    }
                    c.localRemoveAll();
                }
                else
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "RemoveALL NOT from cluster, updating other auxiliaries for region" );
                    }
                    c.removeAll();
                }

                // update registered listeners
                if ( !fromCluster || ( fromCluster && remoteCacheServerAttributes.isLocalClusterConsistency() ) )
                {
                    ICacheEventQueue<K, V>[] qlist = getEventQList( cacheDesc, requesterId );

                    for ( int i = 0; i < qlist.length; i++ )
                    {
                        qlist[i].addRemoveAllEvent();
                    }
                }
            }
        }
    }

    /**
     * How many put events have we received.
     * <p>
     * @return puts
     */
    // Currently only intended for use by unit tests
    int getPutCount()
    {
        return puts;
    }

    /**
     * Frees the specified remote cache.
     * <p>
     * @param cacheName
     * @throws IOException
     */
    @Override
    public void dispose( String cacheName )
        throws IOException
    {
        dispose( cacheName, 0 );
    }

    /**
     * Frees the specified remote cache.
     * <p>
     * @param cacheName
     * @param requesterId
     * @throws IOException
     */
    public void dispose( String cacheName, long requesterId )
        throws IOException
    {
        ICacheEvent<String> cacheEvent = createICacheEvent( cacheName, "none", requesterId, ICacheEventLogger.DISPOSE_EVENT );
        try
        {
            processDispose( cacheName, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * @param cacheName
     * @param requesterId
     * @throws IOException
     */
    private void processDispose( String cacheName, long requesterId )
        throws IOException
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Dispose request received from listener [" + requesterId + "]" );
        }

        CacheListeners<K, V> cacheDesc = cacheListenersMap.get( cacheName );

        // this is dangerous
        if ( cacheDesc != null )
        {
            // best attempt to achieve ordered free-cache-op and notification.
            synchronized ( cacheDesc )
            {
                ICacheEventQueue<K, V>[] qlist = getEventQList( cacheDesc, requesterId );

                for ( int i = 0; i < qlist.length; i++ )
                {
                    qlist[i].addDisposeEvent();
                }
                cacheManager.freeCache( cacheName );
            }
        }
    }

    /**
     * Frees all remote caches.
     * <p>
     * @throws IOException
     */
    @Override
    public void release()
        throws IOException
    {
        for (CacheListeners<K, V> cacheDesc : cacheListenersMap.values())
        {
            ICacheEventQueue<K, V>[] qlist = getEventQList( cacheDesc, 0 );

            for ( int i = 0; i < qlist.length; i++ )
            {
                qlist[i].addDisposeEvent();
            }
        }
        cacheManager.release();
    }

    /**
     * Returns the cache listener for the specified cache. Creates the cache and the cache
     * descriptor if they do not already exist.
     * <p>
     * @param cacheName
     * @return The cacheListeners value
     */
    protected CacheListeners<K, V> getCacheListeners( String cacheName )
    {
        CacheListeners<K, V> cacheListeners = cacheListenersMap.computeIfAbsent(cacheName, key -> {
            CompositeCache<K, V> cache = cacheManager.getCache(key);
            return new CacheListeners<K, V>( cache );
        });

        return cacheListeners;
    }

    /**
     * Gets the clusterListeners attribute of the RemoteCacheServer object.
     * <p>
     * TODO may be able to remove this
     * @param cacheName
     * @return The clusterListeners value
     */
    protected CacheListeners<K, V> getClusterListeners( String cacheName )
    {
        CacheListeners<K, V> cacheListeners = clusterListenersMap.computeIfAbsent(cacheName, key -> {
            CompositeCache<K, V> cache = cacheManager.getCache( cacheName );
            return new CacheListeners<K, V>( cache );
        });

        return cacheListeners;
    }

    /**
     * Gets the eventQList attribute of the RemoteCacheServer object. This returns the event queues
     * stored in the cacheListeners object for a particular region, if the queue is not for this
     * requester.
     * <p>
     * Basically, this makes sure that a request from a particular local cache, identified by its
     * listener id, does not result in a call to that same listener.
     * <p>
     * @param cacheListeners
     * @param requesterId
     * @return The eventQList value
     */
    @SuppressWarnings("unchecked") // No generic arrays in java
    private ICacheEventQueue<K, V>[] getEventQList( CacheListeners<K, V> cacheListeners, long requesterId )
    {
        ICacheEventQueue<K, V>[] list = cacheListeners.eventQMap.values().toArray( new ICacheEventQueue[0] );
        int count = 0;
        // Set those not qualified to null; Count those qualified.
        for ( int i = 0; i < list.length; i++ )
        {
            ICacheEventQueue<K, V> q = list[i];
            if ( q.isWorking() && q.getListenerId() != requesterId )
            {
                count++;
            }
            else
            {
                list[i] = null;
            }
        }
        if ( count == list.length )
        {
            // All qualified.
            return list;
        }

        // Returns only the qualified.
        ICacheEventQueue<K, V>[] qq = new ICacheEventQueue[count];
        count = 0;
        for ( int i = 0; i < list.length; i++ )
        {
            if ( list[i] != null )
            {
                qq[count++] = list[i];
            }
        }
        return qq;
    }

    /**
     * Removes dead event queues. Should clean out deregistered listeners.
     * <p>
     * @param eventQMap
     */
    private static <KK, VV> void cleanupEventQMap( Map<Long, ICacheEventQueue<KK, VV>> eventQMap )
    {
        synchronized ( eventQMap )
        {
            // this does not care if the q is alive (i.e. if
            // there are active threads; it cares if the queue
            // is working -- if it has not encountered errors
            // above the failure threshold
            eventQMap.entrySet().removeIf(e -> !e.getValue().isWorking());
        }
    }

    /**
     * Subscribes to the specified remote cache.
     * <p>
     * If the client id is 0, then the remote cache server will increment it's local count and
     * assign an id to the client.
     * <p>
     * @param cacheName the specified remote cache.
     * @param listener object to notify for cache changes. must be synchronized since there are
     *            remote calls involved.
     * @throws IOException
     */
    @Override
    @SuppressWarnings("unchecked") // Need to cast to specific return type from getClusterListeners()
    public <KK, VV> void addCacheListener( String cacheName, ICacheListener<KK, VV> listener )
        throws IOException
    {
        if ( cacheName == null || listener == null )
        {
            throw new IllegalArgumentException( "cacheName and listener must not be null" );
        }
        CacheListeners<KK, VV> cacheListeners;

        IRemoteCacheListener<KK, VV> ircl = (IRemoteCacheListener<KK, VV>) listener;

        String listenerAddress = ircl.getLocalHostAddress();

        RemoteType remoteType = ircl.getRemoteType();
        if ( remoteType == RemoteType.CLUSTER )
        {
            log.debug( "adding cluster listener, listenerAddress [" + listenerAddress + "]" );
            cacheListeners = (CacheListeners<KK, VV>)getClusterListeners( cacheName );
        }
        else
        {
            log.debug( "adding normal listener, listenerAddress [" + listenerAddress + "]" );
            cacheListeners = (CacheListeners<KK, VV>)getCacheListeners( cacheName );
        }
        Map<Long, ICacheEventQueue<KK, VV>> eventQMap = cacheListeners.eventQMap;
        cleanupEventQMap( eventQMap );

        // synchronized ( listenerId )
        synchronized ( ICacheListener.class )
        {
            long id = 0;
            try
            {
                id = listener.getListenerId();
                // clients probably shouldn't do this.
                if ( id == 0 )
                {
                    // must start at one so the next gets recognized
                    long listenerIdB = nextListenerId();
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "listener id=" + ( listenerIdB & 0xff ) + " addded for cache [" + cacheName
                            + "], listenerAddress [" + listenerAddress + "]" );
                    }
                    listener.setListenerId( listenerIdB );
                    id = listenerIdB;

                    // in case it needs synchronization
                    String message = "Adding vm listener under new id = [" + listenerIdB + "], listenerAddress ["
                        + listenerAddress + "]";
                    logApplicationEvent( "RemoteCacheServer", "addCacheListener", message );
                    if ( log.isInfoEnabled() )
                    {
                        log.info( message );
                    }
                }
                else
                {
                    String message = "Adding listener under existing id = [" + id + "], listenerAddress ["
                        + listenerAddress + "]";
                    logApplicationEvent( "RemoteCacheServer", "addCacheListener", message );
                    if ( log.isInfoEnabled() )
                    {
                        log.info( message );
                    }
                    // should confirm the the host is the same as we have on
                    // record, just in case a client has made a mistake.
                }

                // relate the type to an id
                this.idTypeMap.put( Long.valueOf( id ), remoteType);
                if ( listenerAddress != null )
                {
                    this.idIPMap.put( Long.valueOf( id ), listenerAddress );
                }
            }
            catch ( IOException ioe )
            {
                String message = "Problem setting listener id, listenerAddress [" + listenerAddress + "]";
                log.error( message, ioe );

                if ( cacheEventLogger != null )
                {
                    cacheEventLogger.logError( "RemoteCacheServer", "addCacheListener", message + " - "
                        + ioe.getMessage() );
                }
            }

            CacheEventQueueFactory<KK, VV> fact = new CacheEventQueueFactory<KK, VV>();
            ICacheEventQueue<KK, VV> q = fact.createCacheEventQueue( listener, id, cacheName, remoteCacheServerAttributes
                .getEventQueuePoolName(), remoteCacheServerAttributes.getEventQueueType() );

            eventQMap.put(Long.valueOf(listener.getListenerId()), q);

            if ( log.isInfoEnabled() )
            {
                log.info( cacheListeners );
            }
        }
    }

    /**
     * Subscribes to all remote caches.
     * <p>
     * @param listener The feature to be added to the CacheListener attribute
     * @throws IOException
     */
    @Override
    public <KK, VV> void addCacheListener( ICacheListener<KK, VV> listener )
        throws IOException
    {
        for (String cacheName : cacheListenersMap.keySet())
        {
            addCacheListener( cacheName, listener );

            if ( log.isDebugEnabled() )
            {
                log.debug( "Adding listener for cache [" + cacheName + "]" );
            }
        }
    }

    /**
     * Unsubscribe this listener from this region. If the listener is registered, it will be removed
     * from the event queue map list.
     * <p>
     * @param cacheName
     * @param listener
     * @throws IOException
     */
    @Override
    public <KK, VV> void removeCacheListener( String cacheName, ICacheListener<KK, VV> listener )
        throws IOException
    {
        removeCacheListener( cacheName, listener.getListenerId() );
    }

    /**
     * Unsubscribe this listener from this region. If the listener is registered, it will be removed
     * from the event queue map list.
     * <p>
     * @param cacheName
     * @param listenerId
     */
    public void removeCacheListener( String cacheName, long listenerId )
    {
        String message = "Removing listener for cache region = [" + cacheName + "] and listenerId [" + listenerId + "]";
        logApplicationEvent( "RemoteCacheServer", "removeCacheListener", message );
        if ( log.isInfoEnabled() )
        {
            log.info( message );
        }

        boolean isClusterListener = isRequestFromCluster( listenerId );

        CacheListeners<K, V> cacheDesc = null;

        if ( isClusterListener )
        {
            cacheDesc = getClusterListeners( cacheName );
        }
        else
        {
            cacheDesc = getCacheListeners( cacheName );
        }
        Map<Long, ICacheEventQueue<K, V>> eventQMap = cacheDesc.eventQMap;
        cleanupEventQMap( eventQMap );
        ICacheEventQueue<K, V> q = eventQMap.remove( Long.valueOf( listenerId ) );

        if ( q != null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Found queue for cache region = [" + cacheName + "] and listenerId  [" + listenerId + "]" );
            }
            q.destroy();
            cleanupEventQMap( eventQMap );
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Did not find queue for cache region = [" + cacheName + "] and listenerId [" + listenerId
                    + "]" );
            }
        }

        // cleanup
        idTypeMap.remove( Long.valueOf( listenerId ) );
        idIPMap.remove( Long.valueOf( listenerId ) );

        if ( log.isInfoEnabled() )
        {
            log.info( "After removing listener [" + listenerId + "] cache region " + cacheName + "'s listener size ["
                + cacheDesc.eventQMap.size() + "]" );
        }
    }

    /**
     * Unsubscribes from all remote caches.
     * <p>
     * @param listener
     * @throws IOException
     */
    @Override
    public <KK, VV> void removeCacheListener( ICacheListener<KK, VV> listener )
        throws IOException
    {
        for (String cacheName : cacheListenersMap.keySet())
        {
            removeCacheListener( cacheName, listener );

            if ( log.isInfoEnabled() )
            {
                log.info( "Removing listener for cache [" + cacheName + "]" );
            }
        }
    }

    /**
     * Shuts down the remote server.
     * <p>
     * @throws IOException
     */
    @Override
    public void shutdown()
        throws IOException
    {
        shutdown("", Registry.REGISTRY_PORT);
    }

    /**
     * Shuts down a server at a particular host and port. Then it calls shutdown on the cache
     * itself.
     * <p>
     * @param host
     * @param port
     * @throws IOException
     */
    @Override
    public void shutdown( String host, int port )
        throws IOException
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Received shutdown request. Shutting down server." );
        }

        synchronized (listenerId)
        {
            for (String cacheName : cacheListenersMap.keySet())
            {
                for (int i = 0; i <= listenerId[0]; i++)
                {
                    removeCacheListener( cacheName, i );
                }

                if ( log.isInfoEnabled() )
                {
                    log.info( "Removing listener for cache [" + cacheName + "]" );
                }
            }

            cacheListenersMap.clear();
            clusterListenersMap.clear();
        }
        RemoteCacheServerFactory.shutdownImpl( host, port );
        this.cacheManager.shutDown();
    }

    /**
     * Called by the RMI runtime sometime after the runtime determines that the reference list, the
     * list of clients referencing the remote object, becomes empty.
     */
    // TODO: test out the DGC.
    @Override
    public void unreferenced()
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "*** Server now unreferenced and subject to GC. ***" );
        }
    }

    /**
     * Returns the next generated listener id [0,255].
     * <p>
     * @return the listener id of a client. This should be unique for this server.
     */
    private long nextListenerId()
    {
        long id = 0;
        if ( listenerId[0] == Integer.MAX_VALUE )
        {
            synchronized ( listenerId )
            {
                id = listenerId[0];
                listenerId[0] = 0;
                // TODO: record & check if the generated id is currently being
                // used by a valid listener. Currently if the id wraps after
                // Long.MAX_VALUE,
                // we just assume it won't collide with an existing listener who
                // is live.
            }
        }
        else
        {
            synchronized ( listenerId )
            {
                id = ++listenerId[0];
            }
        }
        return id;
    }

    /**
     * Gets the stats attribute of the RemoteCacheServer object.
     * <p>
     * @return The stats value
     * @throws IOException
     */
    @Override
    public String getStats()
        throws IOException
    {
        return cacheManager.getStats();
    }

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param item
     * @param requesterId
     * @param eventName
     * @return ICacheEvent
     */
    private ICacheEvent<ICacheElement<K, V>> createICacheEvent( ICacheElement<K, V> item, long requesterId, String eventName )
    {
        if ( cacheEventLogger == null )
        {
            return new CacheEvent<ICacheElement<K, V>>();
        }
        String ipAddress = getExtraInfoForRequesterId( requesterId );
        return cacheEventLogger
            .createICacheEvent( "RemoteCacheServer", item.getCacheName(), eventName, ipAddress, item );
    }

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @param eventName
     * @return ICacheEvent
     */
    private <T> ICacheEvent<T> createICacheEvent( String cacheName, T key, long requesterId, String eventName )
    {
        if ( cacheEventLogger == null )
        {
            return new CacheEvent<T>();
        }
        String ipAddress = getExtraInfoForRequesterId( requesterId );
        return cacheEventLogger.createICacheEvent( "RemoteCacheServer", cacheName, eventName, ipAddress, key );
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

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param cacheEvent
     */
    protected <T> void logICacheEvent( ICacheEvent<T> cacheEvent )
    {
        if ( cacheEventLogger != null )
        {
            cacheEventLogger.logICacheEvent( cacheEvent );
        }
    }

    /**
     * Ip address for the client, if one is stored.
     * <p>
     * Protected for testing.
     * <p>
     * @param requesterId
     * @return String
     */
    protected String getExtraInfoForRequesterId( long requesterId )
    {
        String ipAddress = idIPMap.get( Long.valueOf( requesterId ) );
        return ipAddress;
    }

    /**
     * Allows it to be injected.
     * <p>
     * @param cacheEventLogger
     */
    public void setCacheEventLogger( ICacheEventLogger cacheEventLogger )
    {
        this.cacheEventLogger = cacheEventLogger;
    }
}
