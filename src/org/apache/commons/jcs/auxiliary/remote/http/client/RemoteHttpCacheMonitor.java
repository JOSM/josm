package org.apache.commons.jcs.auxiliary.remote.http.client;

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

import org.apache.commons.jcs.auxiliary.remote.http.client.behavior.IRemoteHttpCacheClient;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Upon the notification of a connection error, the monitor changes to operate in a time driven
 * mode. That is, it attempts to recover the connections on a periodic basis. When all failed
 * connections are restored, it changes back to the failure driven mode.
 */
public class RemoteHttpCacheMonitor
    implements Runnable
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteHttpCacheMonitor.class );

    /** The remote cache that we are monitoring */
    private static RemoteHttpCacheMonitor instance;

    /** Time between checks */
    private static long idlePeriod = 3 * 1000;

    /** Set of remote caches to monitor. This are added on error, if not before. */
    private final Set<RemoteHttpCache<?, ?>> remoteHttpCaches = new HashSet<RemoteHttpCache<?, ?>>();

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
        if ( idlePeriod > RemoteHttpCacheMonitor.idlePeriod )
        {
            RemoteHttpCacheMonitor.idlePeriod = idlePeriod;
        }
    }

    /** Constructor for the RemoteCacheMonitor object */
    private RemoteHttpCacheMonitor()
    {
        super();
    }

    /**
     * Returns the singleton instance.
     * <p>
     * @return The instance value
     */
    static RemoteHttpCacheMonitor getInstance()
    {
        synchronized ( RemoteHttpCacheMonitor.class )
        {
            if ( instance == null )
            {
                return instance = new RemoteHttpCacheMonitor();
            }
        }
        return instance;
    }

    /**
     * Notifies the cache monitor that an error occurred, and kicks off the error recovery process.
     * <p>
     * @param remoteCache
     */
    public void notifyError( RemoteHttpCache<?, ?> remoteCache )
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Notified of an error. " + remoteCache );
        }
        bad();
        synchronized ( this )
        {
            remoteHttpCaches.add( remoteCache );
            notify();
        }
    }

    // Run forever.

    // Avoid the use of any synchronization in the process of monitoring for
    // performance reasons.
    // If exception is thrown owing to synchronization,
    // just skip the monitoring until the next round.
    /** Main processing method for the RemoteCacheMonitor object */
    @Override
    public void run()
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Monitoring daemon started" );
        }
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

            // The "allright" flag must be false here.
            // Simply presume we can fix all the errors until proven otherwise.
            synchronized ( this )
            {
                alright = true;
            }

            // Make a copy
            Set<RemoteHttpCache<?, ?>> remoteCachesToExamine =
                new HashSet<RemoteHttpCache<?, ?>>();
            synchronized ( this )
            {
                for (RemoteHttpCache<?, ?> remoteCache : this.remoteHttpCaches)
                {
                    remoteCachesToExamine.add( remoteCache );
                }
            }
            // If any cache is in error, it strongly suggests all caches
            // managed by the
            // same RmicCacheManager instance are in error. So we fix
            // them once and for all.
            for (RemoteHttpCache<?, ?> remoteCache : remoteCachesToExamine)
            {
                try
                {
                    if ( remoteCache.getStatus() == CacheStatus.ERROR )
                    {
                        RemoteHttpCacheAttributes attributes = remoteCache.getRemoteHttpCacheAttributes();

                        IRemoteHttpCacheClient<Serializable, Serializable> remoteService = RemoteHttpCacheManager.getInstance()
                            .createRemoteHttpCacheClientForAttributes( attributes );

                        if ( log.isInfoEnabled() )
                        {
                            log.info( "Performing Alive check on service " + remoteService );
                        }
                        // If we can't fix them, just skip and re-try in
                        // the next round.
                        if ( remoteService.isAlive() )
                        {
                            remoteCache.fixCache( remoteService );
                        }
                        else
                        {
                            bad();
                        }
                        break;
                    }
                }
                catch ( Exception ex )
                {
                    bad();
                    // Problem encountered in fixing the caches managed by a
                    // RemoteCacheManager instance.
                    // Soldier on to the next RemoteHttpCache.
                    log.error( ex );
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
