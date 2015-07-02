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

/** These are used to configure the JDBCDiskCachePoolAccess class. */
public class JDBCDiskCachePoolAccessAttributes
{
    /** The name of the pool.  */
    private String poolName;

    /** URI to the db. */
    private String url;

    /** username for the db */
    private String userName;

    /** password for the database */
    private String password;

    /** This is the default limit on the maximum number of active connections. */
    public static final int DEFAULT_MAX_ACTIVE = 10;

    /** Max connections allowed */
    private int maxActive = DEFAULT_MAX_ACTIVE;

    /** The name of the database. */
    private String database = "";

    /** The driver */
    private String driverClassName;

    /**
     * @param poolName the poolName to set
     */
    public void setPoolName( String poolName )
    {
        this.poolName = poolName;
    }

    /**
     * @return the poolName
     */
    public String getPoolName()
    {
        return poolName;
    }

    /**
     * @param connectURI the connectURI to set
     */
    public void setUrl( String connectURI )
    {
        this.url = connectURI;
    }

    /**
     * @return the connectURI
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName( String userName )
    {
        this.userName = userName;
    }

    /**
     * @return the userName
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * @param password the password to set
     */
    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * @return the password
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param maxActive the maxActive to set
     */
    public void setMaxActive( int maxActive )
    {
        this.maxActive = maxActive;
    }

    /**
     * @return the maxActive
     */
    public int getMaxActive()
    {
        return maxActive;
    }

    /**
     * @param database the database to set
     */
    public void setDatabase( String database )
    {
        this.database = database;
    }

    /**
     * @return the database
     */
    public String getDatabase()
    {
        return database;
    }

    /**
     * @param driverClassName the driverClassName to set
     */
    public void setDriverClassName( String driverClassName )
    {
        this.driverClassName = driverClassName;
    }

    /**
     * @return the driverClassName
     */
    public String getDriverClassName()
    {
        return driverClassName;
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
        buf.append( "\nJDBCDiskCachePoolAccessAttributes" );
        buf.append( "\n UserName [" + getUserName() + "]" );
        buf.append( "\n Url [" + getUrl() + "]" );
        buf.append( "\n PoolName [" + getPoolName() + "]" );
        buf.append( "\n Database [" + getDatabase() + "]" );
        buf.append( "\n DriverClassName [" + getDriverClassName() + "]" );
        buf.append( "\n MaxActive [" + getMaxActive() + "]" );
        return buf.toString();
    }
}
