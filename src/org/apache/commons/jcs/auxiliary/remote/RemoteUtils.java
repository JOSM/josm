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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides some basic utilities for doing things such as starting
 * the registry properly.
 */
public class RemoteUtils
{
    /** The logger. */
    private static final Log log = LogFactory.getLog(RemoteUtils.class);

    /** No instances please. */
    private RemoteUtils()
    {
        super();
    }

    /**
     * Creates and exports a registry on the specified port of the local host.
     * <p>
     *
     * @param port
     * @return the registry
     */
    public static Registry createRegistry(int port)
    {
        Registry registry = null;

        // if ( log.isInfoEnabled() )
        // {
        // log.info( "createRegistry> Setting security manager" );
        // }
        //
        // System.setSecurityManager( new RMISecurityManager() );

        if (port < 1024)
        {
            if (log.isWarnEnabled())
            {
                log.warn("createRegistry> Port chosen was less than 1024, will use default [" + Registry.REGISTRY_PORT + "] instead.");
            }
            port = Registry.REGISTRY_PORT;
        }

        try
        {
            registry = LocateRegistry.createRegistry(port);
            log.info("createRegistry> Created the registry on port " + port);
        }
        catch (RemoteException e)
        {
            log.warn("createRegistry> Problem creating registry. It may already be started. " + e.getMessage());
        }
        catch (Throwable t)
        {
            log.error("createRegistry> Problem creating registry.", t);
        }

        if (registry == null)
        {
            try
            {
                registry = LocateRegistry.getRegistry(port);
            }
            catch (RemoteException e)
            {
                log.error("createRegistry> Problem getting a registry reference.", e);
            }
        }

        return registry;
    }

    /**
     * Loads properties for the named props file.
     * First tries class path, then file, then URL
     * <p>
     *
     * @param propFile
     * @return The properties object for the file
     * @throws IOException
     */
    public static Properties loadProps(String propFile)
            throws IOException
    {
        InputStream is = RemoteUtils.class.getResourceAsStream(propFile);

        if (null == is) // not found in class path
        {
            // Try root of class path
            if (propFile != null && !propFile.startsWith("/"))
            {
                is = RemoteUtils.class.getResourceAsStream("/" + propFile);
            }
        }
        
        if (null == is) // not found in class path
        {
            if (new File(propFile).exists())
            {
                // file found
                is = new FileInputStream(propFile);
            }
            else
            {
                // try URL
                is = new URL(propFile).openStream();
            }
        }

        Properties props = new Properties();
        try
        {
            props.load(is);
            if (log.isDebugEnabled())
            {
                log.debug("props.size=" + props.size());
            }

            if (log.isDebugEnabled())
            {
                Enumeration<Object> en = props.keys();
                StringBuilder buf = new StringBuilder();
                while (en.hasMoreElements())
                {
                    String key = (String) en.nextElement();
                    buf.append("\n" + key + " = " + props.getProperty(key));
                }
                log.debug(buf.toString());
            }

        }
        catch (Exception ex)
        {
            log.error("Error loading remote properties, for file name [" + propFile + "]", ex);
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        return props;
    }

    /**
     * Configure a custom socket factory to set the timeout value. This sets the
     * global socket factory. It's used only if a custom factory is not
     * configured for the specific object.
     * <p>
     *
     * @param timeoutMillis
     */
    public static void configureGlobalCustomSocketFactory(final int timeoutMillis)
    {
        try
        {
            // Don't set a socket factory if the setting is -1
            if (timeoutMillis > 0)
            {
                if (log.isInfoEnabled())
                {
                    log.info("RmiSocketFactoryTimeoutMillis [" + timeoutMillis + "]. "
                            + " Configuring a custom socket factory.");
                }

                // use this socket factory to add a timeout.
                RMISocketFactory.setSocketFactory(new RMISocketFactory()
                {
                    @Override
                    public Socket createSocket(String host, int port)
                            throws IOException
                    {
                        Socket socket = new Socket();
                        socket.setSoTimeout(timeoutMillis);
                        socket.setSoLinger(false, 0);
                        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
                        return socket;
                    }

                    @Override
                    public ServerSocket createServerSocket(int port)
                            throws IOException
                    {
                        return new ServerSocket(port);
                    }
                });
            }
        }
        catch (IOException e)
        {
            // Only try to do it once. Otherwise we
            // Generate errors for each region on construction.
            RMISocketFactory factoryInUse = RMISocketFactory.getSocketFactory();
            if (factoryInUse != null && !factoryInUse.getClass().getName().startsWith("org.apache.commons.jcs"))
            {
                log.info("Could not create new custom socket factory. " + e.getMessage() + " Factory in use = "
                        + RMISocketFactory.getSocketFactory());
            }
        }
    }

    /**
     * Get the naming url used for RMI registration
     *
     * @param location
     *            the remote location
     * @param serviceName
     *            the remote service name
     * @return the URL for RMI lookup
     */
    public static String getNamingURL(final RemoteLocation location, final String serviceName)
    {
        return getNamingURL(location.getHost(), location.getPort(), serviceName);
    }

    /**
     * Get the naming url used for RMI registration
     *
     * @param registryHost
     *            the remote host
     * @param registryPort
     *            the remote port
     * @param serviceName
     *            the remote service name
     * @return the URL for RMI lookup
     */
    public static String getNamingURL(final String registryHost, final int registryPort, final String serviceName)
    {
        if (registryHost.contains(":"))
        { // TODO improve this check? See also JCS-133
            return "//[" + registryHost.replaceFirst("%", "%25") + "]:" + registryPort + "/" + serviceName;
        }
        final String registryURL = "//" + registryHost + ":" + registryPort + "/" + serviceName;
        return registryURL;
    }
}
