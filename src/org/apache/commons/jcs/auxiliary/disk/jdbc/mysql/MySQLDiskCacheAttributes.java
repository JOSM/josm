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

import org.apache.commons.jcs.auxiliary.disk.jdbc.JDBCDiskCacheAttributes;

/**
 * This has additional attributes that are particular to the MySQL disk cache.
 * <p>
 * @author Aaron Smuts
 */
public class MySQLDiskCacheAttributes
    extends JDBCDiskCacheAttributes
{
    /** Don't change. */
    private static final long serialVersionUID = -6535808344813320061L;

    /**
     * For now this is a simple comma delimited list of HH:MM:SS times to optimize
     * the table. If none is supplied, then no optimizations will be performed.
     * <p>
     * In the future we can add a chron like scheduling system. This is to meet
     * a pressing current need.
     * <p>
     * 03:01,15:00 will cause the optimizer to run at 3 am and at 3 pm.
     */
    private String optimizationSchedule = null;

    /**
     * If true, we will balk, that is return null during optimization rather than block.
     */
    public static final boolean DEFAULT_BALK_DURING_OPTIMIZATION = true;

    /**
     * If true, we will balk, that is return null during optimization rather than block.
     * <p>
     * <a href="http://en.wikipedia.org/wiki/Balking_pattern">Balking</a>
     */
    private boolean balkDuringOptimization = DEFAULT_BALK_DURING_OPTIMIZATION;

    /**
     * @param optimizationSchedule The optimizationSchedule to set.
     */
    public void setOptimizationSchedule( String optimizationSchedule )
    {
        this.optimizationSchedule = optimizationSchedule;
    }

    /**
     * @return Returns the optimizationSchedule.
     */
    public String getOptimizationSchedule()
    {
        return optimizationSchedule;
    }

    /**
     * @param balkDuringOptimization The balkDuringOptimization to set.
     */
    public void setBalkDuringOptimization( boolean balkDuringOptimization )
    {
        this.balkDuringOptimization = balkDuringOptimization;
    }

    /**
     * Should we return null while optimizing the table.
     * <p>
     * @return Returns the balkDuringOptimization.
     */
    public boolean isBalkDuringOptimization()
    {
        return balkDuringOptimization;
    }

    /**
     * For debugging.
     * <p>
     * @return debug string
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nMySQLDiskCacheAttributes" );
        buf.append( "\n OptimizationSchedule [" + getOptimizationSchedule() + "]" );
        buf.append( "\n BalkDuringOptimization [" + isBalkDuringOptimization() + "]" );
        buf.append( super.toString() );
        return buf.toString();
    }
}
