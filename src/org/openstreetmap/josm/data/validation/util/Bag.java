// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * A very simple bag to store multiple occurences of a same key.
 * <p>
 * The bag will keep, for each key, a list of values.
 *
 * @author frsantos
 *
 * @param <K> The key class
 * @param <V> The value class
 */
public class Bag<K,V> extends HashMap<K, List<V>>
{
    /** Serializable ID */
    private static final long serialVersionUID = 5374049172859211610L;

    /**
     * Returns the list of elements with the same key
     * @param key The key to obtain the elements
     * @return the list of elements with the same key
     */
    public List<V> get(K key)
    {
        return super.get(key);
    }

    /**
     * Adds an element to the bag
     * @param key The key of the element
     * @param value The element to add
     */
    public void add(K key, V value)
    {
        List<V> values = get(key);
        if( values == null )
        {
            values = new ArrayList<V>();
            put(key, values);
        }
        values.add(value);
    }

    /**
     * Adds an element to the bag
     * @param key The key of the element
     * @param value The element to add
     */
    public void add(K key)
    {
        List<V> values = get(key);
        if( values == null )
        {
            values = new ArrayList<V>();
            put(key, values);
        }
    }

    /**
     * Constructor
     */
    public Bag()
    {
        super();
    }

    /**
     * Constructor
     *
     * @param initialCapacity The initial capacity
     */
    public Bag(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Returns true if the bag contains a value for a key
     * @param key The key
     * @param value The value
     * @return true if the key contains the value
     */
    public boolean contains(K key, V value)
    {
        List<V> values = get(key);
        return (values == null) ? false : values.contains(value);
    }
}
