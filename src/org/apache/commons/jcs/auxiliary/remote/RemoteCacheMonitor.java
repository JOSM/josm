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

import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to monitor and repair any failed connection for the remote cache service. By default the
 * monitor operates in a failure driven mode. That is, it goes into a wait state until there is an
 * error. TODO consider moving this into an active monitoring mode. Upon the notification of a
 * connection error, the monitor changes to operate in a time driven mode. That is, it attempts to
 * recover the connections on a periodic basis. When all failed connections are restored, it changes
 * back to the failure driven mode.
 */
public class RemoteCacheMonitor
    implements Runnable
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteCacheMonitor.class );

    /** The remote cache that we are monitoring */
    private static RemoteCacheMonitor instance;

    /** Time between checks */
    private static long idlePeriod = 30 * 1000;

    // minimum 30 seconds.
    //private static long idlePeriod = 3*1000; // for debugging.

    /**
     * Must make sure RemoteCacheMonitor is started before any remote error can be detected!
     */
    private boolean alright = true;

    /** Time driven mode */
    static final int TIME = 0;

    /** Error driven mode -- only check on health if there is an error */
    static final int ERROR = 1;

    /** The mode to use */
    static int mode = ERROR;

    /**
     * Configures the idle period between repairs.
     * <p>
     * @param idlePeriod The new idlePeriod value
     */
    public static void setIdlePeriod( long idlePeriod )
    {
        if ( idlePeriod > RemoteCacheMonitor.idlePeriod )
        {
            RemoteCacheMonitor.idlePeriod = idlePeriod;
        }
    }

    /** Constructor for the RemoteCacheMonitor object */
    private RemoteCacheMonitor()
    {
        super();
    }

    /**
     * Returns the singleton instance.
     * <p>
     * @return The instance value
     */
    static RemoteCacheMonitor getInstance()
    {
        synchronized ( RemoteCacheMonitor.class )
        {
            if ( instance == null )
            {
                return instance = new RemoteCacheMonitor();
            }
        }
        return instance;
    }

    /**
     * Notifies the cache monitor that an error occurred, and kicks off the error recovery process.
     */
    public void notifyError()
    {
        log.debug( "Notified of an error." );
        bad();
        synchronized ( this )
        {
            notify();
        }
    }

    // Run forever.

    // Avoid the use of any synchronization in the process of monitoring for
    // performance reason.
    // If exception is thrown owing to synchronization,
    // just skip the monitoring until the next round.
    /** Main processing method for the RemoteCacheMonitor object */
    @Override
    public void run()
    {
        log.debug( "Monitoring daemon started" );
        do
        {
            if ( mode == ERROR )
            {
                synchronized ( this )
                {
                    if ( alright )
                    {
                        // make this configurable, comment out wait to enter
                        // time driven mode
                        // Failure driven mode.
                        try
                        {
                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "FAILURE DRIVEN MODE: cache monitor waiting for error" );
                            }
                            wait();
                            // wake up only if there is an error.
                        }
                        catch ( InterruptedException ignore )
                        {
                            // swallow
                        }
                    }
                }
            }
            else
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "TIME DRIVEN MODE: cache monitor sleeping for " + idlePeriod );
                }
                // Time driven mode: sleep between each round of recovery
                // attempt.
                // will need to test not just check status
            }

            try
            {
                Thread.sleep( idlePeriod );
            }
            catch ( InterruptedException ex )
            {
                // ignore;
            }

            // The "alright" flag must be false here.
            // Simply presume we can fix all the errors until proven otherwise.
            synchronized ( this )
            {
                alright = true;
            }
            //p("cache monitor running.");
            // Monitor each RemoteCacheManager instance one after the other.
            // Each RemoteCacheManager corresponds to one remote connection.
            for (RemoteCacheManager mgr : RemoteCacheManager.instances.values())
            {
                try
                {
                    // If any cache is in error, it strongly suggests all caches
                    // managed by the
                    // same RmicCacheManager instance are in error. So we fix
                    // them once and for all.
                    for (RemoteCacheNoWait<?, ?> c : mgr.caches.values())
                    {
                        if ( c.getStatus() == CacheStatus.ERROR )
                        {
                            RemoteCacheRestore repairer = new RemoteCacheRestore( mgr );
                            // If we can't fix them, just skip and re-try in
                            // the next round.
                            if ( repairer.canFix() )
                            {
                                repairer.fix();
                            }
                            else
                            {
                                bad();
                            }
                            break;
                        }
                    }
                }
                catch ( Exception ex )
                {
                    bad();
                    // Problem encountered in fixing the caches managed by a
                    // RemoteCacheManager instance.
                    // Soldier on to the next RemoteCacheManager instance.
                    log.error( "Problem fixing caches for manager." + mgr, ex );
                }
            }
        }
        while ( true );
    }

    /** Sets the "aright" flag to false in a critical section. */
    private synchronized void bad()
    {
        alright = false;
    }
}
