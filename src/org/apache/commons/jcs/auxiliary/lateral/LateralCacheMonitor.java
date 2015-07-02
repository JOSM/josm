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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.LateralTCPCacheFactory;
import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.behavior.ITCPLateralCacheAttributes;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to monitor and repair any failed connection for the lateral cache service. By default the
 * monitor operates in a failure driven mode. That is, it goes into a wait state until there is an
 * error. Upon the notification of a connection error, the monitor changes to operate in a time
 * driven mode. That is, it attempts to recover the connections on a periodic basis. When all failed
 * connections are restored, it changes back to the failure driven mode.
 */
public class LateralCacheMonitor extends Thread
{
    /** The logger */
    private static final Log log = LogFactory.getLog( LateralCacheMonitor.class );

    /** How long to wait between runs */
    private static long idlePeriod = 20 * 1000;

    /**
     * Must make sure LateralCacheMonitor is started before any lateral error can be detected!
     */
    private boolean allright = true;

    /**
     * Map of caches to monitor
     */
    private ConcurrentHashMap<String, LateralCacheNoWait<?, ?>> caches;

    /**
     * Reference to the factory
     */
    private LateralTCPCacheFactory factory;

    /**
     * shutdown flag
     */
    private boolean shutdown = false;

    /** code for eror */
    private static final int ERROR = 1;

    /** The mode we are running in. Error driven */
    private static int mode = ERROR;

    /**
     * Configures the idle period between repairs.
     * <p>
     * @param idlePeriod The new idlePeriod value
     */
    public static void setIdlePeriod( long idlePeriod )
    {
        if ( idlePeriod > LateralCacheMonitor.idlePeriod )
        {
            LateralCacheMonitor.idlePeriod = idlePeriod;
        }
    }

    /**
     * Allows close classes, ie testers to set the idle period to something testable.
     * <p>
     * @param idlePeriod
     */
    protected static void forceShortIdlePeriod( long idlePeriod )
    {
        LateralCacheMonitor.idlePeriod = idlePeriod;
    }

    /**
     * Constructor for the LateralCacheMonitor object
     * <p>
     * It's the clients responsibility to decide how many of these there will be.
     *
     * @param factory a reference to the factory that manages the service instances
     */
    public LateralCacheMonitor(LateralTCPCacheFactory factory)
    {
        super("JCS-LateralCacheMonitor");
        this.factory = factory;
        this.caches = new ConcurrentHashMap<String, LateralCacheNoWait<?,?>>();
    }

    /**
     * Add a cache to be monitored
     *
     * @param cache the cache
     */
    public void addCache(LateralCacheNoWait<?, ?> cache)
    {
        this.caches.put(cache.getCacheName(), cache);

        // if not yet started, go ahead
        if (this.getState() == Thread.State.NEW)
        {
            this.start();
        }
    }

    /**
     * Notifies the cache monitor that an error occurred, and kicks off the error recovery process.
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
     * Notifies the cache monitor that the service shall shut down
     */
    public void notifyShutdown()
    {
        synchronized ( this )
        {
            this.shutdown = true;
            notify();
        }
    }

    /**
     * Main processing method for the LateralCacheMonitor object
     */
    @Override
    public void run()
    {
        do
        {
            if ( mode == ERROR )
            {
                if ( log.isDebugEnabled() )
                {
                    if ( allright )
                    {
                        log.debug( "ERROR DRIVEN MODE: allright = " + allright
                            + ", connection monitor will wait for an error." );
                    }
                    else
                    {
                        log.debug( "ERROR DRIVEN MODE: allright = " + allright + " connection monitor running." );
                    }
                }

                synchronized ( this )
                {
                    if ( allright )
                    {
                        // Failure driven mode.
                        try
                        {
                            wait();
                            // wake up only if there is an error.
                        }
                        catch ( InterruptedException ignore )
                        {
                            //no op, this is expected
                        }
                    }
                }
            }
            else
            {
                log.debug( "TIME DRIVEN MODE: connection monitor will sleep for " + idlePeriod + " after this run." );
                // Time driven mode: sleep between each round of recovery
                // attempt.
                // will need to test not just check status
            }

            // check for requested shutdown
            synchronized ( this )
            {
                if (shutdown)
                {
                    log.info( "Shutting down cache monitor" );
                    this.caches.clear();
                    return;
                }
            }

            // The "allright" flag must be false here.
            // Simply presume we can fix all the errors until proven otherwise.
            synchronized ( this )
            {
                allright = true;
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "Cache monitor running." );
            }

            // Monitor each cache instance one after the other.
            log.info( "Number of caches to monitor = " + caches.size() );
            //for
            for (Map.Entry<String, LateralCacheNoWait<?, ?>> entry : caches.entrySet())
            {
                String cacheName = entry.getKey();

                @SuppressWarnings("unchecked") // Downcast to match service
                LateralCacheNoWait<Serializable, Serializable> c =
                        (LateralCacheNoWait<Serializable, Serializable>) entry.getValue();
                if ( c.getStatus() == CacheStatus.ERROR )
                {
                    log.info( "Found LateralCacheNoWait in error, " + cacheName );

                    ITCPLateralCacheAttributes lca = (ITCPLateralCacheAttributes)c.getAuxiliaryCacheAttributes();

                    // Get service instance
                    ICacheServiceNonLocal<Serializable, Serializable> cacheService = factory.getCSNLInstance(lca);

                    // If we can't fix them, just skip and re-try in the
                    // next round.
                    if (cacheService instanceof ZombieCacheServiceNonLocal)
                    {
                        continue;
                    }

                    c.fixCache(cacheService);
                }
            }

            try
            {
                // don't want to sleep after waking from an error
                // run immediately and sleep here.
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Lateral cache monitor sleeping for " + idlePeriod + " between runs." );
                }

                Thread.sleep( idlePeriod );
            }
            catch ( InterruptedException ex )
            {
                // ignore;
            }
        }
        while ( true );
    }

    /**
     * Sets the "allright" flag to false in a critical section.
     */
    private void bad()
    {
        if ( allright )
        {
            synchronized ( this )
            {
                allright = false;
            }
        }
    }
}
