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

import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.CacheStatus;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to provide access to multiple services under nowait protection. Factory should construct
 * NoWaitFacade to give to the composite cache out of caches it constructs from the varies manager
 * to lateral services.
 * <p>
 * Typically, we only connect to one remote server per facade. We use a list of one
 * RemoteCacheNoWait.
 */
public class RemoteCacheNoWaitFacade<K, V>
    extends AbstractRemoteCacheNoWaitFacade<K, V>
{
    /** log instance */
    private static final Log log = LogFactory.getLog( RemoteCacheNoWaitFacade.class );

    /**
     * Constructs with the given remote cache, and fires events to any listeners.
     * <p>
     * @param noWaits
     * @param rca
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     */
    public RemoteCacheNoWaitFacade( RemoteCacheNoWait<K, V>[] noWaits, RemoteCacheAttributes rca,
                                    ICompositeCacheManager cacheMgr, ICacheEventLogger cacheEventLogger,
                                    IElementSerializer elementSerializer )
    {
        super( noWaits, rca, cacheMgr, cacheEventLogger, elementSerializer );
    }

    /**
     * Begin the failover process if this is a local cache. Clustered remote caches do not failover.
     * <p>
     * @param i The no wait in error.
     */
    @Override
    protected void failover( int i )
    {
        if ( log.isDebugEnabled() )
        {
            log.info( "in failover for " + i );
        }

        if ( getRemoteCacheAttributes().getRemoteType() == RemoteType.LOCAL )
        {
            if ( noWaits[i].getStatus() == CacheStatus.ERROR )
            {
                // start failover, primary recovery process
                RemoteCacheFailoverRunner<K, V> runner =
                    new RemoteCacheFailoverRunner<K, V>( this, getCompositeCacheManager(),
                      super.getCacheEventLogger(), super.getElementSerializer() );
                runner.notifyError();
                Thread t = new Thread( runner );
                t.setDaemon( true );
                t.start();

                if ( getCacheEventLogger() != null )
                {
                    getCacheEventLogger().logApplicationEvent( "RemoteCacheNoWaitFacade", "InitiatedFailover",
                                                               noWaits[i] + " was in error." );
                }
            }
            else
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "The noWait is not in error" );
                }
            }
        }
    }

}
