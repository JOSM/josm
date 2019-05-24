package org.apache.commons.jcs.utils.threadpool;

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

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This manages threadpools for an application
 * <p>
 * It is a singleton since threads need to be managed vm wide.
 * <p>
 * This manager forces you to use a bounded queue. By default it uses the current thread for
 * execution when the buffer is full and no free threads can be created.
 * <p>
 * You can specify the props file to use or pass in a properties object prior to configuration.
 * <p>
 * If set, the Properties object will take precedence.
 * <p>
 * If a value is not set for a particular pool, the hard coded defaults in <code>PoolConfiguration</code> will be used.
 * You can configure default settings by specifying <code>thread_pool.default</code> in the properties, ie "cache.ccf"
 * <p>
 * @author Aaron Smuts
 */
public class ThreadPoolManager
{
    /** The logger */
    private static final Log log = LogFactory.getLog( ThreadPoolManager.class );

    /** The default config, created using property defaults if present, else those above. */
    private PoolConfiguration defaultConfig;

    /** the root property name */
    private static final String PROP_NAME_ROOT = "thread_pool";

    /** default property file name */
    private static final String DEFAULT_PROP_NAME_ROOT = "thread_pool.default";

    /** the scheduler root property name */
    private static final String PROP_NAME_SCHEDULER_ROOT = "scheduler_pool";

    /** default scheduler property file name */
    private static final String DEFAULT_PROP_NAME_SCHEDULER_ROOT = "scheduler_pool.default";

   /**
     * You can specify the properties to be used to configure the thread pool. Setting this post
     * initialization will have no effect.
     */
    private static volatile Properties props = null;

    /** singleton instance */
    private static ThreadPoolManager INSTANCE = null;

    /** Map of names to pools. */
    private ConcurrentHashMap<String, ExecutorService> pools;

    /** Map of names to scheduler pools. */
    private ConcurrentHashMap<String, ScheduledExecutorService> schedulerPools;

    /**
     * No instances please. This is a singleton.
     */
    private ThreadPoolManager()
    {
        this.pools = new ConcurrentHashMap<>();
        this.schedulerPools = new ConcurrentHashMap<>();
        configure();
    }

    /**
     * Creates a pool based on the configuration info.
     * <p>
     * @param config the pool configuration
     * @param threadNamePrefix prefix for the thread names of the pool
     * @return A ThreadPool wrapper
     */
    public ExecutorService createPool( PoolConfiguration config, String threadNamePrefix)
    {
    	return createPool(config, threadNamePrefix, Thread.NORM_PRIORITY);
    }

    /**
     * Creates a pool based on the configuration info.
     * <p>
     * @param config the pool configuration
     * @param threadNamePrefix prefix for the thread names of the pool
     * @param threadPriority the priority of the created threads
     * @return A ThreadPool wrapper
     */
    public ExecutorService createPool( PoolConfiguration config, String threadNamePrefix, int threadPriority )
    {
        BlockingQueue<Runnable> queue = null;
        if ( config.isUseBoundary() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Creating a Bounded Buffer to use for the pool" );
            }

            queue = new LinkedBlockingQueue<>(config.getBoundarySize());
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Creating a non bounded Linked Queue to use for the pool" );
            }
            queue = new LinkedBlockingQueue<>();
        }

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            config.getStartUpSize(),
            config.getMaximumPoolSize(),
            config.getKeepAliveTime(),
            TimeUnit.MILLISECONDS,
            queue,
            new DaemonThreadFactory(threadNamePrefix, threadPriority));

        // when blocked policy
        switch (config.getWhenBlockedPolicy())
        {
            case ABORT:
                pool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
                break;

            case RUN:
                pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                break;

            case WAIT:
                throw new RuntimeException("POLICY_WAIT no longer supported");

            case DISCARDOLDEST:
                pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                break;

            default:
                break;
        }

        pool.prestartAllCoreThreads();

        return pool;
    }

    /**
     * Creates a scheduler pool based on the configuration info.
     * <p>
     * @param config the pool configuration
     * @param threadNamePrefix prefix for the thread names of the pool
     * @param threadPriority the priority of the created threads
     * @return A ScheduledExecutorService
     */
    public ScheduledExecutorService createSchedulerPool( PoolConfiguration config, String threadNamePrefix, int threadPriority )
    {
    	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
    			config.getMaximumPoolSize(),
    			new DaemonThreadFactory(threadNamePrefix, threadPriority));

        return scheduler;
    }

    /**
     * Returns a configured instance of the ThreadPoolManger To specify a configuration file or
     * Properties object to use call the appropriate setter prior to calling getInstance.
     * <p>
     * @return The single instance of the ThreadPoolManager
     */
    public static synchronized ThreadPoolManager getInstance()
    {
        if ( INSTANCE == null )
        {
            INSTANCE = new ThreadPoolManager();
        }
        return INSTANCE;
    }

    /**
     * Dispose of the instance of the ThreadPoolManger and shut down all thread pools
     */
    public static synchronized void dispose()
    {
        if ( INSTANCE != null )
        {
            for ( ExecutorService pool : INSTANCE.pools.values() )
            {
                try
                {
                    pool.shutdownNow();
                }
                catch (Throwable t)
                {
                    log.warn("Failed to close pool " + pool, t);
                }
            }

            for ( ScheduledExecutorService pool : INSTANCE.schedulerPools.values() )
            {
                try
                {
                    pool.shutdownNow();
                }
                catch (Throwable t)
                {
                    log.warn("Failed to close pool " + pool, t);
                }
            }

            INSTANCE = null;
        }
    }

    /**
     * Returns an executor service by name. If a service by this name does not exist in the configuration file or
     * properties, one will be created using the default values.
     * <p>
     * Services are lazily created.
     * <p>
     * @param name
     * @return The executor service configured for the name.
     */
    public ExecutorService getExecutorService( String name )
    {
    	ExecutorService pool = pools.computeIfAbsent(name, key -> {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Creating pool for name [" + key + "]" );
            }
            PoolConfiguration config = loadConfig( PROP_NAME_ROOT + "." + key );
            return createPool( config, "JCS-ThreadPoolManager-" + key + "-" );
    	});

        return pool;
    }

    /**
     * Returns a scheduler pool by name. If a pool by this name does not exist in the configuration file or
     * properties, one will be created using the default values.
     * <p>
     * Pools are lazily created.
     * <p>
     * @param name
     * @return The scheduler pool configured for the name.
     */
    public ScheduledExecutorService getSchedulerPool( String name )
    {
    	ScheduledExecutorService pool = schedulerPools.computeIfAbsent(name, key -> {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Creating scheduler pool for name [" + key + "]" );
            }

            PoolConfiguration defaultSchedulerConfig = loadConfig( DEFAULT_PROP_NAME_SCHEDULER_ROOT );
            PoolConfiguration config = loadConfig( PROP_NAME_SCHEDULER_ROOT + "." + key,
                    defaultSchedulerConfig );
            return createSchedulerPool( config, "JCS-ThreadPoolManager-" + key + "-", Thread.NORM_PRIORITY );
    	});

        return pool;
    }

    /**
     * Returns the names of all configured pools.
     * <p>
     * @return ArrayList of string names
     */
    public ArrayList<String> getPoolNames()
    {
        return new ArrayList<>(pools.keySet());
    }

    /**
     * This will be used if it is not null on initialization. Setting this post initialization will
     * have no effect.
     * <p>
     * @param props The props to set.
     */
    public static void setProps( Properties props )
    {
        ThreadPoolManager.props = props;
    }

    /**
     * Initialize the ThreadPoolManager and create all the pools defined in the configuration.
     */
    private void configure()
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Initializing ThreadPoolManager" );
        }

        if ( props == null )
        {
            log.warn( "No configuration settings found.  Using hardcoded default values for all pools." );
            props = new Properties();
        }

        // set intial default and then override if new
        // settings are available
        defaultConfig = new PoolConfiguration();
        defaultConfig = loadConfig( DEFAULT_PROP_NAME_ROOT );
    }

    /**
     * Configures the PoolConfiguration settings.
     * <p>
     * @param root the configuration key prefix
     * @return PoolConfiguration
     */
    private PoolConfiguration loadConfig( String root )
    {
    	return loadConfig(root, defaultConfig);
    }

    /**
     * Configures the PoolConfiguration settings.
     * <p>
     * @param root the configuration key prefix
     * @param defaultPoolConfiguration the default configuration
     * @return PoolConfiguration
     */
    private PoolConfiguration loadConfig( String root, PoolConfiguration defaultPoolConfiguration )
    {
        PoolConfiguration config = defaultPoolConfiguration.clone();
        PropertySetter.setProperties( config, props, root + "." );

        if ( log.isDebugEnabled() )
        {
            log.debug( root + " PoolConfiguration = " + config );
        }

        return config;
    }
}
