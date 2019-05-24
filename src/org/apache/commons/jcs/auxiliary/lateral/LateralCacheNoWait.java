package org.apache.commons.jcs.auxiliary.lateral;

import java.io.IOException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.engine.CacheAdaptor;
import org.apache.commons.jcs.engine.CacheEventQueueFactory;
import org.apache.commons.jcs.engine.CacheInfo;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to queue up update requests to the underlying cache. These requests will be processed in
 * their order of arrival via the cache event queue processor.
 */
public class LateralCacheNoWait<K, V>
    extends AbstractAuxiliaryCache<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( LateralCacheNoWait.class );

    /** The cache */
    private final LateralCache<K, V> cache;

    /** The event queue */
    private ICacheEventQueue<K, V> eventQueue;

    /** times get called */
    private int getCount = 0;

    /** times remove called */
    private int removeCount = 0;

    /** times put called */
    private int putCount = 0;

    /**
     * Constructs with the given lateral cache, and fires up an event queue for asynchronous
     * processing.
     * <p>
     * @param cache
     */
    public LateralCacheNoWait( LateralCache<K, V> cache )
    {
        this.cache = cache;

        if ( log.isDebugEnabled() )
        {
            log.debug( "Constructing LateralCacheNoWait, LateralCache = [" + cache + "]" );
        }

        CacheEventQueueFactory<K, V> fact = new CacheEventQueueFactory<>();
        this.eventQueue = fact.createCacheEventQueue( new CacheAdaptor<>( cache ), CacheInfo.listenerId, cache
            .getCacheName(), cache.getAuxiliaryCacheAttributes().getEventQueuePoolName(), cache
            .getAuxiliaryCacheAttributes().getEventQueueType() );

        // need each no wait to handle each of its real updates and removes,
        // since there may
        // be more than one per cache? alternative is to have the cache
        // perform updates using a different method that specifies the listener
        // this.q = new CacheEventQueue(new CacheAdaptor(this),
        // LateralCacheInfo.listenerId, cache.getCacheName());
        if ( cache.getStatus() == CacheStatus.ERROR )
        {
            eventQueue.destroy();
        }
    }

    /**
     * @param ce
     * @throws IOException
     */
    @Override
    public void update( ICacheElement<K, V> ce )
        throws IOException
    {
        putCount++;
        try
        {
            eventQueue.addPutEvent( ce );
        }
        catch ( IOException ex )
        {
            log.error( ex );
            eventQueue.destroy();
        }
    }

    /**
     * Synchronously reads from the lateral cache.
     * <p>
     * @param key
     * @return ICacheElement&lt;K, V&gt; if found, else null
     */
    @Override
    public ICacheElement<K, V> get( K key )
    {
        getCount++;
        if ( this.getStatus() != CacheStatus.ERROR )
        {
            try
            {
                return cache.get( key );
            }
            catch ( UnmarshalException ue )
            {
                log.debug( "Retrying the get owing to UnmarshalException..." );
                try
                {
                    return cache.get( key );
                }
                catch ( IOException ex )
                {
                    log.error( "Failed in retrying the get for the second time." );
                    eventQueue.destroy();
                }
            }
            catch ( IOException ex )
            {
                eventQueue.destroy();
            }
        }
        return null;
    }

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param keys
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMultiple(Set<K> keys)
    {
        if ( keys != null && !keys.isEmpty() )
        {
            Map<K, ICacheElement<K, V>> elements = keys.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> get(key))).entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            entry -> entry.getKey(),
                            entry -> entry.getValue()));

            return elements;
        }

        return new HashMap<>();
    }

    /**
     * Synchronously reads from the lateral cache.
     * <p>
     * @param pattern
     * @return ICacheElement&lt;K, V&gt; if found, else empty
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatching(String pattern)
    {
        getCount++;
        if ( this.getStatus() != CacheStatus.ERROR )
        {
            try
            {
                return cache.getMatching( pattern );
            }
            catch ( UnmarshalException ue )
            {
                log.debug( "Retrying the get owing to UnmarshalException." );
                try
                {
                    return cache.getMatching( pattern );
                }
                catch ( IOException ex )
                {
                    log.error( "Failed in retrying the get for the second time." );
                    eventQueue.destroy();
                }
            }
            catch ( IOException ex )
            {
                eventQueue.destroy();
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Return the keys in this cache.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet() throws IOException
    {
        try
        {
            return cache.getKeySet();
        }
        catch ( IOException ex )
        {
            log.error( ex );
            eventQueue.destroy();
        }
        return Collections.emptySet();
    }

    /**
     * Adds a remove request to the lateral cache.
     * <p>
     * @param key
     * @return always false
     */
    @Override
    public boolean remove( K key )
    {
        removeCount++;
        try
        {
            eventQueue.addRemoveEvent( key );
        }
        catch ( IOException ex )
        {
            log.error( ex );
            eventQueue.destroy();
        }
        return false;
    }

    /** Adds a removeAll request to the lateral cache. */
    @Override
    public void removeAll()
    {
        try
        {
            eventQueue.addRemoveAllEvent();
        }
        catch ( IOException ex )
        {
            log.error( ex );
            eventQueue.destroy();
        }
    }

    /** Adds a dispose request to the lateral cache. */
    @Override
    public void dispose()
    {
        try
        {
            eventQueue.addDisposeEvent();
        }
        catch ( IOException ex )
        {
            log.error( ex );
            eventQueue.destroy();
        }
    }

    /**
     * No lateral invocation.
     * <p>
     * @return The size value
     */
    @Override
    public int getSize()
    {
        return cache.getSize();
    }

    /**
     * No lateral invocation.
     * <p>
     * @return The cacheType value
     */
    @Override
    public CacheType getCacheType()
    {
        return cache.getCacheType();
    }

    /**
     * Returns the asyn cache status. An error status indicates either the lateral connection is not
     * available, or the asyn queue has been unexpectedly destroyed. No lateral invocation.
     * <p>
     * @return The status value
     */
    @Override
    public CacheStatus getStatus()
    {
        return eventQueue.isWorking() ? cache.getStatus() : CacheStatus.ERROR;
    }

    /**
     * Gets the cacheName attribute of the LateralCacheNoWait object
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return cache.getCacheName();
    }

    /**
     * Replaces the lateral cache service handle with the given handle and reset the queue by
     * starting up a new instance.
     * <p>
     * @param lateral
     */
    public void fixCache( ICacheServiceNonLocal<K, V> lateral )
    {
        cache.fixCache( lateral );
        resetEventQ();
    }

    /**
     * Resets the event q by first destroying the existing one and starting up new one.
     */
    public void resetEventQ()
    {
        if ( eventQueue.isWorking() )
        {
            eventQueue.destroy();
        }
        CacheEventQueueFactory<K, V> fact = new CacheEventQueueFactory<>();
        this.eventQueue = fact.createCacheEventQueue( new CacheAdaptor<>( cache ), CacheInfo.listenerId, cache
            .getCacheName(), cache.getAuxiliaryCacheAttributes().getEventQueuePoolName(), cache
            .getAuxiliaryCacheAttributes().getEventQueueType() );
    }

    /**
     * @return Returns the AuxiliaryCacheAttributes.
     */
    @Override
    public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
    {
        return cache.getAuxiliaryCacheAttributes();
    }

    /**
     * getStats
     * @return String
     */
    @Override
    public String getStats()
    {
        return getStatistics().toString();
    }

    /**
     * this won't be called since we don't do ICache logging here.
     * <p>
     * @return String
     */
    @Override
    public String getEventLoggingExtraInfo()
    {
        return "Lateral Cache No Wait";
    }

    /**
     * @return statistics about this communication
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "Lateral Cache No Wait" );

        ArrayList<IStatElement<?>> elems = new ArrayList<>();

        // get the stats from the event queue too
        IStats eqStats = this.eventQueue.getStatistics();
        elems.addAll(eqStats.getStatElements());

        elems.add(new StatElement<>( "Get Count", Integer.valueOf(this.getCount) ) );
        elems.add(new StatElement<>( "Remove Count", Integer.valueOf(this.removeCount) ) );
        elems.add(new StatElement<>( "Put Count", Integer.valueOf(this.putCount) ) );
        elems.add(new StatElement<>( "Attributes", cache.getAuxiliaryCacheAttributes() ) );

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * @return debugging info.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( " LateralCacheNoWait " );
        buf.append( " Status = " + this.getStatus() );
        buf.append( " cache = [" + cache.toString() + "]" );
        return buf.toString();
    }
}
