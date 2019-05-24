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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEventHandler;

/**
 * This it the element attribute descriptor class. Each element in the cache has an ElementAttribute
 * object associated with it. An ElementAttributes object can be associated with an element in 3
 * ways:
 * <ol>
 * <li>When the item is put into the cache, you can associate an element attributes object.</li>
 * <li>If not attributes object is include when the element is put into the cache, then the default
 * attributes for the region will be used.</li>
 * <li>The element attributes can be reset. This effectively results in a retrieval followed by a
 * put. Hence, this is the same as 1.</li>
 * </ol>
 */
public class ElementAttributes
    implements IElementAttributes
{
    /** Don't change. */
    private static final long serialVersionUID = 7814990748035017441L;

    /** Can this item be flushed to disk */
    private boolean IS_SPOOL = true;

    /** Is this item laterally distributable */
    private boolean IS_LATERAL = true;

    /** Can this item be sent to the remote cache */
    private boolean IS_REMOTE = true;

    /**
     * You can turn off expiration by setting this to true. This causes the cache to bypass both max
     * life and idle time expiration.
     */
    private boolean IS_ETERNAL = true;

    /** Max life seconds */
    private long maxLife = -1;

    /**
     * The maximum time an entry can be idle. Setting this to -1 causes the idle time check to be
     * ignored.
     */
    private long maxIdleTime = -1;

    /** The byte size of the field. Must be manually set. */
    private int size = 0;

    /** The creation time. This is used to enforce the max life. */
    private long createTime = 0;

    /** The last access time. This is used to enforce the max idel time. */
    private long lastAccessTime = 0;

    /**
     * The list of Event handlers to use. This is transient, since the event handlers cannot usually
     * be serialized. This means that you cannot attach a post serialization event to an item.
     * <p>
     * TODO we need to check that when an item is passed to a non-local cache that if the local
     * cache had a copy with event handlers, that those handlers are used.
     */
    private transient ArrayList<IElementEventHandler> eventHandlers;

    private long timeFactor = 1000;

    /**
     * Constructor for the IElementAttributes object
     */
    public ElementAttributes()
    {
        this.createTime = System.currentTimeMillis();
        this.lastAccessTime = this.createTime;
    }

    /**
     * Constructor for the IElementAttributes object
     * <p>
     * @param attr
     */
    protected ElementAttributes( ElementAttributes attr )
    {
        IS_ETERNAL = attr.IS_ETERNAL;

        // waterfall onto disk, for pure disk set memory to 0
        IS_SPOOL = attr.IS_SPOOL;

        // lateral
        IS_LATERAL = attr.IS_LATERAL;

        // central rmi store
        IS_REMOTE = attr.IS_REMOTE;

        maxLife = attr.maxLife;
        // time-to-live
        maxIdleTime = attr.maxIdleTime;
        size = attr.size;
    }

    /**
     * Sets the maxLife attribute of the IAttributes object.
     * <p>
     * @param mls The new MaxLifeSeconds value
     */
    @Override
    public void setMaxLife(long mls)
    {
        this.maxLife = mls;
    }

    /**
     * Sets the maxLife attribute of the IAttributes object. How many seconds it can live after
     * creation.
     * <p>
     * If this is exceeded the element will not be returned, instead it will be removed. It will be
     * removed on retrieval, or removed actively if the memory shrinker is turned on.
     * @return The MaxLifeSeconds value
     */
    @Override
    public long getMaxLife()
    {
        return this.maxLife;
    }

    /**
     * Sets the idleTime attribute of the IAttributes object. This is the maximum time the item can
     * be idle in the cache, that is not accessed.
     * <p>
     * If this is exceeded the element will not be returned, instead it will be removed. It will be
     * removed on retrieval, or removed actively if the memory shrinker is turned on.
     * @param idle The new idleTime value
     */
    @Override
    public void setIdleTime( long idle )
    {
        this.maxIdleTime = idle;
    }

    /**
     * Size in bytes. This is not used except in the admin pages. It will be 0 by default
     * and is only updated when the element is serialized.
     * <p>
     * @param size The new size value
     */
    @Override
    public void setSize( int size )
    {
        this.size = size;
    }

    /**
     * Gets the size attribute of the IAttributes object
     * <p>
     * @return The size value
     */
    @Override
    public int getSize()
    {
        return size;
    }

    /**
     * Gets the createTime attribute of the IAttributes object.
     * <p>
     * This should be the current time in milliseconds returned by the sysutem call when the element
     * is put in the cache.
     * <p>
     * Putting an item in the cache overrides any existing items.
     * @return The createTime value
     */
    @Override
    public long getCreateTime()
    {
        return createTime;
    }

    /**
     * Sets the createTime attribute of the IElementAttributes object
     */
    public void setCreateTime()
    {
        createTime = System.currentTimeMillis();
    }

    /**
     * Gets the idleTime attribute of the IAttributes object.
     * <p>
     * @return The idleTime value
     */
    @Override
    public long getIdleTime()
    {
        return this.maxIdleTime;
    }

    /**
     * Gets the time left to live of the IAttributes object.
     * <p>
     * This is the (max life + create time) - current time.
     * @return The TimeToLiveSeconds value
     */
    @Override
    public long getTimeToLiveSeconds()
    {
        final long now = System.currentTimeMillis();
        final long timeFactorForMilliseconds = getTimeFactorForMilliseconds();
        return ( this.getCreateTime() + this.getMaxLife() * timeFactorForMilliseconds - now ) / 1000;
    }

    /**
     * Gets the LastAccess attribute of the IAttributes object.
     * <p>
     * @return The LastAccess value.
     */
    @Override
    public long getLastAccessTime()
    {
        return this.lastAccessTime;
    }

    /**
     * Sets the LastAccessTime as now of the IElementAttributes object
     */
    @Override
    public void setLastAccessTimeNow()
    {
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * only for use from test code
     */
    public void setLastAccessTime(long time)
    {
        this.lastAccessTime = time;
    }

    /**
     * Can this item be spooled to disk
     * <p>
     * By default this is true.
     * @return The spoolable value
     */
    @Override
    public boolean getIsSpool()
    {
        return this.IS_SPOOL;
    }

    /**
     * Sets the isSpool attribute of the IElementAttributes object
     * <p>
     * By default this is true.
     * @param val The new isSpool value
     */
    @Override
    public void setIsSpool( boolean val )
    {
        this.IS_SPOOL = val;
    }

    /**
     * Is this item laterally distributable. Can it be sent to auxiliaries of type lateral.
     * <p>
     * By default this is true.
     * @return The isLateral value
     */
    @Override
    public boolean getIsLateral()
    {
        return this.IS_LATERAL;
    }

    /**
     * Sets the isLateral attribute of the IElementAttributes object
     * <p>
     * By default this is true.
     * @param val The new isLateral value
     */
    @Override
    public void setIsLateral( boolean val )
    {
        this.IS_LATERAL = val;
    }

    /**
     * Can this item be sent to the remote cache
     * @return true if the item can be sent to a remote auxiliary
     */
    @Override
    public boolean getIsRemote()
    {
        return this.IS_REMOTE;
    }

    /**
     * Sets the isRemote attribute of the ElementAttributes object
     * @param val The new isRemote value
     */
    @Override
    public void setIsRemote( boolean val )
    {
        this.IS_REMOTE = val;
    }

    /**
     * You can turn off expiration by setting this to true. The max life value will be ignored.
     * <p>
     * @return true if the item cannot expire.
     */
    @Override
    public boolean getIsEternal()
    {
        return this.IS_ETERNAL;
    }

    /**
     * Sets the isEternal attribute of the ElementAttributes object. True means that the item should
     * never expire. If can still be removed if it is the least recently used, and you are using the
     * LRUMemory cache. it just will not be filtered for expiration by the cache hub.
     * <p>
     * @param val The new isEternal value
     */
    @Override
    public void setIsEternal( boolean val )
    {
        this.IS_ETERNAL = val;
    }

    /**
     * Adds a ElementEventHandler. Handler's can be registered for multiple events. A registered
     * handler will be called at every recognized event.
     * <p>
     * The alternative would be to register handlers for each event. Or maybe The handler interface
     * should have a method to return whether it cares about certain events.
     * <p>
     * @param eventHandler The ElementEventHandler to be added to the list.
     */
    @Override
    public void addElementEventHandler( IElementEventHandler eventHandler )
    {
        // lazy here, no concurrency problems expected
        if ( this.eventHandlers == null )
        {
            this.eventHandlers = new ArrayList<>();
        }
        this.eventHandlers.add( eventHandler );
    }

    /**
     * Sets the eventHandlers of the IElementAttributes object.
     * <p>
     * This add the references to the local list. Subsequent changes in the caller's list will not
     * be reflected.
     * <p>
     * @param eventHandlers List of IElementEventHandler objects
     */
    @Override
    public void addElementEventHandlers( List<IElementEventHandler> eventHandlers )
    {
        if ( eventHandlers == null )
        {
            return;
        }

        for (IElementEventHandler handler : eventHandlers)
        {
            addElementEventHandler(handler);
        }
    }

    @Override
    public long getTimeFactorForMilliseconds()
    {
        return timeFactor;
    }

    @Override
    public void setTimeFactorForMilliseconds(long factor)
    {
        this.timeFactor = factor;
    }

    /**
     * Gets the elementEventHandlers. Returns null if none exist. Makes checking easy.
     * <p>
     * @return The elementEventHandlers List of IElementEventHandler objects
     */
    @Override
    public ArrayList<IElementEventHandler> getElementEventHandlers()
    {
        return this.eventHandlers;
    }

    /**
     * For logging and debugging the element IElementAttributes.
     * <p>
     * @return String info about the values.
     */
    @Override
    public String toString()
    {
        StringBuilder dump = new StringBuilder();

        dump.append( "[ IS_LATERAL = " ).append( IS_LATERAL );
        dump.append( ", IS_SPOOL = " ).append( IS_SPOOL );
        dump.append( ", IS_REMOTE = " ).append( IS_REMOTE );
        dump.append( ", IS_ETERNAL = " ).append( IS_ETERNAL );
        dump.append( ", MaxLifeSeconds = " ).append( this.getMaxLife() );
        dump.append( ", IdleTime = " ).append( this.getIdleTime() );
        dump.append( ", CreateTime = " ).append( this.getCreateTime() );
        dump.append( ", LastAccessTime = " ).append( this.getLastAccessTime() );
        dump.append( ", getTimeToLiveSeconds() = " ).append( String.valueOf( getTimeToLiveSeconds() ) );
        dump.append( ", createTime = " ).append( String.valueOf( createTime ) ).append( " ]" );

        return dump.toString();
    }

    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public IElementAttributes clone()
    {
        try
        {
        	ElementAttributes c = (ElementAttributes) super.clone();
        	c.setCreateTime();
            return c;
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException("Clone not supported. This should never happen.", e);
        }
    }
}
