package org.apache.commons.jcs.auxiliary.disk.jdbc.mysql;

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
import java.util.Map;

import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCache;
import org.apache.commons.jcs.auxiliary.disk.jdbc.TableState;
import org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory.DataSourceFactory;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The MySQLDiskCache extends the core JDBCDiskCache.
 * <p>
 * Although the generic JDBC Disk Cache can be used for MySQL, the MySQL JDBC Disk Cache has
 * additional features, such as table optimization that are particular to MySQL.
 * <p>
 * @author Aaron Smuts
 */
public class MySQLDiskCache<K, V>
	extends JDBCDiskCache<K, V>
{
    /** local logger */
    private static final Log log = LogFactory.getLog( MySQLDiskCache.class );

    /** config attributes */
    private final MySQLDiskCacheAttributes mySQLDiskCacheAttributes;

    /**
     * Delegates to the super and makes use of the MySQL specific parameters used for scheduled
     * optimization.
     * <p>
     * @param attributes the configuration object for this cache
     * @param dsFactory the DataSourceFactory for this cache
     * @param tableState an object to track table operations
     * @param compositeCacheManager the global cache manager
     * @throws SQLException if the pool access could not be set up
     */
    public MySQLDiskCache( MySQLDiskCacheAttributes attributes, DataSourceFactory dsFactory,
    		TableState tableState, ICompositeCacheManager compositeCacheManager ) throws SQLException
    {
        super( attributes, dsFactory, tableState, compositeCacheManager );

        mySQLDiskCacheAttributes = attributes;

        if ( log.isDebugEnabled() )
        {
            log.debug( "MySQLDiskCacheAttributes = " + attributes );
        }
    }

    /**
     * This delegates to the generic JDBC disk cache. If we are currently optimizing, then this
     * method will balk and return null.
     * <p>
     * @param key Key to locate value for.
     * @return An object matching key, or null.
     */
    @Override
    protected ICacheElement<K, V> processGet( K key )
    {
        if ( this.getTableState().getState() == TableState.OPTIMIZATION_RUNNING )
        {
            if ( this.mySQLDiskCacheAttributes.isBalkDuringOptimization() )
            {
                return null;
            }
        }
        return super.processGet( key );
    }

    /**
     * This delegates to the generic JDBC disk cache. If we are currently optimizing, then this
     * method will balk and return null.
     * <p>
     * @param pattern used for like query.
     * @return An object matching key, or null.
     */
    @Override
    protected Map<K, ICacheElement<K, V>> processGetMatching( String pattern )
    {
        if ( this.getTableState().getState() == TableState.OPTIMIZATION_RUNNING )
        {
            if ( this.mySQLDiskCacheAttributes.isBalkDuringOptimization() )
            {
                return null;
            }
        }
        return super.processGetMatching( pattern );
    }

    /**
     * @param pattern
     * @return String to use in the like query.
     */
    @Override
    public String constructLikeParameterFromPattern( String pattern )
    {
        String likePattern = pattern.replaceAll( "\\.\\+", "%" );
        likePattern = likePattern.replaceAll( "\\.", "_" );

        if ( log.isDebugEnabled() )
        {
            log.debug( "pattern = [" + likePattern + "]" );
        }

        return likePattern;
    }

    /**
     * This delegates to the generic JDBC disk cache. If we are currently optimizing, then this
     * method will balk and do nothing.
     * <p>
     * @param element
     */
    @Override
    protected void processUpdate( ICacheElement<K, V> element )
    {
        if ( this.getTableState().getState() == TableState.OPTIMIZATION_RUNNING )
        {
            if ( this.mySQLDiskCacheAttributes.isBalkDuringOptimization() )
            {
                return;
            }
        }
        super.processUpdate( element );
    }

    /**
     * Removed the expired. (now - create time) &gt; max life seconds * 1000
     * <p>
     * If we are currently optimizing, then this method will balk and do nothing.
     * <p>
     * TODO consider blocking and trying again.
     * <p>
     * @return the number deleted
     */
    @Override
    protected int deleteExpired()
    {
        if ( this.getTableState().getState() == TableState.OPTIMIZATION_RUNNING )
        {
            if ( this.mySQLDiskCacheAttributes.isBalkDuringOptimization() )
            {
                return -1;
            }
        }
        return super.deleteExpired();
    }
}
