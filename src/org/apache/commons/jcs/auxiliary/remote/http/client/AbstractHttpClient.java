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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class simply configures the http multithreaded connection manager.
 * <p>
 * This is abstract because it can do anything. Child classes can overwrite whatever they want.
 */
public abstract class AbstractHttpClient
{
    /** The connection manager. */
    private MultiThreadedHttpConnectionManager connectionManager;

    /** The client */
    private HttpClient httpClient;

    /** Configuration settings. */
    private RemoteHttpCacheAttributes remoteHttpCacheAttributes;

    /** The Logger. */
    private static final Log log = LogFactory.getLog( AbstractHttpClient.class );

    /**
     * Sets the default Properties File and Heading, and creates the HttpClient and connection
     * manager.
     * <p>
     * @param remoteHttpCacheAttributes
     */
    public AbstractHttpClient( RemoteHttpCacheAttributes remoteHttpCacheAttributes )
    {
        this.remoteHttpCacheAttributes = remoteHttpCacheAttributes;
        this.connectionManager = new MultiThreadedHttpConnectionManager();
        this.httpClient = new HttpClient(this.connectionManager);

        configureClient();
    }

    /**
     * Configures the http client.
     */
    protected void configureClient()
    {
        if ( getRemoteHttpCacheAttributes().getMaxConnectionsPerHost() > 0 )
        {
            this.connectionManager.getParams()
                .setMaxTotalConnections(getRemoteHttpCacheAttributes().getMaxConnectionsPerHost());
            this.connectionManager.getParams()
                .setDefaultMaxConnectionsPerHost(getRemoteHttpCacheAttributes().getMaxConnectionsPerHost());
        }

        this.connectionManager.getParams().setSoTimeout( getRemoteHttpCacheAttributes().getSocketTimeoutMillis() );

        String httpVersion = getRemoteHttpCacheAttributes().getHttpVersion();
        if ( httpVersion != null )
        {
            if ( "1.1".equals( httpVersion ) )
            {
                this.httpClient.getParams().setParameter( "http.protocol.version", HttpVersion.HTTP_1_1 );
            }
            else if ( "1.0".equals( httpVersion ) )
            {
                this.httpClient.getParams().setParameter( "http.protocol.version", HttpVersion.HTTP_1_0 );
            }
            else
            {
                log.warn( "Unrecognized value for 'httpVersion': [" + httpVersion + "]" );
            }
        }

        this.connectionManager.getParams()
            .setConnectionTimeout(getRemoteHttpCacheAttributes().getConnectionTimeoutMillis());

        // By default we instruct HttpClient to ignore cookies.
        this.httpClient.getParams().setCookiePolicy( CookiePolicy.IGNORE_COOKIES );
    }

    /**
     * Extracted method that can be overwritten to do additional things to the post before the call
     * is made.
     * <p>
     * @param post the post that is about to get executed.
     * @throws IOException on i/o error
     */
    protected final void doWebserviceCall( HttpMethod post )
        throws IOException
    {
        HttpState httpState = preProcessWebserviceCall( post );
        this.httpClient.executeMethod( null, post, httpState );
        postProcessWebserviceCall( post, httpState );
    }

    /**
     * Called before the executeMethod on the client.
     * <p>
     * @param post http method
     * @return HttpState
     * @throws IOException
     */
    protected abstract HttpState preProcessWebserviceCall( HttpMethod post )
        throws IOException;

    /**
     * Called after the executeMethod on the client.
     * <p>
     * @param post http method
     * @param httpState state
     * @throws IOException
     */
    protected abstract void postProcessWebserviceCall( HttpMethod post, HttpState httpState )
        throws IOException;

    /**
     * @return the remoteHttpCacheAttributes
     */
    protected RemoteHttpCacheAttributes getRemoteHttpCacheAttributes()
    {
        return remoteHttpCacheAttributes;
    }
}
