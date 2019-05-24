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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheConfigurator;
import org.apache.commons.jcs.auxiliary.remote.http.behavior.IRemoteHttpCacheConstants;
import org.apache.commons.jcs.auxiliary.remote.value.RemoteCacheRequest;
import org.apache.commons.jcs.auxiliary.remote.value.RemoteCacheResponse;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.io.ObjectInputStreamClassLoaderAware;
import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This servlet simply reads and writes objects. The requests are packaged in a general wrapper. The
 * processor works on the wrapper object and returns a response wrapper.
 */
public class RemoteHttpCacheServlet
    extends HttpServlet
{
    /** Don't change. */
    private static final long serialVersionUID = 8752849397531933346L;

    /** The Logger. */
    private static final Log log = LogFactory.getLog( RemoteHttpCacheServlet.class );

    /** The cache manager */
    private static CompositeCacheManager cacheMgr;

    /** The service that does the work. */
    private static ICacheServiceNonLocal<Serializable, Serializable> remoteCacheService;

    /** This needs to be standard, since the other side is standard */
    private final StandardSerializer serializer = new StandardSerializer();

    /** Number of service calls. */
    private int serviceCalls = 0;

    /** The interval at which we will log the count. */
    private final int logInterval = 100;

    /**
     * Initializes the cache.
     * <p>
     * This provides an easy extension point. Simply extend this servlet and override the init
     * method to change the way the properties are loaded.
     * @param config
     * @throws ServletException
     */
    @Override
    public void init( ServletConfig config )
        throws ServletException
    {
        try
        {
            cacheMgr = CompositeCacheManager.getInstance();
        }
        catch (CacheException e)
        {
            throw new ServletException(e);
        }

        remoteCacheService = createRemoteHttpCacheService( cacheMgr );

        super.init( config );
    }

    /**
     * Read the request, call the processor, write the response.
     * <p>
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        incrementServiceCallCount();
        if ( log.isDebugEnabled() )
        {
            log.debug( "Servicing a request. " + request );
        }

        RemoteCacheRequest<Serializable, Serializable> remoteRequest = readRequest( request );
        RemoteCacheResponse<Object> cacheResponse = processRequest( remoteRequest );

        writeResponse( response, cacheResponse );
    }

    /**
     * Read the request from the input stream.
     * <p>
     * @param request
     * @return RemoteHttpCacheRequest
     */
    protected RemoteCacheRequest<Serializable, Serializable> readRequest( HttpServletRequest request )
    {
        RemoteCacheRequest<Serializable, Serializable> remoteRequest = null;
        try
        {
            InputStream inputStream = request.getInputStream();
            if ( log.isDebugEnabled() )
            {
                log.debug( "After getting input stream and before reading it" );
            }

            remoteRequest = readRequestFromStream( inputStream );
        }
        catch ( Exception e )
        {
            log.error( "Could not get a RemoteHttpCacheRequest object from the input stream.", e );
        }
        return remoteRequest;
    }

    /**
     * Reads the response from the stream and then closes it.
     * <p>
     * @param inputStream
     * @return RemoteHttpCacheRequest
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected RemoteCacheRequest<Serializable, Serializable> readRequestFromStream( InputStream inputStream )
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream ois = new ObjectInputStreamClassLoaderAware( inputStream, null );

        @SuppressWarnings("unchecked") // Need to cast from Object
        RemoteCacheRequest<Serializable, Serializable> remoteRequest
            = (RemoteCacheRequest<Serializable, Serializable>) ois.readObject();
        ois.close();
        return remoteRequest;
    }

    /**
     * Write the response to the output stream.
     * <p>
     * @param response
     * @param cacheResponse
     */
    protected void writeResponse( HttpServletResponse response, RemoteCacheResponse<Object> cacheResponse )
    {
        try
        {
            response.setContentType( "application/octet-stream" );

            byte[] responseAsByteAray = serializer.serialize( cacheResponse );
            response.setContentLength( responseAsByteAray.length );

            OutputStream outputStream = response.getOutputStream();
            if ( log.isDebugEnabled() )
            {
                log.debug( "Opened output stream.  Response size: " + responseAsByteAray.length );
            }
            // WRITE
            outputStream.write( responseAsByteAray );
            outputStream.flush();
            outputStream.close();
        }
        catch ( IOException e )
        {
            log.error( "Problem writing response. " + cacheResponse, e );
        }
    }

    /**
     * Processes the request. It will call the appropriate method on the service
     * <p>
     * @param request
     * @return RemoteHttpCacheResponse, never null
     */
    protected RemoteCacheResponse<Object> processRequest( RemoteCacheRequest<Serializable, Serializable> request )
    {
        RemoteCacheResponse<Object> response = new RemoteCacheResponse<>();

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
                        ICacheElement<Serializable, Serializable> element =
                            remoteCacheService.get( request.getCacheName(), request.getKey(), request.getRequesterId() );
                        response.setPayload(element);
                        break;
                    case GET_MULTIPLE:
                        Map<Serializable, ICacheElement<Serializable, Serializable>> elementMap =
                            remoteCacheService.getMultiple( request.getCacheName(), request.getKeySet(), request.getRequesterId() );
                        if ( elementMap != null )
                        {
                            Map<Serializable, ICacheElement<Serializable, Serializable>> map = new HashMap<>();
                            map.putAll(elementMap);
                            response.setPayload(map);
                        }
                        break;
                    case GET_MATCHING:
                        Map<Serializable, ICacheElement<Serializable, Serializable>> elementMapMatching =
                            remoteCacheService.getMatching( request.getCacheName(), request.getPattern(), request.getRequesterId() );
                        if ( elementMapMatching != null )
                        {
                            Map<Serializable, ICacheElement<Serializable, Serializable>> map = new HashMap<>();
                            map.putAll(elementMapMatching);
                            response.setPayload(map);
                        }
                        break;
                    case REMOVE:
                        remoteCacheService.remove( request.getCacheName(), request.getKey(), request.getRequesterId() );
                        break;
                    case REMOVE_ALL:
                        remoteCacheService.removeAll( request.getCacheName(), request.getRequesterId() );
                        break;
                    case UPDATE:
                        remoteCacheService.update( request.getCacheElement(), request.getRequesterId() );
                        break;
                    case ALIVE_CHECK:
                    case DISPOSE:
                        response.setSuccess( true );
                        // DO NOTHING
                        break;
                    case GET_KEYSET:
                        Set<Serializable> keys = remoteCacheService.getKeySet( request.getCacheName() );
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

        return response;
    }

    /**
     * Configures the attributes and the event logger and constructs a service.
     * <p>
     * @param cacheManager
     * @return RemoteHttpCacheService
     */
    protected <K, V> RemoteHttpCacheService<K, V> createRemoteHttpCacheService( ICompositeCacheManager cacheManager )
    {
        Properties props = cacheManager.getConfigurationProperties();
        ICacheEventLogger cacheEventLogger = configureCacheEventLogger( props );
        RemoteHttpCacheServerAttributes attributes = configureRemoteHttpCacheServerAttributes( props );

        RemoteHttpCacheService<K, V> service = new RemoteHttpCacheService<>( cacheManager, attributes, cacheEventLogger );
        if ( log.isInfoEnabled() )
        {
            log.info( "Created new RemoteHttpCacheService " + service );
        }
        return service;
    }

    /**
     * Tries to get the event logger.
     * <p>
     * @param props
     * @return ICacheEventLogger
     */
    protected ICacheEventLogger configureCacheEventLogger( Properties props )
    {
        ICacheEventLogger cacheEventLogger = AuxiliaryCacheConfigurator
            .parseCacheEventLogger( props, IRemoteHttpCacheConstants.HTTP_CACHE_SERVER_PREFIX );

        return cacheEventLogger;
    }

    /**
     * Configure.
     * <p>
     * jcs.remotehttpcache.serverattributes.ATTRIBUTENAME=ATTRIBUTEVALUE
     * <p>
     * @param prop
     * @return RemoteCacheServerAttributesconfigureRemoteCacheServerAttributes
     */
    protected RemoteHttpCacheServerAttributes configureRemoteHttpCacheServerAttributes( Properties prop )
    {
        RemoteHttpCacheServerAttributes rcsa = new RemoteHttpCacheServerAttributes();

        // configure automatically
        PropertySetter.setProperties( rcsa, prop,
                                      IRemoteHttpCacheConstants.HTTP_CACHE_SERVER_ATTRIBUTES_PROPERTY_PREFIX + "." );

        return rcsa;
    }

    /**
     * @param rcs the remoteCacheService to set
     */
    protected void setRemoteCacheService(ICacheServiceNonLocal<Serializable, Serializable> rcs)
    {
        remoteCacheService = rcs;
    }

    /**
     * Log some details.
     */
    private void incrementServiceCallCount()
    {
        // not thread safe, but it doesn't have to be accurate
        serviceCalls++;
        if ( log.isInfoEnabled() )
        {
            if ( serviceCalls % logInterval == 0 )
            {
                log.info( "serviceCalls = " + serviceCalls );
            }
        }
    }

    /** Release the cache manager. */
    @Override
    public void destroy()
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Servlet Destroyed, shutting down JCS." );
        }

        cacheMgr.shutDown();
    }

    /**
     * Get servlet information
     * <p>
     * @return basic info
     */
    @Override
    public String getServletInfo()
    {
        return "RemoteHttpCacheServlet";
    }
}
