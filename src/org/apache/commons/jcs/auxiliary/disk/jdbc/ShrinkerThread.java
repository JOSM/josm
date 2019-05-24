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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Calls delete expired on the disk caches. The shrinker is run by a clock daemon. The shrinker
 * calls delete on each region. It pauses between calls.
 * <p>
 * @author Aaron Smuts
 */
public class ShrinkerThread
    implements Runnable
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( ShrinkerThread.class );

    /** A set of JDBCDiskCache objects to call deleteExpired on. */
    private final Set<JDBCDiskCache<?, ?>> shrinkSet =
        Collections.synchronizedSet( new HashSet<>() );

    /** Default time period to use. */
    private static final long DEFAULT_PAUSE_BETWEEN_REGION_CALLS_MILLIS = 5000;

    /**
     * How long should we wait between calls to deleteExpired when we are iterating through the list
     * of regions. Delete can lock the table. We want to give clients a chance to get some work
     * done.
     */
    private long pauseBetweenRegionCallsMillis = DEFAULT_PAUSE_BETWEEN_REGION_CALLS_MILLIS;

    /**
     * Does nothing special.
     */
    protected ShrinkerThread()
    {
        super();
    }

    /**
     * Adds a JDBC disk cache to the set of disk cache to shrink.
     * <p>
     * @param diskCache
     */
    public void addDiskCacheToShrinkList( JDBCDiskCache<?, ?> diskCache )
    {
        // the set will prevent dupes.
        // we could also just add these to a hashmap by region name
        // but that might cause a problem if you wanted to use two different
        // jbdc disk caches for the same region.
        shrinkSet.add( diskCache );
    }

    /**
     * Calls deleteExpired on each item in the set. It pauses between each call.
     */
    @Override
    public void run()
    {
        try
        {
            deleteExpiredFromAllRegisteredRegions();
        }
        catch ( Throwable e )
        {
            log.error( "Caught an expcetion while trying to delete expired items.", e );
        }
    }

    /**
     * Deletes the expired items from all the registered regions.
     */
    private void deleteExpiredFromAllRegisteredRegions()
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Running JDBC disk cache shrinker.  Number of regions [" + shrinkSet.size() + "]" );
        }

        Object[] caches = null;

        synchronized ( shrinkSet )
        {
            caches = this.shrinkSet.toArray();
        }

        if ( caches != null )
        {
            for ( int i = 0; i < caches.length; i++ )
            {
                JDBCDiskCache<?, ?> cache = (JDBCDiskCache<?, ?>) caches[i];

                long start = System.currentTimeMillis();
                int deleted = cache.deleteExpired();
                long end = System.currentTimeMillis();

                if ( log.isInfoEnabled() )
                {
                    log.info( "Deleted [" + deleted + "] expired for region [" + cache.getCacheName() + "] for table ["
                        + cache.getTableName() + "] in " + ( end - start ) + " ms." );
                }

                // don't pause after the last call to delete expired.
                if ( i < caches.length - 1 )
                {
                    if ( log.isInfoEnabled() )
                    {
                        log.info( "Pausing for [" + this.getPauseBetweenRegionCallsMillis()
                            + "] ms. before shrinking the next region." );
                    }

                    try
                    {
                        Thread.sleep( this.getPauseBetweenRegionCallsMillis() );
                    }
                    catch ( InterruptedException e )
                    {
                        log.warn( "Interrupted while waiting to delete expired for the next region." );
                    }
                }
            }
        }
    }

    /**
     * How long should we wait between calls to deleteExpired when we are iterating through the list
     * of regions.
     * <p>
     * @param pauseBetweenRegionCallsMillis The pauseBetweenRegionCallsMillis to set.
     */
    public void setPauseBetweenRegionCallsMillis( long pauseBetweenRegionCallsMillis )
    {
        this.pauseBetweenRegionCallsMillis = pauseBetweenRegionCallsMillis;
    }

    /**
     * How long should we wait between calls to deleteExpired when we are iterating through the list
     * of regions.
     * <p>
     * @return Returns the pauseBetweenRegionCallsMillis.
     */
    public long getPauseBetweenRegionCallsMillis()
    {
        return pauseBetweenRegionCallsMillis;
    }
}
