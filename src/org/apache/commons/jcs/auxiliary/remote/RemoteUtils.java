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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides some basic utilities for doing things such as starting the registry properly.
 */
public class RemoteUtils
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( RemoteUtils.class );

    /** No instances please. */
    private RemoteUtils()
    {
        super();
    }

    /**
     * Creates and exports a registry on the specified port of the local host.
     * <p>
     * @param port
     * @return the registry
     */
    public static Registry createRegistry( int port )
    {
    	Registry registry = null;

//        if ( log.isInfoEnabled() )
//        {
//            log.info( "createRegistry> Setting security manager" );
//        }
//
//        System.setSecurityManager( new RMISecurityManager() );

        if ( port < 1024 )
        {
            if ( log.isWarnEnabled() )
            {
                log.warn( "createRegistry> Port chosen was less than 1024, will use default [" + Registry.REGISTRY_PORT + "] instead." );
            }
            port = Registry.REGISTRY_PORT;
        }

        try
        {
            registry = LocateRegistry.createRegistry( port );
            log.info("createRegistry> Created the registry on port " + port);
        }
        catch ( RemoteException e )
        {
            log.warn( "createRegistry> Problem creating registry. It may already be started. " + e.getMessage() );
        }
        catch ( Throwable t )
        {
            log.error( "createRegistry> Problem creating registry.", t );
        }

        if (registry == null)
        {
        	try
        	{
            	registry = LocateRegistry.getRegistry( port );
			}
        	catch (RemoteException e)
        	{
                log.error( "createRegistry> Problem getting a registry reference.", e );
			}
        }

        return registry;
    }

    /**
     * Loads properties for the named props file.
     * <p>
     * @param propFile
     * @return The properties object for the file
     * @throws IOException
     */
    public static Properties loadProps( String propFile )
        throws IOException
    {
        InputStream is = RemoteUtils.class.getResourceAsStream( propFile );
        Properties props = new Properties();
        try
        {
            props.load( is );
            if ( log.isDebugEnabled() )
            {
                log.debug( "props.size=" + props.size() );
            }

            if ( log.isDebugEnabled() )
            {
                Enumeration<Object> en = props.keys();
                StringBuilder buf = new StringBuilder();
                while ( en.hasMoreElements() )
                {
                    String key = (String) en.nextElement();
                    buf.append( "\n" + key + " = " + props.getProperty( key ) );
                }
                log.debug( buf.toString() );
            }

        }
        catch ( Exception ex )
        {
            log.error( "Error loading remote properties, for file name [" + propFile + "]", ex );
        }
        finally
        {
            if ( is != null )
            {
                is.close();
            }
        }
        return props;
    }

    /**
     * Configure a custom socket factory to set the timeout value. This sets the global socket
     * factory. It's used only if a custom factory is not configured for the specific object.
     * <p>
     * @param timeoutMillis
     */
    public static void configureGlobalCustomSocketFactory( final int timeoutMillis )
    {
        try
        {
            // Don't set a socket factory if the setting is -1
            if ( timeoutMillis > 0 )
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "RmiSocketFactoryTimeoutMillis [" + timeoutMillis + "]. "
                        + " Configuring a custom socket factory." );
                }

                // use this socket factory to add a timeout.
                RMISocketFactory.setSocketFactory( new RMISocketFactory()
                {
                    @Override
                    public Socket createSocket( String host, int port )
                        throws IOException
                    {
                        Socket socket = new Socket();
                        socket.setSoTimeout( timeoutMillis );
                        socket.setSoLinger( false, 0 );
                        socket.connect( new InetSocketAddress( host, port ), timeoutMillis );
                        return socket;
                    }

                    @Override
                    public ServerSocket createServerSocket( int port )
                        throws IOException
                    {
                        return new ServerSocket( port );
                    }
                } );
            }
        }
        catch ( Exception e )
        {
            // Only try to do it once. Otherwise we
            // Generate errors for each region on construction.
            RMISocketFactory factoryInUse = RMISocketFactory.getSocketFactory();
            if ( factoryInUse != null && !factoryInUse.getClass().getName().startsWith( "org.apache.commons.jcs" ) )
            {
                log.info( "Could not create new custom socket factory. " + e.getMessage() + " Factory in use = "
                    + RMISocketFactory.getSocketFactory() );
            }
        }
    }

    /**
     * Get the naming url used for RMI registration
     *
     * @param registryHost
     * @param registryPort
     * @param serviceName
     * @return
     */
    public static String getNamingURL(final String registryHost, final int registryPort, final String serviceName)
    {
        if (registryHost.contains(":")) { // TODO improve this check? See also JCS-133
            return "//[" + registryHost.replaceFirst("%", "%25") + "]:" + registryPort + "/" + serviceName;
        }
        final String registryURL = "//" + registryHost + ":" + registryPort + "/" + serviceName;
        return registryURL;
    }

    /** Pattern for parsing server:port */
    private static final Pattern SERVER_COLON_PORT = Pattern.compile("(\\S+)\\s*:\\s*(\\d+)");

    /**
     * Parse remote server and port from the string representation server:port and store them in
     * the RemoteCacheAttributes
     *
     * @param server the input string
     * @param rca the target attribute object
     */
    public static void parseServerAndPort(final String server, final RemoteCacheAttributes rca)
    {
        Matcher match = SERVER_COLON_PORT.matcher(server);

        if (match.find() && match.groupCount() == 2)
        {
            rca.setRemoteHost( match.group(1) );
            rca.setRemotePort( Integer.parseInt( match.group(2) ) );
        }
        else
        {
            log.error("Invalid server descriptor: " + server);
        }
    }
}
