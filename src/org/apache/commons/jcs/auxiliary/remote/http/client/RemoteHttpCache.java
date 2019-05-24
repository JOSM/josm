package org.apache.commons.jcs.auxiliary.remote.http.client;

import java.io.IOException;

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

import org.apache.commons.jcs.auxiliary.remote.AbstractRemoteAuxiliaryCache;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
import org.apache.commons.jcs.engine.ZombieCacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This uses an http client as the service.
 */
public class RemoteHttpCache<K, V>
    extends AbstractRemoteAuxiliaryCache<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( RemoteHttpCache.class );

    /** for error notifications */
    private RemoteHttpCacheMonitor monitor;

    /** Keep the child copy here for the restore process. */
    private RemoteHttpCacheAttributes remoteHttpCacheAttributes;

    /**
     * Constructor for the RemoteCache object. This object communicates with a remote cache server.
     * One of these exists for each region. This also holds a reference to a listener. The same
     * listener is used for all regions for one remote server. Holding a reference to the listener
     * allows this object to know the listener id assigned by the remote cache.
     * <p>
     * @param remoteHttpCacheAttributes
     * @param remote
     * @param listener
     * @param monitor the cache monitor
     */
    public RemoteHttpCache( RemoteHttpCacheAttributes remoteHttpCacheAttributes, ICacheServiceNonLocal<K, V> remote,
                            IRemoteCacheListener<K, V> listener, RemoteHttpCacheMonitor monitor )
    {
        super( remoteHttpCacheAttributes, remote, listener );

        this.remoteHttpCacheAttributes = remoteHttpCacheAttributes;
        this.monitor = monitor;
    }

    /**
     * Nothing right now. This should setup a zombie and initiate recovery.
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
        // we should not switch if the existing is a zombie.
        if ( !( getRemoteCacheService() instanceof ZombieCacheServiceNonLocal ) )
        {
            String message = "Disabling remote cache due to error: " + msg;
            logError( cacheName, "", message );
            log.error( message, ex );

            setRemoteCacheService( new ZombieCacheServiceNonLocal<>( getRemoteCacheAttributes().getZombieQueueMaxSize() ) );

            monitor.notifyError( this );
        }

        if ( ex instanceof IOException )
        {
            throw (IOException) ex;
        }
        throw new IOException( ex.getMessage() );
    }

    /**
     * @return url of service
     */
    @Override
    public String getEventLoggingExtraInfo()
    {
        return null;
    }

    /**
     * @return the remoteHttpCacheAttributes
     */
    public RemoteHttpCacheAttributes getRemoteHttpCacheAttributes()
    {
        return remoteHttpCacheAttributes;
    }
}
