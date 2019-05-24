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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCache;
import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCacheFactory;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This factory should create hsql disk caches.
 * <p>
 * @author Aaron Smuts
 */
public class HSQLDiskCacheFactory
    extends JDBCDiskCacheFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( HSQLDiskCacheFactory.class );

    /** The databases. */
    private Set<String> databases;

    /**
     * This factory method should create an instance of the hsqlcache.
     * <p>
     * @param rawAttr
     * @param compositeCacheManager
     * @param cacheEventLogger
     * @param elementSerializer
     * @return JDBCDiskCache
     * @throws SQLException if the creation of the cache instance fails
     */
    @Override
    public <K, V> JDBCDiskCache<K, V> createCache( AuxiliaryCacheAttributes rawAttr,
			ICompositeCacheManager compositeCacheManager,
			ICacheEventLogger cacheEventLogger,
			IElementSerializer elementSerializer )
			throws SQLException
    {
        setupDatabase( (JDBCDiskCacheAttributes) rawAttr );
        return super.createCache(rawAttr, compositeCacheManager, cacheEventLogger, elementSerializer);
    }

    /**
     * Initialize this factory
     */
    @Override
    public void initialize()
    {
        super.initialize();
        this.databases = Collections.synchronizedSet( new HashSet<>() );
    }

    /**
     * Creates the database if it doesn't exist, registers the driver class, etc.
     * <p>
     * @param attributes
     * @throws SQLException
     */
    protected void setupDatabase( JDBCDiskCacheAttributes attributes )
        throws SQLException
    {
        if ( attributes == null )
        {
            throw new SQLException( "The attributes are null." );
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

        try
        {
            Class.forName( driver ).newInstance();
        }
        catch (Exception e)
        {
            throw new SQLException( "Could not initialize driver " + driver, e );
        }

        Connection cConn = DriverManager.getConnection( database, user, password );
        setupTable( cConn, attributes.getTableName() );

        if ( log.isInfoEnabled() )
        {
            log.info( "Finished setting up database [" + database + "]" );
        }

        databases.add( database );
    }

    /**
     * SETUP TABLE FOR CACHE
     * <p>
     * @param cConn
     * @param tableName
     */
    protected void setupTable( Connection cConn, String tableName ) throws SQLException
    {
        // TODO make the cached nature of the table configurable
        StringBuilder createSql = new StringBuilder();
        createSql.append( "CREATE CACHED TABLE ").append( tableName );
        createSql.append( "( " );
        createSql.append( "CACHE_KEY             VARCHAR(250)          NOT NULL, " );
        createSql.append( "REGION                VARCHAR(250)          NOT NULL, " );
        createSql.append( "ELEMENT               BINARY, " );
        createSql.append( "CREATE_TIME           TIMESTAMP, " );
        createSql.append( "UPDATE_TIME_SECONDS   BIGINT, " );
        createSql.append( "MAX_LIFE_SECONDS      BIGINT, " );
        createSql.append( "SYSTEM_EXPIRE_TIME_SECONDS      BIGINT, " );
        createSql.append( "IS_ETERNAL            CHAR(1), " );
        createSql.append( "PRIMARY KEY (CACHE_KEY, REGION) " );
        createSql.append( ");" );

        try (Statement sStatement = cConn.createStatement())
        {
            sStatement.execute( createSql.toString() );
        }
        catch ( SQLException e )
        {
            if (!"23000".equals(e.getSQLState()))
            {
                throw e;
            }
        }
    }
}
