package org.apache.commons.jcs.auxiliary.lateral;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheListener;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to provide access to multiple services under nowait protection. Composite factory should
 * construct LateralCacheNoWaitFacade to give to the composite cache out of caches it constructs
 * from the varies manager to lateral services. Perhaps the lateralcache factory should be able to
 * do this.
 */
public class LateralCacheNoWaitFacade<K, V>
    extends AbstractAuxiliaryCache<K, V>
{
    /** The logger */
    private static final Log log = LogFactory.getLog( LateralCacheNoWaitFacade.class );

    /** The queuing facade to the client. */
    public LateralCacheNoWait<K, V>[] noWaits;

    /** The region name */
    private final String cacheName;

    /** A cache listener */
    private ILateralCacheListener<K, V> listener;

    /** User configurable attributes. */
    private final ILateralCacheAttributes lateralCacheAttributes;

    /** Disposed state of this facade */
    private boolean disposed = false;

    /**
     * Constructs with the given lateral cache, and fires events to any listeners.
     * <p>
     * @param noWaits
     * @param cattr
     */
    public LateralCacheNoWaitFacade(ILateralCacheListener<K, V> listener, LateralCacheNoWait<K, V>[] noWaits, ILateralCacheAttributes cattr )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "CONSTRUCTING NO WAIT FACADE" );
        }
        this.listener = listener;
        this.noWaits = noWaits;
        this.cacheName = cattr.getCacheName();
        this.lateralCacheAttributes = cattr;
    }

    /**
     * Tells you if the no wait is in the list or not.
     * <p>
     * @param noWait
     * @return true if the noWait is in the list.
     */
    public boolean containsNoWait( LateralCacheNoWait<K, V> noWait )
    {
        Optional<LateralCacheNoWait<K, V>> optional = Arrays.stream(noWaits)
                // we know noWait isn't null
                .filter(nw -> noWait.equals( nw ))
                .findFirst();

        return optional.isPresent();
    }

    /**
     * Adds a no wait to the list if it isn't already in the list.
     * <p>
     * @param noWait
     * @return true if it wasn't already contained
     */
    public synchronized boolean addNoWait( LateralCacheNoWait<K, V> noWait )
    {
        if ( noWait == null )
        {
            return false;
        }

        if ( containsNoWait( noWait ) )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "No Wait already contained, [" + noWait + "]" );
            }
            return false;
        }

        @SuppressWarnings("unchecked") // No generic arrays in java
        LateralCacheNoWait<K, V>[] newArray = new LateralCacheNoWait[noWaits.length + 1];

        System.arraycopy( noWaits, 0, newArray, 0, noWaits.length );

        // set the last position to the new noWait
        newArray[noWaits.length] = noWait;

        noWaits = newArray;

        return true;
    }

    /**
     * Removes a no wait from the list if it is already there.
     * <p>
     * @param noWait
     * @return true if it was already in the array
     */
    public synchronized boolean removeNoWait( LateralCacheNoWait<K, V> noWait )
    {
        if ( noWait == null )
        {
            return false;
        }

        int position = -1;
        for ( int i = 0; i < noWaits.length; i++ )
        {
            // we know noWait isn't null
            if ( noWait.equals( noWaits[i] ) )
            {
                position = i;
                break;
            }
        }

        if ( position == -1 )
        {
            return false;
        }

        @SuppressWarnings("unchecked") // No generic arrays in java
        LateralCacheNoWait<K, V>[] newArray = new LateralCacheNoWait[noWaits.length - 1];

        System.arraycopy( noWaits, 0, newArray, 0, position );
        if ( noWaits.length != position )
        {
            System.arraycopy( noWaits, position + 1, newArray, position, noWaits.length - position - 1 );
        }
        noWaits = newArray;

        return true;
    }

    /**
     * @param ce
     * @throws IOException
     */
    @Override
    public void update( ICacheElement<K, V> ce )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "updating through lateral cache facade, noWaits.length = " + noWaits.length );
        }

        for (LateralCacheNoWait<K, V> nw : noWaits)
        {
            nw.update( ce );
        }
    }

    /**
     * Synchronously reads from the lateral cache.
     * <p>
     * @param key
     * @return ICacheElement
     */
    @Override
    public ICacheElement<K, V> get( K key )
    {
        Optional<ICacheElement<K, V>> optional = Arrays.stream(noWaits)
            .map(nw -> nw.get( key ))
            .filter(obj -> obj != null)
            .findFirst();

        if (optional.isPresent())
        {
            return optional.get();
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
     * Synchronously reads from the lateral cache. Get a response from each! This will be slow.
     * Merge them.
     * <p>
     * @param pattern
     * @return ICacheElement
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatching(String pattern)
    {
        Map<K, ICacheElement<K, V>> elements = new HashMap<>();
        for (LateralCacheNoWait<K, V> nw : noWaits)
        {
            elements.putAll( nw.getMatching( pattern ) );
        }
        return elements;
    }

    /**
     * Return the keys in this cache.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet() throws IOException
    {
        HashSet<K> allKeys = new HashSet<>();
        for (LateralCacheNoWait<K, V> nw : noWaits)
        {
            if ( nw != null )
            {
                Set<K> keys = nw.getKeySet();
                if (keys != null)
                {
                    allKeys.addAll( keys );
                }
            }
        }
        return allKeys;
    }

    /**
     * Adds a remove request to the lateral cache.
     * <p>
     * @param key
     * @return always false.
     */
    @Override
    public boolean remove( K key )
    {
        Arrays.stream(noWaits).forEach(nw -> nw.remove( key ));
        return false;
    }

    /**
     * Adds a removeAll request to the lateral cache.
     */
    @Override
    public void removeAll()
    {
        Arrays.stream(noWaits).forEach(nw -> nw.removeAll());
    }

    /** Adds a dispose request to the lateral cache. */
    @Override
    public void dispose()
    {
        try
        {
            if ( listener != null )
            {
                listener.dispose();
                listener = null;
            }

            Arrays.stream(noWaits).forEach(nw -> nw.dispose());
        }
        finally
        {
            disposed = true;
        }
    }

    /**
     * No lateral invocation.
     * @return The size value
     */
    @Override
    public int getSize()
    {
        return 0;
        //cache.getSize();
    }

    /**
     * Gets the cacheType attribute of the LateralCacheNoWaitFacade object.
     * <p>
     * @return The cacheType value
     */
    @Override
    public CacheType getCacheType()
    {
        return CacheType.LATERAL_CACHE;
    }

    /**
     * Gets the cacheName attribute of the LateralCacheNoWaitFacade object.
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return "";
        //cache.getCacheName();
    }

    /**
     * Gets the status attribute of the LateralCacheNoWaitFacade object
     * @return The status value
     */
    @Override
    public CacheStatus getStatus()
    {
        if (disposed)
        {
            return CacheStatus.DISPOSED;
        }

        if (noWaits.length == 0 || listener != null)
        {
            return CacheStatus.ALIVE;
        }

        List<CacheStatus> statii = Arrays.stream(noWaits)
                .map(nw -> nw.getStatus())
                .collect(Collectors.toList());

        // It's alive if ANY of its nowaits is alive
        if (statii.contains(CacheStatus.ALIVE))
        {
            return CacheStatus.ALIVE;
        }
        // It's alive if ANY of its nowaits is in error, but
        // none are alive, then it's in error
        if (statii.contains(CacheStatus.ERROR))
        {
            return CacheStatus.ERROR;
        }

        // Otherwise, it's been disposed, since it's the only status left
        return CacheStatus.DISPOSED;
    }

    /**
     * @return Returns the AuxiliaryCacheAttributes.
     */
    @Override
    public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
    {
        return this.lateralCacheAttributes;
    }

    /**
     * @return "LateralCacheNoWaitFacade: " + cacheName;
     */
    @Override
    public String toString()
    {
        return "LateralCacheNoWaitFacade: " + cacheName;
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
     * getStats
     * @return String
     */
    @Override
    public String getStats()
    {
        return getStatistics().toString();
    }

    /**
     * @return IStats
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "Lateral Cache No Wait Facade" );

        ArrayList<IStatElement<?>> elems = new ArrayList<>();

        if ( noWaits != null )
        {
            elems.add(new StatElement<>( "Number of No Waits", Integer.valueOf(noWaits.length) ) );

            for ( LateralCacheNoWait<K, V> lcnw : noWaits )
            {
                if ( lcnw != null )
                {
                    // get the stats from the super too
                    IStats sStats = lcnw.getStatistics();
                    elems.addAll(sStats.getStatElements());
                }
            }
        }

        stats.setStatElements( elems );

        return stats;
    }
}
