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

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class provides access to the connection pool. It ensures that the various resources that
 * need to access the tables will be able to use the same pool.
 * <p>
 * @author Aaron Smuts
 */
public class JDBCDiskCachePoolAccess
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( JDBCDiskCachePoolAccess.class );

    /** The default Pool Name to which the connection pool will be keyed. */
    public static final String DEFAULT_POOL_NAME = "jcs";

    /** The name of the pool. */
    private String poolName = DEFAULT_POOL_NAME;

    /** default jdbc driver. */
    private static final String DRIVER_NAME = "jdbc:apache:commons:dbcp:";

    /**
     * Configures the pool name to use for the pool access.
     * <p>
     * This pool name should be unique to the database. It is used as part of the URL each time we
     * lookup a connection from the driver manager.
     * <p>
     * @param poolName
     */
    public JDBCDiskCachePoolAccess( String poolName )
    {
        // we can default to jcs if there is only one database in use.
        if ( poolName != null )
        {
            setPoolName( poolName );
        }
        else
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "The pool name supplied was null. Using default instead." );
            }
        }
    }

    /**
     * Gets a connection from the pool.
     * <p>
     * @return Connection
     * @throws SQLException
     */
    public Connection getConnection()
        throws SQLException
    {
        Connection con;
        try
        {
            con = DriverManager.getConnection( getPoolUrl() );
        }
        catch ( SQLException e )
        {
            log.error( "Problem getting connection.", e );
            throw e;
        }

        return con;
    }

    /**
     * How many are idle in the pool.
     * <p>
     * @return number idle
     */
    public int getNumIdleInPool()
    {
        int numIdle = 0;
        try
        {
            PoolingDriver driver = (PoolingDriver) DriverManager.getDriver( DRIVER_NAME );
            ObjectPool connectionPool = driver.getConnectionPool( this.getPoolName() );

            if ( log.isDebugEnabled() )
            {
                log.debug( connectionPool );
            }
            numIdle = connectionPool.getNumIdle();
        }
        catch ( SQLException e )
        {
            log.error( e );
        }
        return numIdle;
    }

    /**
     * How many are active in the pool.
     * <p>
     * @return number active
     */
    public int getNumActiveInPool()
    {
        int numActive = 0;
        try
        {
            PoolingDriver driver = (PoolingDriver) DriverManager.getDriver( DRIVER_NAME );
            ObjectPool connectionPool = driver.getConnectionPool( this.getPoolName() );

            if ( log.isDebugEnabled() )
            {
                log.debug( connectionPool );
            }
            numActive = connectionPool.getNumActive();
        }
        catch ( SQLException e )
        {
            log.error( e );
        }
        return numActive;
    }

    /**
     * @throws Exception
     */
    public void shutdownDriver()
        throws SQLException
    {
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver( DRIVER_NAME );
        driver.closePool( this.getPoolName() );
    }

    /**
     * @return Returns the poolUrl.
     */
    public String getPoolUrl()
    {
        return DRIVER_NAME + this.getPoolName();
    }

    /**
     * @param poolName The poolName to set.
     */
    public void setPoolName( String poolName ) // TODO does it make sense to change the pool name after construction?
    {
        this.poolName = poolName;
    }

    /**
     * @return Returns the poolName.
     */
    public String getPoolName()
    {
        return poolName;
    }

    /**
     * @param connectURI
     * @param userName
     * @param password
     * @param maxActive max connections
     * @throws SQLException if a database access error occurs
     */
    public void setupDriver( String connectURI, String userName, String password, int maxActive )
        throws SQLException
    {
        // First, we'll need a ObjectPool that serves as the
        // actual pool of connections.
        // We'll use a GenericObjectPool instance, although
        // any ObjectPool implementation will suffice.
        ObjectPool connectionPool = new GenericObjectPool( null, maxActive );

        // TODO make configurable
        // By default the size is 8!!!!!!!
        ( (GenericObjectPool) connectionPool ).setMaxIdle( -1 );

        // Next, we'll create a ConnectionFactory that the
        // pool will use to create Connections.
        // We'll use the DriverManagerConnectionFactory,
        // using the connect string passed in the command line
        // arguments.
        // Properties props = new Properties();
        // props.setProperty( "user", userName );
        // props.setProperty( "password", password );
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory( connectURI, userName, password );

        // Now we'll create the PoolableConnectionFactory, which wraps
        // the "real" Connections created by the ConnectionFactory with
        // the classes that implement the pooling functionality.
        // PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory( connectionFactory, connectionPool, null, null, false, true );

        // Finally, we create the PoolingDriver itself...
        try
        {
            // com.mysql.jdbc.Driver
            Class.forName( "org.apache.commons.dbcp.PoolingDriver" );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SQLException("Couldn't find class for driver [org.apache.commons.dbcp.PoolingDriver]", e );
        }
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver( DRIVER_NAME );

        // ...and register our pool with it.
        driver.registerPool( this.getPoolName(), connectionPool );

        // Now we can just use the connect string
        // "jdbc:apache:commons:dbcp:jcs"
        // to access our pool of Connections.
    }

    /**
     * @throws Exception
     */
    public void logDriverStats()
        throws SQLException
    {
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver( DRIVER_NAME );
        ObjectPool connectionPool = driver.getConnectionPool( this.getPoolName() );

        if ( connectionPool != null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( connectionPool );
            }

            if ( log.isInfoEnabled() )
            {
                log.info( "NumActive: " + getNumActiveInPool() );
                log.info( "NumIdle: " + getNumIdleInPool() );
            }
        }
        else
        {
            log.warn( "Could not find pool." );
        }
    }
}
