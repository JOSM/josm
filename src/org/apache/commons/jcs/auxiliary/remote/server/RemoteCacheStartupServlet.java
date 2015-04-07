package org.apache.commons.jcs.auxiliary.remote.server;

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

import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;
import org.apache.commons.jcs.utils.net.HostNameUtil;
import org.apache.commons.jcs.utils.props.PropertyLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * This servlet can be used to startup the JCS remote cache. It is easy to deploy the remote server
 * in a tomcat base. This give you an easy way to monitor its activity.
 * <p>
 * <code>
 *  <servlet>
        <servlet-name>JCSRemoteCacheStartupServlet</servlet-name>
        <servlet-class>
             org.apache.commons.jcs.auxiliary.remote.server.RemoteCacheStartupServlet
        </servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>


    <servlet-mapping>
        <servlet-name>JCSRemoteCacheStartupServlet</servlet-name>
        <url-pattern>/jcs</url-pattern>
    </servlet-mapping>
 * </code>
 * @author Aaron Smuts
 */
public class RemoteCacheStartupServlet
    extends HttpServlet
{
    /** Don't change */
    private static final long serialVersionUID = 1L;

    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteCacheStartupServlet.class );

    /** The default port to start the registry on.  */
    private static final int DEFAULT_REGISTRY_PORT = 1101;

    /** properties file name */
    private static final String DEFAULT_PROPS_FILE_NAME = "cache";

    /** properties file Suffix */
    private static final String DEFAULT_PROPS_FILE_SUFFIX = "ccf";

    /** properties file name, must set prior to calling get instance */
    private final String propsFileName = DEFAULT_PROPS_FILE_NAME;

    /** properties file name, must set prior to calling get instance */
    private final String fullPropsFileName = DEFAULT_PROPS_FILE_NAME + "." + DEFAULT_PROPS_FILE_SUFFIX;

    /**
     * Starts the registry and then tries to bind to it.
     * <p>
     * Gets the port from a props file. Uses the local host name for the registry host. Tries to
     * start the registry, ignoring failure. Starts the server.
     * <p>
     * @throws ServletException
     */
    @Override
    public void init()
        throws ServletException
    {
        super.init();
        // TODO load from props file or get as init param or get from jndi, or
        // all three
        int registryPort = DEFAULT_REGISTRY_PORT;

        Properties props = PropertyLoader.loadProperties( propsFileName );
        if ( props != null )
        {
            String portS = props.getProperty( "registry.port", String.valueOf( DEFAULT_REGISTRY_PORT ) );

            try
            {
                registryPort = Integer.parseInt( portS );
            }
            catch ( NumberFormatException e )
            {
                log.error( "Problem converting port to an int.", e );
            }
        }

        // we will always use the local machine for the registry
        String registryHost;
        try
        {
            registryHost = HostNameUtil.getLocalHostAddress();

            if ( log.isDebugEnabled() )
            {
                log.debug( "registryHost = [" + registryHost + "]" );
            }

            if ( "localhost".equals( registryHost ) || "127.0.0.1".equals( registryHost ) )
            {
                log.warn( "The local address [" + registryHost
                    + "] is INVALID.  Other machines must be able to use the address to reach this server." );
            }

            try
            {
                RemoteCacheServerFactory.startup( registryHost, registryPort, "/" + fullPropsFileName );
                if ( log.isInfoEnabled() )
                {
                    log.info( "Remote JCS Server started with properties from " + fullPropsFileName );
                }
            }
            catch ( IOException e )
            {
                log.error( "Problem starting remote cache server.", e );
            }
        }
        catch ( UnknownHostException e )
        {
            log.error( "Could not get local address to use for the registry!", e );
        }
    }

    /**
     * It just dumps the stats.
     * <p>
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        String stats = "";

        try
        {
            stats = CompositeCacheManager.getInstance().getStats();
        }
        catch (CacheException e)
        {
            throw new ServletException(e);
        }

        if ( log.isInfoEnabled() )
        {
            log.info( stats );
        }

        try
        {
            String characterEncoding = response.getCharacterEncoding();
            if (characterEncoding == null)
            {
                characterEncoding = "UTF-8";
                response.setCharacterEncoding(characterEncoding);
            }
            OutputStream os = response.getOutputStream();
            os.write( stats.getBytes(characterEncoding) );
            os.close();
        }
        catch ( IOException e )
        {
            log.error( "Problem writing response.", e );
        }
    }

    /**
     * shuts the cache down.
     */
    @Override
    public void destroy()
    {
        super.destroy();

        log.info( "Shutting down remote cache " );

        try
        {
            CompositeCacheManager.getInstance().shutDown();
        }
        catch (CacheException e)
        {
            log.error("Could not retrieve cache manager instance", e);
        }
    }
}
