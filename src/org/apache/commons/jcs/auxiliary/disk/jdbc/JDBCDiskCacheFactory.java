package org.apache.commons.jcs.auxiliary.disk.jdbc;

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

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory.DataSourceFactory;
import org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory.JndiDataSourceFactory;
import org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory.SharedPoolDataSourceFactory;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.behavior.IRequireScheduler;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This factory should create JDBC auxiliary caches.
 * <p>
 * @author Aaron Smuts
 */
public class JDBCDiskCacheFactory
    extends AbstractAuxiliaryCacheFactory
    implements IRequireScheduler
{
    /** The logger */
    private static final Log log = LogFactory.getLog( JDBCDiskCacheFactory.class );

    /**
     * A map of TableState objects to table names. Each cache has a table state object, which is
     * used to determine if any long processes such as deletes or optimizations are running.
     */
    private ConcurrentMap<String, TableState> tableStates;

    /** The background scheduler, one for all regions. Injected by the configurator */
    protected ScheduledExecutorService scheduler;

    /**
     * A map of table name to shrinker threads. This allows each table to have a different setting.
     * It assumes that there is only one jdbc disk cache auxiliary defined per table.
     */
    private ConcurrentMap<String, ShrinkerThread> shrinkerThreadMap;

    /** Pool name to DataSourceFactories */
    private ConcurrentMap<String, DataSourceFactory> dsFactories;

    /** props prefix */
    protected static final String POOL_CONFIGURATION_PREFIX = "jcs.jdbcconnectionpool.";

    /** .attributes */
    protected static final String ATTRIBUTE_PREFIX = ".attributes";

    /**
     * This factory method should create an instance of the jdbc cache.
     * <p>
     * @param rawAttr specific cache configuration attributes
     * @param compositeCacheManager the global cache manager
     * @param cacheEventLogger a specific logger for cache events
     * @param elementSerializer a serializer for cache elements
     * @return JDBCDiskCache the cache instance
     * @throws SQLException if the cache instance could not be created
     */
    @Override
    public <K, V> JDBCDiskCache<K, V> createCache( AuxiliaryCacheAttributes rawAttr,
            ICompositeCacheManager compositeCacheManager,
            ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
            throws SQLException
    {
        JDBCDiskCacheAttributes cattr = (JDBCDiskCacheAttributes) rawAttr;
        TableState tableState = getTableState( cattr.getTableName() );
        DataSourceFactory dsFactory = getDataSourceFactory(cattr, compositeCacheManager.getConfigurationProperties());

        JDBCDiskCache<K, V> cache = new JDBCDiskCache<>( cattr, dsFactory, tableState, compositeCacheManager );
        cache.setCacheEventLogger( cacheEventLogger );
        cache.setElementSerializer( elementSerializer );

        // create a shrinker if we need it.
        createShrinkerWhenNeeded( cattr, cache );

        return cache;
    }

    /**
     * Initialize this factory
     */
    @Override
    public void initialize()
    {
        super.initialize();
        this.tableStates = new ConcurrentHashMap<>();
        this.shrinkerThreadMap = new ConcurrentHashMap<>();
        this.dsFactories = new ConcurrentHashMap<>();
    }

    /**
     * Dispose of this factory, clean up shared resources
     */
    @Override
    public void dispose()
    {
        this.tableStates.clear();

        for (DataSourceFactory dsFactory : this.dsFactories.values())
        {
        	try
        	{
				dsFactory.close();
			}
        	catch (SQLException e)
        	{
        		log.error("Could not close data source factory " + dsFactory.getName(), e);
			}
        }

        this.dsFactories.clear();
        this.shrinkerThreadMap.clear();
        super.dispose();
    }

    /**
     * Get a table state for a given table name
     *
     * @param tableName
     * @return a cached instance of the table state
     */
    protected TableState getTableState(String tableName)
    {
        return tableStates.computeIfAbsent(tableName, key -> new TableState(key));
    }

    /**
	 * @see org.apache.commons.jcs.engine.behavior.IRequireScheduler#setScheduledExecutorService(java.util.concurrent.ScheduledExecutorService)
	 */
	@Override
	public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutor)
	{
		this.scheduler = scheduledExecutor;
	}

	/**
     * Get the scheduler service
     *
     * @return the scheduler
     */
    protected ScheduledExecutorService getScheduledExecutorService()
    {
        return scheduler;
    }

    /**
     * If UseDiskShrinker is true then we will create a shrinker daemon if necessary.
     * <p>
     * @param cattr
     * @param raf
     */
    protected void createShrinkerWhenNeeded( JDBCDiskCacheAttributes cattr, JDBCDiskCache<?, ?> raf )
    {
        // add cache to shrinker.
        if ( cattr.isUseDiskShrinker() )
        {
            ScheduledExecutorService shrinkerService = getScheduledExecutorService();
            ShrinkerThread shrinkerThread = shrinkerThreadMap.computeIfAbsent(cattr.getTableName(), key -> {
                ShrinkerThread newShrinkerThread = new ShrinkerThread();

                long intervalMillis = Math.max( 999, cattr.getShrinkerIntervalSeconds() * 1000 );
                if ( log.isInfoEnabled() )
                {
                    log.info( "Setting the shrinker to run every [" + intervalMillis + "] ms. for table ["
                        + key + "]" );
                }
                shrinkerService.scheduleAtFixedRate(newShrinkerThread, 0, intervalMillis, TimeUnit.MILLISECONDS);

                return newShrinkerThread;
            });

            shrinkerThread.addDiskCacheToShrinkList( raf );
        }
    }

    /**
     * manages the DataSourceFactories.
     * <p>
     * @param cattr the cache configuration
     * @param configProps the configuration properties object
     * @return a DataSourceFactory
     * @throws SQLException if a database access error occurs
     */
    protected DataSourceFactory getDataSourceFactory( JDBCDiskCacheAttributes cattr,
                                                      Properties configProps ) throws SQLException
    {
    	String poolName = null;

    	if (cattr.getConnectionPoolName() == null)
    	{
    		poolName = cattr.getCacheName() + "." + JDBCDiskCacheAttributes.DEFAULT_POOL_NAME;
        }
        else
        {
            poolName = cattr.getConnectionPoolName();
        }


    	DataSourceFactory dsFactory = this.dsFactories.computeIfAbsent(poolName, key -> {
    	    DataSourceFactory newDsFactory;
            JDBCDiskCacheAttributes dsConfig = null;

            if (cattr.getConnectionPoolName() == null)
            {
                dsConfig = cattr;
            }
            else
            {
                dsConfig = new JDBCDiskCacheAttributes();
                String dsConfigAttributePrefix = POOL_CONFIGURATION_PREFIX + key + ATTRIBUTE_PREFIX;
                PropertySetter.setProperties( dsConfig,
                        configProps,
                        dsConfigAttributePrefix + "." );

                dsConfig.setConnectionPoolName(key);
            }

            if ( dsConfig.getJndiPath() != null )
            {
                newDsFactory = new JndiDataSourceFactory();
            }
            else
            {
                newDsFactory = new SharedPoolDataSourceFactory();
            }

            try
            {
                newDsFactory.initialize(dsConfig);
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
    	    return newDsFactory;
    	});

    	return dsFactory;
    }
}
