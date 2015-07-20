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

import java.io.Serializable;

/**
 * This is used by various elements of the JDBC disk cache to indicate the
 * status of a table. The MySQL disk cache, for instance, marks the status as
 * optimizing when a scheduled optimization is taking place. This allows the
 * cache to balk rather than block during long running optimizations.
 * <p>
 * @author Aaron Smuts
 */
public class TableState
    implements Serializable
{
    /** Don't change. */
    private static final long serialVersionUID = -6625081552084964885L;

    /** Name of the table whose state this reflects. */
    private String tableName;

    /**
     * The table is free. It can be accessed and no potentially table locking
     * jobs are running.
     */
    public static final int FREE = 0;

    /** A potentially table locking deletion is running */
    public static final int DELETE_RUNNING = 1;

    /** A table locking optimization is running. */
    public static final int OPTIMIZATION_RUNNING = 2;

    /** we might want to add error */
    private int state = FREE;

    /**
     * Construct a usable table state.
     * <p>
     * @param tableName
     */
    public TableState( String tableName )
    {
        this.setTableName( tableName );
    }

    /**
     * @param tableName
     *            The tableName to set.
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
     * @param state
     *            The state to set.
     */
    public void setState( int state )
    {
        this.state = state;
    }

    /**
     * @return Returns the state.
     */
    public int getState()
    {
        return state;
    }

    /**
     * Write out the values for debugging purposes.
     * <p>
     * @return String
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append( "TableState " );
        str.append( "\n TableName = " + getTableName() );
        str.append( "\n State = " + getState() );
        return str.toString();
    }
}
