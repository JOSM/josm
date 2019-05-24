package org.apache.commons.jcs.auxiliary.remote.util;

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

import java.util.Set;

import org.apache.commons.jcs.auxiliary.remote.value.RemoteCacheRequest;
import org.apache.commons.jcs.auxiliary.remote.value.RemoteRequestType;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This creates request objects. You could write your own client and use the objects from this
 * factory.
 */
public class RemoteCacheRequestFactory
{
    /** The Logger. */
    private static final Log log = LogFactory.getLog( RemoteCacheRequestFactory.class );

    /**
     * Create generic request
     * @param cacheName cache name
     * @param requestType type of request
     * @param requesterId id of requester
     * @return the request
     */
    private static <K, V> RemoteCacheRequest<K, V> createRequest(String cacheName, RemoteRequestType requestType, long requesterId)
    {
        RemoteCacheRequest<K, V> request = new RemoteCacheRequest<>();
        request.setCacheName( cacheName );
        request.setRequestType( requestType );
        request.setRequesterId( requesterId );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Created: " + request );
        }

        return request;
    }

    /**
     * Creates a get Request.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createGetRequest( String cacheName, K key, long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(cacheName, RemoteRequestType.GET, requesterId);
        request.setKey( key );

        return request;
    }

    /**
     * Creates a getMatching Request.
     * <p>
     * @param cacheName
     * @param pattern
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createGetMatchingRequest( String cacheName, String pattern, long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(cacheName, RemoteRequestType.GET_MATCHING, requesterId);
        request.setPattern( pattern );

        return request;
    }

    /**
     * Creates a getMultiple Request.
     * <p>
     * @param cacheName
     * @param keys
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createGetMultipleRequest( String cacheName, Set<K> keys, long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(cacheName, RemoteRequestType.GET_MULTIPLE, requesterId);
        request.setKeySet(keys);

        return request;
    }

    /**
     * Creates a remove Request.
     * <p>
     * @param cacheName
     * @param key
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createRemoveRequest( String cacheName, K key, long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(cacheName, RemoteRequestType.REMOVE, requesterId);
        request.setKey( key );

        return request;
    }

    /**
     * Creates a GetKeySet Request.
     * <p>
     * @param cacheName
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static RemoteCacheRequest<String, String> createGetKeySetRequest( String cacheName, long requesterId )
    {
        RemoteCacheRequest<String, String> request = createRequest(cacheName, RemoteRequestType.GET_KEYSET, requesterId);
        request.setKey( cacheName );

        return request;
    }

    /**
     * Creates a removeAll Request.
     * <p>
     * @param cacheName
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createRemoveAllRequest( String cacheName, long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(cacheName, RemoteRequestType.REMOVE_ALL, requesterId);

        return request;
    }

    /**
     * Creates a dispose Request.
     * <p>
     * @param cacheName
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createDisposeRequest( String cacheName, long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(cacheName, RemoteRequestType.DISPOSE, requesterId);

        return request;
    }

    /**
     * Creates an Update Request.
     * <p>
     * @param cacheElement
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createUpdateRequest( ICacheElement<K, V> cacheElement, long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(null, RemoteRequestType.UPDATE, requesterId);
        if ( cacheElement != null )
        {
            request.setCacheName( cacheElement.getCacheName() );
            request.setCacheElement( cacheElement );
            request.setKey( cacheElement.getKey() );
        }
        else
        {
            log.error( "Can't create a proper update request for a null cache element." );
        }

        return request;
    }

    /**
     * Creates an alive check Request.
     * <p>
     * @param requesterId
     * @return RemoteHttpCacheRequest
     */
    public static <K, V> RemoteCacheRequest<K, V> createAliveCheckRequest( long requesterId )
    {
        RemoteCacheRequest<K, V> request = createRequest(null, RemoteRequestType.ALIVE_CHECK, requesterId);

        return request;
    }
}
