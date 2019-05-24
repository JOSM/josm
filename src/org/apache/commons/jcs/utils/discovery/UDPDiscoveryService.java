package org.apache.commons.jcs.utils.discovery;

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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.engine.behavior.IRequireScheduler;
import org.apache.commons.jcs.engine.behavior.IShutdownObserver;
import org.apache.commons.jcs.utils.discovery.behavior.IDiscoveryListener;
import org.apache.commons.jcs.utils.net.HostNameUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This service creates a listener that can create lateral caches and add them to the no wait list.
 * <p>
 * It also creates a sender that periodically broadcasts its availability.
 * <p>
 * The sender also broadcasts a request for other caches to broadcast their addresses.
 * <p>
 * @author Aaron Smuts
 */
public class UDPDiscoveryService
    implements IShutdownObserver, IRequireScheduler
{
    /** The logger */
    private static final Log log = LogFactory.getLog( UDPDiscoveryService.class );

    /** thread that listens for messages */
    private Thread udpReceiverThread;

    /** the runnable that the receiver thread runs */
    private UDPDiscoveryReceiver receiver;

    /** the runnable that sends messages via the clock daemon */
    private UDPDiscoverySenderThread sender = null;

    /** attributes */
    private UDPDiscoveryAttributes udpDiscoveryAttributes = null;

    /** is this shut down? */
    private boolean shutdown = false;

    /** This is a set of services that have been discovered. */
    private Set<DiscoveredService> discoveredServices = new CopyOnWriteArraySet<>();

    /** This a list of regions that are configured to use discovery. */
    private final Set<String> cacheNames = new CopyOnWriteArraySet<>();

    /** Set of listeners. */
    private final Set<IDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();

    /**
     * @param attributes
     */
    public UDPDiscoveryService( UDPDiscoveryAttributes attributes)
    {
        udpDiscoveryAttributes = attributes.clone();

        try
        {
            // todo, you should be able to set this
            udpDiscoveryAttributes.setServiceAddress( HostNameUtil.getLocalHostAddress() );
        }
        catch ( UnknownHostException e )
        {
            log.error( "Couldn't get localhost address", e );
        }

        try
        {
            // todo need some kind of recovery here.
            receiver = new UDPDiscoveryReceiver( this, getUdpDiscoveryAttributes().getUdpDiscoveryAddr(),
                                                 getUdpDiscoveryAttributes().getUdpDiscoveryPort() );
        }
        catch ( IOException e )
        {
            log.error( "Problem creating UDPDiscoveryReceiver, address ["
                + getUdpDiscoveryAttributes().getUdpDiscoveryAddr() + "] port ["
                + getUdpDiscoveryAttributes().getUdpDiscoveryPort() + "] we won't be able to find any other caches", e );
        }

        // create a sender thread
        sender = new UDPDiscoverySenderThread( getUdpDiscoveryAttributes(), getCacheNames() );
    }

    /**
     * @see org.apache.commons.jcs.engine.behavior.IRequireScheduler#setScheduledExecutorService(java.util.concurrent.ScheduledExecutorService)
     */
    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutor)
    {
        if (sender != null)
        {
            scheduledExecutor.scheduleAtFixedRate(sender, 0, 15, TimeUnit.SECONDS);
        }

        /** removes things that have been idle for too long */
        UDPCleanupRunner cleanup = new UDPCleanupRunner( this );
        // I'm going to use this as both, but it could happen
        // that something could hang around twice the time using this as the
        // delay and the idle time.
        scheduledExecutor.scheduleAtFixedRate(cleanup, 0, getUdpDiscoveryAttributes().getMaxIdleTimeSec(), TimeUnit.SECONDS);
    }

    /**
     * Send a passive broadcast in response to a request broadcast. Never send a request for a
     * request. We can respond to our own requests, since a request broadcast is not intended as a
     * connection request. We might want to only send messages, so we would send a request, but
     * never a passive broadcast.
     */
    protected void serviceRequestBroadcast()
    {
        // create this connection each time.
        // more robust
        try (UDPDiscoverySender sender = new UDPDiscoverySender(
                getUdpDiscoveryAttributes().getUdpDiscoveryAddr(),
                getUdpDiscoveryAttributes().getUdpDiscoveryPort() ))
        {
            sender.passiveBroadcast( getUdpDiscoveryAttributes().getServiceAddress(), getUdpDiscoveryAttributes()
                .getServicePort(), this.getCacheNames() );

            // todo we should consider sending a request broadcast every so
            // often.

            if ( log.isDebugEnabled() )
            {
                log.debug( "Called sender to issue a passive broadcast" );
            }
        }
        catch ( IOException e )
        {
            log.error( "Problem calling the UDP Discovery Sender. address ["
                + getUdpDiscoveryAttributes().getUdpDiscoveryAddr() + "] port ["
                + getUdpDiscoveryAttributes().getUdpDiscoveryPort() + "]", e );
        }
    }

    /**
     * Adds a region to the list that is participating in discovery.
     * <p>
     * @param cacheName
     */
    public void addParticipatingCacheName( String cacheName )
    {
        cacheNames.add( cacheName );
        sender.setCacheNames( getCacheNames() );
    }

    /**
     * Removes the discovered service from the list and calls the discovery listener.
     * <p>
     * @param service
     */
    public void removeDiscoveredService( DiscoveredService service )
    {
        boolean contained = getDiscoveredServices().remove( service );

        if ( contained )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Removing " + service );
            }
        }

        for (IDiscoveryListener listener : getDiscoveryListeners())
        {
            listener.removeDiscoveredService( service );
        }
    }

    /**
     * Add a service to the list. Update the held copy if we already know about it.
     * <p>
     * @param discoveredService discovered service
     */
    protected void addOrUpdateService( DiscoveredService discoveredService )
    {
        synchronized ( getDiscoveredServices() )
        {
            // Since this is a set we can add it over an over.
            // We want to replace the old one, since we may add info that is not part of the equals.
            // The equals method on the object being added is intentionally restricted.
            if ( !getDiscoveredServices().contains( discoveredService ) )
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "Set does not contain service. I discovered " + discoveredService );
                }
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Adding service in the set " + discoveredService );
                }
                getDiscoveredServices().add( discoveredService );
            }
            else
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Set contains service." );
                }
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Updating service in the set " + discoveredService );
                }

                // Update the list of cache names if it has changed.
                DiscoveredService theOldServiceInformation = null;
                // need to update the time this sucks. add has no effect convert to a map
                for (DiscoveredService service1 : getDiscoveredServices())
                {
                    if ( discoveredService.equals( service1 ) )
                    {
                        theOldServiceInformation = service1;
                        break;
                    }
                }
                if ( theOldServiceInformation != null )
                {
                    if ( !theOldServiceInformation.getCacheNames().equals( discoveredService.getCacheNames() ) )
                    {
                        if ( log.isInfoEnabled() )
                        {
                            log.info( "List of cache names changed for service: " + discoveredService );
                        }
                    }
                }

                // replace it, we want to reset the payload and the last heard from time.
                getDiscoveredServices().remove( discoveredService );
                getDiscoveredServices().add( discoveredService );
            }
        }
        // Always Notify the listeners
        // If we don't do this, then if a region using the default config is initialized after notification,
        // it will never get the service in it's no wait list.
        // Leave it to the listeners to decide what to do.
        for (IDiscoveryListener listener : getDiscoveryListeners())
        {
            listener.addDiscoveredService( discoveredService );
        }

    }

    /**
     * Get all the cache names we have facades for.
     * <p>
     * @return ArrayList
     */
    protected ArrayList<String> getCacheNames()
    {
        ArrayList<String> names = new ArrayList<>();
        names.addAll( cacheNames );
        return names;
    }

    /**
     * @param attr The UDPDiscoveryAttributes to set.
     */
    public void setUdpDiscoveryAttributes( UDPDiscoveryAttributes attr )
    {
        this.udpDiscoveryAttributes = attr;
    }

    /**
     * @return Returns the lca.
     */
    public UDPDiscoveryAttributes getUdpDiscoveryAttributes()
    {
        return this.udpDiscoveryAttributes;
    }

    /**
     * Start necessary receiver thread
     */
    public void startup()
    {
        udpReceiverThread = new Thread(receiver);
        udpReceiverThread.setDaemon(true);
        // udpReceiverThread.setName( t.getName() + "--UDPReceiver" );
        udpReceiverThread.start();
    }

    /**
     * Shuts down the receiver.
     */
    @Override
    public void shutdown()
    {
        if ( !shutdown )
        {
            shutdown = true;

            if ( log.isInfoEnabled() )
            {
                log.info( "Shutting down UDP discovery service receiver." );
            }

            try
            {
                // no good way to do this right now.
                receiver.shutdown();
                udpReceiverThread.interrupt();
            }
            catch ( Exception e )
            {
                log.error( "Problem interrupting UDP receiver thread." );
            }

            if ( log.isInfoEnabled() )
            {
                log.info( "Shutting down UDP discovery service sender." );
            }

            // also call the shutdown on the sender thread itself, which
            // will result in a remove command.
            try
            {
                sender.shutdown();
            }
            catch ( Exception e )
            {
                log.error( "Problem issuing remove broadcast via UDP sender." );
            }
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Shutdown already called." );
            }
        }
    }

    /**
     * @return Returns the discoveredServices.
     */
    public synchronized Set<DiscoveredService> getDiscoveredServices()
    {
        return discoveredServices;
    }

    /**
     * @return the discoveryListeners
     */
    private Set<IDiscoveryListener> getDiscoveryListeners()
    {
        return discoveryListeners;
    }

    /**
     * @return the discoveryListeners
     */
    public Set<IDiscoveryListener> getCopyOfDiscoveryListeners()
    {
        Set<IDiscoveryListener> copy = new HashSet<>();
        copy.addAll( getDiscoveryListeners() );
        return copy;
    }

    /**
     * Adds a listener.
     * <p>
     * @param listener
     * @return true if it wasn't already in the set
     */
    public boolean addDiscoveryListener( IDiscoveryListener listener )
    {
        return getDiscoveryListeners().add( listener );
    }

    /**
     * Removes a listener.
     * <p>
     * @param listener
     * @return true if it was in the set
     */
    public boolean removeDiscoveryListener( IDiscoveryListener listener )
    {
        return getDiscoveryListeners().remove( listener );
    }
}
