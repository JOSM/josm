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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * This class simply configures the http multithreaded connection manager.
 * <p>
 * This is abstract because it can do anything. Child classes can overwrite whatever they want.
 */
public abstract class AbstractHttpClient
{
    /** The client */
    private HttpClient httpClient;

    /** The protocol version */
    private HttpVersion httpVersion;

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

        String httpVersion = getRemoteHttpCacheAttributes().getHttpVersion();
        if ( "1.1".equals( httpVersion ) )
        {
            this.httpVersion = HttpVersion.HTTP_1_1;
        }
        else if ( "1.0".equals( httpVersion ) )
        {
            this.httpVersion = HttpVersion.HTTP_1_0;
        }
        else
        {
            log.warn( "Unrecognized value for 'httpVersion': [" + httpVersion + "], defaulting to 1.1" );
            this.httpVersion = HttpVersion.HTTP_1_1;
        }

        HttpClientBuilder builder = HttpClientBuilder.create();
        configureClient(builder);
        this.httpClient = builder.build();
    }

    /**
     * Configures the http client.
     *
     * @param builder client builder to configure
     */
    protected void configureClient(HttpClientBuilder builder)
    {
        if ( getRemoteHttpCacheAttributes().getMaxConnectionsPerHost() > 0 )
        {
            builder.setMaxConnTotal(getRemoteHttpCacheAttributes().getMaxConnectionsPerHost());
            builder.setMaxConnPerRoute(getRemoteHttpCacheAttributes().getMaxConnectionsPerHost());
        }

        builder.setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(getRemoteHttpCacheAttributes().getConnectionTimeoutMillis())
                .setSocketTimeout(getRemoteHttpCacheAttributes().getSocketTimeoutMillis())
                // By default we instruct HttpClient to ignore cookies.
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build());
    }

    /**
     * Execute the web service call
     * <p>
     * @param builder builder for the post request
     *
     * @return the call response
     *
     * @throws IOException on i/o error
     */
    protected final HttpResponse doWebserviceCall( RequestBuilder builder )
        throws IOException
    {
        preProcessWebserviceCall( builder.setVersion(httpVersion) );
        HttpUriRequest request = builder.build();
        HttpResponse httpResponse = this.httpClient.execute( request );
        postProcessWebserviceCall( request, httpResponse );

        return httpResponse;
    }

    /**
     * Called before the execute call on the client.
     * <p>
     * @param requestBuilder http method request builder
     *
     * @throws IOException
     */
    protected abstract void preProcessWebserviceCall( RequestBuilder requestBuilder )
        throws IOException;

    /**
     * Called after the execute call on the client.
     * <p>
     * @param request http request
     * @param httpState result of execution
     *
     * @throws IOException
     */
    protected abstract void postProcessWebserviceCall( HttpUriRequest request, HttpResponse httpState )
        throws IOException;

    /**
     * @return the remoteHttpCacheAttributes
     */
    protected RemoteHttpCacheAttributes getRemoteHttpCacheAttributes()
    {
        return remoteHttpCacheAttributes;
    }
}
