package org.apache.commons.jcs.auxiliary.remote.http.server;

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

import org.apache.commons.jcs.auxiliary.remote.value.RemoteCacheRequest;
import org.apache.commons.jcs.auxiliary.remote.value.RemoteCacheResponse;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Servlet deserializes the request object. The request object is passed to the processor. The
 * processor then calls the service which does the work of talking to the cache.
 * <p>
 * This is essentially an adaptor on top of the service.
 */
public class RemoteCacheServiceAdaptor<K extends Serializable, V extends Serializable>
{
    /** The Logger. */
    private static final Log log = LogFactory.getLog( RemoteCacheServiceAdaptor.class );

    /** The service that does the work. */
    private ICacheServiceNonLocal<K, V> remoteCacheService;

    /** This is for testing without the factory. */
    protected RemoteCacheServiceAdaptor()
    {
        // for testing.
    }

    /**
     * Create a process with a cache manager.
     * <p>
     * @param cacheManager
     */
    public RemoteCacheServiceAdaptor( ICompositeCacheManager cacheManager )
    {
        ICacheServiceNonLocal<K, V> rcs = RemoteHttpCacheSeviceFactory.createRemoteHttpCacheService( cacheManager );
        setRemoteCacheService( rcs );
    }

    /**
     * Processes the request. It will call the appropriate method on the service
     * <p>
     * @param request
     * @return RemoteHttpCacheResponse, never null
     */
    @SuppressWarnings( "unchecked" ) // need to cast to correct return type
    public <T> RemoteCacheResponse<T> processRequest( RemoteCacheRequest<K, V> request )
    {
        RemoteCacheResponse<Object> response = new RemoteCacheResponse<Object>();

        if ( request == null )
        {
            String message = "The request is null. Cannot process";
            log.warn( message );
            response.setSuccess( false );
            response.setErrorMessage( message );
        }
        else
        {
            try
            {
                switch ( request.getRequestType() )
                {
                    case GET:
                        ICacheElement<K, V> element = getRemoteCacheService().get( request.getCacheName(), request.getKey(),
                                                                             request.getRequesterId() );
                        response.setPayload(element);
                        break;
                    case GET_MULTIPLE:
                        Map<K, ICacheElement<K, V>> elementMap = getRemoteCacheService().getMultiple( request.getCacheName(),
                                                                              request.getKeySet(),
                                                                              request.getRequesterId() );
                        if ( elementMap != null )
                        {
                            Map<K, ICacheElement<K, V>> map = new HashMap<K, ICacheElement<K,V>>();
                            map.putAll(elementMap);
                            response.setPayload(map);
                        }
                        break;
                    case GET_MATCHING:
                        Map<K, ICacheElement<K, V>> elementMapMatching = getRemoteCacheService().getMatching( request.getCacheName(),
                                                                                      request.getPattern(),
                                                                                      request.getRequesterId() );
                        if ( elementMapMatching != null )
                        {
                            Map<K, ICacheElement<K, V>> map = new HashMap<K, ICacheElement<K,V>>();
                            map.putAll(elementMapMatching);
                            response.setPayload(map);
                        }
                        break;
                    case REMOVE:
                        getRemoteCacheService().remove( request.getCacheName(), request.getKey(),
                                                        request.getRequesterId() );
                        break;
                    case REMOVE_ALL:
                        getRemoteCacheService().removeAll( request.getCacheName(), request.getRequesterId() );
                        break;
                    case UPDATE:
                        getRemoteCacheService().update( request.getCacheElement(), request.getRequesterId() );
                        break;
                    case ALIVE_CHECK:
                        response.setSuccess( true );
                        break;
                    case DISPOSE:
                        response.setSuccess( true );
                        // DO NOTHING
                        break;
                    case GET_KEYSET:
                        Set<K> keys = getRemoteCacheService().getKeySet( request.getCacheName() );
                        response.setPayload( keys );
                        break;
                    default:
                        String message = "Unknown event type.  Cannot process " + request;
                        log.warn( message );
                        response.setSuccess( false );
                        response.setErrorMessage( message );
                        break;
                }
            }
            catch ( IOException e )
            {
                String message = "Problem processing request. " + request + " Error: " + e.getMessage();
                log.error( message, e );
                response.setSuccess( false );
                response.setErrorMessage( message );
            }
        }

        return (RemoteCacheResponse<T>)response;
    }

    /**
     * @param remoteHttpCacheService the remoteHttpCacheService to set
     */
    public void setRemoteCacheService( ICacheServiceNonLocal<K, V> remoteHttpCacheService )
    {
        this.remoteCacheService = remoteHttpCacheService;
    }

    /**
     * @return the remoteHttpCacheService
     */
    public ICacheServiceNonLocal<K, V> getRemoteCacheService()
    {
        return remoteCacheService;
    }
}
