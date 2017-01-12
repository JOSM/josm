package org.apache.commons.jcs.auxiliary;

import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;

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

/**
 * This has common attributes used by all auxiliaries.
 */
public abstract class AbstractAuxiliaryCacheAttributes
    implements AuxiliaryCacheAttributes
{
    /** Don't change */
    private static final long serialVersionUID = -6594609334959187673L;

    /** cacheName */
    private String cacheName;

    /** name */
    private String name;

    /** eventQueueType -- pooled, or single threaded */
    private ICacheEventQueue.QueueType eventQueueType;

    /** Named when pooled */
    private String eventQueuePoolName;

    /**
     * @param name
     */
    @Override
    public void setCacheName( String name )
    {
        this.cacheName = name;
    }

    /**
     * Gets the cacheName attribute of the AuxiliaryCacheAttributes object
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return this.cacheName;
    }

    /**
     * This is the name of the auxiliary in configuration file.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes#setName(java.lang.String)
     */
    @Override
    public void setName( String s )
    {
        this.name = s;
    }

    /**
     * Gets the name attribute of the AuxiliaryCacheAttributes object
     * <p>
     * @return The name value
     */
    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * SINGLE is the default. If you choose POOLED, the value of EventQueuePoolName will be used
     * <p>
     * @param queueType SINGLE or POOLED
     */
    @Override
    public void setEventQueueType( ICacheEventQueue.QueueType queueType )
    {
        this.eventQueueType = queueType;
    }

    /**
     * @return SINGLE or POOLED
     */
    @Override
    public ICacheEventQueue.QueueType getEventQueueType()
    {
        return eventQueueType;
    }

    /**
     * If you choose a POOLED event queue type, the value of EventQueuePoolName will be used. This
     * is ignored if the pool type is SINGLE
     * <p>
     * @param s SINGLE or POOLED
     */
    @Override
    public void setEventQueuePoolName( String s )
    {
        eventQueuePoolName = s;
    }

    /**
     * Sets the pool name to use. If a pool is not found by this name, the thread pool manager will
     * return a default configuration.
     * <p>
     * @return name of thread pool to use for this auxiliary
     */
    @Override
    public String getEventQueuePoolName()
    {
        return eventQueuePoolName;
    }

    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public AbstractAuxiliaryCacheAttributes clone()
    {
        try
        {
            return (AbstractAuxiliaryCacheAttributes)super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException("Clone not supported. This should never happen.", e);
        }
    }
}
