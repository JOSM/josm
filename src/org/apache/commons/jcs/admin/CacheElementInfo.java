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
 * Stores info on a cache element for the template
 */
public class CacheElementInfo
{
    /** element key */
    private final String key;

    /** is it eternal */
    private final boolean eternal;

    /** when it was created */
    private final String createTime;

    /** max life */
    private final long maxLifeSeconds;

    /** when it will expire */
    private final long expiresInSeconds;

    /**
     * Parameterized constructor
     *
	 * @param key element key
	 * @param eternal is it eternal
	 * @param createTime when it was created
	 * @param maxLifeSeconds max life
	 * @param expiresInSeconds when it will expire
	 */
    @ConstructorProperties({"key", "eternal", "createTime", "maxLifeSeconds", "expiresInSeconds"})
    public CacheElementInfo(String key, boolean eternal, String createTime,
			long maxLifeSeconds, long expiresInSeconds)
    {
		super();
		this.key = key;
		this.eternal = eternal;
		this.createTime = createTime;
		this.maxLifeSeconds = maxLifeSeconds;
		this.expiresInSeconds = expiresInSeconds;
	}

	/**
     * @return a string representation of the key
     */
    public String getKey()
    {
        return this.key;
    }

    /**
     * @return true if the item does not expire
     */
    public boolean isEternal()
    {
        return this.eternal;
    }

    /**
     * @return the time the object was created
     */
    public String getCreateTime()
    {
        return this.createTime;
    }

    /**
     * Ignored if isEternal
     * @return the longest this object can live.
     */
    public long getMaxLifeSeconds()
    {
        return this.maxLifeSeconds;
    }

    /**
     * Ignored if isEternal
     * @return how many seconds until this object expires.
     */
    public long getExpiresInSeconds()
    {
        return this.expiresInSeconds;
    }

    /**
     * @return string info on the item
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nCacheElementInfo " );
        buf.append( "\n Key [" ).append( getKey() ).append( "]" );
        buf.append( "\n Eternal [" ).append( isEternal() ).append( "]" );
        buf.append( "\n CreateTime [" ).append( getCreateTime() ).append( "]" );
        buf.append( "\n MaxLifeSeconds [" ).append( getMaxLifeSeconds() ).append( "]" );
        buf.append( "\n ExpiresInSeconds [" ).append( getExpiresInSeconds() ).append( "]" );

        return buf.toString();
    }
}
