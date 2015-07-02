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

import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Client proxy for an RMI remote cache.
 * <p>
 * This handles gets, updates, and removes. It also initiates failover recovery when an error is
 * encountered.
 */
public class RemoteCache<K, V>
    extends AbstractRemoteAuxiliaryCache<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( RemoteCache.class );

    /**
     * Constructor for the RemoteCache object. This object communicates with a remote cache server.
     * One of these exists for each region. This also holds a reference to a listener. The same
     * listener is used for all regions for one remote server. Holding a reference to the listener
     * allows this object to know the listener id assigned by the remote cache.
     * <p>
     * @param cattr
     * @param remote
     * @param listener
     */
    public RemoteCache( IRemoteCacheAttributes cattr, ICacheServiceNonLocal<K, V> remote, IRemoteCacheListener<K, V> listener )
    {
        super( cattr, remote, listener );

        RemoteUtils.configureGlobalCustomSocketFactory( getRemoteCacheAttributes().getRmiSocketFactoryTimeoutMillis() );
    }

    /**
     * @return IStats object
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "Remote Cache" );

        ArrayList<IStatElement<?>> elems = new ArrayList<IStatElement<?>>();

        elems.add(new StatElement<String>( "Remote Host:Port", getIPAddressForService() ) );
        elems.add(new StatElement<String>( "Remote Type", this.getRemoteCacheAttributes().getRemoteTypeName() ) );

//      if ( this.getRemoteCacheAttributes().getRemoteType() == RemoteType.CLUSTER )
//      {
//          // something cluster specific
//      }

        // get the stats from the super too
        IStats sStats = super.getStatistics();
        elems.addAll(sStats.getStatElements());

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * Handles exception by disabling the remote cache service before re-throwing the exception in
     * the form of an IOException.
     * <p>
     * @param ex
     * @param msg
     * @param eventName
     * @throws IOException
     */
    @Override
    protected void handleException( Exception ex, String msg, String eventName )
        throws IOException
    {
        String message = "Disabling remote cache due to error: " + msg;

        logError( cacheName, "", message );
        log.error( message, ex );

        // we should not switch if the existing is a zombie.
        if ( getRemoteCacheService() == null || !( getRemoteCacheService() instanceof ZombieCacheServiceNonLocal ) )
        {
            // TODO make configurable
            setRemoteCacheService( new ZombieCacheServiceNonLocal<K, V>( getRemoteCacheAttributes().getZombieQueueMaxSize() ) );
        }
        // may want to flush if region specifies
        // Notify the cache monitor about the error, and kick off the recovery
        // process.
        RemoteCacheMonitor.getInstance().notifyError();

        // initiate failover if local
        @SuppressWarnings("unchecked") // Need to cast because of common map for all facades
        RemoteCacheNoWaitFacade<K, V> rcnwf = (RemoteCacheNoWaitFacade<K, V>)RemoteCacheFactory.getFacades()
            .get( getRemoteCacheAttributes().getCacheName() );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Initiating failover, rcnf = " + rcnwf );
        }

        if ( rcnwf != null && rcnwf.getRemoteCacheAttributes().getRemoteType() == RemoteType.LOCAL )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Found facade, calling failover" );
            }
            // may need to remove the noWait index here. It will be 0 if it is
            // local since there is only 1 possible listener.
            rcnwf.failover( 0 );
        }

        if ( ex instanceof IOException )
        {
            throw (IOException) ex;
        }
        throw new IOException( ex.getMessage() );
    }

    /**
     * Debugging info.
     * <p>
     * @return basic info about the RemoteCache
     */
    @Override
    public String toString()
    {
        return "RemoteCache: " + cacheName + " attributes = " + getRemoteCacheAttributes();
    }

    /**
     * Gets the extra info for the event log.
     * <p>
     * @return disk location
     */
    @Override
    public String getEventLoggingExtraInfo()
    {
        return getIPAddressForService();
    }

    /**
     * IP address for the service, if one is stored.
     * <p>
     * Protected for testing.
     * <p>
     * @return String
     */
    protected String getIPAddressForService()
    {
        String ipAddress = this.getRemoteCacheAttributes().getRemoteHost() + ":"
            + this.getRemoteCacheAttributes().getRemotePort();
        return ipAddress;
    }
}
