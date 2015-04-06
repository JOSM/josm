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

import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This manages instances of the jdbc disk cache. It maintains one for each region. One for all
 * regions would work, but this gives us more detailed stats by region.
 */
public class JDBCDiskCacheManager
    extends JDBCDiskCacheManagerAbstractTemplate
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( JDBCDiskCacheManager.class );

    /** Singleton instance */
    private static JDBCDiskCacheManager instance;

    /** User configurable settings. */
    private final JDBCDiskCacheAttributes defaultJDBCDiskCacheAttributes;

    /** The cache manager instance */
    private ICompositeCacheManager compositeCacheManager;

    /**
     * Constructor for the HSQLCacheManager object
     * <p>
     * @param cattr
     * @param compositeCacheManager
     * @param cacheEventLogger
     * @param elementSerializer
     */
    private JDBCDiskCacheManager( JDBCDiskCacheAttributes cattr, ICompositeCacheManager compositeCacheManager, ICacheEventLogger cacheEventLogger,
          IElementSerializer elementSerializer )
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Creating JDBCDiskCacheManager with " + cattr );
        }
        defaultJDBCDiskCacheAttributes = cattr;
        setElementSerializer( elementSerializer );
        setCacheEventLogger( cacheEventLogger );
        setCompositeCacheManager( compositeCacheManager );
    }

    /**
     * Gets the defaultCattr attribute of the HSQLCacheManager object
     * <p>
     * @return The defaultCattr value
     */
    public JDBCDiskCacheAttributes getDefaultJDBCDiskCacheAttributes()
    {
        return defaultJDBCDiskCacheAttributes;
    }

    /**
     * Gets the instance attribute of the HSQLCacheManager class
     * <p>
     * @param cattr
     * @param compositeCacheManager
     * @param cacheEventLogger
     * @param elementSerializer
     * @return The instance value
     */
    public static JDBCDiskCacheManager getInstance( JDBCDiskCacheAttributes cattr, ICompositeCacheManager compositeCacheManager, ICacheEventLogger cacheEventLogger,
        IElementSerializer elementSerializer )
    {
        synchronized ( JDBCDiskCacheManager.class )
        {
            if ( instance == null )
            {
                instance = new JDBCDiskCacheManager( cattr, compositeCacheManager, cacheEventLogger, elementSerializer );
            }
        }
        clients++;
        return instance;
    }

    /**
     * Gets the cache attribute of the HSQLCacheManager object
     * <p>
     * @param cacheName
     * @return The cache value
     */
    @Override
    public <K, V> JDBCDiskCache<K, V> getCache( String cacheName )
    {
        JDBCDiskCacheAttributes cattr = (JDBCDiskCacheAttributes) defaultJDBCDiskCacheAttributes.copy();
        cattr.setCacheName( cacheName );
        return getCache( cattr );
    }

    /**
     * Creates a JDBCDiskCache using the supplied attributes.
     * <p>
     * @param cattr
     * @param tableState
     * @return AuxiliaryCache
     * @throws SQLException if database operations fail
     */
    @Override
    protected <K, V> JDBCDiskCache<K, V> createJDBCDiskCache( JDBCDiskCacheAttributes cattr, TableState tableState ) throws SQLException
    {
        JDBCDiskCache<K, V> raf;
        raf = new JDBCDiskCache<K, V>( cattr, tableState, getCompositeCacheManager() );
        return raf;
    }

    /**
     * @param compositeCacheManager the compositeCacheManager to set
     */
    protected void setCompositeCacheManager( ICompositeCacheManager compositeCacheManager )
    {
        this.compositeCacheManager = compositeCacheManager;
    }

    /**
     * @return the compositeCacheManager
     */
    protected ICompositeCacheManager getCompositeCacheManager()
    {
        return compositeCacheManager;
    }
}
