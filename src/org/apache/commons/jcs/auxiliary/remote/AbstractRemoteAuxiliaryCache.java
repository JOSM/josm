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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheEventLogging;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheClient;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheElementSerialized;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.IZombie;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.jcs.utils.serialization.SerializationConversionUtil;
import org.apache.commons.jcs.utils.threadpool.ThreadPoolManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Abstract base for remote caches. I'm trying to break out and reuse common functionality. */
public abstract class AbstractRemoteAuxiliaryCache<K, V>
    extends AbstractAuxiliaryCacheEventLogging<K, V>
    implements IRemoteCacheClient<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( AbstractRemoteAuxiliaryCache.class );

    /**
     * This does the work. In an RMI instances, it will be a remote reference. In an http remote
     * cache it will be an http client. In zombie mode it is replaced with a balking facade.
     */
    private ICacheServiceNonLocal<K, V> remoteCacheService;

    /** The cacheName */
    protected final String cacheName;

    /** The listener. This can be null. */
    private IRemoteCacheListener<K, V> remoteCacheListener;

    /** The configuration values. TODO, we'll need a base here. */
    private IRemoteCacheAttributes remoteCacheAttributes;

    /** A thread pool for gets if configured. */
    private ExecutorService pool = null;

    /** Should we get asynchronously using a pool. */
    private boolean usePoolForGet = false;

    /**
     * Creates the base.
     * <p>
     * @param cattr
     * @param remote
     * @param listener
     */
    public AbstractRemoteAuxiliaryCache( IRemoteCacheAttributes cattr, ICacheServiceNonLocal<K, V> remote,
                                         IRemoteCacheListener<K, V> listener )
    {
        this.setRemoteCacheAttributes( cattr );
        this.cacheName = cattr.getCacheName();
        this.setRemoteCacheService( remote );
        this.setRemoteCacheListener( listener );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Construct> cacheName=" + cattr.getCacheName() );
            log.debug( "irca = " + getRemoteCacheAttributes() );
            log.debug( "remote = " + remote );
            log.debug( "listener = " + listener );
        }

        // use a pool if it is greater than 0
        if ( log.isDebugEnabled() )
        {
            log.debug( "GetTimeoutMillis() = " + getRemoteCacheAttributes().getGetTimeoutMillis() );
        }

        if ( getRemoteCacheAttributes().getGetTimeoutMillis() > 0 )
        {
            pool = ThreadPoolManager.getInstance().getExecutorService( getRemoteCacheAttributes().getThreadPoolName() );
            if ( log.isDebugEnabled() )
            {
                log.debug( "Thread Pool = " + pool );
            }
            usePoolForGet = true;
        }
    }

    /**
     * Synchronously dispose the remote cache; if failed, replace the remote handle with a zombie.
     * <p>
     * @throws IOException
     */
    @Override
    protected void processDispose()
        throws IOException
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Disposing of remote cache." );
        }
        try
        {
            if ( getRemoteCacheListener() != null )
            {
                getRemoteCacheListener().dispose();
            }
        }
        catch ( IOException ex )
        {
            log.error( "Couldn't dispose", ex );
            handleException( ex, "Failed to dispose [" + cacheName + "]", ICacheEventLogger.DISPOSE_EVENT );
        }
    }

    /**
     * Synchronously get from the remote cache; if failed, replace the remote handle with a zombie.
     * <p>
     * Use threadpool to timeout if a value is set for GetTimeoutMillis
     * <p>
     * If we are a cluster client, we need to leave the Element in its serialized form. Cluster
     * clients cannot deserialize objects. Cluster clients get ICacheElementSerialized objects from
     * other remote servers.
     * <p>
     * @param key
     * @return ICacheElement, a wrapper around the key, value, and attributes
     * @throws IOException
     */
    @Override
    protected ICacheElement<K, V> processGet( K key )
        throws IOException
    {
        ICacheElement<K, V> retVal = null;
        try
        {
            if ( usePoolForGet )
            {
                retVal = getUsingPool( key );
            }
            else
            {
                retVal = getRemoteCacheService().get( cacheName, key, getListenerId() );
            }

            // Eventually the instance of will not be necessary.
            if ( retVal instanceof ICacheElementSerialized )
            {
                // Never try to deserialize if you are a cluster client. Cluster
                // clients are merely intra-remote cache communicators. Remote caches are assumed
                // to have no ability to deserialize the objects.
                if ( this.getRemoteCacheAttributes().getRemoteType() != RemoteType.CLUSTER )
                {
                    retVal = SerializationConversionUtil.getDeSerializedCacheElement( (ICacheElementSerialized<K, V>) retVal,
                            super.getElementSerializer() );
                }
            }
        }
        catch ( Exception ex )
        {
            handleException( ex, "Failed to get [" + key + "] from [" + cacheName + "]", ICacheEventLogger.GET_EVENT );
        }
        return retVal;
    }

    /**
     * This allows gets to timeout in case of remote server machine shutdown.
     * <p>
     * @param key
     * @return ICacheElement
     * @throws IOException
     */
    public ICacheElement<K, V> getUsingPool( final K key )
        throws IOException
    {
        int timeout = getRemoteCacheAttributes().getGetTimeoutMillis();

        try
        {
            Callable<ICacheElement<K, V>> command = new Callable<ICacheElement<K, V>>()
            {
                @Override
                public ICacheElement<K, V> call()
                    throws IOException
                {
                    return getRemoteCacheService().get( cacheName, key, getListenerId() );
                }
            };

            // execute using the pool
            Future<ICacheElement<K, V>> future = pool.submit(command);

            // used timed get in order to timeout
            ICacheElement<K, V> ice = future.get(timeout, TimeUnit.MILLISECONDS);

            if ( log.isDebugEnabled() )
            {
                if ( ice == null )
                {
                    log.debug( "nothing found in remote cache" );
                }
                else
                {
                    log.debug( "found item in remote cache" );
                }
            }
            return ice;
        }
        catch ( TimeoutException te )
        {
            log.warn( "TimeoutException, Get Request timed out after " + timeout );
            throw new IOException( "Get Request timed out after " + timeout );
        }
        catch ( InterruptedException ex )
        {
            log.warn( "InterruptedException, Get Request timed out after " + timeout );
            throw new IOException( "Get Request timed out after " + timeout );
        }
        catch (ExecutionException ex)
        {
            // assume that this is an IOException thrown by the callable.
            log.error( "ExecutionException, Assuming an IO exception thrown in the background.", ex );
            throw new IOException( "Get Request timed out after " + timeout );
        }
    }

    /**
     * Calls get matching on the server. Each entry in the result is unwrapped.
     * <p>
     * @param pattern
     * @return Map
     * @throws IOException
     */
    @Override
    public Map<K, ICacheElement<K, V>> processGetMatching( String pattern )
        throws IOException
    {
        Map<K, ICacheElement<K, V>> results = new HashMap<>();
        try
        {
            Map<K, ICacheElement<K, V>> rawResults = getRemoteCacheService().getMatching( cacheName, pattern, getListenerId() );

            // Eventually the instance of will not be necessary.
            if ( rawResults != null )
            {
                for (Map.Entry<K, ICacheElement<K, V>> entry : rawResults.entrySet())
                {
                    ICacheElement<K, V> unwrappedResult = null;
                    if ( entry.getValue() instanceof ICacheElementSerialized )
                    {
                        // Never try to deserialize if you are a cluster client. Cluster
                        // clients are merely intra-remote cache communicators. Remote caches are assumed
                        // to have no ability to deserialize the objects.
                        if ( this.getRemoteCacheAttributes().getRemoteType() != RemoteType.CLUSTER )
                        {
                            unwrappedResult = SerializationConversionUtil
                                .getDeSerializedCacheElement( (ICacheElementSerialized<K, V>) entry.getValue(),
                                        super.getElementSerializer() );
                        }
                    }
                    else
                    {
                        unwrappedResult = entry.getValue();
                    }
                    results.put( entry.getKey(), unwrappedResult );
                }
            }
        }
        catch ( Exception ex )
        {
            handleException( ex, "Failed to getMatching [" + pattern + "] from [" + cacheName + "]",
                             ICacheEventLogger.GET_EVENT );
        }
        return results;
    }

    /**
     * Synchronously remove from the remote cache; if failed, replace the remote handle with a
     * zombie.
     * <p>
     * @param key
     * @return boolean, whether or not the item was removed
     * @throws IOException
     */
    @Override
    protected boolean processRemove( K key )
        throws IOException
    {
        if ( !this.getRemoteCacheAttributes().getGetOnly() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "remove> key=" + key );
            }
            try
            {
                getRemoteCacheService().remove( cacheName, key, getListenerId() );
            }
            catch ( Exception ex )
            {
                handleException( ex, "Failed to remove " + key + " from " + cacheName, ICacheEventLogger.REMOVE_EVENT );
            }
            return true;
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
        if ( !this.getRemoteCacheAttributes().getGetOnly() )
        {
            try
            {
                getRemoteCacheService().removeAll( cacheName, getListenerId() );
            }
            catch ( Exception ex )
            {
                handleException( ex, "Failed to remove all from " + cacheName, ICacheEventLogger.REMOVEALL_EVENT );
            }
        }
    }

    /**
     * Serializes the object and then calls update on the remote server with the byte array. The
     * byte array is wrapped in a ICacheElementSerialized. This allows the remote server to operate
     * without any knowledge of caches classes.
     * <p>
     * @param ce
     * @throws IOException
     */
    @Override
    protected void processUpdate( ICacheElement<K, V> ce )
        throws IOException
    {
        if ( !getRemoteCacheAttributes().getGetOnly() )
        {
            ICacheElementSerialized<K, V> serialized = null;
            try
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "sending item to remote server" );
                }

                // convert so we don't have to know about the object on the
                // other end.
                serialized = SerializationConversionUtil.getSerializedCacheElement( ce, super.getElementSerializer() );

                remoteCacheService.update( serialized, getListenerId() );
            }
            catch ( NullPointerException npe )
            {
                log.error( "npe for ce = " + ce + "ce.attr = " + ce.getElementAttributes(), npe );
            }
            catch ( Exception ex )
            {
                // event queue will wait and retry
                handleException( ex, "Failed to put [" + ce.getKey() + "] to " + ce.getCacheName(),
                                 ICacheEventLogger.UPDATE_EVENT );
            }
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "get only mode, not sending to remote server" );
            }
        }
    }

    /**
     * Return the keys in this cache.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet()
        throws IOException
    {
        return getRemoteCacheService().getKeySet(cacheName);
    }

    /**
     * Allows other member of this package to access the listener. This is mainly needed for
     * deregistering a listener.
     * <p>
     * @return IRemoteCacheListener, the listener for this remote server
     */
    @Override
    public IRemoteCacheListener<K, V> getListener()
    {
        return getRemoteCacheListener();
    }

    /**
     * let the remote cache set a listener_id. Since there is only one listener for all the regions
     * and every region gets registered? the id shouldn't be set if it isn't zero. If it is we
     * assume that it is a reconnect.
     * <p>
     * @param id The new listenerId value
     */
    public void setListenerId( long id )
    {
        if ( getRemoteCacheListener() != null )
        {
            try
            {
                getRemoteCacheListener().setListenerId( id );

                if ( log.isDebugEnabled() )
                {
                    log.debug( "set listenerId = " + id );
                }
            }
            catch ( Exception e )
            {
                log.error( "Problem setting listenerId", e );
            }
        }
    }

    /**
     * Gets the listenerId attribute of the RemoteCacheListener object
     * <p>
     * @return The listenerId value
     */
    @Override
    public long getListenerId()
    {
        if ( getRemoteCacheListener() != null )
        {
            try
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "get listenerId = " + getRemoteCacheListener().getListenerId() );
                }
                return getRemoteCacheListener().getListenerId();
            }
            catch ( Exception e )
            {
                log.error( "Problem getting listenerId", e );
            }
        }
        return -1;
    }

    /**
     * Returns the current cache size.
     * @return The size value
     */
    @Override
    public int getSize()
    {
        return 0;
    }

    /**
     * Custom exception handling some children.  This should be used to initiate failover.
     * <p>
     * @param ex
     * @param msg
     * @param eventName
     * @throws IOException
     */
    protected abstract void handleException( Exception ex, String msg, String eventName )
        throws IOException;

    /**
     * Gets the stats attribute of the RemoteCache object.
     * <p>
     * @return The stats value
     */
    @Override
    public String getStats()
    {
        return getStatistics().toString();
    }

    /**
     * @return IStats object
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "AbstractRemoteAuxiliaryCache" );

        ArrayList<IStatElement<?>> elems = new ArrayList<>();

        elems.add(new StatElement<>( "Remote Type", this.getRemoteCacheAttributes().getRemoteTypeName() ) );

//      if ( this.getRemoteCacheAttributes().getRemoteType() == RemoteType.CLUSTER )
//      {
//          // something cluster specific
//      }

        elems.add(new StatElement<>( "UsePoolForGet", Boolean.valueOf(usePoolForGet) ) );

        if ( pool != null )
        {
            elems.add(new StatElement<>( "Pool", pool ) );
        }

        if ( getRemoteCacheService() instanceof ZombieCacheServiceNonLocal )
        {
            elems.add(new StatElement<>( "Zombie Queue Size",
                    Integer.valueOf(( (ZombieCacheServiceNonLocal<K, V>) getRemoteCacheService() ).getQueueSize()) ) );
        }

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * Returns the cache status. An error status indicates the remote connection is not available.
     * <p>
     * @return The status value
     */
    @Override
    public CacheStatus getStatus()
    {
        return getRemoteCacheService() instanceof IZombie ? CacheStatus.ERROR : CacheStatus.ALIVE;
    }

    /**
     * Replaces the current remote cache service handle with the given handle. If the current remote
     * is a Zombie, then it propagates any events that are queued to the restored service.
     * <p>
     * @param restoredRemote ICacheServiceNonLocal -- the remote server or proxy to the remote server
     */
    @Override
    public void fixCache( ICacheServiceNonLocal<?, ?> restoredRemote )
    {
        @SuppressWarnings("unchecked") // Don't know how to do this properly
        ICacheServiceNonLocal<K, V> remote = (ICacheServiceNonLocal<K, V>)restoredRemote;
        ICacheServiceNonLocal<K, V> prevRemote = getRemoteCacheService();
        if ( prevRemote instanceof ZombieCacheServiceNonLocal )
        {
            ZombieCacheServiceNonLocal<K, V> zombie = (ZombieCacheServiceNonLocal<K, V>) prevRemote;
            setRemoteCacheService( remote );
            try
            {
                zombie.propagateEvents( remote );
            }
            catch ( Exception e )
            {
                try
                {
                    handleException( e, "Problem propagating events from Zombie Queue to new Remote Service.",
                                     "fixCache" );
                }
                catch ( IOException e1 )
                {
                    // swallow, since this is just expected kick back.  Handle always throws
                }
            }
        }
        else
        {
            setRemoteCacheService( remote );
        }
    }


    /**
     * Gets the cacheType attribute of the RemoteCache object
     * @return The cacheType value
     */
    @Override
    public CacheType getCacheType()
    {
        return CacheType.REMOTE_CACHE;
    }

    /**
     * Gets the cacheName attribute of the RemoteCache object.
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return cacheName;
    }

    /**
     * @param remote the remote to set
     */
    protected void setRemoteCacheService( ICacheServiceNonLocal<K, V> remote )
    {
        this.remoteCacheService = remote;
    }

    /**
     * @return the remote
     */
    protected ICacheServiceNonLocal<K, V> getRemoteCacheService()
    {
        return remoteCacheService;
    }

    /**
     * @return Returns the AuxiliaryCacheAttributes.
     */
    @Override
    public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
    {
        return getRemoteCacheAttributes();
    }

    /**
     * @param remoteCacheAttributes the remoteCacheAttributes to set
     */
    protected void setRemoteCacheAttributes( IRemoteCacheAttributes remoteCacheAttributes )
    {
        this.remoteCacheAttributes = remoteCacheAttributes;
    }

    /**
     * @return the remoteCacheAttributes
     */
    protected IRemoteCacheAttributes getRemoteCacheAttributes()
    {
        return remoteCacheAttributes;
    }

    /**
     * @param remoteCacheListener the remoteCacheListener to set
     */
    protected void setRemoteCacheListener( IRemoteCacheListener<K, V> remoteCacheListener )
    {
        this.remoteCacheListener = remoteCacheListener;
    }

    /**
     * @return the remoteCacheListener
     */
    protected IRemoteCacheListener<K, V> getRemoteCacheListener()
    {
        return remoteCacheListener;
    }
}
