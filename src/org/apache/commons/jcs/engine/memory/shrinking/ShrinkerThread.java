package org.apache.commons.jcs.engine.memory.shrinking;

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

import java.util.Set;

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.event.behavior.ElementEventType;
import org.apache.commons.jcs.engine.memory.behavior.IMemoryCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A background memory shrinker. Memory problems and concurrent modification exception caused by
 * acting directly on an iterator of the underlying memory cache should have been solved.
 * @version $Id: ShrinkerThread.java 1719092 2015-12-10 15:07:30Z tv $
 */
public class ShrinkerThread<K, V>
    implements Runnable
{
    /** The logger */
    private static final Log log = LogFactory.getLog( ShrinkerThread.class );

    /** The CompositeCache instance which this shrinker is watching */
    private final CompositeCache<K, V> cache;

    /** Maximum memory idle time for the whole cache */
    private final long maxMemoryIdleTime;

    /** Maximum number of items to spool per run. Default is -1, or no limit. */
    private final int maxSpoolPerRun;

    /** Should we limit the number spooled per run. If so, the maxSpoolPerRun will be used. */
    private boolean spoolLimit = false;

    /**
     * Constructor for the ShrinkerThread object.
     * <p>
     * @param cache The MemoryCache which the new shrinker should watch.
     */
    public ShrinkerThread( CompositeCache<K, V> cache )
    {
        super();

        this.cache = cache;

        long maxMemoryIdleTimeSeconds = cache.getCacheAttributes().getMaxMemoryIdleTimeSeconds();

        if ( maxMemoryIdleTimeSeconds < 0 )
        {
            this.maxMemoryIdleTime = -1;
        }
        else
        {
            this.maxMemoryIdleTime = maxMemoryIdleTimeSeconds * 1000;
        }

        this.maxSpoolPerRun = cache.getCacheAttributes().getMaxSpoolPerRun();
        if ( this.maxSpoolPerRun != -1 )
        {
            this.spoolLimit = true;
        }

    }

    /**
     * Main processing method for the ShrinkerThread object
     */
    @Override
    public void run()
    {
        shrink();
    }

    /**
     * This method is called when the thread wakes up. First the method obtains an array of keys for
     * the cache region. It iterates through the keys and tries to get the item from the cache
     * without affecting the last access or position of the item. The item is checked for
     * expiration, the expiration check has 3 parts:
     * <ol>
     * <li>Has the cacheattributes.MaxMemoryIdleTimeSeconds defined for the region been exceeded? If
     * so, the item should be move to disk.</li> <li>Has the item exceeded MaxLifeSeconds defined in
     * the element attributes? If so, remove it.</li> <li>Has the item exceeded IdleTime defined in
     * the element attributes? If so, remove it. If there are event listeners registered for the
     * cache element, they will be called.</li>
     * </ol>
     * TODO Change element event handling to use the queue, then move the queue to the region and
     *       access via the Cache.
     */
    protected void shrink()
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Shrinking memory cache for: " + this.cache.getCacheName() );
        }

        IMemoryCache<K, V> memCache = cache.getMemoryCache();

        try
        {
            Set<K> keys = memCache.getKeySet();
            int size = keys.size();
            if ( log.isDebugEnabled() )
            {
                log.debug( "Keys size: " + size );
            }

            ICacheElement<K, V> cacheElement;
            IElementAttributes attributes;

            int spoolCount = 0;

            for (K key : keys)
            {
                cacheElement = memCache.getQuiet( key );

                if ( cacheElement == null )
                {
                    continue;
                }

                attributes = cacheElement.getElementAttributes();

                boolean remove = false;

                long now = System.currentTimeMillis();

                // If the element is not eternal, check if it should be
                // removed and remove it if so.
                if ( !cacheElement.getElementAttributes().getIsEternal() )
                {
                    remove = cache.isExpired( cacheElement, now,
                            ElementEventType.EXCEEDED_MAXLIFE_BACKGROUND,
                            ElementEventType.EXCEEDED_IDLETIME_BACKGROUND );

                    if ( remove )
                    {
                        memCache.remove( cacheElement.getKey() );
                    }
                }

                // If the item is not removed, check is it has been idle
                // long enough to be spooled.

                if ( !remove && maxMemoryIdleTime != -1 )
                {
                    if ( !spoolLimit || spoolCount < this.maxSpoolPerRun )
                    {
                        final long lastAccessTime = attributes.getLastAccessTime();

                        if ( lastAccessTime + maxMemoryIdleTime < now )
                        {
                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Exceeded memory idle time: " + cacheElement.getKey() );
                            }

                            // Shouldn't we ensure that the element is
                            // spooled before removing it from memory?
                            // No the disk caches have a purgatory. If it fails
                            // to spool that does not affect the
                            // responsibilities of the memory cache.

                            spoolCount++;

                            memCache.remove( cacheElement.getKey() );

                            memCache.waterfal( cacheElement );

                            key = null;
                            cacheElement = null;
                        }
                    }
                    else
                    {
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "spoolCount = '" + spoolCount + "'; " + "maxSpoolPerRun = '" + maxSpoolPerRun
                                + "'" );
                        }

                        // stop processing if limit has been reached.
                        if ( spoolLimit && spoolCount >= this.maxSpoolPerRun )
                        {
                            return;
                        }
                    }
                }
            }
        }
        catch ( Throwable t )
        {
            log.info( "Unexpected trouble in shrink cycle", t );

            // concurrent modifications should no longer be a problem
            // It is up to the IMemoryCache to return an array of keys

            // stop for now
            return;
        }
    }
}
