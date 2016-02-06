package org.apache.commons.jcs.auxiliary.disk;

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

import org.apache.commons.jcs.engine.CacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;

/**
 * Implementation of cache elements in purgatory.
 * 
 * Elements are stored in purgatory when they are spooled to the auxiliary cache, but have not yet
 * been written to disk.
 */
public class PurgatoryElement<K, V>
    extends CacheElement<K, V>
{
    /** Don't change */
    private static final long serialVersionUID = -8152034342684135628L;

    /** Is the element ready to be spooled? */
    private boolean spoolable = false;

    /** Wrapped cache Element */
    private ICacheElement<K, V> cacheElement;

    /**
     * Constructor for the PurgatoryElement&lt;K, V&gt; object
     * 
     * @param cacheElement CacheElement
     */
    public PurgatoryElement( ICacheElement<K, V> cacheElement )
    {
        super(cacheElement.getCacheName(),
                cacheElement.getKey(), cacheElement.getVal(),
                cacheElement.getElementAttributes());
        this.cacheElement = cacheElement;
    }

    /**
     * Gets the spoolable property.
     * 
     * @return The spoolable value
     */
    public boolean isSpoolable()
    {
        return spoolable;
    }

    /**
     * Sets the spoolable property.
     * 
     * @param spoolable The new spoolable value
     */
    public void setSpoolable( boolean spoolable )
    {
        this.spoolable = spoolable;
    }

    /**
     * Get the wrapped cache element.
     * 
     * @return ICacheElement
     */
    public ICacheElement<K, V> getCacheElement()
    {
        return cacheElement;
    }

    // ------------------------------------------------ interface ICacheElement

    /**
     * @return cacheElement.getCacheName();
     * @see ICacheElement#getCacheName
     */
    @Override
    public String getCacheName()
    {
        return cacheElement.getCacheName();
    }

    /**
     * @return cacheElement.getKey();
     * @see ICacheElement#getKey
     */
    @Override
    public K getKey()
    {
        return cacheElement.getKey();
    }

    /**
     * @return cacheElement.getVal();
     * @see ICacheElement#getVal
     */
    @Override
    public V getVal()
    {
        return cacheElement.getVal();
    }

    /**
     * @return cacheElement.getElementAttributes();
     * @see ICacheElement#getElementAttributes
     */
    @Override
    public IElementAttributes getElementAttributes()
    {
        return cacheElement.getElementAttributes();
    }

    /**
     * @param attr
     * @see ICacheElement#setElementAttributes
     */
    @Override
    public void setElementAttributes( IElementAttributes attr )
    {
        cacheElement.setElementAttributes( attr );
    }

    /**
     * @return debug string
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "[PurgatoryElement: " );
        buf.append( " isSpoolable = " + isSpoolable() );
        buf.append( " CacheElement = " + getCacheElement() );
        buf.append( " CacheName = " + getCacheName() );
        buf.append( " Key = " + getKey() );
        buf.append( " Value = " + getVal() );
        buf.append( " ElementAttributes = " + getElementAttributes() );
        buf.append( "]" );
        return buf.toString();
    }
}
