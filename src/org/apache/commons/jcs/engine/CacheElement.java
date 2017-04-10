package org.apache.commons.jcs.engine;

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

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;

/**
 * Generic element wrapper. Often stuffed inside another.
 */
public class CacheElement<K, V>
    implements ICacheElement<K, V>
{
    /** Don't change */
    private static final long serialVersionUID = -6062305728297627263L;

    /** The name of the cache region. This is a namespace. */
    private final String cacheName;

    /** This is the cache key by which the value can be referenced. */
    private final K key;

    /** This is the cached value, reference by the key. */
    private final V val;

    /**
     * These attributes hold information about the element and what it is
     * allowed to do.
     */
    private IElementAttributes attr;

    /**
     * Constructor for the CacheElement object
     * <p>
     * @param cacheName
     * @param key
     * @param val
     */
    public CacheElement( String cacheName, K key, V val )
    {
        this.cacheName = cacheName;
        this.key = key;
        this.val = val;
    }

    /**
     * Constructor for the CacheElement object
     * <p>
     * @param cacheName
     * @param key
     * @param val
     * @param attrArg
     */
    public CacheElement( String cacheName, K key, V val, IElementAttributes attrArg )
    {
        this(cacheName, key, val);
        this.attr = attrArg;
    }

    /**
     * Gets the cacheName attribute of the CacheElement object
     * <p>
     * @return The cacheName value
     */
    @Override
    public String getCacheName()
    {
        return this.cacheName;
    }

    /**
     * Gets the key attribute of the CacheElement object
     * <p>
     * @return The key value
     */
    @Override
    public K getKey()
    {
        return this.key;
    }

    /**
     * Gets the val attribute of the CacheElement object
     * <p>
     * @return The val value
     */
    @Override
    public V getVal()
    {
        return this.val;
    }

    /**
     * Sets the attributes attribute of the CacheElement object
     * <p>
     * @param attr
     *            The new IElementAttributes value
     */
    @Override
    public void setElementAttributes( IElementAttributes attr )
    {
        this.attr = attr;
    }

    /**
     * Gets the IElementAttributes attribute of the CacheElement object
     * <p>
     * @return The IElementAttributes value, never null
     */
    @Override
    public IElementAttributes getElementAttributes()
    {
        // create default attributes if they are null
        // this shouldn't happen, but could if a corrupt
        // object was sent over the wire.
        if ( this.attr == null )
        {
            this.attr = new ElementAttributes();
        }
        return this.attr;
    }

    /**
     * @return a hash of the key only
     */
    @Override
    public int hashCode()
    {
        return key.hashCode();
    }

    /**
     * For debugging only.
     * <p>
     * @return String representation
     */
    @Override
    public String toString()
    {
        return "[CacheElement: cacheName [" + cacheName + "], key [" + key + "], val [" + val + "], attr [" + attr
            + "]";
    }
}
