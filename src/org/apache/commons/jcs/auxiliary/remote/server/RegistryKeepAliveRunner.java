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

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.apache.commons.jcs.auxiliary.remote.RemoteUtils;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class tries to keep the registry alive. If if is able to create a registry, it will also
 * rebind the remote cache server.
 */
public class RegistryKeepAliveRunner
    implements Runnable
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RegistryKeepAliveRunner.class );

    /** The URL of the service to look for. */
    private String namingURL;

    /** The service name. */
    private String serviceName;

    /** the port on which to start the registry */
    private int registryPort;

    /** An optional event logger */
    private ICacheEventLogger cacheEventLogger;

    /** the registry */
    private Registry registry;

    /**
     * @param registryHost - Hostname of the registry
     * @param registryPort - the port on which to start the registry
     * @param serviceName
     */
    public RegistryKeepAliveRunner( String registryHost, int registryPort, String serviceName )
    {
        this.namingURL = RemoteUtils.getNamingURL(registryHost, registryPort, serviceName);
        this.serviceName = serviceName;
        this.registryPort = registryPort;
    }

    /**
     * Tries to lookup the server. If unsuccessful it will rebind the server using the factory
     * rebind method.
     * <p>
     */
    @Override
    public void run()
    {
        checkAndRestoreIfNeeded();
    }

    /**
     * Tries to lookup the server. If unsuccessful it will rebind the server using the factory
     * rebind method.
     */
    protected void checkAndRestoreIfNeeded()
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "looking up server " + namingURL );
        }
        try
        {
            Object obj = Naming.lookup( namingURL );

            // Successful connection to the remote server.
            String message = "RMI registry looks fine.  Found [" + obj + "] in registry [" + namingURL + "]";
            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logApplicationEvent( "RegistryKeepAliveRunner", "Naming.lookup", message );
            }
            if ( log.isDebugEnabled() )
            {
                log.debug( message );
            }
        }
        catch ( Exception ex )
        {
            // Failed to connect to the remote server.
            String message = "Problem finding server at [" + namingURL
                + "].  Will attempt to start registry and rebind.";
            log.error( message, ex );
            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logError( "RegistryKeepAliveRunner", "Naming.lookup", message + ":" + ex.getMessage() );
            }
            createAndRegister( serviceName );
        }
    }

    /**
     * Creates the registry and registers the server.
     * <p>
     * @param serviceName the service name
     */
    protected void createAndRegister( String serviceName )
    {
        createReqistry( serviceName );
        registerServer( serviceName );
    }

    /**
     * Try to create the registry. Log errors
     * <p>
     * @param serviceName the service name
     */
    protected void createReqistry( String serviceName )
    {
        // TODO: Refactor method signature. This is ugly but required to keep the binary API compatibility
        this.registry = RemoteUtils.createRegistry(registryPort);

        if ( cacheEventLogger != null )
        {
            if (this.registry != null)
            {
                cacheEventLogger.logApplicationEvent( "RegistryKeepAliveRunner", "createRegistry",
                        "Successfully created registry [" + serviceName + "]." );
            }
            else
            {
                cacheEventLogger.logError( "RegistryKeepAliveRunner", "createRegistry",
                        "Could not start registry [" + serviceName + "]." );
            }
        }
    }

    /**
     * Try to rebind the server.
     * <p>
     * @param serviceName the service name
     */
    protected void registerServer( String serviceName )
    {
        try
        {
            // try to rebind anyway
            Remote server = RemoteCacheServerFactory.getRemoteCacheServer();

            if ( server == null )
            {
                throw new RemoteException( "Cannot register the server until it is created." );
            }

            this.registry.rebind( serviceName, server );
            String message = "Successfully rebound server to registry [" + serviceName + "].";
            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logApplicationEvent( "RegistryKeepAliveRunner", "registerServer", message );
            }
            if ( log.isInfoEnabled() )
            {
                log.info( message );
            }
        }
        catch ( RemoteException e )
        {
            String message = "Could not rebind server to registry [" + serviceName + "].";
            log.error( message, e );
            if ( cacheEventLogger != null )
            {
                cacheEventLogger.logError( "RegistryKeepAliveRunner", "registerServer", message + ":"
                    + e.getMessage() );
            }
        }
    }

    /**
     * Allows it to be injected.
     * <p>
     * @param cacheEventLogger
     */
    public void setCacheEventLogger( ICacheEventLogger cacheEventLogger )
    {
        this.cacheEventLogger = cacheEventLogger;
    }
}
