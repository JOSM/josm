package org.apache.commons.jcs.engine.logging;

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

import org.apache.commons.jcs.engine.logging.behavior.ICacheEvent;

import java.util.Date;

/** It's returned from create and passed into log. */
public class CacheEvent<K>
    implements ICacheEvent<K>
{
    /** Don't change. */
    private static final long serialVersionUID = -5913139566421714330L;

    /** The time at which this object was created. */
    private final long createTime = System.currentTimeMillis();

    /** The auxiliary or other source of the event. */
    private String source;

    /** The cache region */
    private String region;

    /** The event name: update, get, remove, etc. */
    private String eventName;

    /** disk location, ip, etc. */
    private String optionalDetails;

    /** The key that was put or retrieved. */
    private K key;

    /**
     * @param source the source to set
     */
    @Override
	public void setSource( String source )
    {
        this.source = source;
    }

    /**
     * @return the source
     */
    @Override
	public String getSource()
    {
        return source;
    }

    /**
     * @param region the region to set
     */
    @Override
	public void setRegion( String region )
    {
        this.region = region;
    }

    /**
     * @return the region
     */
    @Override
	public String getRegion()
    {
        return region;
    }

    /**
     * @param eventName the eventName to set
     */
    @Override
	public void setEventName( String eventName )
    {
        this.eventName = eventName;
    }

    /**
     * @return the eventName
     */
    @Override
	public String getEventName()
    {
        return eventName;
    }

    /**
     * @param optionalDetails the optionalDetails to set
     */
    @Override
	public void setOptionalDetails( String optionalDetails )
    {
        this.optionalDetails = optionalDetails;
    }

    /**
     * @return the optionalDetails
     */
    @Override
	public String getOptionalDetails()
    {
        return optionalDetails;
    }

    /**
     * @param key the key to set
     */
    @Override
	public void setKey( K key )
    {
        this.key = key;
    }

    /**
     * @return the key
     */
    @Override
	public K getKey()
    {
        return key;
    }

    /**
     * The time at which this object was created.
     * <p>
     * @return the createTime
     */
    public long getCreateTime()
    {
        return createTime;
    }

    /**
     * @return reflection toString
     */
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("CacheEvent: ").append(eventName).append(" Created: ").append(new Date(createTime));
    	if (source != null)
    	{
        	sb.append(" Source: ").append(source);
    	}
    	if (region != null)
    	{
        	sb.append(" Region: ").append(region);
    	}
    	if (key != null)
    	{
        	sb.append(" Key: ").append(key);
    	}
    	if (optionalDetails != null)
    	{
        	sb.append(" Details: ").append(optionalDetails);
    	}
        return sb.toString();
    }
}
