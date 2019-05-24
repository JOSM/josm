package org.apache.commons.jcs.auxiliary.lateral;

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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheEventLogging;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
import org.apache.commons.jcs.engine.CacheInfo;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.IZombie;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Lateral distributor. Returns null on get by default. Net search not implemented.
 */
public class LateralCache<K, V>
    extends AbstractAuxiliaryCacheEventLogging<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( LateralCache.class );

    /** generalize this, use another interface */
    private final ILateralCacheAttributes lateralCacheAttributes;

    /** The region name */
    final String cacheName;

    /** either http, socket.udp, or socket.tcp can set in config */
    private ICacheServiceNonLocal<K, V> lateralCacheService;

    /** Monitors the connection. */
    private LateralCacheMonitor monitor;

    /**
     * Constructor for the LateralCache object
     * <p>
     * @param cattr
     * @param lateral
     * @param monitor
     */
    public LateralCache( ILateralCacheAttributes cattr, ICacheServiceNonLocal<K, V> lateral, LateralCacheMonitor monitor )
    {
        this.cacheName = cattr.getCacheName();
        this.lateralCacheAttributes = cattr;
        this.lateralCacheService = lateral;
        this.monitor = monitor;
    }

    /**
     * Constructor for the LateralCache object
     * <p>
     * @param cattr
     */
    public LateralCache( ILateralCacheAttributes cattr )
    {
        this.cacheName = cattr.getCacheName();
        this.lateralCacheAttributes = cattr;
    }

    /**
     * Update lateral.
     * <p>
     * @param ce
     * @throws IOException
     */
    @Override
    protected void processUpdate( ICacheElement<K, V> ce )
        throws IOException
    {
        try
        {
            if (ce != null)
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "update: lateral = [" + lateralCacheService + "], " + "CacheInfo.listenerId = "
                        + CacheInfo.listenerId );
                }
                lateralCacheService.update( ce, CacheInfo.listenerId );
            }
        }
        catch ( IOException ex )
        {
            handleException( ex, "Failed to put [" + ce.getKey() + "] to " + ce.getCacheName() + "@" + lateralCacheAttributes );
        }
    }

    /**
     * The performance costs are too great. It is not recommended that you enable lateral gets.
     * <p>
     * @param key
     * @return ICacheElement&lt;K, V&gt; or null
     * @throws IOException
     */
    @Override
    protected ICacheElement<K, V> processGet( K key )
        throws IOException
    {
        ICacheElement<K, V> obj = null;

        if ( this.lateralCacheAttributes.getPutOnlyMode() )
        {
            return null;
        }
        try
        {
            obj = lateralCacheService.get( cacheName, key );
        }
        catch ( Exception e )
        {
            log.error( e );
            handleException( e, "Failed to get [" + key + "] from " + lateralCacheAttributes.getCacheName() + "@" + lateralCacheAttributes );
        }
        return obj;
    }

    /**
     * @param pattern
     * @return A map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     * @throws IOException
     */
    @Override
    protected Map<K, ICacheElement<K, V>> processGetMatching( String pattern )
        throws IOException
    {
        if ( this.lateralCacheAttributes.getPutOnlyMode() )
        {
            return Collections.emptyMap();
        }
        try
        {
            return lateralCacheService.getMatching( cacheName, pattern );
        }
        catch ( IOException e )
        {
            log.error( e );
            handleException( e, "Failed to getMatching [" + pattern + "] from " + lateralCacheAttributes.getCacheName() + "@" + lateralCacheAttributes );
            return Collections.emptyMap();
        }
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
            return lateralCacheService.getKeySet( cacheName );
        }
        catch ( IOException ex )
        {
            handleException( ex, "Failed to get key set from " + lateralCacheAttributes.getCacheName() + "@"
                + lateralCacheAttributes );
        }
        return Collections.emptySet();
    }

    /**
     * Synchronously remove from the remote cache; if failed, replace the remote handle with a
     * zombie.
     * <p>
     * @param key
     * @return false always
     * @throws IOException
     */
    @Override
    protected boolean processRemove( K key )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "removing key:" + key );
        }

        try
        {
            lateralCacheService.remove( cacheName, key, CacheInfo.listenerId );
        }
        catch ( IOException ex )
        {
            handleException( ex, "Failed to remove " + key + " from " + lateralCacheAttributes.getCacheName() + "@" + lateralCacheAttributes );
        }
        return false;
    }

    /**
     * Synchronously removeAll from the remote cache; if failed, replace the remote handle with a
     * zombie.
     * <p>
     * @throws IOException
     */
    @Override
    protected void processRemoveAll()
        throws IOException
    {
        try
        {
            lateralCacheService.removeAll( cacheName, CacheInfo.listenerId );
        }
        catch ( IOException ex )
        {
            handleException( ex, "Failed to remove all from " + lateralCacheAttributes.getCacheName() + "@" + lateralCacheAttributes );
        }
    }

    /**
     * Synchronously dispose the cache. Not sure we want this.
     * <p>
     * @throws IOException
     */
    @Override
    protected void processDispose()
        throws IOException
    {
        log.debug( "Disposing of lateral cache" );

        ///* HELP: This section did nothing but generate compilation warnings.
        // TODO: may limit this functionality. It is dangerous.
        // asmuts -- Added functionality to help with warnings. I'm not getting
        // any.
        try
        {
            lateralCacheService.dispose( this.lateralCacheAttributes.getCacheName() );
            // Should remove connection
        }
        catch ( IOException ex )
        {
            log.error( "Couldn't dispose", ex );
            handleException( ex, "Failed to dispose " + lateralCacheAttributes.getCacheName() );
        }
    }

    /**
     * Returns the cache status.
     * <p>
     * @return The status value
     */
    @Override
    public CacheStatus getStatus()
    {
        return this.lateralCacheService instanceof IZombie ? CacheStatus.ERROR : CacheStatus.ALIVE;
    }

    /**
     * Returns the current cache size.
     * <p>
     * @return The size value
     */
    @Override
    public int getSize()
    {
        return 0;
    }

    /**
     * Gets the cacheType attribute of the LateralCache object
     * <p>
     * @return The cacheType value
     */
    @Override
    public CacheType getCacheType()
    {
        return CacheType.LATERAL_CACHE;
    }

    /**
     * Gets the cacheName attribute of the LateralCache object
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return cacheName;
    }

    /**
     * Not yet sure what to do here.
     * <p>
     * @param ex
     * @param msg
     * @throws IOException
     */
    private void handleException( Exception ex, String msg )
        throws IOException
    {
        log.error( "Disabling lateral cache due to error " + msg, ex );

        lateralCacheService = new ZombieCacheServiceNonLocal<>( lateralCacheAttributes.getZombieQueueMaxSize() );
        // may want to flush if region specifies
        // Notify the cache monitor about the error, and kick off the recovery
        // process.
        monitor.notifyError();

        // could stop the net search if it is built and try to reconnect?
        if ( ex instanceof IOException )
        {
            throw (IOException) ex;
        }
        throw new IOException( ex.getMessage() );
    }

    /**
     * Replaces the current remote cache service handle with the given handle.
     * <p>
     * @param restoredLateral
     */
    public void fixCache( ICacheServiceNonLocal<K, V> restoredLateral )
    {
        if ( this.lateralCacheService != null && this.lateralCacheService instanceof ZombieCacheServiceNonLocal )
        {
            ZombieCacheServiceNonLocal<K, V> zombie = (ZombieCacheServiceNonLocal<K, V>) this.lateralCacheService;
            this.lateralCacheService = restoredLateral;
            try
            {
                zombie.propagateEvents( restoredLateral );
            }
            catch ( Exception e )
            {
                try
                {
                    handleException( e, "Problem propagating events from Zombie Queue to new Lateral Service." );
                }
                catch ( IOException e1 )
                {
                    // swallow, since this is just expected kick back.  Handle always throws
                }
            }
        }
        else
        {
            this.lateralCacheService = restoredLateral;
        }
    }

    /**
     * getStats
     * <p>
     * @return String
     */
    @Override
    public String getStats()
    {
        return "";
    }

    /**
     * @return Returns the AuxiliaryCacheAttributes.
     */
    @Override
    public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
    {
        return lateralCacheAttributes;
    }

    /**
     * @return debugging data.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\n LateralCache " );
        buf.append( "\n Cache Name [" + lateralCacheAttributes.getCacheName() + "]" );
        buf.append( "\n cattr =  [" + lateralCacheAttributes + "]" );
        return buf.toString();
    }

    /**
     * @return extra data.
     */
    @Override
    public String getEventLoggingExtraInfo()
    {
        return null;
    }

    /**
     * The NoWait on top does not call out to here yet.
     * <p>
     * @return almost nothing
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "LateralCache" );
        return stats;
    }
}
