package org.apache.commons.jcs.engine.memory.lru;

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

import org.apache.commons.jcs.engine.CacheConstants;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.group.GroupAttrName;
import org.apache.commons.jcs.engine.memory.AbstractMemoryCache;
import org.apache.commons.jcs.engine.memory.util.MemoryElementDescriptor;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is a test memory manager using the jdk1.4 LinkedHashMap.
 */
public class LHMLRUMemoryCache<K extends Serializable, V extends Serializable>
    extends AbstractMemoryCache<K, V>
{
    /** The Logger. */
    private static final Log log = LogFactory.getLog( LRUMemoryCache.class );

    /** number of hits */
    private int hitCnt = 0; // TODO should these be long values?

    /** number of misses */
    private int missCnt = 0;

    /** number of puts */
    private int putCnt = 0;

    /**
     * For post reflection creation initialization
     * <p>
     * @param hub
     */
    @Override
    public synchronized void initialize( CompositeCache<K, V> hub )
    {
        super.initialize( hub );
        log.info( "initialized LHMLRUMemoryCache for " + cacheName );
    }

    /**
     * Returns a synchronized LHMSpooler
     * <p>
     * @return Collections.synchronizedMap( new LHMSpooler() )
     */
    @Override
    public Map<K, MemoryElementDescriptor<K, V>> createMap()
    {
        return Collections.synchronizedMap( new LHMSpooler() );
    }

    /**
     * Puts an item to the cache.
     * <p>
     * @param ce Description of the Parameter
     * @throws IOException
     */
    @Override
    public void update( ICacheElement<K, V> ce )
        throws IOException
    {
        putCnt++;
        map.put( ce.getKey(), new MemoryElementDescriptor<K, V>(ce) );
    }

    /**
     * Get an item from the cache without affecting its last access time or position. There is no
     * way to do this with the LinkedHashMap!
     * <p>
     * @param key Identifies item to find
     * @return Element matching key if found, or null
     * @throws IOException
     */
    @Override
    public ICacheElement<K, V> getQuiet( K key )
        throws IOException
    {
        return map.get( key ).ce;
    }

    /**
     * Get an item from the cache
     * <p>
     * @param key Identifies item to find
     * @return ICacheElement<K, V> if found, else null
     * @throws IOException
     */
    @Override
    public synchronized ICacheElement<K, V> get( K key )
        throws IOException
    {
        MemoryElementDescriptor<K, V> me = null;

        if ( log.isDebugEnabled() )
        {
            log.debug( "getting item from cache " + cacheName + " for key " + key );
        }

        me = map.get( key );

        if ( me != null )
        {
            hitCnt++;
            if ( log.isDebugEnabled() )
            {
                log.debug( cacheName + ": LRUMemoryCache hit for " + key );
            }
            return me.ce;
        }
        else
        {
            missCnt++;
            log.debug( cacheName + ": LRUMemoryCache miss for " + key );
        }

        return null;
    }

    /**
     * Removes an item from the cache. This method handles hierarchical removal. If the key is a
     * String and ends with the CacheConstants.NAME_COMPONENT_DELIMITER, then all items with keys
     * starting with the argument String will be removed.
     * <p>
     * @param key
     * @return true if removed
     * @throws IOException
     */
    @Override
    public synchronized boolean remove( K key )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "removing item for key: " + key );
        }

        boolean removed = false;

        // handle partial removal
        if ( key instanceof String && ( (String) key ).endsWith( CacheConstants.NAME_COMPONENT_DELIMITER ) )
        {
            // remove all keys of the same name hierarchy.
            synchronized ( map )
            {
                for (Iterator<Map.Entry<K, MemoryElementDescriptor<K, V>>> itr = map.entrySet().iterator(); itr.hasNext(); )
                {
                    Map.Entry<K, MemoryElementDescriptor<K, V>> entry = itr.next();
                    K k = entry.getKey();

                    if ( k instanceof String && ( (String) k ).startsWith( key.toString() ) )
                    {
                        itr.remove();
                        removed = true;
                    }
                }
            }
        }
        else if ( key instanceof GroupAttrName && ((GroupAttrName<?>)key).attrName == null )
        {
            // remove all keys of the same name hierarchy.
            synchronized ( map )
            {
                for (Iterator<Map.Entry<K, MemoryElementDescriptor<K, V>>> itr = map.entrySet().iterator(); itr.hasNext(); )
                {
                    Map.Entry<K, MemoryElementDescriptor<K, V>> entry = itr.next();
                    K k = entry.getKey();

                    if ( k instanceof GroupAttrName &&
                        ((GroupAttrName<?>)k).groupId.equals(((GroupAttrName<?>)key).groupId) )
                    {
                        itr.remove();
                        removed = true;
                    }
                }
            }
        }
        else
        {
            // remove single item.
            MemoryElementDescriptor<K, V> me = map.remove( key );
            if ( me != null )
            {
                removed = true;
            }
        }

        return removed;
    }

    /**
     * Get an Array of the keys for all elements in the memory cache
     * <p>
     * @return An Object[]
     */
    @Override
    public Set<K> getKeySet()
    {
        // need a better locking strategy here.
        synchronized ( this )
        {
            // may need to lock to map here?
            return new LinkedHashSet<K>(map.keySet());
        }
    }

    /**
     * This returns semi-structured information on the memory cache, such as the size, put count,
     * hit count, and miss count.
     * <p>
     * @return IStats
     */
    @Override
    public synchronized IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "LHMLRU Memory Cache" );

        ArrayList<IStatElement<?>> elems = new ArrayList<IStatElement<?>>();

        elems.add(new StatElement<Integer>( "Map Size", Integer.valueOf(map.size()) ) );
        elems.add(new StatElement<Integer>( "Put Count", Integer.valueOf(putCnt) ) );
        elems.add(new StatElement<Integer>( "Hit Count", Integer.valueOf(hitCnt) ) );
        elems.add(new StatElement<Integer>( "Miss Count", Integer.valueOf(missCnt) ) );

        stats.setStatElements( elems );

        // int rate = ((hitCnt + missCnt) * 100) / (hitCnt * 100) * 100;
        // buf.append("\n Hit Rate = " + rate + " %" );

        return stats;
    }

    // ---------------------------------------------------------- debug methods

    /**
     * Dump the cache entries from first to last for debugging.
     */
    public void dumpCacheEntries()
    {
        dumpMap();
    }

    /**
     * This can't be implemented.
     * <p>
     * @param numberToFree
     * @return 0
     * @throws IOException
     */
    @Override
    public int freeElements( int numberToFree )
        throws IOException
    {
        // can't be implemented using the LHM
        return 0;
    }

    // ---------------------------------------------------------- extended map

    /**
     * Implementation of removeEldestEntry in LinkedHashMap
     */
    public class LHMSpooler
        extends java.util.LinkedHashMap<K, MemoryElementDescriptor<K, V>>
    {
        /** Don't change. */
        private static final long serialVersionUID = -1255907868906762484L;

        /**
         * Initialize to a small size--for now, 1/2 of max 3rd variable "true" indicates that it
         * should be access and not time governed. This could be configurable.
         */
        public LHMSpooler()
        {
            super( (int) ( cache.getCacheAttributes().getMaxObjects() * .5 ), .75F, true );
        }

        /**
         * Remove eldest. Automatically called by LinkedHashMap.
         * <p>
         * @param eldest
         * @return true if removed
         */
        @SuppressWarnings("synthetic-access")
        @Override
        protected boolean removeEldestEntry( Map.Entry<K, MemoryElementDescriptor<K, V>> eldest )
        {
            ICacheElement<K, V> element = eldest.getValue().ce;

            if ( size() <= cache.getCacheAttributes().getMaxObjects() )
            {
                return false;
            }
            else
            {

                if ( log.isDebugEnabled() )
                {
                    log.debug( "LHMLRU max size: " + cache.getCacheAttributes().getMaxObjects()
                        + ".  Spooling element, key: " + element.getKey() );
                }
                spoolToDisk( element );

                if ( log.isDebugEnabled() )
                {
                    log.debug( "LHMLRU size: " + map.size() );
                }
            }
            return true;
        }

        /**
         * Puts the element in the DiskStore
         * <p>
         * @param element The CacheElement
         */
        @SuppressWarnings("synthetic-access")
        private void spoolToDisk( ICacheElement<K, V> element )
        {
            cache.spoolToDisk( element );

            if ( log.isDebugEnabled() )
            {
                log.debug( cache.getCacheName() + "Spooled element to disk: " + element.getKey() );
            }
        }
    }
}
