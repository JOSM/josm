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

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** An abstract base for the No Wait Facade.  Different implementations will failover differently. */
public abstract class AbstractRemoteCacheNoWaitFacade<K, V>
    extends AbstractAuxiliaryCache<K, V>
{
    /** log instance */
    private static final Log log = LogFactory.getLog( AbstractRemoteCacheNoWaitFacade.class );

    /** The connection to a remote server, or a zombie. */
    public RemoteCacheNoWait<K, V>[] noWaits; // TODO privatise if possible

    /** The cache name */
    private final String cacheName;

    /** holds failover and cluster information */
    private IRemoteCacheAttributes remoteCacheAttributes;

    /** A cache manager */
    private ICompositeCacheManager compositeCacheManager;

    /**
     * Constructs with the given remote cache, and fires events to any listeners.
     * <p>
     * @param noWaits
     * @param rca
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     */
    public AbstractRemoteCacheNoWaitFacade( RemoteCacheNoWait<K, V>[] noWaits, RemoteCacheAttributes rca,
                                    ICompositeCacheManager cacheMgr, ICacheEventLogger cacheEventLogger,
                                    IElementSerializer elementSerializer )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "CONSTRUCTING NO WAIT FACADE" );
        }
        this.noWaits = noWaits;
        this.remoteCacheAttributes = rca;
        this.cacheName = rca.getCacheName();
        setCompositeCacheManager( cacheMgr );
        setCacheEventLogger( cacheEventLogger );
        setElementSerializer( elementSerializer );
    }

    /**
     * Put an element in the cache.
     * <p>
     * @param ce
     * @throws IOException
     */
    @Override
    public void update( ICacheElement<K, V> ce )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "updating through cache facade, noWaits.length = " + noWaits.length );
        }
        int i = 0;
        try
        {
            for ( ; i < noWaits.length; i++ )
            {
                noWaits[i].update( ce );
                // an initial move into a zombie will lock this to primary
                // recovery. will not discover other servers until primary
                // reconnect
                // and subsequent error
            }
        }
        catch ( Exception ex )
        {
            String message = "Problem updating no wait.  Will initiate failover if the noWait is in error.";
            log.error( message, ex );

            if ( getCacheEventLogger() != null )
            {
                getCacheEventLogger().logError(
                                                "RemoteCacheNoWaitFacade",
                                                ICacheEventLogger.UPDATE_EVENT,
                                                message + ":" + ex.getMessage() + " REGION: " + ce.getCacheName()
                                                    + " ELEMENT: " + ce );
            }

            // can handle failover here? Is it safe to try the others?
            // check to see it the noWait is now a zombie
            // if it is a zombie, then move to the next in the failover list
            // will need to keep them in order or a count
            failover( i );
            // should start a failover thread
            // should probably only failover if there is only one in the noWait
            // list
            // Should start a background thread to restore the original primary if we are in failover state.
        }
    }

    /**
     * Synchronously reads from the remote cache.
     * <p>
     * @param key
     * @return Either an ICacheElement<K, V> or null if it is not found.
     */
    @Override
    public ICacheElement<K, V> get( K key )
    {
        for ( int i = 0; i < noWaits.length; i++ )
        {
            try
            {
                ICacheElement<K, V> obj = noWaits[i].get( key );
                if ( obj != null )
                {
                    return obj;
                }
            }
            catch ( IOException ex )
            {
                log.debug( "Failed to get." );
                return null;
            }
        }
        return null;
    }

    /**
     * Synchronously read from the remote cache.
     * <p>
     * @param pattern
     * @return map
     * @throws IOException
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatching( String pattern )
        throws IOException
    {
        for ( int i = 0; i < noWaits.length; i++ )
        {
            try
            {
                return noWaits[i].getMatching( pattern );
            }
            catch ( IOException ex )
            {
                log.debug( "Failed to getMatching." );
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Gets multiple items from the cache based on the given set of keys.
     * <p>
     * @param keys
     * @return a map of K key to ICacheElement<K, V> element, or an empty map if there is no
     *         data in cache for any of these keys
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMultiple( Set<K> keys )
    {
        if ( keys != null && !keys.isEmpty() )
        {
            for ( int i = 0; i < noWaits.length; i++ )
            {
                try
                {
                    return noWaits[i].getMultiple( keys );
                }
                catch ( IOException ex )
                {
                    log.debug( "Failed to get." );
                }
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
        HashSet<K> allKeys = new HashSet<K>();
        for ( int i = 0; i < noWaits.length; i++ )
        {
            AuxiliaryCache<K, V> aux = noWaits[i];
            if ( aux != null )
            {
                Set<K> keys = aux.getKeySet();
                if(keys != null)
                {
                    allKeys.addAll( keys );
                }
            }
        }
        return allKeys;
    }

    /**
     * Adds a remove request to the remote cache.
     * <p>
     * @param key
     * @return whether or not it was removed, right now it return false.
     */
    @Override
    public boolean remove( K key )
    {
        try
        {
            for ( int i = 0; i < noWaits.length; i++ )
            {
                noWaits[i].remove( key );
            }
        }
        catch ( Exception ex )
        {
            log.error( ex );
        }
        return false;
    }

    /**
     * Adds a removeAll request to the remote cache.
     */
    @Override
    public void removeAll()
    {
        try
        {
            for ( int i = 0; i < noWaits.length; i++ )
            {
                noWaits[i].removeAll();
            }
        }
        catch ( Exception ex )
        {
            log.error( ex );
        }
    }

    /** Adds a dispose request to the remote cache. */
    @Override
    public void dispose()
    {
        try
        {
            for ( int i = 0; i < noWaits.length; i++ )
            {
                noWaits[i].dispose();
            }
        }
        catch ( Exception ex )
        {
            log.error( "Problem in dispose.", ex );
        }
    }

    /**
     * No remote invocation.
     * <p>
     * @return The size value
     */
    @Override
    public int getSize()
    {
        return 0;
        // cache.getSize();
    }

    /**
     * Gets the cacheType attribute of the RemoteCacheNoWaitFacade object.
     * <p>
     * @return The cacheType value
     */
    @Override
    public CacheType getCacheType()
    {
        return CacheType.REMOTE_CACHE;
    }

    /**
     * Gets the cacheName attribute of the RemoteCacheNoWaitFacade object.
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return remoteCacheAttributes.getCacheName();
    }

    /**
     * Gets the status attribute of the RemoteCacheNoWaitFacade object
     * <p>
     * Return ALIVE if any are alive.
     * <p>
     * @return The status value
     */
    @Override
    public CacheStatus getStatus()
    {
        for ( int i = 0; i < noWaits.length; i++ )
        {
            if ( noWaits[i].getStatus() == CacheStatus.ALIVE )
            {
                return CacheStatus.ALIVE;
            }
        }

        return CacheStatus.DISPOSED;
    }

    /**
     * String form of some of the configuration information for the remote cache.
     * <p>
     * @return Some info for logging.
     */
    @Override
    public String toString()
    {
        return "RemoteCacheNoWaitFacade: " + cacheName + ", rca = " + remoteCacheAttributes;
    }

    /**
     * Begin the failover process if this is a local cache. Clustered remote caches do not failover.
     * <p>
     * @param i The no wait in error.
     */
    abstract void failover( int i );


    /**
     * @return Returns the AuxiliaryCacheAttributes.
     */
    @Override
    public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
    {
        return this.remoteCacheAttributes;
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
     * @return statistics about the cache region
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "Remote Cache No Wait Facade" );

        ArrayList<IStatElement<?>> elems = new ArrayList<IStatElement<?>>();

        if ( noWaits != null )
        {
            elems.add(new StatElement<Integer>( "Number of No Waits", Integer.valueOf(noWaits.length) ) );

            for ( RemoteCacheNoWait<K, V> rcnw : noWaits )
            {
                // get the stats from the super too
                IStats sStats = rcnw.getStatistics();
                elems.addAll(sStats.getStatElements());
            }
        }

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * This typically returns end point info .
     * <p>
     * @return the name
     */
    @Override
    public String getEventLoggingExtraInfo()
    {
        return "Remote Cache No Wait Facade";
    }

    /**
     * Gets the remoteCacheAttributes attribute of the RemoteCacheNoWaitFacade object
     * <p>
     * @return The remoteCacheAttributes value
     */
    public IRemoteCacheAttributes getRemoteCacheAttributes()
    {
        return remoteCacheAttributes;
    }

    /**
     * Sets the remoteCacheAttributes attribute of the RemoteCacheNoWaitFacade object.
     * <p>
     * @param rca The new remoteCacheAttributes value
     */
    public void setRemoteCacheAttributes( IRemoteCacheAttributes rca )
    {
        this.remoteCacheAttributes = rca;
    }

    /**
     * @param compositeCacheManager the compositeCacheManager to set
     */
    protected void setCompositeCacheManager( ICompositeCacheManager compositeCacheManager )
    {
        this.compositeCacheManager = compositeCacheManager;
    }

    /**
     * @return the compositeCacheManager
     */
    protected ICompositeCacheManager getCompositeCacheManager()
    {
        return compositeCacheManager;
    }
}
