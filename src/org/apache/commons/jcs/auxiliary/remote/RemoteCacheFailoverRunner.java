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

import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The RemoteCacheFailoverRunner tries to establish a connection with a failover
 * server, if any are defined. Once a failover connection is made, it will
 * attempt to replace the failover with the primary remote server.
 * <p>
 * It works by switching out the RemoteCacheNoWait inside the Facade.
 * <p>
 * Client (i.e.) the CompositeCache has reference to a RemoteCacheNoWaitFacade.
 * This facade is created by the RemoteCacheFactory. The factory maintains a set
 * of managers, one for each remote server. Typically, there will only be one
 * manager.
 * <p>
 * If you use multiple remote servers, you may want to set one or more as
 * failovers. If a local cache cannot connect to the primary server, or looses
 * its connection to the primary server, it will attempt to restore that
 * Connection in the background. If failovers are defined, the Failover runner
 * will try to connect to a failover until the primary is restored.
 *
 */
public class RemoteCacheFailoverRunner<K, V>
    implements Runnable
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteCacheFailoverRunner.class );

    /** The facade returned to the composite cache. */
    private final RemoteCacheNoWaitFacade<K, V> facade;

    /** How long to wait between reconnect attempts. */
    private static long idlePeriod = 20 * 1000;

    /** Have we reconnected. */
    private boolean alright = true;

    /** The cache manager */
    private final ICompositeCacheManager cacheMgr;

    /** The event logger. */
    private final ICacheEventLogger cacheEventLogger;

    /** The serializer. */
    private final IElementSerializer elementSerializer;

    /**
     * Constructor for the RemoteCacheFailoverRunner object. This allows the
     * FailoverRunner to modify the facade that the CompositeCache references.
     *
     * @param facade
     *            the facade the CompositeCache talks to.
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     */
    public RemoteCacheFailoverRunner( RemoteCacheNoWaitFacade<K, V> facade, ICompositeCacheManager cacheMgr,
                                      ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        this.facade = facade;
        this.cacheMgr = cacheMgr;
        this.cacheEventLogger = cacheEventLogger;
        this.elementSerializer = elementSerializer;
    }

    /**
     * Notifies the cache monitor that an error occurred, and kicks off the
     * error recovery process.
     */
    public void notifyError()
    {
        bad();
        synchronized ( this )
        {
            notify();
        }
    }

    /**
     * Main processing method for the RemoteCacheFailoverRunner object.
     * <p>
     * If we do not have a connection with any failover server, this will try to
     * connect one at a time. If no connection can be made, it goes to sleep for
     * a while (20 seconds).
     * <p>
     * Once a connection with a failover is made, we will try to reconnect to
     * the primary server.
     * <p>
     * The primary server is the first server defines in the FailoverServers
     * list.
     */
    @Override
    public void run()
    {
        // start the main work of connecting to a failover and then restoring
        // the primary.
        connectAndRestore();

        if ( log.isInfoEnabled() )
        {
            log.info( "Exiting failover runner. Failover index = " + facade.getRemoteCacheAttributes().getFailoverIndex() );
            if ( facade.getRemoteCacheAttributes().getFailoverIndex() <= 0 )
            {
                log.info( "Failover index is <= 0, meaning we are not " + "connected to a failover server." );
            }
            else if ( facade.getRemoteCacheAttributes().getFailoverIndex() > 0 )
            {
                log.info( "Failover index is > 0, meaning we are " + "connected to a failover server." );
            }
            // log if we are alright or not.
        }
    }

    /**
     * This is the main loop. If there are failovers defined, then this will
     * continue until the primary is re-connected. If no failovers are defined,
     * this will exit automatically.
     */
    @SuppressWarnings("unchecked") // No generic arrays in java
    private void connectAndRestore()
    {
        do
        {
            log.info( "Remote cache FAILOVER RUNNING." );

            // there is no active listener
            if ( !alright )
            {
                // Monitor each RemoteCacheManager instance one after the other.
                // Each RemoteCacheManager corresponds to one remote connection.
                String[] failovers = facade.getRemoteCacheAttributes().getFailovers();
                // we should probably check to see if there are any failovers,
                // even though the caller
                // should have already.

                if ( failovers == null )
                {
                    log.warn( "Remote is misconfigured, failovers was null." );
                    return;
                }
                else if ( failovers.length == 1 )
                {
                    // if there is only the primary, return out of this
                    if ( log.isInfoEnabled() )
                    {
                        log.info( "No failovers defined, exiting failover runner." );
                        return;
                    }
                }

                int fidx = facade.getRemoteCacheAttributes().getFailoverIndex();
                log.debug( "fidx = " + fidx + " failovers.length = " + failovers.length );

                // shouldn't we see if the primary is backup?
                // If we don't check the primary, if it gets connected in the
                // background,
                // we will disconnect it only to put it right back
                int i = fidx; // + 1; // +1 skips the primary
                if ( log.isDebugEnabled() )
                {
                    log.debug( "stating at failover i = " + i );
                }

                // try them one at a time until successful
                for ( ; i < failovers.length && !alright; i++ )
                {
                    String server = failovers[i];
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Trying server [" + server + "] at failover index i = " + i );
                    }

                    RemoteCacheAttributes rca = null;
                    try
                    {
                        rca = (RemoteCacheAttributes) facade.getRemoteCacheAttributes().copy();
                        RemoteUtils.parseServerAndPort(server, rca);
                        RemoteCacheManager rcm = RemoteCacheManager.getInstance( rca, cacheMgr, cacheEventLogger, elementSerializer );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "RemoteCacheAttributes for failover = " + rca.toString() );
                        }

                        // add a listener if there are none, need to tell rca
                        // what number it is at
                        ICache<K, V> ic = rcm.getCache( rca.getCacheName() );
                        if ( ic.getStatus() == CacheStatus.ALIVE )
                        {
                            // may need to do this more gracefully
                            log.debug( "resetting no wait" );
                            facade.noWaits = new RemoteCacheNoWait[1];
                            facade.noWaits[0] = (RemoteCacheNoWait<K, V>) ic;
                            facade.getRemoteCacheAttributes().setFailoverIndex( i );

                            synchronized ( this )
                            {
                                if ( log.isDebugEnabled() )
                                {
                                    log.debug( "setting ALRIGHT to true" );
                                    if ( i > 0 )
                                    {
                                        log.debug( "Moving to Primary Recovery Mode, failover index = " + i );
                                    }
                                    else
                                    {
                                        if ( log.isInfoEnabled() )
                                        {
                                            String message = "No need to connect to failover, the primary server is back up.";
                                            log.info( message );
                                        }
                                    }
                                }

                                alright = true;

                                if ( log.isInfoEnabled() )
                                {
                                    log.info( "CONNECTED to host = [" + rca.getRemoteHost() + "] port = ["
                                        + rca.getRemotePort() + "]" );
                                }
                            }
                        }
                    }
                    catch ( Exception ex )
                    {
                        bad();
                        // Problem encountered in fixing the caches managed by a
                        // RemoteCacheManager instance.
                        // Soldier on to the next RemoteCacheManager instance.
                        String remoteHost = (rca == null) ? "null" : rca.getRemoteHost();
                        int remotePort = (rca == null) ? 0 : rca.getRemotePort();
                        if ( i == 0 )
                        {
                            log.warn( "FAILED to connect, as expected, to primary" + remoteHost + ":"
                                + remotePort, ex );
                        }
                        else
                        {
                            log.error( "FAILED to connect to failover [" + remoteHost + ":"
                                + remotePort + "]", ex );
                        }
                    }
                }
            }
            // end if !alright
            // get here if while index >0 and alright, meaning that we are
            // connected to some backup server.
            else
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "ALRIGHT is true " );
                }
                if ( log.isInfoEnabled() )
                {
                    log.info( "Failover runner is in primary recovery mode. Failover index = "
                        + facade.getRemoteCacheAttributes().getFailoverIndex() + "\n" + "Will now try to reconnect to primary server." );
                }
            }

            boolean primaryRestoredSuccessfully = false;
            // if we are not connected to the primary, try.
            if ( facade.getRemoteCacheAttributes().getFailoverIndex() > 0 )
            {
                primaryRestoredSuccessfully = restorePrimary();
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Primary recovery success state = " + primaryRestoredSuccessfully );
                }
            }

            if ( !primaryRestoredSuccessfully )
            {
                // Time driven mode: sleep between each round of recovery
                // attempt.
                try
                {
                    log.warn( "Failed to reconnect to primary server. Cache failover runner is going to sleep for "
                        + idlePeriod + " milliseconds." );
                    Thread.sleep( idlePeriod );
                }
                catch ( InterruptedException ex )
                {
                    // ignore;
                }
            }

            // try to bring the listener back to the primary
        }
        while ( facade.getRemoteCacheAttributes().getFailoverIndex() > 0 || !alright );
        // continue if the primary is not restored or if things are not alright.

    }

    /**
     * Try to restore the primary server.
     * <p>
     * Once primary is restored the failover listener must be deregistered.
     * <p>
     * The primary server is the first server defines in the FailoverServers
     * list.
     *
     * @return boolean value indicating whether the restoration was successful
     */
    @SuppressWarnings("unchecked") // No generic arrays in java
    private boolean restorePrimary()
    {
        // try to move back to the primary
        String[] failovers = facade.getRemoteCacheAttributes().getFailovers();
        String server = failovers[0];

        if ( log.isInfoEnabled() )
        {
            log.info( "Trying to restore connection to primary remote server [" + server + "]" );
        }

        try
        {
            RemoteCacheAttributes rca = (RemoteCacheAttributes) facade.getRemoteCacheAttributes().copy();
            RemoteUtils.parseServerAndPort(server, rca);
            RemoteCacheManager rcm = RemoteCacheManager.getInstance( rca, cacheMgr, cacheEventLogger, elementSerializer );

            // add a listener if there are none, need to tell rca what number it
            // is at
            ICache<K, V> ic = rcm.getCache( rca.getCacheName() );
            // by default the listener id should be 0, else it will be the
            // listener
            // Originally associated with the remote cache. either way is fine.
            // We just don't want the listener id from a failover being used.
            // If the remote server was rebooted this could be a problem if new
            // locals were also added.

            if ( ic.getStatus() == CacheStatus.ALIVE )
            {
                try
                {
                    // we could have more than one listener registered right
                    // now.
                    // this will not result in a loop, only duplication
                    // stop duplicate listening.
                    if ( facade.noWaits[0] != null && facade.noWaits[0].getStatus() == CacheStatus.ALIVE )
                    {
                        int fidx = facade.getRemoteCacheAttributes().getFailoverIndex();

                        if ( fidx > 0 )
                        {
                            String serverOld = failovers[fidx];

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Failover Index = " + fidx + " the server at that index is ["
                                    + serverOld + "]" );
                            }

                            if ( serverOld != null )
                            {
                                // create attributes that reflect the
                                // previous failed over configuration.
                                RemoteCacheAttributes rcaOld = (RemoteCacheAttributes) facade.getRemoteCacheAttributes().copy();
                                RemoteUtils.parseServerAndPort(serverOld, rcaOld);
                                RemoteCacheManager rcmOld = RemoteCacheManager.getInstance( rcaOld, cacheMgr, cacheEventLogger, elementSerializer );

                                if ( rcmOld != null )
                                {
                                    // manager can remove by name if
                                    // necessary
                                    rcmOld.removeRemoteCacheListener( rcaOld );
                                }
                                if ( log.isInfoEnabled() )
                                {
                                    log.info( "Successfully deregistered from FAILOVER remote server = "
                                        + serverOld );
                                }
                            }
                        }
                        else if ( fidx == 0 )
                        {
                            // this should never happen. If there are no
                            // failovers this shouldn't get called.
                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "No need to restore primary, it is already restored." );
                                return true;
                            }
                        }
                        else if ( fidx < 0 )
                        {
                            // this should never happen
                            log.warn( "Failover index is less than 0, this shouldn't happen" );
                        }
                    }
                }
                catch ( IOException e )
                {
                    // TODO, should try again, or somehow stop the listener
                    log.error(
                               "Trouble trying to deregister old failover listener prior to restoring the primary = "
                                   + server, e );
                }

                // Restore primary
                // may need to do this more gracefully, letting the failover finish in the background
                RemoteCacheNoWait<K, V> failoverNoWait = facade.noWaits[0];

                // swap in a new one
                facade.noWaits = new RemoteCacheNoWait[1];
                facade.noWaits[0] = (RemoteCacheNoWait<K, V>) ic;
                facade.getRemoteCacheAttributes().setFailoverIndex( 0 );

                if ( log.isInfoEnabled() )
                {
                    String message = "Successfully reconnected to PRIMARY remote server.  Substituted primary for failoverNoWait [" + failoverNoWait + "]";
                    log.info( message );

                    if ( facade.getCacheEventLogger() != null )
                    {
                        facade.getCacheEventLogger().logApplicationEvent( "RemoteCacheFailoverRunner", "RestoredPrimary",
                                                                          message );
                    }
                }
                return true;
            }

            // else all right
            // if the failover index was at 0 here, we would be in a bad
            // situation, unless there were just
            // no failovers configured.
            if ( log.isDebugEnabled() )
            {
                log.debug( "Primary server status in error, not connected." );
            }
        }
        catch ( NumberFormatException ex )
        {
            log.error( ex );
        }
        return false;
    }

    /**
     * Sets the "alright" flag to false in a critical section. This flag
     * indicates whether or not we are connected to any server at all. If we are
     * connected to a secondary server, then alright will be true, but we will
     * continue to try to restore the connection with the primary server.
     * <p>
     * The primary server is the first server defines in the FailoverServers
     * list.
     */
    private void bad()
    {
        if ( alright )
        {
            synchronized ( this )
            {
                alright = false;
            }
        }
    }
}
