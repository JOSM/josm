package org.apache.commons.jcs.engine.memory;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.jcs.engine.CacheConstants;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.group.GroupAttrName;
import org.apache.commons.jcs.engine.memory.util.MemoryElementDescriptor;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.jcs.utils.struct.DoubleLinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains methods that are common to memory caches using the double linked list, such
 * as the LRU, MRU, FIFO, and LIFO caches.
 * <p>
 * Children can control the expiration algorithm by controlling the update and get. The last item in the list will be the one
 * removed when the list fills. For instance LRU should more items to the front as they are used. FIFO should simply add new items
 * to the front of the list.
 */
public abstract class AbstractDoubleLinkedListMemoryCache<K, V> extends AbstractMemoryCache<K, V>
{
    /** The logger. */
    private static final Log log = LogFactory.getLog(AbstractDoubleLinkedListMemoryCache.class);

    /** thread-safe double linked list for lru */
    protected DoubleLinkedList<MemoryElementDescriptor<K, V>> list; // TODO privatise

    /** number of hits */
    private AtomicInteger hitCnt;

    /** number of misses */
    private AtomicInteger missCnt;

    /** number of puts */
    private AtomicInteger putCnt;

    /**
     * For post reflection creation initialization.
     * <p>
     *
     * @param hub
     */
    @Override
    public void initialize(CompositeCache<K, V> hub)
    {
        lock.lock();
        try
        {
            super.initialize(hub);
            hitCnt = new AtomicInteger(0);
            missCnt = new AtomicInteger(0);
            putCnt = new AtomicInteger(0);
            list = new DoubleLinkedList<MemoryElementDescriptor<K, V>>();
            log.info("initialized MemoryCache for " + getCacheName());
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * This is called by super initialize.
     *
     * NOTE: should return a thread safe map
     *
     * <p>
     *
     * @return new ConcurrentHashMap()
     */
    @Override
    public Map<K, MemoryElementDescriptor<K, V>> createMap()
    {
        return new ConcurrentHashMap<K, MemoryElementDescriptor<K, V>>();
    }

    /**
     * Calls the abstract method updateList.
     * <p>
     * If the max size is reached, an element will be put to disk.
     * <p>
     *
     * @param ce
     *            The cache element, or entry wrapper
     * @throws IOException
     */
    @Override
    public final void update(ICacheElement<K, V> ce) throws IOException
    {
        putCnt.incrementAndGet();

        lock.lock();
        try
        {
            MemoryElementDescriptor<K, V> newNode = adjustListForUpdate(ce);

            // this should be synchronized if we were not using a ConcurrentHashMap
            MemoryElementDescriptor<K, V> oldNode = map.put(newNode.ce.getKey(), newNode);

            // If the node was the same as an existing node, remove it.
            if (oldNode != null && newNode.ce.getKey().equals(oldNode.ce.getKey()))
            {
                list.remove(oldNode);
            }
        }
        finally
        {
            lock.unlock();
        }

        // If we are over the max spool some
        spoolIfNeeded();
    }

    /**
     * Children implement this to control the cache expiration algorithm
     * <p>
     *
     * @param ce
     * @return MemoryElementDescriptor the new node
     * @throws IOException
     */
    protected abstract MemoryElementDescriptor<K, V> adjustListForUpdate(ICacheElement<K, V> ce) throws IOException;

    /**
     * If the max size has been reached, spool.
     * <p>
     *
     * @throws Error
     */
    private void spoolIfNeeded() throws Error
    {
        int size = map.size();
        // If the element limit is reached, we need to spool

        if (size <= this.getCacheAttributes().getMaxObjects())
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug("In memory limit reached, spooling");
        }

        // Write the last 'chunkSize' items to disk.
        int chunkSizeCorrected = Math.min(size, chunkSize);

        if (log.isDebugEnabled())
        {
            log.debug("About to spool to disk cache, map size: " + size + ", max objects: "
                + this.getCacheAttributes().getMaxObjects() + ", maximum items to spool: " + chunkSizeCorrected);
        }

        // The spool will put them in a disk event queue, so there is no
        // need to pre-queue the queuing. This would be a bit wasteful
        // and wouldn't save much time in this synchronous call.
        lock.lock();

        try
        {
            for (int i = 0; i < chunkSizeCorrected; i++)
            {
                ICacheElement<K, V> lastElement = spoolLastElement();
                if (lastElement == null)
                {
                    break;
                }
            }

            // If this is out of the sync block it can detect a mismatch
            // where there is none.
            if (log.isDebugEnabled() && map.size() != list.size())
            {
                log.debug("update: After spool, size mismatch: map.size() = " + map.size() + ", linked list size = " + list.size());
            }
        }
        finally
        {
            lock.unlock();
        }

        if (log.isDebugEnabled())
        {
            log.debug("update: After spool map size: " + map.size() + " linked list size = " + list.size());
        }
    }

    /**
     * Get an item from the cache If the item is found, it is removed from the list and added first.
     * <p>
     *
     * @param key
     *            Identifies item to find
     * @return ICacheElement<K, V> if found, else null
     * @throws IOException
     */
    @Override
    public final ICacheElement<K, V> get(K key) throws IOException
    {
        ICacheElement<K, V> ce = null;

        if (log.isDebugEnabled())
        {
            log.debug(getCacheName() + ": getting item for key " + key);
        }

        MemoryElementDescriptor<K, V> me = map.get(key);

        if (me != null)
        {
            hitCnt.incrementAndGet();

            lock.lock();
            try
            {
                ce = me.ce;
                // ABSTRACT
                adjustListForGet(me);
            }
            finally
            {
                lock.unlock();
            }

            if (log.isDebugEnabled())
            {
                log.debug(getCacheName() + ": LRUMemoryCache hit for " + key);
            }
        }
        else
        {
            missCnt.incrementAndGet();

            if (log.isDebugEnabled())
            {
                log.debug(getCacheName() + ": LRUMemoryCache miss for " + key);
            }
        }

        if (log.isDebugEnabled())
        {
            verifyCache();
        }

        return ce;
    }

    /**
     * Adjust the list as needed for a get. This allows children to control the algorithm
     * <p>
     *
     * @param me
     */
    protected abstract void adjustListForGet(MemoryElementDescriptor<K, V> me);

    /**
     * This instructs the memory cache to remove the <i>numberToFree</i> according to its eviction
     * policy. For example, the LRUMemoryCache will remove the <i>numberToFree</i> least recently
     * used items. These will be spooled to disk if a disk auxiliary is available.
     * <p>
     *
     * @param numberToFree
     * @return the number that were removed. if you ask to free 5, but there are only 3, you will
     *         get 3.
     * @throws IOException
     */
    @Override
    public int freeElements(int numberToFree) throws IOException
    {
        int freed = 0;

        lock.lock();

        try
        {
            for (; freed < numberToFree; freed++)
            {
                ICacheElement<K, V> element = spoolLastElement();
                if (element == null)
                {
                    break;
                }
            }
        }
        finally
        {
            lock.unlock();
        }

        return freed;
    }

    /**
     * This spools the last element in the LRU, if one exists.
     * <p>
     *
     * @return ICacheElement<K, V> if there was a last element, else null.
     * @throws Error
     */
    private ICacheElement<K, V> spoolLastElement() throws Error
    {
        ICacheElement<K, V> toSpool = null;

        final MemoryElementDescriptor<K, V> last = list.getLast();
        if (last != null)
        {
            toSpool = last.ce;
            if (toSpool != null)
            {
                getCompositeCache().spoolToDisk(toSpool);
                if (map.remove(toSpool.getKey()) == null)
                {
                    log.warn("update: remove failed for key: " + toSpool.getKey());

                    if (log.isDebugEnabled())
                    {
                        verifyCache();
                    }
                }
            }
            else
            {
                throw new Error("update: last.ce is null!");
            }

            list.remove(last);
        }

        return toSpool;
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
            for (Iterator<Map.Entry<K, MemoryElementDescriptor<K, V>>> itr = map.entrySet().iterator(); itr.hasNext();)
            {
                Map.Entry<K, MemoryElementDescriptor<K, V>> entry = itr.next();
                K k = entry.getKey();

                if (k instanceof String && ((String) k).startsWith(key.toString()))
                {
                    lock.lock();
                    try
                    {
                        list.remove(entry.getValue());
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
            for (Iterator<Map.Entry<K, MemoryElementDescriptor<K, V>>> itr = map.entrySet().iterator(); itr.hasNext();)
            {
                Map.Entry<K, MemoryElementDescriptor<K, V>> entry = itr.next();
                K k = entry.getKey();

                if (k instanceof GroupAttrName && ((GroupAttrName<?>) k).groupId.equals(((GroupAttrName<?>) key).groupId))
                {
                    lock.lock();
                    try
                    {
                        list.remove(entry.getValue());
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
                    list.remove(me);
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
     * Remove all of the elements from both the Map and the linked list implementation. Overrides
     * base class.
     * <p>
     *
     * @throws IOException
     */
    @Override
    public void removeAll() throws IOException
    {
        lock.lock();
        try
        {
            list.removeAll();
            map.clear();
        }
        finally
        {
            lock.unlock();
        }
    }

    // --------------------------- internal methods (linked list implementation)
    /**
     * Adds a new node to the start of the link list.
     * <p>
     *
     * @param ce
     *            The feature to be added to the First
     * @return MemoryElementDescriptor
     */
    protected MemoryElementDescriptor<K, V> addFirst(ICacheElement<K, V> ce)
    {
        lock.lock();
        try
        {
            MemoryElementDescriptor<K, V> me = new MemoryElementDescriptor<K, V>(ce);
            list.addFirst(me);
            if ( log.isDebugEnabled() )
            {
                verifyCache(ce.getKey());
            }
            return me;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Adds a new node to the end of the link list.
     * <p>
     *
     * @param ce
     *            The feature to be added to the First
     * @return MemoryElementDescriptor
     */
    protected MemoryElementDescriptor<K, V> addLast(ICacheElement<K, V> ce)
    {
        lock.lock();
        try
        {
            MemoryElementDescriptor<K, V> me = new MemoryElementDescriptor<K, V>(ce);
            list.addLast(me);
            if ( log.isDebugEnabled() )
            {
                verifyCache(ce.getKey());
            }
            return me;
        }
        finally
        {
            lock.unlock();
        }
    }

    // ---------------------------------------------------------- debug methods

    /**
     * Dump the cache entries from first to list for debugging.
     */
    @SuppressWarnings("unchecked")
    // No generics for public fields
    private void dumpCacheEntries()
    {
        log.debug("dumpingCacheEntries");
        for (MemoryElementDescriptor<K, V> me = list.getFirst(); me != null; me = (MemoryElementDescriptor<K, V>) me.next)
        {
            log.debug("dumpCacheEntries> key=" + me.ce.getKey() + ", val=" + me.ce.getVal());
        }
    }

    /**
     * Checks to see if all the items that should be in the cache are. Checks consistency between
     * List and map.
     */
    @SuppressWarnings("unchecked")
    // No generics for public fields
    private void verifyCache()
    {
        boolean found = false;
        log.debug("verifycache[" + getCacheName() + "]: mapContains " + map.size() + " elements, linked list contains "
            + list.size() + " elements");
        log.debug("verifycache: checking linked list by key ");
        for (MemoryElementDescriptor<K, V> li = list.getFirst(); li != null; li = (MemoryElementDescriptor<K, V>) li.next)
        {
            Object key = li.ce.getKey();
            if (!map.containsKey(key))
            {
                log.error("verifycache[" + getCacheName() + "]: map does not contain key : " + li.ce.getKey());
                log.error("li.hashcode=" + li.ce.getKey().hashCode());
                log.error("key class=" + key.getClass());
                log.error("key hashcode=" + key.hashCode());
                log.error("key toString=" + key.toString());
                if (key instanceof GroupAttrName)
                {
                    GroupAttrName<?> name = (GroupAttrName<?>) key;
                    log.error("GroupID hashcode=" + name.groupId.hashCode());
                    log.error("GroupID.class=" + name.groupId.getClass());
                    log.error("AttrName hashcode=" + name.attrName.hashCode());
                    log.error("AttrName.class=" + name.attrName.getClass());
                }
                dumpMap();
            }
            else if (map.get(li.ce.getKey()) == null)
            {
                log.error("verifycache[" + getCacheName() + "]: linked list retrieval returned null for key: " + li.ce.getKey());
            }
        }

        log.debug("verifycache: checking linked list by value ");
        for (MemoryElementDescriptor<K, V> li3 = list.getFirst(); li3 != null; li3 = (MemoryElementDescriptor<K, V>) li3.next)
        {
            if (map.containsValue(li3) == false)
            {
                log.error("verifycache[" + getCacheName() + "]: map does not contain value : " + li3);
                dumpMap();
            }
        }

        log.debug("verifycache: checking via keysets!");
        for (Object val : map.keySet())
        {
            found = false;

            for (MemoryElementDescriptor<K, V> li2 = list.getFirst(); li2 != null; li2 = (MemoryElementDescriptor<K, V>) li2.next)
            {
                if (val.equals(li2.ce.getKey()))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                log.error("verifycache[" + getCacheName() + "]: key not found in list : " + val);
                dumpCacheEntries();
                if (map.containsKey(val))
                {
                    log.error("verifycache: map contains key");
                }
                else
                {
                    log.error("verifycache: map does NOT contain key, what the HECK!");
                }
            }
        }
    }

    /**
     * Logs an error if an element that should be in the cache is not.
     * <p>
     *
     * @param key
     */
    @SuppressWarnings("unchecked")
    // No generics for public fields
    private void verifyCache(K key)
    {
        boolean found = false;

        // go through the linked list looking for the key
        for (MemoryElementDescriptor<K, V> li = list.getFirst(); li != null; li = (MemoryElementDescriptor<K, V>) li.next)
        {
            if (li.ce.getKey() == key)
            {
                found = true;
                log.debug("verifycache(key) key match: " + key);
                break;
            }
        }
        if (!found)
        {
            log.error("verifycache(key)[" + getCacheName() + "], couldn't find key! : " + key);
        }
    }

    /**
     * Get an Array of the keys for all elements in the memory cache
     *
     * @return An Object[]
     */
    @Override
    public Set<K> getKeySet()
    {
        return new LinkedHashSet<K>(map.keySet());
    }

    /**
     * This returns semi-structured information on the memory cache, such as the size, put count,
     * hit count, and miss count.
     * <p>
     *
     * @see org.apache.commons.jcs.engine.memory.behavior.IMemoryCache#getStatistics()
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( /* add algorithm name */"Memory Cache");

        ArrayList<IStatElement<?>> elems = new ArrayList<IStatElement<?>>();

        lock.lock(); // not sure that's really relevant here but not that important
        try
        {
            elems.add(new StatElement<Integer>("List Size", Integer.valueOf(list.size())));
            elems.add(new StatElement<Integer>("Map Size", Integer.valueOf(map.size())));
            elems.add(new StatElement<AtomicInteger>("Put Count", putCnt));
            elems.add(new StatElement<AtomicInteger>("Hit Count", hitCnt));
            elems.add(new StatElement<AtomicInteger>("Miss Count", missCnt));
        }
        finally
        {
            lock.unlock();
        }

        stats.setStatElements(elems);

        return stats;
    }
}
