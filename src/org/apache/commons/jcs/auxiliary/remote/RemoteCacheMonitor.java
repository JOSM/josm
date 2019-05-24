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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheMonitor;

/**
 * Used to monitor and repair any failed connection for the remote cache service. By default the
 * monitor operates in a failure driven mode. That is, it goes into a wait state until there is an
 * error.
 *
 * TODO consider moving this into an active monitoring mode. Upon the notification of a
 * connection error, the monitor changes to operate in a time driven mode. That is, it attempts to
 * recover the connections on a periodic basis. When all failed connections are restored, it changes
 * back to the failure driven mode.
 */
public class RemoteCacheMonitor extends AbstractAuxiliaryCacheMonitor
{
    /**
     * Map of managers to monitor
     */
    private ConcurrentHashMap<RemoteCacheManager, RemoteCacheManager> managers;

    /** Constructor for the RemoteCacheMonitor object */
    public RemoteCacheMonitor()
    {
        super("JCS-RemoteCacheMonitor");
        this.managers = new ConcurrentHashMap<>();
        setIdlePeriod(30000L);
    }

    /**
     * Add a manager to be monitored
     *
     * @param manager the remote cache manager
     */
    public void addManager(RemoteCacheManager manager)
    {
        this.managers.put(manager, manager);

        // if not yet started, go ahead
        if (this.getState() == Thread.State.NEW)
        {
            this.start();
        }
    }

    /**
     * Clean up all resources before shutdown
     */
    @Override
    public void dispose()
    {
        this.managers.clear();
    }

    // Avoid the use of any synchronization in the process of monitoring for
    // performance reason.
    // If exception is thrown owing to synchronization,
    // just skip the monitoring until the next round.
    /** Main processing method for the RemoteCacheMonitor object */
    @Override
    public void doWork()
    {
        // Monitor each RemoteCacheManager instance one after the other.
        // Each RemoteCacheManager corresponds to one remote connection.
        for (RemoteCacheManager mgr : managers.values())
        {
            // If we can't fix them, just skip and re-try in
            // the next round.
            if ( mgr.canFixCaches() )
            {
                mgr.fixCaches();
            }
            else
            {
                allright.set(false);
            }
        }
    }
}
