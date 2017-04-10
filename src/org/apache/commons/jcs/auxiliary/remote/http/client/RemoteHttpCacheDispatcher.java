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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheDispatcher;
import org.apache.commons.jcs.auxiliary.remote.value.RemoteCacheRequest;
import org.apache.commons.jcs.auxiliary.remote.value.RemoteCacheResponse;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Calls the service. */
public class RemoteHttpCacheDispatcher
    extends AbstractHttpClient
    implements IRemoteCacheDispatcher
{
    /** Parameter encoding */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /** Named of the parameter */
    private static final String PARAMETER_REQUEST_TYPE = "RequestType";

    /** Named of the parameter */
    private static final String PARAMETER_KEY = "Key";

    /** Named of the parameter */
    private static final String PARAMETER_CACHE_NAME = "CacheName";

    /** The Logger. */
    private static final Log log = LogFactory.getLog( RemoteHttpCacheDispatcher.class );

    /** This needs to be standard, since the other side is standard */
    private StandardSerializer serializer = new StandardSerializer();

    /**
     * @param remoteHttpCacheAttributes
     */
    public RemoteHttpCacheDispatcher( RemoteHttpCacheAttributes remoteHttpCacheAttributes )
    {
        super( remoteHttpCacheAttributes );
    }

    /**
     * All requests will go through this method.
     * <p>
     * TODO consider taking in a URL instead of using the one in the configuration.
     * <p>
     * @param remoteCacheRequest
     * @return RemoteCacheResponse
     * @throws IOException
     */
    @Override
    public <K, V, T>
        RemoteCacheResponse<T> dispatchRequest( RemoteCacheRequest<K, V> remoteCacheRequest )
        throws IOException
    {
        try
        {
            byte[] requestAsByteArray = serializer.serialize( remoteCacheRequest );

            String url = addParameters( remoteCacheRequest, getRemoteHttpCacheAttributes().getUrl() );

            byte[] responseAsByteArray = processRequest( requestAsByteArray, url );

            RemoteCacheResponse<T> remoteCacheResponse = null;
            try
            {
                remoteCacheResponse = serializer.deSerialize( responseAsByteArray, null );
            }
            catch ( ClassNotFoundException e )
            {
                log.error( "Couldn't deserialize the response.", e );
            }
            return remoteCacheResponse;
        }
        catch ( Exception e )
        {
            throw new IOException("Problem dispatching request.", e);
        }
    }

    /**
     * @param requestAsByteArray
     * @param url
     * @return byte[] - the response
     * @throws IOException
     * @throws HttpException
     */
    protected byte[] processRequest( byte[] requestAsByteArray, String url )
        throws IOException, HttpException
    {
        PostMethod post = new PostMethod( url );
        RequestEntity requestEntity = new ByteArrayRequestEntity( requestAsByteArray );
        post.setRequestEntity( requestEntity );
        doWebserviceCall( post );
        byte[] response = post.getResponseBody();
        return response;
    }

    /**
     * @param remoteCacheRequest
     * @param baseUrl
     * @return String
     */
    protected <K, V> String addParameters( RemoteCacheRequest<K, V> remoteCacheRequest, String baseUrl )
    {
        StringBuilder url = new StringBuilder( baseUrl == null ? "" : baseUrl );

        try
        {
            if ( baseUrl != null && baseUrl.indexOf( "?" ) == -1 )
            {
                url.append( "?" );
            }
            else
            {
                url.append( "&" );
            }

            if ( getRemoteHttpCacheAttributes().isIncludeCacheNameAsParameter() )
            {
                if ( remoteCacheRequest.getCacheName() != null )
                {
                    url.append( PARAMETER_CACHE_NAME + "="
                        + URLEncoder.encode( remoteCacheRequest.getCacheName(), DEFAULT_ENCODING ) );
                }
            }
            if ( getRemoteHttpCacheAttributes().isIncludeKeysAndPatternsAsParameter() )
            {
                String keyValue = "";
                switch ( remoteCacheRequest.getRequestType() )
                {
                    case GET:
                    case REMOVE:
                    case GET_KEYSET:
                        keyValue = remoteCacheRequest.getKey() + "";
                        break;
                    case GET_MATCHING:
                        keyValue = remoteCacheRequest.getPattern();
                        break;
                    case GET_MULTIPLE:
                        keyValue = remoteCacheRequest.getKeySet() + "";
                        break;
                    case UPDATE:
                        keyValue = remoteCacheRequest.getCacheElement().getKey() + "";
                        break;
                    default:
                        break;
                }
                String encodedKeyValue = URLEncoder.encode( keyValue, DEFAULT_ENCODING );
                url.append( "&" + PARAMETER_KEY + "=" + encodedKeyValue );
            }
            if ( getRemoteHttpCacheAttributes().isIncludeRequestTypeasAsParameter() )
            {
                url.append( "&"
                    + PARAMETER_REQUEST_TYPE
                    + "="
                    + URLEncoder.encode( remoteCacheRequest.getRequestType().toString(), DEFAULT_ENCODING ) );
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            log.error( "Couldn't encode URL.", e );
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "Url: " + url.toString() );
        }

        return url.toString();
    }

    /**
     * Called before the executeMethod on the client.
     * <p>
     * @param post http method
     * @return HttpState
     * @throws IOException
     */
    @Override
    protected HttpState preProcessWebserviceCall( HttpMethod post )
        throws IOException
    {
        // do nothing. Child can override.
        return null;
    }

    /**
     * Called after the executeMethod on the client.
     * <p>
     * @param post http method
     * @param httpState state
     * @throws IOException
     */
    @Override
    protected void postProcessWebserviceCall( HttpMethod post, HttpState httpState )
        throws IOException
    {
        // do nothing. Child can override.
    }
}
