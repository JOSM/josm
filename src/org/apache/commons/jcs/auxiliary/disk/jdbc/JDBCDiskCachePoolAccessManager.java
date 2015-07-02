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
import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages JDBCDiskCachePoolAccess instances. If a connectionPoolName value is supplied, the JDBC
 * disk cache will try to use this manager to create a pool. Assuming the name is "MyPool":
 *
 * <pre>
 * jcs.jdbcconnectionpool.MyPool.attributes.userName=MyUserName
 * jcs.jdbcconnectionpool.MyPool.attributes.password=MyPassword
 * jcs.jdbcconnectionpool.MyPool.attributes.url=MyUrl
 * jcs.jdbcconnectionpool.MyPool.attributes.maxActive=MyMaxActive
 * jcs.jdbcconnectionpool.MyPool.attributes.driverClassName=MyDriverClassName
 * </pre>
 */
public class JDBCDiskCachePoolAccessManager
{
    /** Singleton instance */
    private static JDBCDiskCachePoolAccessManager instance;

    /** Pool name to JDBCDiskCachePoolAccess */
    private final Map<String, JDBCDiskCachePoolAccess> pools = new HashMap<String, JDBCDiskCachePoolAccess>();

    /** props prefix */
    public static final String POOL_CONFIGURATION_PREFIX = "jcs.jdbcconnectionpool.";

    /** .attributes */
    public static final String ATTRIBUTE_PREFIX = ".attributes";

    /** The logger. */
    private static final Log log = LogFactory.getLog( JDBCDiskCachePoolAccessManager.class );

    /**
     * Singleton, private
     */
    private JDBCDiskCachePoolAccessManager()
    {
        // empty
    }

    /**
     * returns a singleton instance
     * <p>
     * @return JDBCDiskCachePoolAccessManager
     */
    public static synchronized JDBCDiskCachePoolAccessManager getInstance()
    {
        if ( instance == null )
        {
            instance = new JDBCDiskCachePoolAccessManager();
        }
        return instance;
    }

    /**
     * Returns a pool for the name if one has been created. Otherwise it creates a pool.
     * <p>
     * @param poolName the name of the pool
     * @param props the configuration properties for the pool
     * @return JDBCDiskCachePoolAccess
     * @throws SQLException if a database access error occurs
     */
    public synchronized JDBCDiskCachePoolAccess getJDBCDiskCachePoolAccess( String poolName, Properties props )
        throws SQLException
    {
        JDBCDiskCachePoolAccess poolAccess = pools.get( poolName );

        if ( poolAccess == null )
        {
            JDBCDiskCachePoolAccessAttributes poolAttributes = configurePoolAccessAttributes( poolName, props );
            poolAccess = JDBCDiskCachePoolAccessManager.createPoolAccess( poolAttributes );

            if ( log.isInfoEnabled() )
            {
                log.info( "Created shared pooled access for pool name [" + poolName + "]." );
            }
            pools.put( poolName, poolAccess );
        }

        return poolAccess;
    }



    /**
     * Configures the attributes using the properties.
     * <p>
     * @param poolName the name of the pool
     * @param props the configuration properties for the pool
     * @return JDBCDiskCachePoolAccessAttributes
     */
    protected JDBCDiskCachePoolAccessAttributes configurePoolAccessAttributes( String poolName, Properties props )
    {
        JDBCDiskCachePoolAccessAttributes poolAttributes = new JDBCDiskCachePoolAccessAttributes();

        String poolAccessAttributePrefix = POOL_CONFIGURATION_PREFIX + poolName + ATTRIBUTE_PREFIX;
        PropertySetter.setProperties( poolAttributes, props, poolAccessAttributePrefix + "." );

        poolAttributes.setPoolName( poolName );

        if ( log.isInfoEnabled() )
        {
            log.info( "Configured attributes " + poolAttributes );
        }
        return poolAttributes;
    }

    /**
     * Creates a pool access object and registers the driver.
     * <p>
     * @param driverClassName
     * @param poolName
     * @param fullURL = (url + database)
     * @param userName
     * @param password
     * @param maxActive
     * @return JDBCDiskCachePoolAccess
     * @throws SQLException if a database access error occurs
     */
    public static JDBCDiskCachePoolAccess createPoolAccess( String driverClassName, String poolName, String fullURL,
                                                            String userName, String password, int maxActive )
       throws SQLException
    {
        JDBCDiskCachePoolAccess poolAccess = null;

        if (driverClassName == null)
        {
            throw new SQLException("Driver class name is null");
        }

        try
        {
            // com.mysql.jdbc.Driver
            Class.forName( driverClassName );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SQLException("Couldn't find class for driver [" + driverClassName + "]", e );
        }

        poolAccess = new JDBCDiskCachePoolAccess( poolName );
        poolAccess.setupDriver( fullURL, userName, password, maxActive );
        poolAccess.logDriverStats();

        if ( log.isInfoEnabled() )
        {
            log.info( "Created: " + poolAccess );
        }

        return poolAccess;
    }

    /**
     * Creates a JDBCDiskCachePoolAccess object from the JDBCDiskCacheAttributes. Use this when not
     * using the connection pool manager.
     * <p>
     * @param cattr
     * @return JDBCDiskCachePoolAccess
     * @throws SQLException if a database access error occurs
     */
    public static JDBCDiskCachePoolAccess createPoolAccess( JDBCDiskCacheAttributes cattr )
        throws SQLException
    {
        return JDBCDiskCachePoolAccessManager.createPoolAccess( cattr.getDriverClassName(), cattr.getName(), cattr.getUrl() + cattr.getDatabase(),
                                 cattr.getUserName(), cattr.getPassword(), cattr.getMaxActive() );
    }

    /**
     * Creates a JDBCDiskCachePoolAccess object from the JDBCDiskCachePoolAccessAttributes. This is
     * used by the connection pool manager.
     * <p>
     * @param poolAttributes
     * @return JDBCDiskCachePoolAccess
     * @throws SQLException if a database access error occurs
     */
    public static JDBCDiskCachePoolAccess createPoolAccess( JDBCDiskCachePoolAccessAttributes poolAttributes )
        throws SQLException
    {
        return JDBCDiskCachePoolAccessManager.createPoolAccess( poolAttributes.getDriverClassName(), poolAttributes.getPoolName(), poolAttributes
            .getUrl()
            + poolAttributes.getDatabase(), poolAttributes.getUserName(), poolAttributes.getPassword(), poolAttributes
            .getMaxActive() );
    }
}
