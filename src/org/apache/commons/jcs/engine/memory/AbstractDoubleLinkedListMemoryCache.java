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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.group.GroupAttrName;
import org.apache.commons.jcs.engine.memory.util.MemoryElementDescriptor;
import org.apache.commons.jcs.engine.stats.StatElement;
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

    /**
     * For post reflection creation initialization.
     * <p>
     *
     * @param hub
     */
    @Override
    public void initialize(CompositeCache<K, V> hub)
    {
        super.initialize(hub);
        list = new DoubleLinkedList<>();
        log.info("initialized MemoryCache for " + getCacheName());
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
    public ConcurrentMap<K, MemoryElementDescriptor<K, V>> createMap()
    {
        return new ConcurrentHashMap<>();
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
            final K key = newNode.getCacheElement().getKey();
            MemoryElementDescriptor<K, V> oldNode = map.put(key, newNode);

            // If the node was the same as an existing node, remove it.
            if (oldNode != null && key.equals(oldNode.getCacheElement().getKey()))
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
     * @return ICacheElement&lt;K, V&gt; if there was a last element, else null.
     * @throws Error
     */
    private ICacheElement<K, V> spoolLastElement() throws Error
    {
        ICacheElement<K, V> toSpool = null;

        final MemoryElementDescriptor<K, V> last = list.getLast();
        if (last != null)
        {
            toSpool = last.getCacheElement();
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
     * @see org.apache.commons.jcs.engine.memory.AbstractMemoryCache#get(java.lang.Object)
     */
    @Override
    public ICacheElement<K, V> get(K key) throws IOException
    {
        ICacheElement<K, V> ce = super.get(key);

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
     * Update control structures after get
     * (guarded by the lock)
     *
     * @param me the memory element descriptor
     */
    @Override
    protected void lockedGetElement(MemoryElementDescriptor<K, V> me)
    {
        adjustListForGet(me);
    }

    /**
     * Remove element from control structure
     * (guarded by the lock)
     *
     * @param me the memory element descriptor
     */
    @Override
    protected void lockedRemoveElement(MemoryElementDescriptor<K, V> me)
    {
        list.remove(me);
    }

    /**
     * Removes all cached items from the cache control structures.
     * (guarded by the lock)
     */
    @Override
    protected void lockedRemoveAll()
    {
        list.removeAll();
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
            MemoryElementDescriptor<K, V> me = new MemoryElementDescriptor<>(ce);
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
            MemoryElementDescriptor<K, V> me = new MemoryElementDescriptor<>(ce);
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
            log.debug("dumpCacheEntries> key=" + me.getCacheElement().getKey() + ", val=" + me.getCacheElement().getVal());
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
            K key = li.getCacheElement().getKey();
            if (!map.containsKey(key))
            {
                log.error("verifycache[" + getCacheName() + "]: map does not contain key : " + key);
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
            else if (map.get(key) == null)
            {
                log.error("verifycache[" + getCacheName() + "]: linked list retrieval returned null for key: " + key);
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
                if (val.equals(li2.getCacheElement().getKey()))
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
            if (li.getCacheElement().getKey() == key)
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
     * This returns semi-structured information on the memory cache, such as the size, put count,
     * hit count, and miss count.
     * <p>
     *
     * @see org.apache.commons.jcs.engine.memory.behavior.IMemoryCache#getStatistics()
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = super.getStatistics();
        stats.setTypeName( /* add algorithm name */"Memory Cache");

        List<IStatElement<?>> elems = stats.getStatElements();

        elems.add(new StatElement<>("List Size", Integer.valueOf(list.size())));

        return stats;
    }
}
