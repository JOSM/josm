package org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory;

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

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.InstanceKeyDataSource;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCacheAttributes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A factory that looks up the DataSource using the JDBC2 pool methods.
 *
 * Borrowed and adapted from Apache DB Torque
 *
 * @author <a href="mailto:jmcnally@apache.org">John McNally</a>
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 */
public class SharedPoolDataSourceFactory implements DataSourceFactory
{
    /** The log. */
    private static Log log = LogFactory.getLog(SharedPoolDataSourceFactory.class);

    /** The name of the factory. */
    private String name;

    /** The wrapped <code>DataSource</code>. */
    private SharedPoolDataSource ds = null;

    /**
     * @return the name of the factory.
     */
    @Override
	public String getName()
    {
    	return name;
    }

    /**
     * @see org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory.DataSourceFactory#getDataSource()
     */
    @Override
    public DataSource getDataSource()
    {
        return ds;
    }

    /**
     * @see org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory.DataSourceFactory#initialize(JDBCDiskCacheAttributes)
     */
    @Override
	public void initialize(JDBCDiskCacheAttributes config) throws SQLException
    {
    	this.name = config.getConnectionPoolName();
        ConnectionPoolDataSource cpds = initCPDS(config);
        SharedPoolDataSource dataSource = new SharedPoolDataSource();
        initJdbc2Pool(dataSource, config);
        dataSource.setConnectionPoolDataSource(cpds);
        dataSource.setMaxTotal(config.getMaxTotal());
        this.ds = dataSource;
    }

    /**
     * Closes the pool associated with this factory and releases it.
     * @throws SQLException if the pool cannot be closed properly
     */
    @Override
	public void close() throws SQLException
    {
        try
        {
            ds.close();
        }
        catch (Exception e)
        {
        	throw new SQLException("Exception caught closing data source", e);
        }
        ds = null;
    }

    /**
     * Initializes the ConnectionPoolDataSource.
     *
     * @param config where to read the settings from
     * @throws SQLException if a property set fails
     * @return a configured <code>ConnectionPoolDataSource</code>
     */
    private ConnectionPoolDataSource initCPDS(final JDBCDiskCacheAttributes config)
        throws SQLException
    {
        log.debug("Starting initCPDS");

        DriverAdapterCPDS cpds = new DriverAdapterCPDS();

        try
        {
			cpds.setDriver(config.getDriverClassName());
		}
        catch (ClassNotFoundException e)
        {
			throw new SQLException("Driver class not found " + config.getDriverClassName(), e);
		}

        cpds.setUrl(config.getUrl());
        cpds.setUser(config.getUserName());
        cpds.setPassword(config.getPassword());

        return cpds;
    }

    /**
     * Initializes the Jdbc2PoolDataSource.
     *
     * @param dataSource the dataSource to initialize, not null.
     * @param config where to read the settings from, not null.
     *
     * @throws SQLException if a property set fails.
     */
    private void initJdbc2Pool(final InstanceKeyDataSource dataSource, final JDBCDiskCacheAttributes config)
        throws SQLException
    {
        log.debug("Starting initJdbc2Pool");

        dataSource.setDescription(config.getConnectionPoolName());
    }
}
