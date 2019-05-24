package org.apache.commons.jcs.auxiliary.remote.http.server;

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
import java.util.Map;
import java.util.Set;

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.logging.CacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains common methods for remote cache services. Eventually I hope to extract out
 * much of the RMI server to use this as well. I'm starting with the Http service.
 */
public abstract class AbstractRemoteCacheService<K, V>
    implements ICacheServiceNonLocal<K, V>
{
    /** An optional event logger */
    private transient ICacheEventLogger cacheEventLogger;

    /** The central hub */
    private ICompositeCacheManager cacheManager;

    /** Name of the event log source. */
    private String eventLogSourceName = "AbstractRemoteCacheService";

    /** Number of puts into the cache. */
    private int puts = 0;

    /** The interval at which we will log updates. */
    private final int logInterval = 100;

    /** log instance */
    private static final Log log = LogFactory.getLog( AbstractRemoteCacheService.class );

    /**
     * Creates the super with the needed items.
     * <p>
     * @param cacheManager
     * @param cacheEventLogger
     */
    public AbstractRemoteCacheService( ICompositeCacheManager cacheManager, ICacheEventLogger cacheEventLogger )
    {
        this.cacheManager = cacheManager;
        this.cacheEventLogger = cacheEventLogger;
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
            logUpdateInfo( item );

            processUpdate( item, requesterId );
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * The internal processing is wrapped in event logging calls.
     * <p>
     * @param item
     * @param requesterId
     * @throws IOException
     */
    abstract void processUpdate( ICacheElement<K, V> item, long requesterId )
        throws IOException;

    /**
     * Log some details.
     * <p>
     * @param item
     */
    private void logUpdateInfo( ICacheElement<K, V> item )
    {
        if ( log.isInfoEnabled() )
        {
            // not thread safe, but it doesn't have to be accurate
            puts++;
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
     * Adding the requestor id, allows the cache to determine the source of the get.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @return ICacheElement
     * @throws IOException
     */
    abstract ICacheElement<K, V> processGet( String cacheName, K key, long requesterId )
        throws IOException;

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
     * @throws IOException
     */
    abstract Map<K, ICacheElement<K, V>> processGetMatching( String cacheName, String pattern, long requesterId )
        throws IOException;

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
     * @throws IOException
     */
    abstract Map<K, ICacheElement<K, V>> processGetMultiple( String cacheName, Set<K> keys, long requesterId )
        throws IOException;

    /**
     * Return the keys in this cache.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet( String cacheName )
    {
        return processGetKeySet( cacheName );
    }

    /**
     * Gets the set of keys of objects currently in the cache.
     * <p>
     * @param cacheName
     * @return Set
     */
    public Set<K> processGetKeySet( String cacheName )
    {
        CompositeCache<K, V> cache = getCacheManager().getCache( cacheName );

        return cache.getKeySet();
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
    abstract void processRemove( String cacheName, K key, long requesterId )
        throws IOException;

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
    abstract void processRemoveAll( String cacheName, long requesterId )
        throws IOException;

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
    abstract void processDispose( String cacheName, long requesterId )
        throws IOException;

    /**
     * Gets the stats attribute of the RemoteCacheServer object.
     * <p>
     * @return The stats value
     * @throws IOException
     */
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
    protected ICacheEvent<ICacheElement<K, V>> createICacheEvent( ICacheElement<K, V> item, long requesterId, String eventName )
    {
        if ( cacheEventLogger == null )
        {
            return new CacheEvent<>();
        }
        String ipAddress = getExtraInfoForRequesterId( requesterId );
        return cacheEventLogger.createICacheEvent( getEventLogSourceName(), item.getCacheName(), eventName, ipAddress,
                                                   item );
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
    protected <T> ICacheEvent<T> createICacheEvent( String cacheName, T key, long requesterId, String eventName )
    {
        if ( cacheEventLogger == null )
        {
            return new CacheEvent<>();
        }
        String ipAddress = getExtraInfoForRequesterId( requesterId );
        return cacheEventLogger.createICacheEvent( getEventLogSourceName(), cacheName, eventName, ipAddress, key );
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
    protected abstract String getExtraInfoForRequesterId( long requesterId );

    /**
     * Allows it to be injected.
     * <p>
     * @param cacheEventLogger
     */
    public void setCacheEventLogger( ICacheEventLogger cacheEventLogger )
    {
        this.cacheEventLogger = cacheEventLogger;
    }

    /**
     * @param cacheManager the cacheManager to set
     */
    protected void setCacheManager( ICompositeCacheManager cacheManager )
    {
        this.cacheManager = cacheManager;
    }

    /**
     * @return the cacheManager
     */
    protected ICompositeCacheManager getCacheManager()
    {
        return cacheManager;
    }

    /**
     * @param eventLogSourceName the eventLogSourceName to set
     */
    protected void setEventLogSourceName( String eventLogSourceName )
    {
        this.eventLogSourceName = eventLogSourceName;
    }

    /**
     * @return the eventLogSourceName
     */
    protected String getEventLogSourceName()
    {
        return eventLogSourceName;
    }
}
