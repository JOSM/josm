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
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheMonitor;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICache;

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
public class RemoteCacheFailoverRunner<K, V> extends AbstractAuxiliaryCacheMonitor
{
    /** The facade returned to the composite cache. */
    private final RemoteCacheNoWaitFacade<K, V> facade;

    /** Factory instance */
    private final RemoteCacheFactory cacheFactory;

    /**
     * Constructor for the RemoteCacheFailoverRunner object. This allows the
     * FailoverRunner to modify the facade that the CompositeCache references.
     *
     * @param facade the facade the CompositeCache talks to.
     * @param cacheFactory the cache factory instance
     */
    public RemoteCacheFailoverRunner( RemoteCacheNoWaitFacade<K, V> facade, RemoteCacheFactory cacheFactory )
    {
        super("JCS-RemoteCacheFailoverRunner");
        this.facade = facade;
        this.cacheFactory = cacheFactory;
        setIdlePeriod(20000L);
    }

    /**
     * Clean up all resources before shutdown
     */
    @Override
    protected void dispose()
    {
        // empty
    }

    /**
     * do actual work
     */
    @Override
    protected void doWork()
    {
        // empty
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
            int failoverIndex = facade.getAuxiliaryCacheAttributes().getFailoverIndex();
            log.info( "Exiting failover runner. Failover index = " + failoverIndex);

            if ( failoverIndex <= 0 )
            {
                log.info( "Failover index is <= 0, meaning we are not connected to a failover server." );
            }
            else if ( failoverIndex > 0 )
            {
                log.info( "Failover index is > 0, meaning we are connected to a failover server." );
            }
            // log if we are allright or not.
        }
    }

    /**
     * This is the main loop. If there are failovers defined, then this will
     * continue until the primary is re-connected. If no failovers are defined,
     * this will exit automatically.
     */
    private void connectAndRestore()
    {
        IRemoteCacheAttributes rca0 = facade.getAuxiliaryCacheAttributes();

        do
        {
            log.info( "Remote cache FAILOVER RUNNING." );

            // there is no active listener
            if ( !allright.get() )
            {
                // Monitor each RemoteCacheManager instance one after the other.
                // Each RemoteCacheManager corresponds to one remote connection.
                List<RemoteLocation> failovers = rca0.getFailovers();
                // we should probably check to see if there are any failovers,
                // even though the caller
                // should have already.

                if ( failovers == null )
                {
                    log.warn( "Remote is misconfigured, failovers was null." );
                    return;
                }
                else if ( failovers.size() == 1 )
                {
                    // if there is only the primary, return out of this
                    log.info( "No failovers defined, exiting failover runner." );
                    return;
                }

                int fidx = rca0.getFailoverIndex();
                log.debug( "fidx = " + fidx + " failovers.size = " + failovers.size() );

                // shouldn't we see if the primary is backup?
                // If we don't check the primary, if it gets connected in the
                // background,
                // we will disconnect it only to put it right back
                ListIterator<RemoteLocation> i = failovers.listIterator(fidx); // + 1; // +1 skips the primary
                if ( log.isDebugEnabled() )
                {
                    log.debug( "starting at failover i = " + i.nextIndex() );
                }

                // try them one at a time until successful
                for ( ; i.hasNext() && !allright.get();)
                {
                    RemoteLocation server = i.next();
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Trying server [" + server + "] at failover index i = " + i );
                    }

                    RemoteCacheAttributes rca = (RemoteCacheAttributes) rca0.clone();
                    rca.setRemoteLocation(server);
                    RemoteCacheManager rcm = cacheFactory.getManager( rca );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "RemoteCacheAttributes for failover = " + rca.toString() );
                    }

                    if (rcm != null)
                    {
                        // add a listener if there are none, need to tell rca
                        // what number it is at
                        ICache<K, V> ic = rcm.getCache( rca );
                        if ( ic.getStatus() == CacheStatus.ALIVE )
                        {
                            // may need to do this more gracefully
                            log.debug( "resetting no wait" );
                            facade.restorePrimaryServer((RemoteCacheNoWait<K, V>) ic);
                            rca0.setFailoverIndex( i.nextIndex() );

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "setting ALLRIGHT to true" );
                                if ( i.hasPrevious() )
                                {
                                    log.debug( "Moving to Primary Recovery Mode, failover index = " + i.nextIndex() );
                                }
                                else
                                {
                                    log.debug( "No need to connect to failover, the primary server is back up." );
                                }
                            }

                            allright.set(true);

                            if ( log.isInfoEnabled() )
                            {
                                log.info( "CONNECTED to host = [" + rca.getRemoteLocation() + "]" );
                            }
                        }
                    }
                }
            }
            // end if !allright
            // get here if while index >0 and allright, meaning that we are
            // connected to some backup server.
            else
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "ALLRIGHT is true " );
                }
                if ( log.isInfoEnabled() )
                {
                    log.info( "Failover runner is in primary recovery mode. Failover index = "
                        + rca0.getFailoverIndex() + "\n" + "Will now try to reconnect to primary server." );
                }
            }

            boolean primaryRestoredSuccessfully = false;
            // if we are not connected to the primary, try.
            if ( rca0.getFailoverIndex() > 0 )
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
        while ( rca0.getFailoverIndex() > 0 || !allright.get() );
        // continue if the primary is not restored or if things are not allright.
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
    private boolean restorePrimary()
    {
        IRemoteCacheAttributes rca0 = facade.getAuxiliaryCacheAttributes();
        // try to move back to the primary
        RemoteLocation server = rca0.getFailovers().get(0);

        if ( log.isInfoEnabled() )
        {
            log.info( "Trying to restore connection to primary remote server [" + server + "]" );
        }

        RemoteCacheAttributes rca = (RemoteCacheAttributes) rca0.clone();
        rca.setRemoteLocation(server);
        RemoteCacheManager rcm = cacheFactory.getManager( rca );

        if (rcm != null)
        {
            // add a listener if there are none, need to tell rca what number it
            // is at
            ICache<K, V> ic = rcm.getCache( rca );
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
                    if ( facade.getPrimaryServer() != null && facade.getPrimaryServer().getStatus() == CacheStatus.ALIVE )
                    {
                        int fidx = rca0.getFailoverIndex();

                        if ( fidx > 0 )
                        {
                            RemoteLocation serverOld = rca0.getFailovers().get(fidx);

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Failover Index = " + fidx + " the server at that index is ["
                                    + serverOld + "]" );
                            }

                            if ( serverOld != null )
                            {
                                // create attributes that reflect the
                                // previous failed over configuration.
                                RemoteCacheAttributes rcaOld = (RemoteCacheAttributes) rca0.clone();
                                rcaOld.setRemoteLocation(serverOld);
                                RemoteCacheManager rcmOld = cacheFactory.getManager( rcaOld );

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
                    log.error("Trouble trying to deregister old failover listener prior to restoring the primary = "
                           + server, e );
                }

                // Restore primary
                // may need to do this more gracefully, letting the failover finish in the background
                RemoteCacheNoWait<K, V> failoverNoWait = facade.getPrimaryServer();

                // swap in a new one
                facade.restorePrimaryServer((RemoteCacheNoWait<K, V>) ic);
                rca0.setFailoverIndex( 0 );

                if ( log.isInfoEnabled() )
                {
                    String message = "Successfully reconnected to PRIMARY remote server.  Substituted primary for failoverNoWait ["
                        + failoverNoWait + "]";
                    log.info( message );

                    if ( facade.getCacheEventLogger() != null )
                    {
                        facade.getCacheEventLogger().logApplicationEvent( "RemoteCacheFailoverRunner", "RestoredPrimary", message );
                    }
                }
                return true;
            }
        }

        // else all right
        // if the failover index was at 0 here, we would be in a bad
        // situation, unless there were just
        // no failovers configured.
        if ( log.isDebugEnabled() )
        {
            log.debug( "Primary server status in error, not connected." );
        }

        return false;
    }
}
