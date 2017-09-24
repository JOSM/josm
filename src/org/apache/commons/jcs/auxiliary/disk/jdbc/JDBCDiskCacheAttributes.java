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

import org.apache.commons.jcs.auxiliary.disk.AbstractDiskCacheAttributes;

/**
 * The configurator will set these values based on what is in the cache.ccf file.
 * <p>
 * @author Aaron Smuts
 */
public class JDBCDiskCacheAttributes
    extends AbstractDiskCacheAttributes
{
    /** Don't change */
    private static final long serialVersionUID = -6535808344813320062L;

    /** default */
    private static final String DEFAULT_TABLE_NAME = "JCS_STORE";

    /** DB username */
    private String userName;

    /** DB password */
    private String password;

    /** URL for the db */
    private String url;

    /** The name of the database. */
    private String database = "";

    /** The driver */
    private String driverClassName;

    /** The JNDI path. */
    private String jndiPath;

    /** The time between two JNDI lookups */
    private long jndiTTL = 0L;

    /** The table name */
    private String tableName = DEFAULT_TABLE_NAME;

    /** If false we will insert and if it fails we will update. */
    private boolean testBeforeInsert = true;

    /** This is the default limit on the maximum number of active connections. */
    public static final int DEFAULT_MAX_TOTAL = 10;

    /** Max connections allowed */
    private int maxTotal = DEFAULT_MAX_TOTAL;

    /** This is the default setting for the cleanup routine. */
    public static final int DEFAULT_SHRINKER_INTERVAL_SECONDS = 300;

    /** How often should we remove expired. */
    private int shrinkerIntervalSeconds = DEFAULT_SHRINKER_INTERVAL_SECONDS;

    /** Should we remove expired in the background. */
    private boolean useDiskShrinker = true;

    /** The default Pool Name to which the connection pool will be keyed. */
    public static final String DEFAULT_POOL_NAME = "jcs";

    /**
     * If a pool name is supplied, the manager will attempt to load it. It should be configured in a
     * separate section as follows. Assuming the name is "MyPool":
     *
     * <pre>
     * jcs.jdbcconnectionpool.MyPool.attributes.userName=MyUserName
     * jcs.jdbcconnectionpool.MyPool.attributes.password=MyPassword
     * jcs.jdbcconnectionpool.MyPool.attributes.url=MyUrl
     * jcs.jdbcconnectionpool.MyPool.attributes.maxActive=MyMaxActive
     * jcs.jdbcconnectionpool.MyPool.attributes.driverClassName=MyDriverClassName
     * </pre>
     */
    private String connectionPoolName;

    /**
     * @param userName The userName to set.
     */
    public void setUserName( String userName )
    {
        this.userName = userName;
    }

    /**
     * @return Returns the userName.
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * @param password The password to set.
     */
    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * @return Returns the password.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param url The url to set.
     */
    public void setUrl( String url )
    {
        this.url = url;
    }

    /**
     * @return Returns the url.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * This is appended to the url.
     * @param database The database to set.
     */
    public void setDatabase( String database )
    {
        this.database = database;
    }

    /**
     * @return Returns the database.
     */
    public String getDatabase()
    {
        return database;
    }

    /**
     * @param driverClassName The driverClassName to set.
     */
    public void setDriverClassName( String driverClassName )
    {
        this.driverClassName = driverClassName;
    }

    /**
     * @return Returns the driverClassName.
     */
    public String getDriverClassName()
    {
        return driverClassName;
    }

    /**
	 * @return the jndiPath
	 */
	public String getJndiPath()
	{
		return jndiPath;
	}

	/**
	 * @param jndiPath the jndiPath to set
	 */
	public void setJndiPath(String jndiPath)
	{
		this.jndiPath = jndiPath;
	}

	/**
	 * @return the jndiTTL
	 */
	public long getJndiTTL()
	{
		return jndiTTL;
	}

	/**
	 * @param jndiTTL the jndiTTL to set
	 */
	public void setJndiTTL(long jndiTTL)
	{
		this.jndiTTL = jndiTTL;
	}

	/**
     * @param tableName The tableName to set.
     */
    public void setTableName( String tableName )
    {
        this.tableName = tableName;
    }

    /**
     * @return Returns the tableName.
     */
    public String getTableName()
    {
        return tableName;
    }

    /**
     * If this is true then the disk cache will check to see if the item already exists in the
     * database. If it is false, it will try to insert. If the insert fails it will try to update.
     * <p>
     * @param testBeforeInsert The testBeforeInsert to set.
     */
    public void setTestBeforeInsert( boolean testBeforeInsert )
    {
        this.testBeforeInsert = testBeforeInsert;
    }

    /**
     * @return Returns the testBeforeInsert.
     */
    public boolean isTestBeforeInsert()
    {
        return testBeforeInsert;
    }

    /**
     * @param maxTotal The maxTotal to set.
     */
    public void setMaxTotal( int maxActive )
    {
        this.maxTotal = maxActive;
    }

    /**
     * @return Returns the maxTotal.
     */
    public int getMaxTotal()
    {
        return maxTotal;
    }

    /**
     * @param shrinkerIntervalSecondsArg The shrinkerIntervalSeconds to set.
     */
    public void setShrinkerIntervalSeconds( int shrinkerIntervalSecondsArg )
    {
        this.shrinkerIntervalSeconds = shrinkerIntervalSecondsArg;
    }

    /**
     * @return Returns the shrinkerIntervalSeconds.
     */
    public int getShrinkerIntervalSeconds()
    {
        return shrinkerIntervalSeconds;
    }

    /**
     * @param useDiskShrinker The useDiskShrinker to set.
     */
    public void setUseDiskShrinker( boolean useDiskShrinker )
    {
        this.useDiskShrinker = useDiskShrinker;
    }

    /**
     * @return Returns the useDiskShrinker.
     */
    public boolean isUseDiskShrinker()
    {
        return useDiskShrinker;
    }

    /**
     * @param connectionPoolName the connectionPoolName to set
     */
    public void setConnectionPoolName( String connectionPoolName )
    {
        this.connectionPoolName = connectionPoolName;
    }

    /**
     * @return the connectionPoolName
     */
    public String getConnectionPoolName()
    {
        return connectionPoolName;
    }

    /**
     * For debugging.
     * <p>
     * @return debug string with most of the properties.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nJDBCCacheAttributes" );
        buf.append( "\n UserName [" + getUserName() + "]" );
        buf.append( "\n Url [" + getUrl() + "]" );
        buf.append( "\n Database [" + getDatabase() + "]" );
        buf.append( "\n DriverClassName [" + getDriverClassName() + "]" );
        buf.append( "\n TableName [" + getTableName() + "]" );
        buf.append( "\n TestBeforeInsert [" + isTestBeforeInsert() + "]" );
        buf.append( "\n MaxActive [" + getMaxTotal() + "]" );
        buf.append( "\n AllowRemoveAll [" + isAllowRemoveAll() + "]" );
        buf.append( "\n ShrinkerIntervalSeconds [" + getShrinkerIntervalSeconds() + "]" );
        buf.append( "\n useDiskShrinker [" + isUseDiskShrinker() + "]" );
        return buf.toString();
    }
}
