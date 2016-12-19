package org.apache.commons.jcs.engine.memory.soft;

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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.jcs.engine.CacheConstants;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.group.GroupAttrName;
import org.apache.commons.jcs.engine.memory.AbstractMemoryCache;
import org.apache.commons.jcs.engine.memory.util.MemoryElementDescriptor;
import org.apache.commons.jcs.engine.memory.util.SoftReferenceElementDescriptor;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A JCS IMemoryCache that has {@link SoftReference} to all its values.
 * This cache does not respect {@link ICompositeCacheAttributes#getMaxObjects()}
 * as overflowing is handled by Java GC.
 * <p>
 * The cache also has strong references to a maximum number of objects given by
 * the maxObjects parameter
 *
 * @author halset
 */
public class SoftReferenceMemoryCache<K, V> extends AbstractMemoryCache<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog(SoftReferenceMemoryCache.class);

    /**
     * Strong references to the maxObjects number of newest objects.
     * <p>
     * Trimming is done by {@link #trimStrongReferences()} instead of by
     * overriding removeEldestEntry to be able to control waterfalling as easy
     * as possible
     */
    private LinkedBlockingQueue<ICacheElement<K, V>> strongReferences;

    /**
     * For post reflection creation initialization
     * <p>
     * @param hub
     */
    @Override
    public synchronized void initialize( CompositeCache<K, V> hub )
    {
        super.initialize( hub );
        strongReferences = new LinkedBlockingQueue<ICacheElement<K, V>>();
        log.info( "initialized Soft Reference Memory Cache for " + getCacheName() );
    }

    /**
     * @see org.apache.commons.jcs.engine.memory.AbstractMemoryCache#createMap()
     */
    @Override
    public ConcurrentMap<K, MemoryElementDescriptor<K, V>> createMap()
    {
        return new ConcurrentHashMap<K, MemoryElementDescriptor<K, V>>();
    }

    /**
     * @see org.apache.commons.jcs.engine.memory.behavior.IMemoryCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet()
    {
        Set<K> keys = new HashSet<K>();
        for (Map.Entry<K, MemoryElementDescriptor<K, V>> e : map.entrySet())
        {
            SoftReferenceElementDescriptor<K, V> sred = (SoftReferenceElementDescriptor<K, V>) e.getValue();
            if (sred.getCacheElement() != null)
            {
                keys.add(e.getKey());
            }
        }

        return keys;
    }

    /**
     * Returns the current cache size.
     * <p>
     * @return The size value
     */
    @Override
    public int getSize()
    {
        int size = 0;
        for (MemoryElementDescriptor<K, V> me : map.values())
        {
            SoftReferenceElementDescriptor<K, V> sred = (SoftReferenceElementDescriptor<K, V>) me;
            if (sred.getCacheElement() != null)
            {
                size++;
            }
        }
        return size;
    }

    /**
     * @return statistics about the cache
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = super.getStatistics();
        stats.setTypeName("Soft Reference Memory Cache");

        List<IStatElement<?>> elems = stats.getStatElements();
        int emptyrefs = map.size() - getSize();
        elems.add(new StatElement<Integer>("Empty References", Integer.valueOf(emptyrefs)));
        elems.add(new StatElement<Integer>("Strong References", Integer.valueOf(strongReferences.size())));

        return stats;
    }

    /**
     * Removes an item from the cache. This method handles hierarchical removal. If the key is a
     * String and ends with the CacheConstants.NAME_COMPONENT_DELIMITER, then all items with keys
     * starting with the argument String will be removed.
     * <p>
     *
     * @param key
     * @return true if the removal was successful
     * @throws IOException
     */
    @Override
    public boolean remove(K key) throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("removing item for key: " + key);
        }

        boolean removed = false;

        // handle partial removal
        if (key instanceof String && ((String) key).endsWith(CacheConstants.NAME_COMPONENT_DELIMITER))
        {
            // remove all keys of the same name hierarchy.
            for (Iterator<Map.Entry<K, MemoryElementDescriptor<K, V>>> itr = map.entrySet().iterator();
                    itr.hasNext();)
            {
                Map.Entry<K, MemoryElementDescriptor<K, V>> entry = itr.next();
                K k = entry.getKey();

                if (k instanceof String && ((String) k).startsWith(key.toString()))
                {
                    lock.lock();
                    try
                    {
                        strongReferences.remove(entry.getValue().getCacheElement());
                        itr.remove();
                        removed = true;
                    }
                    finally
                    {
                        lock.unlock();
                    }
                }
            }
        }
        else if (key instanceof GroupAttrName && ((GroupAttrName<?>) key).attrName == null)
        {
            // remove all keys of the same name hierarchy.
            for (Iterator<Map.Entry<K, MemoryElementDescriptor<K, V>>> itr = map.entrySet().iterator();
                    itr.hasNext();)
            {
                Map.Entry<K, MemoryElementDescriptor<K, V>> entry = itr.next();
                K k = entry.getKey();

                if (k instanceof GroupAttrName && ((GroupAttrName<?>) k).groupId.equals(((GroupAttrName<?>) key).groupId))
                {
                    lock.lock();
                    try
                    {
                        strongReferences.remove(entry.getValue().getCacheElement());
                        itr.remove();
                        removed = true;
                    }
                    finally
                    {
                        lock.unlock();
                    }
                }
            }
        }
        else
        {
            // remove single item.
            lock.lock();
            try
            {
                MemoryElementDescriptor<K, V> me = map.remove(key);
                if (me != null)
                {
                    strongReferences.remove(me.getCacheElement());
                    removed = true;
                }
            }
            finally
            {
                lock.unlock();
            }
        }

        return removed;
    }

    /**
     * Removes all cached items from the cache.
     * <p>
     * @throws IOException
     */
    @Override
    public void removeAll() throws IOException
    {
        super.removeAll();
        strongReferences.clear();
    }

    /**
     * Puts an item to the cache.
     * <p>
     * @param ce Description of the Parameter
     * @throws IOException Description of the Exception
     */
    @Override
    public void update(ICacheElement<K, V> ce) throws IOException
    {
        putCnt.incrementAndGet();
        ce.getElementAttributes().setLastAccessTimeNow();

        lock.lock();

        try
        {
            map.put(ce.getKey(), new SoftReferenceElementDescriptor<K, V>(ce));
            strongReferences.add(ce);
            trimStrongReferences();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Trim the number of strong references to equal or below the number given
     * by the maxObjects parameter.
     */
    private void trimStrongReferences()
    {
        int max = getCacheAttributes().getMaxObjects();
        int startsize = strongReferences.size();

        for (int cursize = startsize; cursize > max; cursize--)
        {
            ICacheElement<K, V> ce = strongReferences.poll();
            waterfal(ce);
        }
    }

    /**
     * Get an item from the cache
     * <p>
     * @param key Description of the Parameter
     * @return Description of the Return Value
     * @throws IOException Description of the Exception
     */
    @Override
    public ICacheElement<K, V> get(K key) throws IOException
    {
        ICacheElement<K, V> val = null;
        lock.lock();

        try
        {
            val = getQuiet(key);
            if (val != null)
            {
                val.getElementAttributes().setLastAccessTimeNow();

                // update the ordering of the strong references
                strongReferences.add(val);
                trimStrongReferences();
            }
        }
        finally
        {
            lock.unlock();
        }

        if (val == null)
        {
            missCnt.incrementAndGet();
        }
        else
        {
            hitCnt.incrementAndGet();
        }

        return val;
    }

    /**
     * This can't be implemented.
     * <p>
     * @param numberToFree
     * @return 0
     * @throws IOException
     */
    @Override
    public int freeElements(int numberToFree) throws IOException
    {
        return 0;
    }
}
