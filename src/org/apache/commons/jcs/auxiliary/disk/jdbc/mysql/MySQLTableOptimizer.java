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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.jcs.auxiliary.disk.jdbc.TableState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The MySQL Table Optimizer can optimize MySQL tables. It knows how to optimize for MySQL databases
 * in particular and how to repair the table if it is corrupted in the process.
 * <p>
 * We will probably be able to abstract out a generic optimizer interface from this class in the
 * future.
 * <p>
 * @author Aaron Smuts
 */
public class MySQLTableOptimizer
{
    /** The logger */
    private static final Log log = LogFactory.getLog( MySQLTableOptimizer.class );

    /** The data source */
    private DataSource dataSource = null;

    /** The name of the table. */
    private String tableName = null;

    /** optimizing, etc. */
    private TableState tableState;

    /**
     * This constructs an optimizer with the disk cacn properties.
     * <p>
     * @param attributes
     * @param tableState We mark the table status as optimizing when this is happening.
     * @param dataSource access to the database
     */
    public MySQLTableOptimizer( MySQLDiskCacheAttributes attributes, TableState tableState, DataSource dataSource )
    {
        setTableName( attributes.getTableName() );

        this.tableState = tableState;
        this.dataSource = dataSource;
    }

    /**
     * A scheduler will call this method. When it is called the table state is marked as optimizing.
     * TODO we need to verify that no deletions are running before we call optimize. We should wait
     * if a deletion is in progress.
     * <p>
     * This restores when there is an optimization error. The error output looks like this:
     *
     * <pre>
     *           mysql&gt; optimize table JCS_STORE_FLIGHT_OPTION_ITINERARY;
     *               +---------------------------------------------+----------+----------+---------------------+
     *               | Table                                       | Op       | Msg_type | Msg_text            |
     *               +---------------------------------------------+----------+----------+---------------------+
     *               | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | optimize | error    | 2 when fixing table |
     *               | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | optimize | status   | Operation failed    |
     *               +---------------------------------------------+----------+----------+---------------------+
     *               2 rows in set (51.78 sec)
     * </pre>
     *
     * A successful repair response looks like this:
     *
     * <pre>
     *        mysql&gt; REPAIR TABLE JCS_STORE_FLIGHT_OPTION_ITINERARY;
     *            +---------------------------------------------+--------+----------+----------------------------------------------+
     *            | Table                                       | Op     | Msg_type | Msg_text                                     |
     *            +---------------------------------------------+--------+----------+----------------------------------------------+
     *            | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | repair | error    | 2 when fixing table                          |
     *            | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | repair | warning  | Number of rows changed from 131276 to 260461 |
     *            | jcs_cache.JCS_STORE_FLIGHT_OPTION_ITINERARY | repair | status   | OK                                           |
     *            +---------------------------------------------+--------+----------+----------------------------------------------+
     *            3 rows in set (3 min 5.94 sec)
     * </pre>
     *
     * A successful optimization looks like this:
     *
     * <pre>
     *       mysql&gt; optimize table JCS_STORE_DEFAULT;
     *           +-----------------------------+----------+----------+----------+
     *           | Table                       | Op       | Msg_type | Msg_text |
     *           +-----------------------------+----------+----------+----------+
     *           | jcs_cache.JCS_STORE_DEFAULT | optimize | status   | OK       |
     *           +-----------------------------+----------+----------+----------+
     *           1 row in set (1.10 sec)
     * </pre>
     * @return true if it worked
     */
    public boolean optimizeTable()
    {
        long start = System.currentTimeMillis();
        boolean success = false;

        if ( tableState.getState() == TableState.OPTIMIZATION_RUNNING )
        {
            log
                .warn( "Skipping optimization.  Optimize was called, but the table state indicates that an optimization is currently running." );
            return false;
        }

        try
        {
            tableState.setState( TableState.OPTIMIZATION_RUNNING );
            if ( log.isInfoEnabled() )
            {
                log.info( "Optimizing table [" + this.getTableName() + "]" );
            }

            try (Connection con = dataSource.getConnection())
            {
                // TEST

                try (Statement sStatement = con.createStatement())
                {
                    ResultSet rs = sStatement.executeQuery( "optimize table " + this.getTableName() );

                    // first row is error, then status
                    // if there is only one row in the result set, everything
                    // should be fine.
                    // This may be mysql version specific.
                    if ( rs.next() )
                    {
                        String status = rs.getString( "Msg_type" );
                        String message = rs.getString( "Msg_text" );

                        if ( log.isInfoEnabled() )
                        {
                            log.info( "Message Type: " + status );
                            log.info( "Message: " + message );
                        }

                        if ( "error".equals( status ) )
                        {
                            log.warn( "Optimization was in error. Will attempt to repair the table. Message: "
                                + message );

                            // try to repair the table.
                            success = repairTable( sStatement );
                        }
                        else
                        {
                            success = true;
                        }
                    }

                    // log the table status
                    String statusString = getTableStatus( sStatement );
                    if ( log.isInfoEnabled() )
                    {
                        log.info( "Table status after optimizing table [" + this.getTableName() + "]\n" + statusString );
                    }
                }
                catch ( SQLException e )
                {
                    log.error( "Problem optimizing table [" + this.getTableName() + "]", e );
                    return false;
                }
            }
            catch ( SQLException e )
            {
                log.error( "Problem getting connection.", e );
            }
        }
        finally
        {
            tableState.setState( TableState.FREE );

            long end = System.currentTimeMillis();
            if ( log.isInfoEnabled() )
            {
                log.info( "Optimization of table [" + this.getTableName() + "] took " + ( end - start ) + " ms." );
            }
        }

        return success;
    }

    /**
     * This calls show table status and returns the result as a String.
     * <p>
     * @param sStatement
     * @return String
     * @throws SQLException
     */
    protected String getTableStatus( Statement sStatement )
        throws SQLException
    {
        ResultSet statusResultSet = sStatement.executeQuery( "show table status" );
        StringBuilder statusString = new StringBuilder();
        int numColumns = statusResultSet.getMetaData().getColumnCount();
        while ( statusResultSet.next() )
        {
            statusString.append( "\n" );
            for ( int i = 1; i <= numColumns; i++ )
            {
                statusString.append( statusResultSet.getMetaData().getColumnLabel( i ) + " ["
                    + statusResultSet.getString( i ) + "]  |  " );
            }
        }
        return statusString.toString();
    }

    /**
     * This is called if the optimization is in error.
     * <p>
     * It looks for "OK" in response. If it find "OK" as a message in any result set row, it returns
     * true. Otherwise we assume that the repair failed.
     * <p>
     * @param sStatement
     * @return true if successful
     * @throws SQLException
     */
    protected boolean repairTable( Statement sStatement )
        throws SQLException
    {
        boolean success = false;

        // if( message != null && message.indexOf( ) )
        ResultSet repairResult = sStatement.executeQuery( "repair table " + this.getTableName() );
        StringBuilder repairString = new StringBuilder();
        int numColumns = repairResult.getMetaData().getColumnCount();
        while ( repairResult.next() )
        {
            for ( int i = 1; i <= numColumns; i++ )
            {
                repairString.append( repairResult.getMetaData().getColumnLabel( i ) + " [" + repairResult.getString( i )
                    + "]  |  " );
            }

            String message = repairResult.getString( "Msg_text" );
            if ( "OK".equals( message ) )
            {
                success = true;
            }
        }
        if ( log.isInfoEnabled() )
        {
            log.info( repairString );
        }

        if ( !success )
        {
            log.warn( "Failed to repair the table. " + repairString );
        }
        return success;
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
}
