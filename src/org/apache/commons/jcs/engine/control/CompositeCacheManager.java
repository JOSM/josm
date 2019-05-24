package org.apache.commons.jcs.engine.control;

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
import java.lang.management.ManagementFactory;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.admin.JCSAdminBean;
import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheConstants;
import org.apache.commons.jcs.engine.CacheConstants;
import org.apache.commons.jcs.engine.CompositeCacheAttributes;
import org.apache.commons.jcs.engine.ElementAttributes;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICacheType.CacheType;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.behavior.IProvideScheduler;
import org.apache.commons.jcs.engine.behavior.IShutdownObserver;
import org.apache.commons.jcs.engine.control.event.ElementEventQueue;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEventQueue;
import org.apache.commons.jcs.engine.stats.CacheStats;
import org.apache.commons.jcs.engine.stats.behavior.ICacheStats;
import org.apache.commons.jcs.utils.config.OptionConverter;
import org.apache.commons.jcs.utils.threadpool.DaemonThreadFactory;
import org.apache.commons.jcs.utils.threadpool.ThreadPoolManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages a composite cache. This provides access to caches and is the primary way to shutdown the
 * caching system as a whole.
 * <p>
 * The composite cache manager is responsible for creating / configuring cache regions. It serves as
 * a factory for the ComositeCache class. The CompositeCache is the core of JCS, the hub for various
 * auxiliaries.
 */
public class CompositeCacheManager
    implements IRemoteCacheConstants, ICompositeCacheManager, IProvideScheduler
{
    /** The logger */
    private static final Log log = LogFactory.getLog( CompositeCacheManager.class );

    /** JMX object name */
    public static final String JMX_OBJECT_NAME = "org.apache.commons.jcs:type=JCSAdminBean";

    /** default region prefix */
    private static final String DEFAULT_REGION = "jcs.default";

    /** Caches managed by this cache manager */
    private final ConcurrentMap<String, ICache<?, ?>> caches =
        new ConcurrentHashMap<>();

    /** Number of clients accessing this cache manager */
    private final AtomicInteger clients = new AtomicInteger(0);

    /** Default cache attributes for this cache manager */
    private ICompositeCacheAttributes defaultCacheAttr = new CompositeCacheAttributes();

    /** Default element attributes for this cache manager */
    private IElementAttributes defaultElementAttr = new ElementAttributes();

    /** Used to keep track of configured auxiliaries */
    private final ConcurrentMap<String, AuxiliaryCacheFactory> auxiliaryFactoryRegistry =
        new ConcurrentHashMap<>( );

    /** Used to keep track of attributes for auxiliaries. */
    private final ConcurrentMap<String, AuxiliaryCacheAttributes> auxiliaryAttributeRegistry =
        new ConcurrentHashMap<>( );

    /** Used to keep track of configured auxiliaries */
    private final ConcurrentMap<String, AuxiliaryCache<?, ?>> auxiliaryCaches =
        new ConcurrentHashMap<>( );

    /** Properties with which this manager was configured. This is exposed for other managers. */
    private Properties configurationProperties;

    /** The default auxiliary caches to be used if not preconfigured */
    private String defaultAuxValues;

    /** The Singleton Instance */
    private static CompositeCacheManager instance;

    /** Should we use system property substitutions. */
    private static final boolean DEFAULT_USE_SYSTEM_PROPERTIES = true;

    /** Once configured, you can force a reconfiguration of sorts. */
    private static final boolean DEFAULT_FORCE_RECONFIGURATION = false;

    /** Stack for those waiting for notification of a shutdown. */
    private final LinkedBlockingDeque<IShutdownObserver> shutdownObservers = new LinkedBlockingDeque<>();

    /** The central background scheduler. */
    private ScheduledExecutorService scheduledExecutor;

    /** The central event queue. */
    private IElementEventQueue elementEventQueue;

    /** Shutdown hook thread instance */
    private ShutdownHook shutdownHook;

    /** Indicates whether the instance has been initialized. */
    private boolean isInitialized = false;

    /** Indicates whether configure has been called. */
    private boolean isConfigured = false;

    /** Indicates whether JMX bean has been registered. */
    private boolean isJMXRegistered = false;

    private String jmxName = JMX_OBJECT_NAME;

    /**
     * Gets the CacheHub instance. For backward compatibility, if this creates the instance it will
     * attempt to configure it with the default configuration. If you want to configure from your
     * own source, use {@link #getUnconfiguredInstance}and then call {@link #configure}
     * <p>
     * @return CompositeCacheManager
     * @throws CacheException if the configuration cannot be loaded
     */
    public static synchronized CompositeCacheManager getInstance() throws CacheException
    {
        return getInstance( CacheConstants.DEFAULT_CONFIG );
    }

    /**
     * Initializes the cache manager using the props file for the given name.
     * <p>
     * @param propsFilename
     * @return CompositeCacheManager configured from the give propsFileName
     * @throws CacheException if the configuration cannot be loaded
     */
    public static synchronized CompositeCacheManager getInstance( String propsFilename ) throws CacheException
    {
        if ( instance == null )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Instance is null, creating with config [" + propsFilename + "]" );
            }

            instance = createInstance();
        }

        if (!instance.isInitialized())
        {
            instance.initialize();
        }

        if (!instance.isConfigured())
        {
            instance.configure( propsFilename );
        }

        instance.clients.incrementAndGet();

        return instance;
    }

    /**
     * Get a CacheHub instance which is not configured. If an instance already exists, it will be
     * returned.
     *<p>
     * @return CompositeCacheManager
     */
    public static synchronized CompositeCacheManager getUnconfiguredInstance()
    {
        if ( instance == null )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Instance is null, returning unconfigured instance" );
            }

            instance = createInstance();
        }

        if (!instance.isInitialized())
        {
            instance.initialize();
        }

        instance.clients.incrementAndGet();

        return instance;
    }

    /**
     * Simple factory method, must override in subclasses so getInstance creates / returns the
     * correct object.
     * <p>
     * @return CompositeCacheManager
     */
    protected static CompositeCacheManager createInstance()
    {
        return new CompositeCacheManager();
    }

    /**
     * Default constructor
     */
    protected CompositeCacheManager()
    {
        // empty
    }

    /** Creates a shutdown hook and starts the scheduler service */
    protected void initialize()
    {
        if (!isInitialized)
        {
            this.shutdownHook = new ShutdownHook();
            try
            {
                Runtime.getRuntime().addShutdownHook( shutdownHook );
            }
            catch ( AccessControlException e )
            {
                log.error( "Could not register shutdown hook.", e );
            }

            this.scheduledExecutor = Executors.newScheduledThreadPool(4,
                    new DaemonThreadFactory("JCS-Scheduler-", Thread.MIN_PRIORITY));

            // Register JMX bean
            if (!isJMXRegistered && jmxName != null)
            {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                JCSAdminBean adminBean = new JCSAdminBean(this);
                try
                {
                    ObjectName jmxObjectName = new ObjectName(jmxName);
                    mbs.registerMBean(adminBean, jmxObjectName);
                    isJMXRegistered = true;
                }
                catch (Exception e)
                {
                    log.warn( "Could not register JMX bean.", e );
                }
            }

            this.elementEventQueue = new ElementEventQueue();

            isInitialized = true;
        }
    }

    /**
     * Get the element event queue
     *
     * @return the elementEventQueue
     */
    public IElementEventQueue getElementEventQueue()
    {
        return elementEventQueue;
    }

    /**
     * Get the scheduler service
     *
     * @return the scheduledExecutor
     */
    @Override
    public ScheduledExecutorService getScheduledExecutorService()
    {
        return scheduledExecutor;
    }

    /**
     * Configure with default properties file
     * @throws CacheException if the configuration cannot be loaded
     */
    public void configure() throws CacheException
    {
        configure( CacheConstants.DEFAULT_CONFIG );
    }

    /**
     * Configure from specific properties file.
     * <p>
     * @param propFile Path <u>within classpath </u> to load configuration from
     * @throws CacheException if the configuration cannot be loaded
     */
    public void configure( String propFile ) throws CacheException
    {
        log.info( "Creating cache manager from config file: " + propFile );

        Properties props = new Properties();

        try (InputStream is = getClass().getResourceAsStream( propFile ))
        {
            props.load( is );

            if ( log.isDebugEnabled() )
            {
                log.debug( "File [" + propFile + "] contained " + props.size() + " properties" );
            }
        }
        catch ( IOException ex )
        {
            throw new CacheException("Failed to load properties for name [" + propFile + "]", ex);
        }

        configure( props );
    }

    /**
     * Configure from properties object.
     * <p>
     * This method will call configure, instructing it to use system properties as a default.
     * @param props
     */
    public void configure( Properties props )
    {
        configure( props, DEFAULT_USE_SYSTEM_PROPERTIES );
    }

    /**
     * Configure from properties object, overriding with values from the system properties if
     * instructed.
     * <p>
     * You can override a specific value by passing in a system property:
     * <p>
     * For example, you could override this value in the cache.ccf file by starting up your program
     * with the argument: -Djcs.auxiliary.LTCP.attributes.TcpListenerPort=1111
     * <p>
     * @param props
     * @param useSystemProperties -- if true, values starting with jcs will be put into the props
     *            file prior to configuring the cache.
     */
    public void configure( Properties props, boolean useSystemProperties )
    {
        configure( props, useSystemProperties, DEFAULT_FORCE_RECONFIGURATION );
    }

    /**
     * Configure from properties object, overriding with values from the system properties if
     * instructed.
     * <p>
     * You can override a specific value by passing in a system property:
     * <p>
     * For example, you could override this value in the cache.ccf file by starting up your program
     * with the argument: -Djcs.auxiliary.LTCP.attributes.TcpListenerPort=1111
     * <p>
     * @param props
     * @param useSystemProperties -- if true, values starting with jcs will be put into the props
     *            file prior to configuring the cache.
     * @param forceReconfiguration - if the manager is already configured, we will try again. This
     *            may not work properly.
     */
    public synchronized void configure( Properties props, boolean useSystemProperties, boolean forceReconfiguration )
    {
        if ( props == null )
        {
            log.error( "No properties found.  Please configure the cache correctly." );
            return;
        }

        if ( isConfigured )
        {
            if ( !forceReconfiguration )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Configure called after the manager has been configured.  "
                        + "Force reconfiguration is false.  Doing nothing" );
                }
                return;
            }
            else
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "Configure called after the manager has been configured.  "
                        + "Force reconfiguration is true.  Reconfiguring as best we can." );
                }
            }
        }
        if ( useSystemProperties )
        {
            CompositeCacheConfigurator.overrideWithSystemProperties( props );
        }
        doConfigure( props );
    }

    /**
     * Configure the cache using the supplied properties.
     * <p>
     * @param properties assumed not null
     */
    private void doConfigure( Properties properties )
    {
        // We will expose this for managers that need raw properties.
        this.configurationProperties = properties;

        // set the props value and then configure the ThreadPoolManager
        ThreadPoolManager.setProps( properties );
        ThreadPoolManager poolMgr = ThreadPoolManager.getInstance();
        if ( log.isDebugEnabled() )
        {
            log.debug( "ThreadPoolManager = " + poolMgr );
        }

        // configure the cache
        CompositeCacheConfigurator configurator = newConfigurator();

        long start = System.currentTimeMillis();

        // set default value list
        this.defaultAuxValues = OptionConverter.findAndSubst( CompositeCacheManager.DEFAULT_REGION,
                properties );

        log.info( "Setting default auxiliaries to " + this.defaultAuxValues );

        // set default cache attr
        this.defaultCacheAttr = configurator.parseCompositeCacheAttributes( properties, "",
                new CompositeCacheAttributes(), DEFAULT_REGION );

        log.info( "setting defaultCompositeCacheAttributes to " + this.defaultCacheAttr );

        // set default element attr
        this.defaultElementAttr = configurator.parseElementAttributes( properties, "",
                new ElementAttributes(), DEFAULT_REGION );

        log.info( "setting defaultElementAttributes to " + this.defaultElementAttr );

        // set up system caches to be used by non system caches
        // need to make sure there is no circularity of reference
        configurator.parseSystemRegions( properties, this );

        // setup preconfigured caches
        configurator.parseRegions( properties, this );

        long end = System.currentTimeMillis();
        if ( log.isInfoEnabled() )
        {
            log.info( "Finished configuration in " + ( end - start ) + " ms." );
        }

        isConfigured = true;
    }

    /**
     * Gets the defaultCacheAttributes attribute of the CacheHub object
     * <p>
     * @return The defaultCacheAttributes value
     */
    public ICompositeCacheAttributes getDefaultCacheAttributes()
    {
        return this.defaultCacheAttr.clone();
    }

    /**
     * Gets the defaultElementAttributes attribute of the CacheHub object
     * <p>
     * @return The defaultElementAttributes value
     */
    public IElementAttributes getDefaultElementAttributes()
    {
        return this.defaultElementAttr.clone();
    }

    /**
     * Gets the cache attribute of the CacheHub object
     * <p>
     * @param cacheName
     * @return CompositeCache -- the cache region controller
     */
    @Override
    public <K, V> CompositeCache<K, V> getCache( String cacheName )
    {
        return getCache( cacheName, getDefaultCacheAttributes() );
    }

    /**
     * Gets the cache attribute of the CacheHub object
     * <p>
     * @param cacheName
     * @param cattr
     * @return CompositeCache
     */
    public <K, V> CompositeCache<K, V> getCache( String cacheName, ICompositeCacheAttributes cattr )
    {
        cattr.setCacheName( cacheName );
        return getCache( cattr, getDefaultElementAttributes() );
    }

    /**
     * Gets the cache attribute of the CacheHub object
     * <p>
     * @param cacheName
     * @param cattr
     * @param attr
     * @return CompositeCache
     */
    public <K, V> CompositeCache<K, V>  getCache( String cacheName, ICompositeCacheAttributes cattr, IElementAttributes attr )
    {
        cattr.setCacheName( cacheName );
        return getCache( cattr, attr );
    }

    /**
     * Gets the cache attribute of the CacheHub object
     * <p>
     * @param cattr
     * @return CompositeCache
     */
    public <K, V> CompositeCache<K, V>  getCache( ICompositeCacheAttributes cattr )
    {
        return getCache( cattr, getDefaultElementAttributes() );
    }

    /**
     * If the cache has already been created, then the CacheAttributes and the element Attributes
     * will be ignored. Currently there is no overriding the CacheAttributes once it is set up. You
     * can change the default ElementAttributes for a region later.
     * <p>
     * Overriding the default elemental attributes will require changing the way the attributes are
     * assigned to elements. Get cache creates a cache with defaults if none are specified. We might
     * want to create separate method for creating/getting. . .
     * <p>
     * @param cattr
     * @param attr
     * @return CompositeCache
     */
    @SuppressWarnings("unchecked") // Need to cast because of common map for all caches
    public <K, V> CompositeCache<K, V>  getCache( ICompositeCacheAttributes cattr, IElementAttributes attr )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "attr = " + attr );
        }

        CompositeCache<K, V> cache = (CompositeCache<K, V>) caches.computeIfAbsent(cattr.getCacheName(),
                cacheName -> {
            CompositeCacheConfigurator configurator = newConfigurator();
            return configurator.parseRegion( this.getConfigurationProperties(), this, cacheName,
                                              this.defaultAuxValues, cattr );
        });

        return cache;
    }

    protected CompositeCacheConfigurator newConfigurator() {
        return new CompositeCacheConfigurator();
    }

    /**
     * @param name
     */
    public void freeCache( String name )
    {
        freeCache( name, false );
    }

    /**
     * @param name
     * @param fromRemote
     */
    public void freeCache( String name, boolean fromRemote )
    {
        CompositeCache<?, ?> cache = (CompositeCache<?, ?>) caches.remove( name );

        if ( cache != null )
        {
            cache.dispose( fromRemote );
        }
    }

    /**
     * Calls freeCache on all regions
     */
    public void shutDown()
    {
        synchronized (CompositeCacheManager.class)
        {
            // shutdown element event queue
            this.elementEventQueue.dispose();

            // shutdown all scheduled jobs
            this.scheduledExecutor.shutdownNow();

            // shutdown all thread pools
            ThreadPoolManager.dispose();

            // notify any observers
            IShutdownObserver observer = null;
            while ((observer = shutdownObservers.poll()) != null)
            {
                observer.shutdown();
            }

            // Unregister JMX bean
            if (isJMXRegistered)
            {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                try
                {
                    ObjectName jmxObjectName = new ObjectName(jmxName);
                    mbs.unregisterMBean(jmxObjectName);
                }
                catch (Exception e)
                {
                    log.warn( "Could not unregister JMX bean.", e );
                }

                isJMXRegistered = false;
            }

            // do the traditional shutdown of the regions.
            for (String name : getCacheNames())
            {
                freeCache( name );
            }

            // shut down factories
            for (AuxiliaryCacheFactory factory : auxiliaryFactoryRegistry.values())
            {
                factory.dispose();
            }

            auxiliaryAttributeRegistry.clear();
            auxiliaryFactoryRegistry.clear();

            if (shutdownHook != null)
            {
                try
                {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
                catch (IllegalStateException e)
                {
                    // May fail if the JVM is already shutting down
                }

                this.shutdownHook = null;
            }

            isConfigured = false;
            isInitialized = false;
        }
    }

    /** */
    public void release()
    {
        release( false );
    }

    /**
     * @param fromRemote
     */
    private void release( boolean fromRemote )
    {
        synchronized ( CompositeCacheManager.class )
        {
            // Wait until called by the last client
            if ( clients.decrementAndGet() > 0 )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Release called, but " + clients + " remain" );
                }
                return;
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "Last client called release. There are " + caches.size() + " caches which will be disposed" );
            }

            for (ICache<?, ?> c : caches.values() )
            {
                CompositeCache<?, ?> cache = (CompositeCache<?, ?>) c;

                if ( cache != null )
                {
                    cache.dispose( fromRemote );
                }
            }
        }
    }

    /**
     * Returns a list of the current cache names.
     * @return String[]
     */
    public String[] getCacheNames()
    {
        return caches.keySet().toArray(new String[caches.size()]);
    }

    /**
     * @return ICacheType.CACHE_HUB
     */
    public CacheType getCacheType()
    {
        return CacheType.CACHE_HUB;
    }

    /**
     * @param auxFac
     */
    public void registryFacPut( AuxiliaryCacheFactory auxFac )
    {
        auxiliaryFactoryRegistry.put( auxFac.getName(), auxFac );
    }

    /**
     * @param name
     * @return AuxiliaryCacheFactory
     */
    public AuxiliaryCacheFactory registryFacGet( String name )
    {
        return auxiliaryFactoryRegistry.get( name );
    }

    /**
     * @param auxAttr
     */
    public void registryAttrPut( AuxiliaryCacheAttributes auxAttr )
    {
        auxiliaryAttributeRegistry.put( auxAttr.getName(), auxAttr );
    }

    /**
     * @param name
     * @return AuxiliaryCacheAttributes
     */
    public AuxiliaryCacheAttributes registryAttrGet( String name )
    {
        return auxiliaryAttributeRegistry.get( name );
    }

    /**
     * Add a cache to the map of registered caches
     *
     * @param cacheName the region name
     * @param cache the cache instance
     */
    public void addCache(String cacheName, ICache<?, ?> cache)
    {
        caches.put(cacheName, cache);
    }

    /**
     * Add a cache to the map of registered auxiliary caches
     *
     * @param auxName the auxiliary name
     * @param cacheName the region name
     * @param cache the cache instance
     */
    public void addAuxiliaryCache(String auxName, String cacheName, AuxiliaryCache<?, ?> cache)
    {
        String key = String.format("aux.%s.region.%s", auxName, cacheName);
        auxiliaryCaches.put(key, cache);
    }

    /**
     * Get a cache from the map of registered auxiliary caches
     *
     * @param auxName the auxiliary name
     * @param cacheName the region name
     *
     * @return the cache instance
     */
    @Override
    @SuppressWarnings("unchecked") // because of common map for all auxiliary caches
    public <K, V> AuxiliaryCache<K, V> getAuxiliaryCache(String auxName, String cacheName)
    {
        String key = String.format("aux.%s.region.%s", auxName, cacheName);
        return (AuxiliaryCache<K, V>) auxiliaryCaches.get(key);
    }

    /**
     * Gets stats for debugging. This calls gets statistics and then puts all the results in a
     * string. This returns data for all regions.
     * <p>
     * @return String
     */
    @Override
    public String getStats()
    {
        ICacheStats[] stats = getStatistics();
        if ( stats == null )
        {
            return "NONE";
        }

        // force the array elements into a string.
        StringBuilder buf = new StringBuilder();
        int statsLen = stats.length;
        for ( int i = 0; i < statsLen; i++ )
        {
            buf.append( "\n---------------------------\n" );
            buf.append( stats[i] );
        }
        return buf.toString();
    }

    /**
     * This returns data gathered for all regions and all the auxiliaries they currently uses.
     * <p>
     * @return ICacheStats[]
     */
    public ICacheStats[] getStatistics()
    {
        ArrayList<ICacheStats> cacheStats = new ArrayList<>();
        for (ICache<?, ?> c :  caches.values())
        {
            CompositeCache<?, ?> cache = (CompositeCache<?, ?>) c;
            if ( cache != null )
            {
                cacheStats.add( cache.getStatistics() );
            }
        }
        ICacheStats[] stats = cacheStats.toArray( new CacheStats[0] );
        return stats;
    }

    /**
     * Perhaps the composite cache itself should be the observable object. It doesn't make much of a
     * difference. There are some problems with region by region shutdown. Some auxiliaries are
     * global. They will need to track when every region has shutdown before doing things like
     * closing the socket with a lateral.
     * <p>
     * @param observer
     */
    @Override
    public void registerShutdownObserver( IShutdownObserver observer )
    {
    	if (!shutdownObservers.contains(observer))
    	{
    		shutdownObservers.push( observer );
    	}
    	else
    	{
    		log.warn("Shutdown observer added twice " + observer);
    	}
    }

    /**
     * @param observer
     */
    @Override
    public void deregisterShutdownObserver( IShutdownObserver observer )
    {
        shutdownObservers.remove( observer );
    }

    /**
     * This is exposed so other manager can get access to the props.
     * <p>
     * @return the configurationProperties
     */
    @Override
    public Properties getConfigurationProperties()
    {
        return configurationProperties;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized()
    {
        return isInitialized;
    }

    /**
     * @return the isConfigured
     */
    public boolean isConfigured()
    {
        return isConfigured;
    }

    public void setJmxName(final String name)
    {
        if (isJMXRegistered)
        {
            throw new IllegalStateException("Too late, MBean registration is done");
        }
        jmxName = name;
    }

    /**
     * Called on shutdown. This gives use a chance to store the keys and to optimize even if the
     * cache manager's shutdown method was not called manually.
     */
    class ShutdownHook
        extends Thread
    {
        /**
         * This will persist the keys on shutdown.
         * <p>
         * @see java.lang.Thread#run()
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void run()
        {
            if ( isInitialized() )
            {
                log.info( "Shutdown hook activated.  Shutdown was not called.  Shutting down JCS." );
                shutDown();
            }
        }
    }
}
