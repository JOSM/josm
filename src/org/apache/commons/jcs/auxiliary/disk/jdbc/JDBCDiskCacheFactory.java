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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.threadpool.DaemonThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This factory should create mysql disk caches.
 * <p>
 * @author Aaron Smuts
 */
public class JDBCDiskCacheFactory
    extends AbstractAuxiliaryCacheFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( JDBCDiskCacheFactory.class );

    /**
     * A map of TableState objects to table names. Each cache has a table state object, which is
     * used to determine if any long processes such as deletes or optimizations are running.
     */
    private Map<String, TableState> tableStates;

    /** The background scheduler, one for all regions. */
    protected ScheduledExecutorService scheduler; // TODO this is not accessed in a threadsafe way. Perhaps use IODH idiom?

    /**
     * A map of table name to shrinker threads. This allows each table to have a different setting.
     * It assumes that there is only one jdbc disk cache auxiliary defined per table.
     */
    private Map<String, ShrinkerThread> shrinkerThreadMap;

    /**
     * This factory method should create an instance of the jdbc cache.
     * <p>
     * @param rawAttr
     * @param compositeCacheManager
     * @param cacheEventLogger
     * @param elementSerializer
     * @return JDBCDiskCache
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

        JDBCDiskCache<K, V> cache = new JDBCDiskCache<K, V>( cattr, tableState, compositeCacheManager );
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
        this.tableStates = new HashMap<String, TableState>();
        this.shrinkerThreadMap = new HashMap<String, ShrinkerThread>();
    }

    /**
     * Dispose of this factory, clean up shared resources
     */
    @Override
    public void dispose()
    {
        if (this.scheduler != null)
        {
            this.scheduler.shutdownNow();
            this.scheduler = null;
        }
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
        TableState tableState = tableStates.get( tableName );

        if ( tableState == null )
        {
            tableState = new TableState( tableName );
            tableStates.put(tableName, tableState);
        }

        return tableState;
    }

    /**
     * Get the scheduler service (lazily loaded)
     *
     * @return the scheduler
     */
    protected ScheduledExecutorService getScheduledExecutorService()
    {
        if ( scheduler == null )
        {
            scheduler = Executors.newScheduledThreadPool(2,
                    new DaemonThreadFactory("JCS-JDBCDiskCacheManager-", Thread.MIN_PRIORITY));
        }

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
            ShrinkerThread shrinkerThread = shrinkerThreadMap.get( cattr.getTableName() );
            if ( shrinkerThread == null )
            {
                shrinkerThread = new ShrinkerThread();
                shrinkerThreadMap.put( cattr.getTableName(), shrinkerThread );

                long intervalMillis = Math.max( 999, cattr.getShrinkerIntervalSeconds() * 1000 );
                if ( log.isInfoEnabled() )
                {
                    log.info( "Setting the shrinker to run every [" + intervalMillis + "] ms. for table ["
                        + cattr.getTableName() + "]" );
                }
                shrinkerService.scheduleAtFixedRate(shrinkerThread, 0, intervalMillis, TimeUnit.MILLISECONDS);
            }
            shrinkerThread.addDiskCacheToShrinkList( raf );
        }
    }
}
