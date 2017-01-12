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
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheConstants;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Registered with RemoteCache server. The server updates the local caches via this listener. Each
 * server assigns a unique listener id for a listener.
 * <p>
 * One listener is used per remote cache server. The same listener is used for all the regions that
 * talk to a particular server.
 */
public class RemoteCacheListener<K, V>
    extends AbstractRemoteCacheListener<K, V>
    implements IRemoteCacheConstants
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteCacheListener.class );

    /** Has this client been shutdown. */
    private boolean disposed = false;

    /**
     * Only need one since it does work for all regions, just reference by multiple region names.
     * <p>
     * The constructor exports this object, making it available to receive incoming calls. The
     * callback port is anonymous unless a local port value was specified in the configuration.
     * <p>
     * @param irca cache configuration
     * @param cacheMgr the cache hub
     * @param elementSerializer a custom serializer
     */
    public RemoteCacheListener( IRemoteCacheAttributes irca, ICompositeCacheManager cacheMgr, IElementSerializer elementSerializer )
    {
        super( irca, cacheMgr, elementSerializer );

        // Export this remote object to make it available to receive incoming
        // calls.
        try
        {
            UnicastRemoteObject.exportObject( this, irca.getLocalPort() );
        }
        catch ( RemoteException ex )
        {
            log.error( "Problem exporting object.", ex );
            throw new IllegalStateException( ex.getMessage() );
        }
    }

    /**
     * Deregister itself.
     * <p>
     * @throws IOException
     */
    @Override
    public synchronized void dispose()
        throws IOException
    {
        if ( !disposed )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Unexporting listener." );
            }
            try
            {
                UnicastRemoteObject.unexportObject( this, true );
            }
            catch ( RemoteException ex )
            {
                log.error( "Problem unexporting the listener.", ex );
                throw new IllegalStateException( ex.getMessage() );
            }
            disposed = true;
        }
    }

    /**
     * For easier debugging.
     * <p>
     * @return Basic info on this listener.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\n RemoteCacheListener: " );
        buf.append( super.toString() );
        return buf.toString();
    }
}
