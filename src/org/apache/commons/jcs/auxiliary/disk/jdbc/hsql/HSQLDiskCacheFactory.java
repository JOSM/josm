package org.apache.commons.jcs.auxiliary.disk.jdbc.hsql;

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

import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCacheManager;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This factory should create mysql disk caches.
 * <p>
 * @author Aaron Smuts
 */
public class HSQLDiskCacheFactory
    implements AuxiliaryCacheFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( HSQLDiskCacheFactory.class );

    /** Name for logging, etc. */
    private String name = "HSQLDiskCacheFactory";

    /** The databases. */
    private final Set<String> databases = Collections.synchronizedSet( new HashSet<String>() );

    /**
     * This factory method should create an instance of the hsqlcache.
     * <p>
     * @param rawAttr
     * @param cacheManager
     * @param cacheEventLogger
     * @param elementSerializer
     * @return AuxiliaryCache
     */
    @Override
    public <K, V> AuxiliaryCache<K, V> createCache( AuxiliaryCacheAttributes rawAttr,
    									ICompositeCacheManager cacheManager,
    									ICacheEventLogger cacheEventLogger,
    									IElementSerializer elementSerializer )
    {
        JDBCDiskCacheManager mgr = JDBCDiskCacheManager.getInstance( (JDBCDiskCacheAttributes) rawAttr, cacheManager,
                                                                     cacheEventLogger, elementSerializer );
        try
        {
            setupDatabase( (JDBCDiskCacheAttributes) rawAttr );
        }
        catch ( Exception e )
        {
            // TODO we may not want to try and get the cache at this point.
            log.error( "Problem setting up database.", e );
        }
        return mgr.getCache( (JDBCDiskCacheAttributes) rawAttr );
    }

    /**
     * The name of the factory.
     * <p>
     * @param nameArg
     */
    @Override
    public void setName( String nameArg )
    {
        name = nameArg;
    }

    /**
     * Returns the display name
     * <p>
     * @return name
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Creates the database if it doesn't exist, registers the driver class, etc.
     * <p>
     * @param attributes
     * @throws Exception
     */
    protected void setupDatabase( JDBCDiskCacheAttributes attributes )
        throws Exception
    {
        if ( attributes == null )
        {
            throw new Exception( "The attributes are null." );
        }

        // url should start with "jdbc:hsqldb:"
        String database = attributes.getUrl() + attributes.getDatabase();

        if ( databases.contains( database ) )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "We already setup database [" + database + "]" );
            }
            return;
        }

        // TODO get this from the attributes.
        System.setProperty( "hsqldb.cache_scale", "8" );

        // "org.hsqldb.jdbcDriver"
        String driver = attributes.getDriverClassName();
        // "sa"
        String user = attributes.getUserName();
        // ""
        String password = attributes.getPassword();

        new org.hsqldb.jdbcDriver();
        try
        {
            Class.forName( driver ).newInstance();

            Connection cConn = DriverManager.getConnection( database, user, password );

            setupTABLE( cConn, attributes.getTableName() );

            if ( log.isInfoEnabled() )
            {
                log.info( "Finished setting up database [" + database + "]" );
            }

            databases.add( database );
        }
        catch ( Exception e )
        {
            log.error( "Fatal problem setting up the database.", e );
        }
    }

    /**
     * SETUP TABLE FOR CACHE
     * <p>
     * @param cConn
     * @param tableName
     */
    private void setupTABLE( Connection cConn, String tableName ) throws SQLException
    {
        boolean newT = true;

        // TODO make the cached nature of the table configurable
        StringBuilder createSql = new StringBuilder();
        createSql.append( "CREATE CACHED TABLE " + tableName );
        createSql.append( "( " );
        createSql.append( "CACHE_KEY             VARCHAR(250)          NOT NULL, " );
        createSql.append( "REGION                VARCHAR(250)          NOT NULL, " );
        createSql.append( "ELEMENT               BINARY, " );
        createSql.append( "CREATE_TIME           DATE, " );
        createSql.append( "CREATE_TIME_SECONDS   BIGINT, " );
        createSql.append( "MAX_LIFE_SECONDS      BIGINT, " );
        createSql.append( "SYSTEM_EXPIRE_TIME_SECONDS      BIGINT, " );
        createSql.append( "IS_ETERNAL            CHAR(1), " );
        createSql.append( "PRIMARY KEY (CACHE_KEY, REGION) " );
        createSql.append( ");" );

        Statement sStatement = cConn.createStatement();

        try
        {
            sStatement.executeQuery( createSql.toString() );
            sStatement.close();
        }
        catch ( SQLException e )
        {
            // FIXME: This is not reliable
            if ( e.toString().indexOf( "already exists" ) != -1 )
            {
                newT = false;
            }
            else
            {
                throw e;
            }
        }

        // TODO create an index on SYSTEM_EXPIRE_TIME_SECONDS
        String setupData[] = { "create index iKEY on " + tableName + " (CACHE_KEY, REGION)" };

        if ( newT )
        {
            for ( int i = 1; i < setupData.length; i++ )
            {
                try
                {
                    sStatement.executeQuery( setupData[i] );
                }
                catch ( SQLException e )
                {
                    log.error( "Exception caught when creating index." + e );
                }
            }
        }
    }
}
