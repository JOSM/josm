package org.apache.commons.jcs.utils.struct;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.jcs.engine.control.group.GroupAttrName;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a simple LRUMap. It implements most of the map methods. It is not recommended that you
 * use any but put, get, remove, and clear.
 * <p>
 * Children can implement the processRemovedLRU method if they want to handle the removal of the
 * least recently used item.
 * <p>
 * This class was abstracted out of the LRU Memory cache. Put, remove, and get should be thread
 * safe. It uses a hashtable and our own double linked list.
 * <p>
 * Locking is done on the instance.
 * <p>
 * @author aaron smuts
 */
public abstract class AbstractLRUMap<K, V>
    implements Map<K, V>
{
    /** The logger */
    private static final Log log = LogFactory.getLog( AbstractLRUMap.class );

    /** double linked list for lru */
    private final DoubleLinkedList<LRUElementDescriptor<K, V>> list;

    /** Map where items are stored by key. */
    private final Map<K, LRUElementDescriptor<K, V>> map;

    /** lock to keep map and list synchronous */
    private final Lock lock = new ReentrantLock();

    /** stats */
    private long hitCnt = 0;

    /** stats */
    private long missCnt = 0;

    /** stats */
    private long putCnt = 0;

    /**
     * This creates an unbounded version. Setting the max objects will result in spooling on
     * subsequent puts.
     */
    public AbstractLRUMap()
    {
        list = new DoubleLinkedList<LRUElementDescriptor<K, V>>();

        // normal hashtable is faster for
        // sequential keys.
        map = new ConcurrentHashMap<K, LRUElementDescriptor<K, V>>();
    }


    /**
     * This simply returns the number of elements in the map.
     * <p>
     * @see java.util.Map#size()
     */
    @Override
    public int size()
    {
        return map.size();
    }

    /**
     * This removes all the items. It clears the map and the double linked list.
     * <p>
     * @see java.util.Map#clear()
     */
    @Override
    public void clear()
    {
        lock.lock();
        try
        {
            map.clear();
            list.removeAll();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Returns true if the map is empty.
     * <p>
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    /**
     * Returns true if the map contains an element for the supplied key.
     * <p>
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey( Object key )
    {
        return map.containsKey( key );
    }

    /**
     * This is an expensive operation that determines if the object supplied is mapped to any key.
     * <p>
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue( Object value )
    {
        return map.containsValue( value );
    }

    /**
     * @return map.values();
     */
    @Override
    public Collection<V> values()
    {
        return map.values().stream()
                .map(value -> value.getPayload())
                .collect(Collectors.toList());
    }

    /**
     * @param source
     */
    @Override
    public void putAll( Map<? extends K, ? extends V> source )
    {
        if ( source != null )
        {
            source.entrySet()
                .forEach(entry -> put(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * @param key
     * @return Object
     */
    @Override
    public V get( Object key )
    {
        V retVal;

        if ( log.isDebugEnabled() )
        {
            log.debug( "getting item  for key " + key );
        }

        LRUElementDescriptor<K, V> me = map.get( key );

        if ( me == null )
        {
            missCnt++;
            retVal = null;
        }
        else
        {
            hitCnt++;
            retVal = me.getPayload();
            list.makeFirst( me );
        }

        if ( log.isDebugEnabled() )
        {
            if ( me == null )
            {
                log.debug( "LRUMap miss for " + key );
            }
            else
            {
                log.debug( "LRUMap hit for " + key );
            }
        }

        // verifyCache();
        return retVal;
    }

    /**
     * This gets an element out of the map without adjusting it's position in the LRU. In other
     * words, this does not count as being used. If the element is the last item in the list, it
     * will still be the last time in the list.
     * <p>
     * @param key
     * @return Object
     */
    public V getQuiet( Object key )
    {
        V ce = null;
        LRUElementDescriptor<K, V> me = map.get( key );

        if ( me != null )
        {
            ce = me.getPayload();
        }

        if ( log.isDebugEnabled() )
        {
            if ( me == null )
            {
                log.debug( "LRUMap quiet miss for " + key );
            }
            else
            {
                log.debug( "LRUMap quiet hit for " + key );
            }
        }

        return ce;
    }

    /**
     * @param key
     * @return Object removed
     */
    @Override
    public V remove( Object key )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "removing item for key: " + key );
        }

        // remove single item.
        lock.lock();
        try
        {
            LRUElementDescriptor<K, V> me = map.remove(key);

            if (me != null)
            {
                list.remove(me);
                return me.getPayload();
            }
        }
        finally
        {
            lock.unlock();
        }

        return null;
    }

    /**
     * @param key
     * @param value
     * @return Object
     */
    @Override
    public V put(K key, V value)
    {
        putCnt++;

        LRUElementDescriptor<K, V> old = null;
        LRUElementDescriptor<K, V> me = new LRUElementDescriptor<K, V>(key, value);

        lock.lock();
        try
        {
            list.addFirst( me );
            old = map.put(key, me);

            // If the node was the same as an existing node, remove it.
            if ( old != null && key.equals(old.getKey()))
            {
                list.remove( old );
            }
        }
        finally
        {
            lock.unlock();
        }

        // If the element limit is reached, we need to spool
        if (shouldRemove())
        {
            if (log.isDebugEnabled())
            {
                log.debug( "In memory limit reached, removing least recently used." );
            }

            // The spool will put them in a disk event queue, so there is no
            // need to pre-queue the queuing. This would be a bit wasteful
            // and wouldn't save much time in this synchronous call.
            while (shouldRemove())
            {
                lock.lock();
                try
                {
                    LRUElementDescriptor<K, V> last = list.getLast();
                    if (last != null)
                    {
                        processRemovedLRU(last.getKey(), last.getPayload());
                        if (map.remove(last.getKey()) == null)
                        {
                            log.warn("update: remove failed for key: "
                                    + last.getKey());
                            verifyCache();
                        }
                        list.removeLast();
                    }
                    else
                    {
                        verifyCache();
                        throw new Error("update: last is null!");
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "update: After spool map size: " + map.size() );
            }
            if ( map.size() != list.size() )
            {
                log.error("update: After spool, size mismatch: map.size() = " + map.size() +
                        ", linked list size = " + list.size());
            }
        }

        if ( old != null )
        {
            return old.getPayload();
        }
        return null;
    }

    protected abstract boolean shouldRemove();

    /**
     * Dump the cache entries from first to list for debugging.
     */
    @SuppressWarnings("unchecked") // No generics for public fields
    public void dumpCacheEntries()
    {
        if (log.isDebugEnabled())
        {
            log.debug("dumpingCacheEntries");
            for (LRUElementDescriptor<K, V> me = list.getFirst(); me != null; me = (LRUElementDescriptor<K, V>) me.next)
            {
                log.debug("dumpCacheEntries> key=" + me.getKey() + ", val=" + me.getPayload());
            }
        }
    }

    /**
     * Dump the cache map for debugging.
     */
    public void dumpMap()
    {
        if (log.isDebugEnabled())
        {
            log.debug("dumpingMap");
            map.entrySet().forEach(e ->
                log.debug("dumpMap> key=" + e.getKey() + ", val=" + e.getValue().getPayload()));
        }
    }

    /**
     * Checks to see if all the items that should be in the cache are. Checks consistency between
     * List and map.
     */
    @SuppressWarnings("unchecked") // No generics for public fields
    protected void verifyCache()
    {
        if ( !log.isDebugEnabled() )
        {
            return;
        }

        boolean found = false;
        log.debug( "verifycache: mapContains " + map.size() +
                " elements, linked list contains " + list.size() + " elements" );
        log.debug( "verifycache: checking linked list by key " );
        for (LRUElementDescriptor<K, V> li = list.getFirst(); li != null; li = (LRUElementDescriptor<K, V>) li.next )
        {
            K key = li.getKey();
            if ( !map.containsKey( key ) )
            {
                log.error( "verifycache: map does not contain key : " + li.getKey() );
                log.error( "li.hashcode=" + li.getKey().hashCode() );
                log.error( "key class=" + key.getClass() );
                log.error( "key hashcode=" + key.hashCode() );
                log.error( "key toString=" + key.toString() );
                if ( key instanceof GroupAttrName )
                {
                    GroupAttrName<?> name = (GroupAttrName<?>) key;
                    log.error( "GroupID hashcode=" + name.groupId.hashCode() );
                    log.error( "GroupID.class=" + name.groupId.getClass() );
                    log.error( "AttrName hashcode=" + name.attrName.hashCode() );
                    log.error( "AttrName.class=" + name.attrName.getClass() );
                }
                dumpMap();
            }
            else if ( map.get( li.getKey() ) == null )
            {
                log.error( "verifycache: linked list retrieval returned null for key: " + li.getKey() );
            }
        }

        log.debug( "verifycache: checking linked list by value " );
        for (LRUElementDescriptor<K, V> li3 = list.getFirst(); li3 != null; li3 = (LRUElementDescriptor<K, V>) li3.next )
        {
            if ( map.containsValue( li3 ) == false )
            {
                log.error( "verifycache: map does not contain value : " + li3 );
                dumpMap();
            }
        }

        log.debug( "verifycache: checking via keysets!" );
        map.forEach((key, value) -> {
            boolean _found = false;

            for (LRUElementDescriptor<K, V> li2 = list.getFirst(); li2 != null; li2 = (LRUElementDescriptor<K, V>) li2.next )
            {
                if ( key.equals( li2.getKey() ) )
                {
                    _found = true;
                    break;
                }
            }
            if ( !_found )
            {
                log.error( "verifycache: key not found in list : " + key );
                dumpCacheEntries();
                if ( map.containsKey( key ) )
                {
                    log.error( "verifycache: map contains key" );
                }
                else
                {
                    log.error( "verifycache: map does NOT contain key, what the HECK!" );
                }
            }
        });
    }

    /**
     * This is called when an item is removed from the LRU. We just log some information.
     * <p>
     * Children can implement this method for special behavior.
     * @param key
     * @param value
     */
    protected void processRemovedLRU(K key, V value )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Removing key: [" + key + "] from LRUMap store, value = [" + value + "]" );
            log.debug( "LRUMap store size: '" + this.size() + "'." );
        }
    }

    /**
     * @return IStats
     */
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "LRUMap" );

        ArrayList<IStatElement<?>> elems = new ArrayList<IStatElement<?>>();

        elems.add(new StatElement<Integer>( "List Size", Integer.valueOf(list.size()) ) );
        elems.add(new StatElement<Integer>( "Map Size", Integer.valueOf(map.size()) ) );
        elems.add(new StatElement<Long>( "Put Count", Long.valueOf(putCnt) ) );
        elems.add(new StatElement<Long>( "Hit Count", Long.valueOf(hitCnt) ) );
        elems.add(new StatElement<Long>( "Miss Count", Long.valueOf(missCnt) ) );

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * This returns a set of entries. Our LRUMapEntry is used since the value stored in the
     * underlying map is a node in the double linked list. We wouldn't want to return this to the
     * client, so we construct a new entry with the payload of the node.
     * <p>
     * TODO we should return out own set wrapper, so we can avoid the extra object creation if it
     * isn't necessary.
     * <p>
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        lock.lock();
        try
        {
            return map.entrySet().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<K, V>(
                            entry.getKey(), entry.getValue().getPayload()))
                    .collect(Collectors.toSet());
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * @return map.keySet();
     */
    @Override
    public Set<K> keySet()
    {
        return map.values().stream()
                .map(value -> value.getKey())
                .collect(Collectors.toSet());
    }

}
