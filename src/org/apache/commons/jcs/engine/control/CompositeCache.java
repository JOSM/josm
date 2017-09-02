package org.apache.commons.jcs.engine.control;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.access.exception.ObjectNotFoundException;
import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.engine.CacheConstants;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes.DiskUsagePattern;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.behavior.IRequireScheduler;
import org.apache.commons.jcs.engine.control.event.ElementEvent;
import org.apache.commons.jcs.engine.control.event.behavior.ElementEventType;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEvent;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEventHandler;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEventQueue;
import org.apache.commons.jcs.engine.control.group.GroupId;
import org.apache.commons.jcs.engine.match.KeyMatcherPatternImpl;
import org.apache.commons.jcs.engine.match.behavior.IKeyMatcher;
import org.apache.commons.jcs.engine.memory.behavior.IMemoryCache;
import org.apache.commons.jcs.engine.memory.lru.LRUMemoryCache;
import org.apache.commons.jcs.engine.memory.shrinking.ShrinkerThread;
import org.apache.commons.jcs.engine.stats.CacheStats;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.behavior.ICacheStats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the primary hub for a single cache/region. It controls the flow of items through the
 * cache. The auxiliary and memory caches are plugged in here.
 * <p>
 * This is the core of a JCS region. Hence, this simple class is the core of JCS.
 */
public class CompositeCache<K, V>
    implements ICache<K, V>, IRequireScheduler
{
    /** log instance */
    private static final Log log = LogFactory.getLog( CompositeCache.class );

    /**
     * EventQueue for handling element events. Lazy initialized. One for each region. To be more efficient, the manager
     * should pass a shared queue in.
     */
    private IElementEventQueue elementEventQ;

    /** Auxiliary caches. */
    @SuppressWarnings("unchecked") // OK because this is an empty array
    private AuxiliaryCache<K, V>[] auxCaches = new AuxiliaryCache[0];

    /** is this alive? */
    private AtomicBoolean alive;

    /** Region Elemental Attributes, default. */
    private IElementAttributes attr;

    /** Cache Attributes, for hub and memory auxiliary. */
    private ICompositeCacheAttributes cacheAttr;

    /** How many times update was called. */
    private AtomicInteger updateCount;

    /** How many times remove was called. */
    private AtomicInteger removeCount;

    /** Memory cache hit count */
    private AtomicInteger hitCountRam;

    /** Auxiliary cache hit count (number of times found in ANY auxiliary) */
    private AtomicInteger hitCountAux;

    /** Count of misses where element was not found. */
    private AtomicInteger missCountNotFound;

    /** Count of misses where element was expired. */
    private AtomicInteger missCountExpired;

    /** Cache manager. */
    private CompositeCacheManager cacheManager = null;

    /**
     * The cache hub can only have one memory cache. This could be made more flexible in the future,
     * but they are tied closely together. More than one doesn't make much sense.
     */
    private IMemoryCache<K, V> memCache;

    /** Key matcher used by the getMatching API */
    private IKeyMatcher<K> keyMatcher = new KeyMatcherPatternImpl<K>();

    private ScheduledFuture<?> future;

    /**
     * Constructor for the Cache object
     * <p>
     * @param cattr The cache attribute
     * @param attr The default element attributes
     */
    public CompositeCache( ICompositeCacheAttributes cattr, IElementAttributes attr )
    {
        this.attr = attr;
        this.cacheAttr = cattr;
        this.alive = new AtomicBoolean(true);
        this.updateCount = new AtomicInteger(0);
        this.removeCount = new AtomicInteger(0);
        this.hitCountRam = new AtomicInteger(0);
        this.hitCountAux = new AtomicInteger(0);
        this.missCountNotFound = new AtomicInteger(0);
        this.missCountExpired = new AtomicInteger(0);

        createMemoryCache( cattr );

        if ( log.isInfoEnabled() )
        {
            log.info( "Constructed cache with name [" + cacheAttr.getCacheName() + "] and cache attributes " + cattr );
        }
    }

    /**
     * Injector for Element event queue
     *
     * @param queue
     */
    public void setElementEventQueue( IElementEventQueue queue )
    {
        this.elementEventQ = queue;
    }

    /**
     * Injector for cache manager
     *
     * @param manager
     */
    public void setCompositeCacheManager( CompositeCacheManager manager )
    {
        this.cacheManager = manager;
    }

    /**
     * @see org.apache.commons.jcs.engine.behavior.IRequireScheduler#setScheduledExecutorService(java.util.concurrent.ScheduledExecutorService)
     */
    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutor)
    {
        if ( cacheAttr.isUseMemoryShrinker() )
        {
            future = scheduledExecutor.scheduleAtFixedRate(
                    new ShrinkerThread<K, V>(this), 0, cacheAttr.getShrinkerIntervalSeconds(),
                    TimeUnit.SECONDS);
        }
    }

    /**
     * This sets the list of auxiliary caches for this region.
     * <p>
     * @param auxCaches
     */
    public void setAuxCaches( AuxiliaryCache<K, V>[] auxCaches )
    {
        this.auxCaches = auxCaches;
    }

    /**
     * Get the list of auxiliary caches for this region.
     * <p>
     * @return an array of auxiliary caches, may be empty, never null
     */
    public AuxiliaryCache<K, V>[] getAuxCaches()
    {
        return this.auxCaches;
    }

    /**
     * Standard update method.
     * <p>
     * @param ce
     * @throws IOException
     */
    @Override
    public void update( ICacheElement<K, V> ce )
        throws IOException
    {
        update( ce, false );
    }

    /**
     * Standard update method.
     * <p>
     * @param ce
     * @throws IOException
     */
    public void localUpdate( ICacheElement<K, V> ce )
        throws IOException
    {
        update( ce, true );
    }

    /**
     * Put an item into the cache. If it is localOnly, then do no notify remote or lateral
     * auxiliaries.
     * <p>
     * @param cacheElement the ICacheElement&lt;K, V&gt;
     * @param localOnly Whether the operation should be restricted to local auxiliaries.
     * @throws IOException
     */
    protected void update( ICacheElement<K, V> cacheElement, boolean localOnly )
        throws IOException
    {

        if ( cacheElement.getKey() instanceof String
            && cacheElement.getKey().toString().endsWith( CacheConstants.NAME_COMPONENT_DELIMITER ) )
        {
            throw new IllegalArgumentException( "key must not end with " + CacheConstants.NAME_COMPONENT_DELIMITER
                + " for a put operation" );
        }
        else if ( cacheElement.getKey() instanceof GroupId )
        {
            throw new IllegalArgumentException( "key cannot be a GroupId " + " for a put operation" );
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "Updating memory cache " + cacheElement.getKey() );
        }

        updateCount.incrementAndGet();

        synchronized ( this )
        {
            memCache.update( cacheElement );
            updateAuxiliaries( cacheElement, localOnly );
        }

        cacheElement.getElementAttributes().setLastAccessTimeNow();
    }

    /**
     * This method is responsible for updating the auxiliaries if they are present. If it is local
     * only, any lateral and remote auxiliaries will not be updated.
     * <p>
     * Before updating an auxiliary it checks to see if the element attributes permit the operation.
     * <p>
     * Disk auxiliaries are only updated if the disk cache is not merely used as a swap. If the disk
     * cache is merely a swap, then items will only go to disk when they overflow from memory.
     * <p>
     * This is called by update( cacheElement, localOnly ) after it updates the memory cache.
     * <p>
     * This is protected to make it testable.
     * <p>
     * @param cacheElement
     * @param localOnly
     * @throws IOException
     */
    protected void updateAuxiliaries( ICacheElement<K, V> cacheElement, boolean localOnly )
        throws IOException
    {
        // UPDATE AUXILLIARY CACHES
        // There are 3 types of auxiliary caches: remote, lateral, and disk
        // more can be added if future auxiliary caches don't fit the model
        // You could run a database cache as either a remote or a local disk.
        // The types would describe the purpose.
        if ( log.isDebugEnabled() )
        {
            if ( auxCaches.length > 0 )
            {
                log.debug( "Updating auxiliary caches" );
            }
            else
            {
                log.debug( "No auxiliary cache to update" );
            }
        }

        for ( ICache<K, V> aux : auxCaches )
        {
            if ( aux == null )
            {
                continue;
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "Auxiliary cache type: " + aux.getCacheType() );
            }

            switch (aux.getCacheType())
            {
                // SEND TO REMOTE STORE
                case REMOTE_CACHE:
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "ce.getElementAttributes().getIsRemote() = "
                            + cacheElement.getElementAttributes().getIsRemote() );
                    }

                    if ( cacheElement.getElementAttributes().getIsRemote() && !localOnly )
                    {
                        try
                        {
                            // need to make sure the group cache understands that
                            // the key is a group attribute on update
                            aux.update( cacheElement );
                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Updated remote store for " + cacheElement.getKey() + cacheElement );
                            }
                        }
                        catch ( IOException ex )
                        {
                            log.error( "Failure in updateExclude", ex );
                        }
                    }
                    break;

                // SEND LATERALLY
                case LATERAL_CACHE:
                    // lateral can't do the checking since it is dependent on the
                    // cache region restrictions
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "lateralcache in aux list: cattr " + cacheAttr.isUseLateral() );
                    }
                    if ( cacheAttr.isUseLateral() && cacheElement.getElementAttributes().getIsLateral() && !localOnly )
                    {
                        // DISTRIBUTE LATERALLY
                        // Currently always multicast even if the value is
                        // unchanged, to cause the cache item to move to the front.
                        aux.update( cacheElement );
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "updated lateral cache for " + cacheElement.getKey() );
                        }
                    }
                    break;

                // update disk if the usage pattern permits
                case DISK_CACHE:
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "diskcache in aux list: cattr " + cacheAttr.isUseDisk() );
                    }
                    if ( cacheAttr.isUseDisk()
                        && cacheAttr.getDiskUsagePattern() == DiskUsagePattern.UPDATE
                        && cacheElement.getElementAttributes().getIsSpool() )
                    {
                        aux.update( cacheElement );
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "updated disk cache for " + cacheElement.getKey() );
                        }
                    }
                    break;

                default: // CACHE_HUB
                    break;
            }
        }
    }

    /**
     * Writes the specified element to any disk auxiliaries. Might want to rename this "overflow" in
     * case the hub wants to do something else.
     * <p>
     * If JCS is not configured to use the disk as a swap, that is if the the
     * CompositeCacheAttribute diskUsagePattern is not SWAP_ONLY, then the item will not be spooled.
     * <p>
     * @param ce The CacheElement
     */
    public void spoolToDisk( ICacheElement<K, V> ce )
    {
        // if the item is not spoolable, return
        if ( !ce.getElementAttributes().getIsSpool() )
        {
            // there is an event defined for this.
            handleElementEvent( ce, ElementEventType.SPOOLED_NOT_ALLOWED );
            return;
        }

        boolean diskAvailable = false;

        // SPOOL TO DISK.
        for ( ICache<K, V> aux : auxCaches )
        {
            if ( aux != null && aux.getCacheType() == CacheType.DISK_CACHE )
            {
                diskAvailable = true;

                if ( cacheAttr.getDiskUsagePattern() == DiskUsagePattern.SWAP )
                {
                    // write the last items to disk.2
                    try
                    {
                        handleElementEvent( ce, ElementEventType.SPOOLED_DISK_AVAILABLE );
                        aux.update( ce );
                    }
                    catch ( IOException ex )
                    {
                        // impossible case.
                        log.error( "Problem spooling item to disk cache.", ex );
                        throw new IllegalStateException( ex.getMessage() );
                    }

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "spoolToDisk done for: " + ce.getKey() + " on disk cache[" + aux.getCacheName() + "]" );
                    }
                }
                else
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "DiskCache available, but JCS is not configured to use the DiskCache as a swap." );
                    }
                }
            }
        }

        if ( !diskAvailable )
        {
            try
            {
                handleElementEvent( ce, ElementEventType.SPOOLED_DISK_NOT_AVAILABLE );
            }
            catch ( Exception e )
            {
                log.error( "Trouble handling the ELEMENT_EVENT_SPOOLED_DISK_NOT_AVAILABLE  element event", e );
            }
        }
    }

    /**
     * Gets an item from the cache.
     * <p>
     * @param key
     * @return element from the cache, or null if not present
     * @see org.apache.commons.jcs.engine.behavior.ICache#get(Object)
     */
    @Override
    public ICacheElement<K, V> get( K key )
    {
        return get( key, false );
    }

    /**
     * Do not try to go remote or laterally for this get.
     * <p>
     * @param key
     * @return ICacheElement
     */
    public ICacheElement<K, V> localGet( K key )
    {
        return get( key, true );
    }

    /**
     * Look in memory, then disk, remote, or laterally for this item. The order is dependent on the
     * order in the cache.ccf file.
     * <p>
     * Do not try to go remote or laterally for this get if it is localOnly. Otherwise try to go
     * remote or lateral if such an auxiliary is configured for this region.
     * <p>
     * @param key
     * @param localOnly
     * @return ICacheElement
     */
    protected ICacheElement<K, V> get( K key, boolean localOnly )
    {
        ICacheElement<K, V> element = null;

        boolean found = false;

        final boolean debugEnabled = log.isDebugEnabled(); // tested anyway but don't test it > once
        if (debugEnabled)
        {
            log.debug( "get: key = " + key + ", localOnly = " + localOnly );
        }

        synchronized (this)
        {
            try
            {
                // First look in memory cache
                element = memCache.get( key );

                if ( element != null )
                {
                    // Found in memory cache
                    if ( isExpired( element ) )
                    {
                        missCountExpired.incrementAndGet();
                        remove( key );
                        element = null;
                    }
                    else
                    {
                        // Update counters
                        hitCountRam.incrementAndGet();
                    }

                    found = true;
                }
                else
                {
                    // Item not found in memory. If local invocation look in aux
                    // caches, even if not local look in disk auxiliaries
                    for (AuxiliaryCache<K, V> aux : auxCaches)
                    {
                        if ( aux != null )
                        {
                            CacheType cacheType = aux.getCacheType();

                            if ( !localOnly || cacheType == CacheType.DISK_CACHE )
                            {
                                if (debugEnabled)
                                {
                                    log.debug( "Attempting to get from aux [" + aux.getCacheName() + "] which is of type: "
                                        + cacheType );
                                }

                                try
                                {
                                    element = aux.get( key );
                                }
                                catch ( IOException e )
                                {
                                    log.error( "Error getting from aux", e );
                                }
                            }

                            if (debugEnabled)
                            {
                                log.debug( "Got CacheElement: " + element );
                            }

                            // Item found in one of the auxiliary caches.
                            if ( element != null )
                            {
                                if ( isExpired( element ) )
                                {
                                    if (debugEnabled)
                                    {
                                        log.debug( cacheAttr.getCacheName() + " - Aux cache[" + aux.getCacheName() + "] hit, but element expired." );
                                    }

                                    missCountExpired.incrementAndGet();

                                    // This will tell the remotes to remove the item
                                    // based on the element's expiration policy. The elements attributes
                                    // associated with the item when it created govern its behavior
                                    // everywhere.
                                    remove( key );
                                    element = null;
                                }
                                else
                                {
                                    if (debugEnabled)
                                    {
                                        log.debug( cacheAttr.getCacheName() + " - Aux cache[" + aux.getCacheName() + "] hit" );
                                    }

                                    // Update counters
                                    hitCountAux.incrementAndGet();
                                    copyAuxiliaryRetrievedItemToMemory( element );
                                }

                                found = true;

                                break;
                            }
                        }
                    }
                }
            }
            catch ( IOException e )
            {
                log.error( "Problem encountered getting element.", e );
            }
        }

        if ( !found )
        {
            missCountNotFound.incrementAndGet();

            if (debugEnabled)
            {
                log.debug( cacheAttr.getCacheName() + " - Miss" );
            }
        }
        else if (debugEnabled) // we log here to avoid to log in the synchronized block
        {
            if (element == null)
            {
                log.debug( cacheAttr.getCacheName() + " - Memory cache hit, but element expired" );
            }
            else
            {
                log.debug( cacheAttr.getCacheName() + " - Memory cache hit" );
            }
        }

        if (element != null)
        {
            element.getElementAttributes().setLastAccessTimeNow();
        }

        return element;
    }

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param keys
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMultiple( Set<K> keys )
    {
        return getMultiple( keys, false );
    }

    /**
     * Gets multiple items from the cache based on the given set of keys. Do not try to go remote or
     * laterally for this data.
     * <p>
     * @param keys
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     */
    public Map<K, ICacheElement<K, V>> localGetMultiple( Set<K> keys )
    {
        return getMultiple( keys, true );
    }

    /**
     * Look in memory, then disk, remote, or laterally for these items. The order is dependent on
     * the order in the cache.ccf file. Keep looking in each cache location until either the element
     * is found, or the method runs out of places to look.
     * <p>
     * Do not try to go remote or laterally for this get if it is localOnly. Otherwise try to go
     * remote or lateral if such an auxiliary is configured for this region.
     * <p>
     * @param keys
     * @param localOnly
     * @return ICacheElement
     */
    protected Map<K, ICacheElement<K, V>> getMultiple( Set<K> keys, boolean localOnly )
    {
        Map<K, ICacheElement<K, V>> elements = new HashMap<K, ICacheElement<K, V>>();

        if ( log.isDebugEnabled() )
        {
            log.debug( "get: key = " + keys + ", localOnly = " + localOnly );
        }

        try
        {
            // First look in memory cache
            elements.putAll( getMultipleFromMemory( keys ) );

            // If fewer than all items were found in memory, then keep looking.
            if ( elements.size() != keys.size() )
            {
                Set<K> remainingKeys = pruneKeysFound( keys, elements );
                elements.putAll( getMultipleFromAuxiliaryCaches( remainingKeys, localOnly ) );
            }
        }
        catch ( IOException e )
        {
            log.error( "Problem encountered getting elements.", e );
        }

        // if we didn't find all the elements, increment the miss count by the number of elements not found
        if ( elements.size() != keys.size() )
        {
            missCountNotFound.addAndGet(keys.size() - elements.size());

            if ( log.isDebugEnabled() )
            {
                log.debug( cacheAttr.getCacheName() + " - " + ( keys.size() - elements.size() ) + " Misses" );
            }
        }

        return elements;
    }

    /**
     * Gets items for the keys in the set. Returns a map: key -> result.
     * <p>
     * @param keys
     * @return the elements found in the memory cache
     * @throws IOException
     */
    private Map<K, ICacheElement<K, V>> getMultipleFromMemory( Set<K> keys )
        throws IOException
    {
        Map<K, ICacheElement<K, V>> elementsFromMemory = memCache.getMultiple( keys );

        Iterator<ICacheElement<K, V>> elementFromMemoryIterator = new HashMap<K, ICacheElement<K, V>>( elementsFromMemory ).values().iterator();

        while ( elementFromMemoryIterator.hasNext() )
        {
            ICacheElement<K, V> element = elementFromMemoryIterator.next();

            if ( element != null )
            {
                // Found in memory cache
                if ( isExpired( element ) )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( cacheAttr.getCacheName() + " - Memory cache hit, but element expired" );
                    }

                    missCountExpired.incrementAndGet();
                    remove( element.getKey() );
                    elementsFromMemory.remove( element.getKey() );
                }
                else
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( cacheAttr.getCacheName() + " - Memory cache hit" );
                    }

                    // Update counters
                    hitCountRam.incrementAndGet();
                }
            }
        }
        return elementsFromMemory;
    }

    /**
     * If local invocation look in aux caches, even if not local look in disk auxiliaries.
     * <p>
     * @param keys
     * @param localOnly
     * @return the elements found in the auxiliary caches
     * @throws IOException
     */
    private Map<K, ICacheElement<K, V>> getMultipleFromAuxiliaryCaches( Set<K> keys, boolean localOnly )
        throws IOException
    {
        Map<K, ICacheElement<K, V>> elements = new HashMap<K, ICacheElement<K, V>>();
        Set<K> remainingKeys = new HashSet<K>( keys );

        for ( AuxiliaryCache<K, V> aux : auxCaches )
        {
            if ( aux != null )
            {
                Map<K, ICacheElement<K, V>> elementsFromAuxiliary =
                    new HashMap<K, ICacheElement<K, V>>();

                CacheType cacheType = aux.getCacheType();

                if ( !localOnly || cacheType == CacheType.DISK_CACHE )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Attempting to get from aux [" + aux.getCacheName() + "] which is of type: "
                            + cacheType );
                    }

                    try
                    {
                        elementsFromAuxiliary.putAll( aux.getMultiple( remainingKeys ) );
                    }
                    catch ( IOException e )
                    {
                        log.error( "Error getting from aux", e );
                    }
                }

                if ( log.isDebugEnabled() )
                {
                    log.debug( "Got CacheElements: " + elementsFromAuxiliary );
                }

                processRetrievedElements( aux, elementsFromAuxiliary );

                elements.putAll( elementsFromAuxiliary );

                if ( elements.size() == keys.size() )
                {
                    break;
                }
                else
                {
                    remainingKeys = pruneKeysFound( keys, elements );
                }
            }
        }

        return elements;
    }

    /**
     * Build a map of all the matching elements in all of the auxiliaries and memory.
     * <p>
     * @param pattern
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any matching keys
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatching( String pattern )
    {
        return getMatching( pattern, false );
    }

    /**
     * Build a map of all the matching elements in all of the auxiliaries and memory. Do not try to
     * go remote or laterally for this data.
     * <p>
     * @param pattern
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any matching keys
     */
    public Map<K, ICacheElement<K, V>> localGetMatching( String pattern )
    {
        return getMatching( pattern, true );
    }

    /**
     * Build a map of all the matching elements in all of the auxiliaries and memory. Items in
     * memory will replace from the auxiliaries in the returned map. The auxiliaries are accessed in
     * opposite order. It's assumed that those closer to home are better.
     * <p>
     * Do not try to go remote or laterally for this get if it is localOnly. Otherwise try to go
     * remote or lateral if such an auxiliary is configured for this region.
     * <p>
     * @param pattern
     * @param localOnly
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any matching keys
     */
    protected Map<K, ICacheElement<K, V>> getMatching( String pattern, boolean localOnly )
    {
        Map<K, ICacheElement<K, V>> elements = new HashMap<K, ICacheElement<K, V>>();

        if ( log.isDebugEnabled() )
        {
            log.debug( "get: pattern [" + pattern + "], localOnly = " + localOnly );
        }

        try
        {
            // First look in auxiliaries
            elements.putAll( getMatchingFromAuxiliaryCaches( pattern, localOnly ) );

            // then look in memory, override aux with newer memory items.
            elements.putAll( getMatchingFromMemory( pattern ) );
        }
        catch ( Exception e )
        {
            log.error( "Problem encountered getting elements.", e );
        }

        return elements;
    }

    /**
     * Gets the key array from the memcache. Builds a set of matches. Calls getMultiple with the
     * set. Returns a map: key -&gt; result.
     * <p>
     * @param pattern
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any matching keys
     * @throws IOException
     */
    protected Map<K, ICacheElement<K, V>> getMatchingFromMemory( String pattern )
        throws IOException
    {
        // find matches in key array
        // this avoids locking the memory cache, but it uses more memory
        Set<K> keyArray = memCache.getKeySet();

        Set<K> matchingKeys = getKeyMatcher().getMatchingKeysFromArray( pattern, keyArray );

        // call get multiple
        return getMultipleFromMemory( matchingKeys );
    }

    /**
     * If local invocation look in aux caches, even if not local look in disk auxiliaries.
     * <p>
     * Moves in reverse order of definition. This will allow you to override those that are from the
     * remote with those on disk.
     * <p>
     * @param pattern
     * @param localOnly
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any matching keys
     * @throws IOException
     */
    private Map<K, ICacheElement<K, V>> getMatchingFromAuxiliaryCaches( String pattern, boolean localOnly )
        throws IOException
    {
        Map<K, ICacheElement<K, V>> elements = new HashMap<K, ICacheElement<K, V>>();

        for ( int i = auxCaches.length - 1; i >= 0; i-- )
        {
            AuxiliaryCache<K, V> aux = auxCaches[i];

            if ( aux != null )
            {
                Map<K, ICacheElement<K, V>> elementsFromAuxiliary =
                    new HashMap<K, ICacheElement<K, V>>();

                CacheType cacheType = aux.getCacheType();

                if ( !localOnly || cacheType == CacheType.DISK_CACHE )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Attempting to get from aux [" + aux.getCacheName() + "] which is of type: "
                            + cacheType );
                    }

                    try
                    {
                        elementsFromAuxiliary.putAll( aux.getMatching( pattern ) );
                    }
                    catch ( IOException e )
                    {
                        log.error( "Error getting from aux", e );
                    }

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Got CacheElements: " + elementsFromAuxiliary );
                    }

                    processRetrievedElements( aux, elementsFromAuxiliary );

                    elements.putAll( elementsFromAuxiliary );
                }
            }
        }

        return elements;
    }

    /**
     * Remove expired elements retrieved from an auxiliary. Update memory with good items.
     * <p>
     * @param aux the auxiliary cache instance
     * @param elementsFromAuxiliary
     * @throws IOException
     */
    private void processRetrievedElements( AuxiliaryCache<K, V> aux, Map<K, ICacheElement<K, V>> elementsFromAuxiliary )
        throws IOException
    {
        Iterator<ICacheElement<K, V>> elementFromAuxiliaryIterator = new HashMap<K, ICacheElement<K, V>>( elementsFromAuxiliary ).values().iterator();

        while ( elementFromAuxiliaryIterator.hasNext() )
        {
            ICacheElement<K, V> element = elementFromAuxiliaryIterator.next();

            // Item found in one of the auxiliary caches.
            if ( element != null )
            {
                if ( isExpired( element ) )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( cacheAttr.getCacheName() + " - Aux cache[" + aux.getCacheName() + "] hit, but element expired." );
                    }

                    missCountExpired.incrementAndGet();

                    // This will tell the remote caches to remove the item
                    // based on the element's expiration policy. The elements attributes
                    // associated with the item when it created govern its behavior
                    // everywhere.
                    remove( element.getKey() );
                    elementsFromAuxiliary.remove( element.getKey() );
                }
                else
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( cacheAttr.getCacheName() + " - Aux cache[" + aux.getCacheName() + "] hit" );
                    }

                    // Update counters
                    hitCountAux.incrementAndGet();
                    copyAuxiliaryRetrievedItemToMemory( element );
                }
            }
        }
    }

    /**
     * Copies the item to memory if the memory size is greater than 0. Only spool if the memory
     * cache size is greater than 0, else the item will immediately get put into purgatory.
     * <p>
     * @param element
     * @throws IOException
     */
    private void copyAuxiliaryRetrievedItemToMemory( ICacheElement<K, V> element )
        throws IOException
    {
        if ( memCache.getCacheAttributes().getMaxObjects() > 0 )
        {
            memCache.update( element );
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Skipping memory update since no items are allowed in memory" );
            }
        }
    }

    /**
     * Returns a set of keys that were not found.
     * <p>
     * @param keys
     * @param foundElements
     * @return the original set of cache keys, minus any cache keys present in the map keys of the
     *         foundElements map
     */
    private Set<K> pruneKeysFound( Set<K> keys, Map<K, ICacheElement<K, V>> foundElements )
    {
        Set<K> remainingKeys = new HashSet<K>( keys );

        for (K key : foundElements.keySet())
        {
            remainingKeys.remove( key );
        }

        return remainingKeys;
    }

    /**
     * Get a set of the keys for all elements in the cache
     * <p>
     * @return A set of the key type
     */
    public Set<K> getKeySet()
    {
        return getKeySet(false);
    }

    /**
     * Get a set of the keys for all elements in the cache
     * <p>
     * @param localOnly true if only memory keys are requested
     *
     * @return A set of the key type
     */
    public Set<K> getKeySet(boolean localOnly)
    {
        HashSet<K> allKeys = new HashSet<K>();

        allKeys.addAll( memCache.getKeySet() );
        for ( AuxiliaryCache<K, V> aux : auxCaches )
        {
            if ( aux != null )
            {
                if(!localOnly || aux.getCacheType() == CacheType.DISK_CACHE)
                {
                    try
                    {
                        allKeys.addAll( aux.getKeySet() );
                    }
                    catch ( IOException e )
                    {
                        // ignore
                    }
                }
            }
        }
        return allKeys;
    }

    /**
     * Removes an item from the cache.
     * <p>
     * @param key
     * @return true is it was removed
     * @see org.apache.commons.jcs.engine.behavior.ICache#remove(Object)
     */
    @Override
    public boolean remove( K key )
    {
        return remove( key, false );
    }

    /**
     * Do not propagate removeall laterally or remotely.
     * <p>
     * @param key
     * @return true if the item was already in the cache.
     */
    public boolean localRemove( K key )
    {
        return remove( key, true );
    }

    /**
     * fromRemote: If a remove call was made on a cache with both, then the remote should have been
     * called. If it wasn't then the remote is down. we'll assume it is down for all. If it did come
     * from the remote then the cache is remotely configured and lateral removal is unnecessary. If
     * it came laterally then lateral removal is unnecessary. Does this assume that there is only
     * one lateral and remote for the cache? Not really, the initial removal should take care of the
     * problem if the source cache was similarly configured. Otherwise the remote cache, if it had
     * no laterals, would remove all the elements from remotely configured caches, but if those
     * caches had some other weird laterals that were not remotely configured, only laterally
     * propagated then they would go out of synch. The same could happen for multiple remotes. If
     * this looks necessary we will need to build in an identifier to specify the source of a
     * removal.
     * <p>
     * @param key
     * @param localOnly
     * @return true if the item was in the cache, else false
     */
    protected boolean remove( K key, boolean localOnly )
    {
        removeCount.incrementAndGet();

        boolean removed = false;

        synchronized (this)
        {
            try
            {
                removed = memCache.remove( key );
            }
            catch ( IOException e )
            {
                log.error( e );
            }

            // Removes from all auxiliary caches.
            for ( ICache<K, V> aux : auxCaches )
            {
                if ( aux == null )
                {
                    continue;
                }

                CacheType cacheType = aux.getCacheType();

                // for now let laterals call remote remove but not vice versa

                if ( localOnly && ( cacheType == CacheType.REMOTE_CACHE || cacheType == CacheType.LATERAL_CACHE ) )
                {
                    continue;
                }
                try
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Removing " + key + " from cacheType" + cacheType );
                    }

                    boolean b = aux.remove( key );

                    // Don't take the remote removal into account.
                    if ( !removed && cacheType != CacheType.REMOTE_CACHE )
                    {
                        removed = b;
                    }
                }
                catch ( IOException ex )
                {
                    log.error( "Failure removing from aux", ex );
                }
            }
        }

        return removed;
    }

    /**
     * Clears the region. This command will be sent to all auxiliaries. Some auxiliaries, such as
     * the JDBC disk cache, can be configured to not honor removeAll requests.
     * <p>
     * @see org.apache.commons.jcs.engine.behavior.ICache#removeAll()
     */
    @Override
    public void removeAll()
        throws IOException
    {
        removeAll( false );
    }

    /**
     * Will not pass the remove message remotely.
     * <p>
     * @throws IOException
     */
    public void localRemoveAll()
        throws IOException
    {
        removeAll( true );
    }

    /**
     * Removes all cached items.
     * <p>
     * @param localOnly must pass in false to get remote and lateral aux's updated. This prevents
     *            looping.
     * @throws IOException
     */
    protected void removeAll( boolean localOnly )
        throws IOException
    {
        synchronized (this)
        {
            try
            {
                memCache.removeAll();

                if ( log.isDebugEnabled() )
                {
                    log.debug( "Removed All keys from the memory cache." );
                }
            }
            catch ( IOException ex )
            {
                log.error( "Trouble updating memory cache.", ex );
            }

            // Removes from all auxiliary disk caches.
            for ( ICache<K, V> aux : auxCaches )
            {
                if ( aux != null && ( aux.getCacheType() == CacheType.DISK_CACHE || !localOnly ) )
                {
                    try
                    {
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Removing All keys from cacheType" + aux.getCacheType() );
                        }

                        aux.removeAll();
                    }
                    catch ( IOException ex )
                    {
                        log.error( "Failure removing all from aux", ex );
                    }
                }
            }
        }
    }

    /**
     * Flushes all cache items from memory to auxiliary caches and close the auxiliary caches.
     */
    @Override
    public void dispose()
    {
        dispose( false );
    }

    /**
     * Invoked only by CacheManager. This method disposes of the auxiliaries one by one. For the
     * disk cache, the items in memory are freed, meaning that they will be sent through the
     * overflow channel to disk. After the auxiliaries are disposed, the memory cache is disposed.
     * <p>
     * @param fromRemote
     */
    public void dispose( boolean fromRemote )
    {
         // If already disposed, return immediately
        if ( alive.compareAndSet(true, false) == false )
        {
            return;
        }

        if ( log.isInfoEnabled() )
        {
            log.info( "In DISPOSE, [" + this.cacheAttr.getCacheName() + "] fromRemote [" + fromRemote + "]" );
        }

        synchronized (this)
        {
            // Remove us from the cache managers list
            // This will call us back but exit immediately
            if (cacheManager != null)
            {
                cacheManager.freeCache(getCacheName(), fromRemote);
            }

            // Try to stop shrinker thread
            if (future != null)
            {
                future.cancel(true);
            }

            // Now, shut down the event queue
            if (elementEventQ != null)
            {
                elementEventQ.dispose();
                elementEventQ = null;
            }

            // Dispose of each auxiliary cache, Remote auxiliaries will be
            // skipped if 'fromRemote' is true.
            for ( ICache<K, V> aux : auxCaches )
            {
                try
                {
                    // Skip this auxiliary if:
                    // - The auxiliary is null
                    // - The auxiliary is not alive
                    // - The auxiliary is remote and the invocation was remote
                    if ( aux == null || aux.getStatus() != CacheStatus.ALIVE
                        || ( fromRemote && aux.getCacheType() == CacheType.REMOTE_CACHE ) )
                    {
                        if ( log.isInfoEnabled() )
                        {
                            log.info( "In DISPOSE, [" + this.cacheAttr.getCacheName() + "] SKIPPING auxiliary [" + aux.getCacheName() + "] fromRemote ["
                                + fromRemote + "]" );
                        }
                        continue;
                    }

                    if ( log.isInfoEnabled() )
                    {
                        log.info( "In DISPOSE, [" + this.cacheAttr.getCacheName() + "] auxiliary [" + aux.getCacheName() + "]" );
                    }

                    // IT USED TO BE THE CASE THAT (If the auxiliary is not a lateral, or the cache
                    // attributes
                    // have 'getUseLateral' set, all the elements currently in
                    // memory are written to the lateral before disposing)
                    // I changed this. It was excessive. Only the disk cache needs the items, since only
                    // the disk cache is in a situation to not get items on a put.
                    if ( aux.getCacheType() == CacheType.DISK_CACHE )
                    {
                        int numToFree = memCache.getSize();
                        memCache.freeElements( numToFree );

                        if ( log.isInfoEnabled() )
                        {
                            log.info( "In DISPOSE, [" + this.cacheAttr.getCacheName() + "] put " + numToFree + " into auxiliary " + aux.getCacheName() );
                        }
                    }

                    // Dispose of the auxiliary
                    aux.dispose();
                }
                catch ( IOException ex )
                {
                    log.error( "Failure disposing of aux.", ex );
                }
            }

            if ( log.isInfoEnabled() )
            {
                log.info( "In DISPOSE, [" + this.cacheAttr.getCacheName() + "] disposing of memory cache." );
            }
            try
            {
                memCache.dispose();
            }
            catch ( IOException ex )
            {
                log.error( "Failure disposing of memCache", ex );
            }
        }
    }

    /**
     * Calling save cause the entire contents of the memory cache to be flushed to all auxiliaries.
     * Though this put is extremely fast, this could bog the cache and should be avoided. The
     * dispose method should call a version of this. Good for testing.
     */
    public void save()
    {
        if ( alive.compareAndSet(true, false) == false )
        {
            return;
        }

        synchronized ( this )
        {
            for ( ICache<K, V> aux : auxCaches )
            {
                try
                {
                    if ( aux.getStatus() == CacheStatus.ALIVE )
                    {
                        for (K key : memCache.getKeySet())
                        {
                            ICacheElement<K, V> ce = memCache.get(key);

                            if (ce != null)
                            {
                                aux.update( ce );
                            }
                        }
                    }
                }
                catch ( IOException ex )
                {
                    log.error( "Failure saving aux caches.", ex );
                }
            }
        }
        if ( log.isDebugEnabled() )
        {
            log.debug( "Called save for [" + cacheAttr.getCacheName() + "]" );
        }
    }

    /**
     * Gets the size attribute of the Cache object. This return the number of elements, not the byte
     * size.
     * <p>
     * @return The size value
     */
    @Override
    public int getSize()
    {
        return memCache.getSize();
    }

    /**
     * Gets the cacheType attribute of the Cache object.
     * <p>
     * @return The cacheType value
     */
    @Override
    public CacheType getCacheType()
    {
        return CacheType.CACHE_HUB;
    }

    /**
     * Gets the status attribute of the Cache object.
     * <p>
     * @return The status value
     */
    @Override
    public CacheStatus getStatus()
    {
        return alive.get() ? CacheStatus.ALIVE : CacheStatus.DISPOSED;
    }

    /**
     * Gets stats for debugging.
     * <p>
     * @return String
     */
    @Override
    public String getStats()
    {
        return getStatistics().toString();
    }

    /**
     * This returns data gathered for this region and all the auxiliaries it currently uses.
     * <p>
     * @return Statistics and Info on the Region.
     */
    public ICacheStats getStatistics()
    {
        ICacheStats stats = new CacheStats();
        stats.setRegionName( this.getCacheName() );

        // store the composite cache stats first
        ArrayList<IStatElement<?>> elems = new ArrayList<IStatElement<?>>();

        elems.add(new StatElement<Integer>( "HitCountRam", Integer.valueOf(getHitCountRam()) ) );
        elems.add(new StatElement<Integer>( "HitCountAux", Integer.valueOf(getHitCountAux()) ) );

        stats.setStatElements( elems );

        // memory + aux, memory is not considered an auxiliary internally
        int total = auxCaches.length + 1;
        ArrayList<IStats> auxStats = new ArrayList<IStats>(total);

        auxStats.add(getMemoryCache().getStatistics());

        for ( AuxiliaryCache<K, V> aux : auxCaches )
        {
            auxStats.add(aux.getStatistics());
        }

        // store the auxiliary stats
        stats.setAuxiliaryCacheStats( auxStats );

        return stats;
    }

    /**
     * Gets the cacheName attribute of the Cache object. This is also known as the region name.
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return cacheAttr.getCacheName();
    }

    /**
     * Gets the default element attribute of the Cache object This returns a copy. It does not
     * return a reference to the attributes.
     * <p>
     * @return The attributes value
     */
    public IElementAttributes getElementAttributes()
    {
        if ( attr != null )
        {
            return attr.clone();
        }
        return null;
    }

    /**
     * Sets the default element attribute of the Cache object.
     * <p>
     * @param attr
     */
    public void setElementAttributes( IElementAttributes attr )
    {
        this.attr = attr;
    }

    /**
     * Gets the ICompositeCacheAttributes attribute of the Cache object.
     * <p>
     * @return The ICompositeCacheAttributes value
     */
    public ICompositeCacheAttributes getCacheAttributes()
    {
        return this.cacheAttr;
    }

    /**
     * Sets the ICompositeCacheAttributes attribute of the Cache object.
     * <p>
     * @param cattr The new ICompositeCacheAttributes value
     */
    public void setCacheAttributes( ICompositeCacheAttributes cattr )
    {
        this.cacheAttr = cattr;
        // need a better way to do this, what if it is in error
        this.memCache.initialize( this );
    }

    /**
     * Gets the elementAttributes attribute of the Cache object.
     * <p>
     * @param key
     * @return The elementAttributes value
     * @throws CacheException
     * @throws IOException
     */
    public IElementAttributes getElementAttributes( K key )
        throws CacheException, IOException
    {
        ICacheElement<K, V> ce = get( key );
        if ( ce == null )
        {
            throw new ObjectNotFoundException( "key " + key + " is not found" );
        }
        return ce.getElementAttributes();
    }

    /**
     * Determine if the element is expired based on the values of the element attributes
     *
     * @param element the element
     *
     * @return true if the element is expired
     */
    public boolean isExpired( ICacheElement<K, V> element)
    {
        return isExpired(element, System.currentTimeMillis(),
                ElementEventType.EXCEEDED_MAXLIFE_ONREQUEST,
                ElementEventType.EXCEEDED_IDLETIME_ONREQUEST );
    }

    /**
     * Check if the element is expired based on the values of the element attributes
     *
     * @param element the element
     * @param timestamp the timestamp to compare to
     * @param eventMaxlife the event to fire in case the max life time is exceeded
     * @param eventIdle the event to fire in case the idle time is exceeded
     *
     * @return true if the element is expired
     */
    public boolean isExpired(ICacheElement<K, V> element, long timestamp,
            ElementEventType eventMaxlife, ElementEventType eventIdle)
    {
        try
        {
            IElementAttributes attributes = element.getElementAttributes();

            if ( !attributes.getIsEternal() )
            {
                // Remove if maxLifeSeconds exceeded

                long maxLifeSeconds = attributes.getMaxLife();
                long createTime = attributes.getCreateTime();

                final long timeFactorForMilliseconds = attributes.getTimeFactorForMilliseconds();

                if ( maxLifeSeconds != -1 && ( timestamp - createTime ) > ( maxLifeSeconds * timeFactorForMilliseconds) )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Exceeded maxLife: " + element.getKey() );
                    }

                    handleElementEvent( element, eventMaxlife );

                    return true;
                }
                long idleTime = attributes.getIdleTime();
                long lastAccessTime = attributes.getLastAccessTime();

                // Remove if maxIdleTime exceeded
                // If you have a 0 size memory cache, then the last access will
                // not get updated.
                // you will need to set the idle time to -1.

                if ( ( idleTime != -1 ) && ( timestamp - lastAccessTime ) > idleTime * timeFactorForMilliseconds )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Exceeded maxIdle: " + element.getKey() );
                    }

                    handleElementEvent( element, eventIdle );

                    return true;
                }
            }
        }
        catch ( Exception e )
        {
            log.error( "Error determining expiration period, expiring", e );

            return true;
        }

        return false;
    }

    /**
     * If there are event handlers for the item, then create an event and queue it up.
     * <p>
     * This does not call handle directly; instead the handler and the event are put into a queue.
     * This prevents the event handling from blocking normal cache operations.
     * <p>
     * @param element the item
     * @param eventType the event type
     */
    public void handleElementEvent( ICacheElement<K, V> element, ElementEventType eventType )
    {
        ArrayList<IElementEventHandler> eventHandlers = element.getElementAttributes().getElementEventHandlers();
        if ( eventHandlers != null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Element Handlers are registered.  Create event type " + eventType );
            }
            if ( elementEventQ == null )
            {
                log.warn("No element event queue available for cache " + getCacheName());
                return;
            }
            IElementEvent<ICacheElement<K, V>> event = new ElementEvent<ICacheElement<K, V>>( element, eventType );
            for (IElementEventHandler hand : eventHandlers)
            {
                try
                {
                   elementEventQ.addElementEvent( hand, event );
                }
                catch ( IOException e )
                {
                    log.error( "Trouble adding element event to queue", e );
                }
            }
        }
    }

    /**
     * Create the MemoryCache based on the config parameters.
     * TODO: consider making this an auxiliary, despite its close tie to the CacheHub.
     * TODO: might want to create a memory cache config file separate from that of the hub -- ICompositeCacheAttributes
     * <p>
     * @param cattr
     */
    private void createMemoryCache( ICompositeCacheAttributes cattr )
    {
        if ( memCache == null )
        {
            try
            {
                Class<?> c = Class.forName( cattr.getMemoryCacheName() );
                @SuppressWarnings("unchecked") // Need cast
                IMemoryCache<K, V> newInstance = (IMemoryCache<K, V>) c.newInstance();
                memCache = newInstance;
                memCache.initialize( this );
            }
            catch ( Exception e )
            {
                log.warn( "Failed to init mem cache, using: LRUMemoryCache", e );

                this.memCache = new LRUMemoryCache<K, V>();
                this.memCache.initialize( this );
            }
        }
        else
        {
            log.warn( "Refusing to create memory cache -- already exists." );
        }
    }

    /**
     * Access to the memory cache for instrumentation.
     * <p>
     * @return the MemoryCache implementation
     */
    public IMemoryCache<K, V> getMemoryCache()
    {
        return memCache;
    }

    /**
     * Number of times a requested item was found in the memory cache.
     * <p>
     * @return number of hits in memory
     */
    public int getHitCountRam()
    {
        return hitCountRam.get();
    }

    /**
     * Number of times a requested item was found in and auxiliary cache.
     * @return number of auxiliary hits.
     */
    public int getHitCountAux()
    {
        return hitCountAux.get();
    }

    /**
     * Number of times a requested element was not found.
     * @return number of misses.
     */
    public int getMissCountNotFound()
    {
        return missCountNotFound.get();
    }

    /**
     * Number of times a requested element was found but was expired.
     * @return number of found but expired gets.
     */
    public int getMissCountExpired()
    {
        return missCountExpired.get();
    }

    /**
     * @return Returns the updateCount.
     */
    public int getUpdateCount()
    {
        return updateCount.get();
    }

    /**
     * Sets the key matcher used by get matching.
     * <p>
     * @param keyMatcher
     */
    @Override
    public void setKeyMatcher( IKeyMatcher<K> keyMatcher )
    {
        if ( keyMatcher != null )
        {
            this.keyMatcher = keyMatcher;
        }
    }

    /**
     * Returns the key matcher used by get matching.
     * <p>
     * @return keyMatcher
     */
    public IKeyMatcher<K> getKeyMatcher()
    {
        return this.keyMatcher;
    }

    /**
     * This returns the stats.
     * <p>
     * @return getStats()
     */
    @Override
    public String toString()
    {
        return getStats();
    }
}
