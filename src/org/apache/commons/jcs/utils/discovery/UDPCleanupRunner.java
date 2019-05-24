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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * This class periodically check the lastHeardFrom time on the services.
 * <p>
 * If they exceed the configurable limit, it removes them from the set.
 * <p>
 * @author Aaron Smuts
 */
public class UDPCleanupRunner
    implements Runnable
{
    /** log instance */
    private static final Log log = LogFactory.getLog( UDPCleanupRunner.class );

    /** UDP discovery service */
    private final UDPDiscoveryService discoveryService;

    /** default for max idle time, in seconds */
    private static final long DEFAULT_MAX_IDLE_TIME_SECONDS = 180;

    /** The configured max idle time, in seconds */
    private final long maxIdleTimeSeconds = DEFAULT_MAX_IDLE_TIME_SECONDS;

    /**
     * @param service UDPDiscoveryService
     */
    public UDPCleanupRunner( UDPDiscoveryService service )
    {
        this.discoveryService = service;
    }

    /**
     * This goes through the list of services and removes those that we haven't heard from in longer
     * than the max idle time.
     * <p>
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        long now = System.currentTimeMillis();

        // iterate through the set
        // it is thread safe
        // http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/CopyOnWriteArraySet.
        // html
        // TODO this should get a copy.  you can't simply remove from this.
        // the listeners need to be notified.
        Set<DiscoveredService> toRemove = new HashSet<>();
        // can't remove via the iterator. must remove directly
        for (DiscoveredService service : discoveryService.getDiscoveredServices())
        {
            if ( ( now - service.getLastHearFromTime() ) > ( maxIdleTimeSeconds * 1000 ) )
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "Removing service, since we haven't heard from it in " + maxIdleTimeSeconds
                        + " seconds.  service = " + service );
                }
                toRemove.add( service );
            }
        }

        // remove the bad ones
        for (DiscoveredService service : toRemove)
        {
            // call this so the listeners get notified
            discoveryService.removeDiscoveredService( service );
        }
    }
}
