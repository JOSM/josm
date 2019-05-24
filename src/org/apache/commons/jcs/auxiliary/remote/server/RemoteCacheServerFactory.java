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

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheConfigurator;
import org.apache.commons.jcs.auxiliary.remote.RemoteUtils;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheConstants;
import org.apache.commons.jcs.engine.behavior.ICacheServiceAdmin;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.config.OptionConverter;
import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.jcs.utils.threadpool.DaemonThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides remote cache services. This creates remote cache servers and can proxy command line
 * requests to a running server.
 */
public class RemoteCacheServerFactory
    implements IRemoteCacheConstants
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteCacheServerFactory.class );

    /** The single instance of the RemoteCacheServer object. */
    private static RemoteCacheServer<?, ?> remoteCacheServer;

    /** The name of the service. */
    private static String serviceName = IRemoteCacheConstants.REMOTE_CACHE_SERVICE_VAL;

    /** Executes the registry keep alive. */
    private static ScheduledExecutorService keepAliveDaemon;

    /** A reference to the registry. */
    private static Registry registry = null;

    /** Constructor for the RemoteCacheServerFactory object. */
    private RemoteCacheServerFactory()
    {
        super();
    }

    /**
     * This will allow you to get stats from the server, etc. Perhaps we should provide methods on
     * the factory to do this instead.
     * <p>
     * A remote cache is either a local cache or a cluster cache.
     * </p>
     * @return Returns the remoteCacheServer.
     */
    @SuppressWarnings("unchecked") // Need cast to specific RemoteCacheServer
    public static <K, V> RemoteCacheServer<K, V> getRemoteCacheServer()
    {
        return (RemoteCacheServer<K, V>)remoteCacheServer;
    }

    // ///////////////////// Startup/shutdown methods. //////////////////
    /**
     * Starts up the remote cache server on this JVM, and binds it to the registry on the given host
     * and port.
     * <p>
     * A remote cache is either a local cache or a cluster cache.
     * <p>
     * @param host
     * @param port
     * @param props
     * @throws IOException
     */
    public static void startup( String host, int port, Properties props)
        throws IOException
    {
        if ( remoteCacheServer != null )
        {
            throw new IllegalArgumentException( "Server already started." );
        }

        synchronized ( RemoteCacheServer.class )
        {
            if ( remoteCacheServer != null )
            {
                return;
            }
            if ( host == null )
            {
                host = "";
            }

            RemoteCacheServerAttributes rcsa = configureRemoteCacheServerAttributes(props);

            // These should come from the file!
            rcsa.setRemoteLocation( host, port );
            if ( log.isInfoEnabled() )
            {
                log.info( "Creating server with these attributes: " + rcsa );
            }

            setServiceName( rcsa.getRemoteServiceName() );

            RMISocketFactory customRMISocketFactory = configureObjectSpecificCustomFactory( props );

            RemoteUtils.configureGlobalCustomSocketFactory( rcsa.getRmiSocketFactoryTimeoutMillis() );

            // CONFIGURE THE EVENT LOGGER
            ICacheEventLogger cacheEventLogger = configureCacheEventLogger( props );

            // CREATE SERVER
            if ( customRMISocketFactory != null )
            {
                remoteCacheServer = new RemoteCacheServer<>( rcsa, props, customRMISocketFactory );
            }
            else
            {
                remoteCacheServer = new RemoteCacheServer<>( rcsa, props );
            }

            remoteCacheServer.setCacheEventLogger( cacheEventLogger );

            // START THE REGISTRY
        	registry = RemoteUtils.createRegistry(port);

            // REGISTER THE SERVER
            registerServer( serviceName, remoteCacheServer );

            // KEEP THE REGISTRY ALIVE
            if ( rcsa.isUseRegistryKeepAlive() )
            {
                if ( keepAliveDaemon == null )
                {
                    keepAliveDaemon = Executors.newScheduledThreadPool(1,
                            new DaemonThreadFactory("JCS-RemoteCacheServerFactory-"));
                }
                RegistryKeepAliveRunner runner = new RegistryKeepAliveRunner( host, port, serviceName );
                runner.setCacheEventLogger( cacheEventLogger );
                keepAliveDaemon.scheduleAtFixedRate(runner, 0, rcsa.getRegistryKeepAliveDelayMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Tries to get the event logger by new and old config styles.
     * <p>
     * @param props
     * @return ICacheEventLogger
     */
    protected static ICacheEventLogger configureCacheEventLogger( Properties props )
    {
        ICacheEventLogger cacheEventLogger = AuxiliaryCacheConfigurator
            .parseCacheEventLogger( props, IRemoteCacheConstants.CACHE_SERVER_PREFIX );

        // try the old way
        if ( cacheEventLogger == null )
        {
            cacheEventLogger = AuxiliaryCacheConfigurator.parseCacheEventLogger( props,
                                                                                 IRemoteCacheConstants.PROPERTY_PREFIX );
        }
        return cacheEventLogger;
    }

    /**
     * This configures an object specific custom factory. This will be configured for just this
     * object in the registry. This can be null.
     * <p>
     * @param props
     * @return RMISocketFactory
     */
    protected static RMISocketFactory configureObjectSpecificCustomFactory( Properties props )
    {
        RMISocketFactory customRMISocketFactory =
            OptionConverter.instantiateByKey( props, CUSTOM_RMI_SOCKET_FACTORY_PROPERTY_PREFIX, null );

        if ( customRMISocketFactory != null )
        {
            PropertySetter.setProperties( customRMISocketFactory, props, CUSTOM_RMI_SOCKET_FACTORY_PROPERTY_PREFIX
                + "." );
            if ( log.isInfoEnabled() )
            {
                log.info( "Will use server specific custom socket factory. " + customRMISocketFactory );
            }
        }
        else
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "No server specific custom socket factory defined." );
            }
        }
        return customRMISocketFactory;
    }

    /**
     * Registers the server with the registry. I broke this off because we might want to have code
     * that will restart a dead registry. It will need to rebind the server.
     * <p>
     * @param serviceName the name of the service
     * @param server the server object to bind
     * @throws RemoteException
     */
    protected static void registerServer(String serviceName, Remote server )
        throws RemoteException
    {
        if ( server == null )
        {
            throw new RemoteException( "Cannot register the server until it is created." );
        }

        if ( registry == null )
        {
            throw new RemoteException( "Cannot register the server: Registry is null." );
        }

        if ( log.isInfoEnabled() )
        {
            log.info( "Binding server to " + serviceName );
        }

        registry.rebind( serviceName, server );
    }

    /**
     * Configure.
     * <p>
     * jcs.remotecache.serverattributes.ATTRIBUTENAME=ATTRIBUTEVALUE
     * <p>
     * @param prop
     * @return RemoteCacheServerAttributesconfigureRemoteCacheServerAttributes
     */
    protected static RemoteCacheServerAttributes configureRemoteCacheServerAttributes( Properties prop )
    {
        RemoteCacheServerAttributes rcsa = new RemoteCacheServerAttributes();

        // configure automatically
        PropertySetter.setProperties( rcsa, prop, CACHE_SERVER_ATTRIBUTES_PROPERTY_PREFIX + "." );

        configureManuallyIfValuesArePresent( prop, rcsa );

        return rcsa;
    }

    /**
     * This looks for the old config values.
     * <p>
     * @param prop
     * @param rcsa
     */
    private static void configureManuallyIfValuesArePresent( Properties prop, RemoteCacheServerAttributes rcsa )
    {
        // DEPRECATED CONFIG
        String servicePortStr = prop.getProperty( REMOTE_CACHE_SERVICE_PORT );
        if ( servicePortStr != null )
        {
            try
            {
                int servicePort = Integer.parseInt( servicePortStr );
                rcsa.setServicePort( servicePort );
                log.debug( "Remote cache service uses port number " + servicePort + "." );
            }
            catch ( NumberFormatException ignore )
            {
                log.debug( "Remote cache service port property " + REMOTE_CACHE_SERVICE_PORT
                    + " not specified.  An anonymous port will be used." );
            }
        }

        String socketTimeoutMillisStr = prop.getProperty( SOCKET_TIMEOUT_MILLIS );
        if ( socketTimeoutMillisStr != null )
        {
            try
            {
                int rmiSocketFactoryTimeoutMillis = Integer.parseInt( socketTimeoutMillisStr );
                rcsa.setRmiSocketFactoryTimeoutMillis( rmiSocketFactoryTimeoutMillis );
                log.debug( "Remote cache socket timeout " + rmiSocketFactoryTimeoutMillis + "ms." );
            }
            catch ( NumberFormatException ignore )
            {
                log.debug( "Remote cache socket timeout property " + SOCKET_TIMEOUT_MILLIS
                    + " not specified.  The default will be used." );
            }
        }

        String lccStr = prop.getProperty( REMOTE_LOCAL_CLUSTER_CONSISTENCY );
        if ( lccStr != null )
        {
            boolean lcc = Boolean.parseBoolean( lccStr );
            rcsa.setLocalClusterConsistency( lcc );
        }

        String acgStr = prop.getProperty( REMOTE_ALLOW_CLUSTER_GET );
        if ( acgStr != null )
        {
            boolean acg = Boolean.parseBoolean( lccStr );
            rcsa.setAllowClusterGet( acg );
        }

        // Register the RemoteCacheServer remote object in the registry.
        rcsa.setRemoteServiceName( prop.getProperty( REMOTE_CACHE_SERVICE_NAME, REMOTE_CACHE_SERVICE_VAL ).trim() );
    }

    /**
     * Unbinds the remote server.
     * <p>
     * @param host
     * @param port
     * @throws IOException
     */
    static void shutdownImpl( String host, int port )
        throws IOException
    {
        synchronized ( RemoteCacheServer.class )
        {
            if ( remoteCacheServer == null )
            {
                return;
            }
            log.info( "Unbinding host=" + host + ", port=" + port + ", serviceName=" + getServiceName() );
            try
            {
                Naming.unbind( RemoteUtils.getNamingURL(host, port, getServiceName()) );
            }
            catch ( MalformedURLException ex )
            {
                // impossible case.
                throw new IllegalArgumentException( ex.getMessage() + "; host=" + host + ", port=" + port
                    + ", serviceName=" + getServiceName() );
            }
            catch ( NotBoundException ex )
            {
                // ignore.
            }
            remoteCacheServer.release();
            remoteCacheServer = null;

            // Shut down keepalive scheduler
            if ( keepAliveDaemon != null )
            {
                keepAliveDaemon.shutdownNow();
                keepAliveDaemon = null;
            }

            // Try to release registry
            if (registry != null)
            {
            	UnicastRemoteObject.unexportObject(registry, true);
            	registry = null;
            }
        }
    }

    /**
     * Creates an local RMI registry on the default port, starts up the remote cache server, and
     * binds it to the registry.
     * <p>
     * A remote cache is either a local cache or a cluster cache.
     * <p>
     * @param args The command line arguments
     * @throws Exception
     */
    public static void main( String[] args )
        throws Exception
    {
        Properties prop = args.length > 0 ? RemoteUtils.loadProps( args[args.length - 1] ) : new Properties();

        int port;
        try
        {
            port = Integer.parseInt( prop.getProperty( "registry.port" ) );
        }
        catch ( NumberFormatException ex )
        {
            port = Registry.REGISTRY_PORT;
        }

        // shutdown
        if ( args.length > 0 && args[0].toLowerCase().indexOf( "-shutdown" ) != -1 )
        {
            try
            {
                ICacheServiceAdmin admin = lookupCacheServiceAdmin(prop, port);
                admin.shutdown();
            }
            catch ( Exception ex )
            {
                log.error( "Problem calling shutdown.", ex );
            }
            log.debug( "done." );
            System.exit( 0 );
        }

        // STATS
        if ( args.length > 0 && args[0].toLowerCase().indexOf( "-stats" ) != -1 )
        {
            log.debug( "getting cache stats" );

            try
            {
                ICacheServiceAdmin admin = lookupCacheServiceAdmin(prop, port);

                try
                {
//                    System.out.println( admin.getStats().toString() );
                    log.debug( admin.getStats() );
                }
                catch ( IOException es )
                {
                    log.error( es );
                }
            }
            catch ( Exception ex )
            {
                log.error( "Problem getting stats.", ex );
            }
            log.debug( "done." );
            System.exit( 0 );
        }

        // startup.
        String host = prop.getProperty( "registry.host" );

        if ( host == null || host.trim().equals( "" ) || host.trim().equals( "localhost" ) )
        {
            log.debug( "main> creating registry on the localhost" );
            RemoteUtils.createRegistry( port );
        }
        log.debug( "main> starting up RemoteCacheServer" );
        startup( host, port, prop);
        log.debug( "main> done" );
    }

    /**
     * Look up the remote cache service admin instance
     *
     * @param config the configuration properties
     * @param port the local port
     * @return the admin object instance
     *
     * @throws Exception if lookup fails
     */
    private static ICacheServiceAdmin lookupCacheServiceAdmin(Properties config, int port) throws Exception
    {
        String remoteServiceName = config.getProperty( REMOTE_CACHE_SERVICE_NAME, REMOTE_CACHE_SERVICE_VAL ).trim();
        String registry = RemoteUtils.getNamingURL("", port, remoteServiceName);

        if ( log.isDebugEnabled() )
        {
            log.debug( "looking up server " + registry );
        }
        Object obj = Naming.lookup( registry );
        if ( log.isDebugEnabled() )
        {
            log.debug( "server found" );
        }

        return (ICacheServiceAdmin) obj;
    }

    /**
     * @param serviceName the serviceName to set
     */
    protected static void setServiceName( String serviceName )
    {
        RemoteCacheServerFactory.serviceName = serviceName;
    }

    /**
     * @return the serviceName
     */
    protected static String getServiceName()
    {
        return serviceName;
    }
}
