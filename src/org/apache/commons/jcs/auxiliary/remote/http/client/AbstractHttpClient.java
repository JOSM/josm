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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * This class simply configures the http multithreaded connection manager.
 * <p>
 * This is abstract because it can't do anything. Child classes can overwrite whatever they want.
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
        setRemoteHttpCacheAttributes( remoteHttpCacheAttributes );
        setConnectionManager( new MultiThreadedHttpConnectionManager() );

        // THIS IS NOT THREAD SAFE:
        // setHttpClient( new HttpClient() );
        // THIS IS:
        setHttpClient( new HttpClient( getConnectionManager() ) );

        configureClient();
    }

    /**
     * Configures the http client.
     */
    public void configureClient()
    {
        if ( getRemoteHttpCacheAttributes().getMaxConnectionsPerHost() > 0 )
        {
            getConnectionManager().getParams().setMaxTotalConnections(
                                                                       getRemoteHttpCacheAttributes()
                                                                           .getMaxConnectionsPerHost() );
        }

        getConnectionManager().getParams().setSoTimeout( getRemoteHttpCacheAttributes().getSocketTimeoutMillis() );

        String httpVersion = getRemoteHttpCacheAttributes().getHttpVersion();
        if ( httpVersion != null )
        {
            if ( "1.1".equals( httpVersion ) )
            {
                getHttpClient().getParams().setParameter( "http.protocol.version", HttpVersion.HTTP_1_1 );
            }
            else if ( "1.0".equals( httpVersion ) )
            {
                getHttpClient().getParams().setParameter( "http.protocol.version", HttpVersion.HTTP_1_0 );
            }
            else
            {
                log.warn( "Unrecognized value for 'httpVersion': [" + httpVersion + "]" );
            }
        }

        getConnectionManager().getParams().setConnectionTimeout(
                                                                 getRemoteHttpCacheAttributes()
                                                                     .getConnectionTimeoutMillis() );

        // By default we instruct HttpClient to ignore cookies.
        String cookiePolicy = CookiePolicy.IGNORE_COOKIES;
        getHttpClient().getParams().setCookiePolicy( cookiePolicy );
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
        getHttpClient().executeMethod( null, post, httpState );
        postProcessWebserviceCall( post, httpState );
    }

    /**
     * Called before the executeMethod on the client.
     * <p>
     * @param post http method
     * @return HttpState
     * @throws IOException
     */
    public abstract HttpState preProcessWebserviceCall( HttpMethod post )
        throws IOException;

    /**
     * Called after the executeMethod on the client.
     * <p>
     * @param post http method
     * @param httpState state
     * @throws IOException
     */
    public abstract void postProcessWebserviceCall( HttpMethod post, HttpState httpState )
        throws IOException;

    /**
     * @return Returns the httpClient.
     */
    private HttpClient getHttpClient()
    {
        return httpClient;
    }

    /**
     * @param httpClient The httpClient to set.
     */
    private void setHttpClient( HttpClient httpClient )
    {
        this.httpClient = httpClient;
    }

    /**
     * @return Returns the connectionManager.
     */
    public MultiThreadedHttpConnectionManager getConnectionManager()
    {
        return connectionManager;
    }

    /**
     * @param connectionManager The connectionManager to set.
     */
    public void setConnectionManager( MultiThreadedHttpConnectionManager connectionManager )
    {
        this.connectionManager = connectionManager;
    }

    /**
     * @param remoteHttpCacheAttributes the remoteHttpCacheAttributes to set
     */
    public void setRemoteHttpCacheAttributes( RemoteHttpCacheAttributes remoteHttpCacheAttributes )
    {
        this.remoteHttpCacheAttributes = remoteHttpCacheAttributes;
    }

    /**
     * @return the remoteHttpCacheAttributes
     */
    public RemoteHttpCacheAttributes getRemoteHttpCacheAttributes()
    {
        return remoteHttpCacheAttributes;
    }
}
