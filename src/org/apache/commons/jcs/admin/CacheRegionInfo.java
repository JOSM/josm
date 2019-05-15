package org.apache.commons.jcs.admin;

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

import java.beans.ConstructorProperties;



/**
 * Stores info on a cache region for the template
 */
public class CacheRegionInfo
{
    /** The name of the cache region */
    private final String cacheName;

    /** The size of the cache region */
    private final int cacheSize;

    /** The status of the cache region */
    private final String cacheStatus;

    /** The statistics of the cache region */
    private final String cacheStatistics;

    /** The number of memory hits in the cache region */
    private final long hitCountRam;

    /** The number of auxiliary hits in the cache region */
    private final long hitCountAux;

    /** The number of misses in the cache region because the items were not found */
    private final long missCountNotFound;

    /** The number of misses in the cache region because the items were expired */
    private final long missCountExpired;

    /** The number of bytes counted so far, will be a total of all items */
    private final long byteCount;

    /**
     * Parameterized constructor
     *
	 * @param cacheName The name of the cache region
	 * @param cacheSize The size of the cache region
	 * @param cacheStatus The status of the cache region
	 * @param cacheStatistics The statistics of the cache region
	 * @param hitCountRam The number of memory hits in the cache region
	 * @param hitCountAux The number of auxiliary hits in the cache region
	 * @param missCountNotFound The number of misses in the cache region because the items were not found
	 * @param missCountExpired The number of misses in the cache region because the items were expired
	 * @param byteCount The number of bytes counted so far, will be a total of all items
	 */
    @ConstructorProperties({"cacheName", "cacheSize", "cacheStatus", "cacheStatistics",
    	"hitCountRam", "hitCountAux", "missCountNotFound", "missCountExpired", "byteCount"})
	public CacheRegionInfo(String cacheName, int cacheSize, String cacheStatus,
			String cacheStatistics, long hitCountRam, long hitCountAux,
			long missCountNotFound, long missCountExpired, long byteCount)
	{
		super();
		this.cacheName = cacheName;
		this.cacheSize = cacheSize;
		this.cacheStatus = cacheStatus;
		this.cacheStatistics = cacheStatistics;
		this.hitCountRam = hitCountRam;
		this.hitCountAux = hitCountAux;
		this.missCountNotFound = missCountNotFound;
		this.missCountExpired = missCountExpired;
		this.byteCount = byteCount;
	}

	/**
	 * @return the cacheName
	 */
	public String getCacheName()
	{
		return this.cacheName;
	}

	/**
	 * @return the cacheSize
	 */
	public int getCacheSize()
	{
		return this.cacheSize;
	}

	/**
     * @return a status string
     */
    public String getCacheStatus()
    {
        return this.cacheStatus;
    }

    /**
     * Return the statistics for the region.
     * <p>
     * @return String
     */
    public String getCacheStatistics()
    {
        return this.cacheStatistics;
    }

    /**
	 * @return the hitCountRam
	 */
	public long getHitCountRam()
	{
		return hitCountRam;
	}

	/**
	 * @return the hitCountAux
	 */
	public long getHitCountAux()
	{
		return hitCountAux;
	}

	/**
	 * @return the missCountNotFound
	 */
	public long getMissCountNotFound()
	{
		return missCountNotFound;
	}

	/**
	 * @return the missCountExpired
	 */
	public long getMissCountExpired()
	{
		return missCountExpired;
	}

	/**
     * @return total byte count
     */
    public long getByteCount()
    {
        return this.byteCount;
    }

    /**
     * @return string info on the region
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nCacheRegionInfo " );
        if ( cacheName != null )
        {
            buf.append( "\n CacheName [" + cacheName + "]" );
            buf.append( "\n Status [" + cacheStatus + "]" );
        }
        buf.append( "\n ByteCount [" + getByteCount() + "]" );

        return buf.toString();
    }
}
