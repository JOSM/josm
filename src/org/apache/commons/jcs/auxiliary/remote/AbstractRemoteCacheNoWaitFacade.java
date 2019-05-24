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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCache;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** An abstract base for the No Wait Facade.  Different implementations will failover differently. */
public abstract class AbstractRemoteCacheNoWaitFacade<K, V>
    extends AbstractAuxiliaryCache<K, V>
{
    /** log instance */
    private static final Log log = LogFactory.getLog( AbstractRemoteCacheNoWaitFacade.class );

    /** The connection to a remote server, or a zombie. */
    protected List<RemoteCacheNoWait<K, V>> noWaits;

    /** holds failover and cluster information */
    private IRemoteCacheAttributes remoteCacheAttributes;

    /**
     * Constructs with the given remote cache, and fires events to any listeners.
     * <p>
     * @param noWaits
     * @param rca
     * @param cacheEventLogger
     * @param elementSerializer
     */
    public AbstractRemoteCacheNoWaitFacade( List<RemoteCacheNoWait<K,V>> noWaits, IRemoteCacheAttributes rca,
                                    ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "CONSTRUCTING NO WAIT FACADE" );
        }
        this.remoteCacheAttributes = rca;
        setCacheEventLogger( cacheEventLogger );
        setElementSerializer( elementSerializer );
        this.noWaits = new ArrayList<>(noWaits);
        for (RemoteCacheNoWait<K,V> nw : this.noWaits)
        {
            // FIXME: This cast is very brave. Remove this.
            ((RemoteCache<K, V>)nw.getRemoteCache()).setFacade(this);
        }
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
            log.debug( "updating through cache facade, noWaits.length = " + noWaits.size() );
        }

        for (RemoteCacheNoWait<K, V> nw : noWaits)
        {
            try
            {
                nw.update( ce );
                // an initial move into a zombie will lock this to primary
                // recovery. will not discover other servers until primary
                // reconnect
                // and subsequent error
            }
            catch ( IOException ex )
            {
                String message = "Problem updating no wait. Will initiate failover if the noWait is in error.";
                log.error( message, ex );

                if ( getCacheEventLogger() != null )
                {
                    getCacheEventLogger().logError( "RemoteCacheNoWaitFacade",
                                                    ICacheEventLogger.UPDATE_EVENT,
                                                    message + ":" + ex.getMessage() + " REGION: " + ce.getCacheName()
                                                        + " ELEMENT: " + ce );
                }

                // can handle failover here? Is it safe to try the others?
                // check to see it the noWait is now a zombie
                // if it is a zombie, then move to the next in the failover list
                // will need to keep them in order or a count
                failover( nw );
                // should start a failover thread
                // should probably only failover if there is only one in the noWait
                // list
                // Should start a background thread to restore the original primary if we are in failover state.
            }
        }
    }

    /**
     * Synchronously reads from the remote cache.
     * <p>
     * @param key
     * @return Either an ICacheElement&lt;K, V&gt; or null if it is not found.
     */
    @Override
    public ICacheElement<K, V> get( K key )
    {
        for (RemoteCacheNoWait<K, V> nw : noWaits)
        {
            try
            {
                ICacheElement<K, V> obj = nw.get( key );
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
        for (RemoteCacheNoWait<K, V> nw : noWaits)
        {
            try
            {
                return nw.getMatching( pattern );
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
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMultiple( Set<K> keys )
    {
        if ( keys != null && !keys.isEmpty() )
        {
            for (RemoteCacheNoWait<K, V> nw : noWaits)
            {
                try
                {
                    return nw.getMultiple( keys );
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
        HashSet<K> allKeys = new HashSet<>();
        for (RemoteCacheNoWait<K, V> nw : noWaits)
        {
            if ( nw != null )
            {
                Set<K> keys = nw.getKeySet();
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
            for (RemoteCacheNoWait<K, V> nw : noWaits)
            {
                nw.remove( key );
            }
        }
        catch ( IOException ex )
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
            for (RemoteCacheNoWait<K, V> nw : noWaits)
            {
                nw.removeAll();
            }
        }
        catch ( IOException ex )
        {
            log.error( ex );
        }
    }

    /** Adds a dispose request to the remote cache. */
    @Override
    public void dispose()
    {
        for (RemoteCacheNoWait<K, V> nw : noWaits)
        {
            nw.dispose();
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
        for (RemoteCacheNoWait<K, V> nw : noWaits)
        {
            if ( nw.getStatus() == CacheStatus.ALIVE )
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
        return "RemoteCacheNoWaitFacade: " + remoteCacheAttributes.getCacheName() + ", rca = " + remoteCacheAttributes;
    }

    /**
     * Begin the failover process if this is a local cache. Clustered remote caches do not failover.
     * <p>
     * @param rcnw The no wait in error.
     */
    protected abstract void failover( RemoteCacheNoWait<K, V> rcnw );

    /**
     * Get the primary server from the list of failovers
     *
     * @return a no wait
     */
    public RemoteCacheNoWait<K, V> getPrimaryServer()
    {
        return noWaits.get(0);
    }

    /**
     * restore the primary server in the list of failovers
     *
     */
    public void restorePrimaryServer(RemoteCacheNoWait<K, V> rcnw)
    {
        noWaits.clear();
        noWaits.add(rcnw);
    }

    /**
     * @return Returns the AuxiliaryCacheAttributes.
     */
    @Override
    public IRemoteCacheAttributes getAuxiliaryCacheAttributes()
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

        ArrayList<IStatElement<?>> elems = new ArrayList<>();

        if ( noWaits != null )
        {
            elems.add(new StatElement<>( "Number of No Waits", Integer.valueOf(noWaits.size()) ) );

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
}
