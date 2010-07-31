/*
 *  JOSMng - a Java Open Street Map editor, the next generation.
 *
 *  Copyright (C) 2008 Petr Nejedly <P.Nejedly@sh.cvut.cz>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.openstreetmap.josm.data.osm;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Set-like class that allows looking up equivalent preexising instance.
 * It is useful whereever one would use self-mapping construct like
 * <code>Map<T,T>.put(t,t), that is, for caches, uniqueness filters or similar.
 *
 * The semantics of equivalency can be external to the object, using the
 * {@link Hash} interface. The set also supports querying for entries using
 * different key type, in case you can provide a Hash implementation
 * that can resolve the equality.
 *
 * <h2>Examples</h2>
 * <ul><li>A String cache:
 * <pre>
 * Storage<String> cache = new Storage(); // use default Hash
 * for (String input : data) {
 *     String onlyOne = cache.putIfUnique(input);
 *     ....
 * }
 * </pre></li>
 * <li>Identity-based set:
 * <pre>
 * Storage<Object> identity = new Storage(new Hash<Object,Object> {
 *     public int getHashCode(Object o) {
 *         return System.identityHashCode(o);
 *     }
 *     public boolean equals(Object o1, Object o2) {
 *         return o1 == o2;
 *     }
 *  });
 * </pre></li>
 * <li>An object with int ID and id-based lookup:
 * <pre>
 * class Thing { int id; }
 * Storage<Thing> things = new Storage(new Hash<Thing,Thing>() {
 *     public int getHashCode(Thing t) {
 *         return t.id;
 *     }
 *     public boolean equals(Thing t1, Thing t2) {
 *         return t1 == t2;
 *     }
 *  });
 * Map<Integer,Thing> fk = things.foreignKey(new Hash<Integer,Thing>() {
 *     public int getHashCode(Integer i) {
 *         return i.getIntValue();
 *     }
 *     public boolean equals(Integer k, Thing t) {
 *         return t.id == k.getIntvalue();
 *     }
 * }
 *
 * things.put(new Thing(3));
 * assert things.get(new Thing(3)) == fk.get(3);
 * </pre></li>
 *
 *
 * @author nenik
 */
public class Storage<T> extends AbstractSet<T> {
    private final Hash<? super T,? super T> hash;
    private T[] data;
    private int mask;
    private int size;
    private transient volatile int modCount = 0;
    private float loadFactor = 0.6f;
    private final boolean safeIterator;
    private boolean arrayCopyNecessary;

    public Storage() {
        this(Storage.<T>defaultHash());
    }

    public Storage(int capacity) {
        this(Storage.<T>defaultHash(), capacity, false);
    }

    public Storage(Hash<? super T,? super T> ha) {
        this(ha, 16, false);
    }

    public Storage(Hash<? super T, ? super T> ha, int capacity, boolean safeIterator) {
        this.hash = ha;
        int cap = 1 << (int)(Math.ceil(Math.log(capacity/loadFactor) / Math.log(2)));
        data = (T[]) new Object[cap];
        mask = data.length - 1;
        this.safeIterator = safeIterator;
    }

    private void copyArray() {
        if (arrayCopyNecessary) {
            T[] newData = (T[]) new Object[data.length];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
            arrayCopyNecessary = false;
        }
    }

    // --------------- Collection implementation ------------------------
    @Override
    public synchronized int size() {
        return size;
    }

    @Override
    public synchronized Iterator<T> iterator() {
        if (safeIterator) {
            arrayCopyNecessary = true;
            return new SafeReadonlyIter(data);
        } else
            return new Iter();

    }

    @Override
    public synchronized boolean contains(Object o) {
        int bucket = getBucket(hash, (T)o);
        return bucket >= 0;
    }

    @Override 
    public synchronized boolean add(T t) {
        T orig = putUnique(t);
        return orig == t;
    }

    @Override
    public synchronized boolean remove(Object o) {
        T orig = removeElem((T)o);
        return orig != null;
    }
    
    @Override
    public synchronized void clear() {
        copyArray();
        modCount++;
        size = 0;
        for (int i = 0; i<data.length; i++) {
            data[i] = null;
        }
    }

    @Override
    public synchronized int hashCode() {
        int h = 0;
        for (T t : this) {
            h += hash.getHashCode(t);
        }
        return h;
    }

    // ----------------- Extended API ----------------------------

    public synchronized T put(T t) {
        copyArray();
        modCount++;
        ensureSpace();

        int bucket = getBucket(hash, t);
        if (bucket < 0) {
            size++;
            bucket = ~bucket;
            assert data[bucket] == null;
        }

        T old = data[bucket];
        data[bucket] = t;

        return old;
    }

    public synchronized T get(T t) {
        int bucket = getBucket(hash, t);
        return bucket < 0 ? null : data[bucket];
    }

    public synchronized T putUnique(T t) {
        copyArray();
        modCount++;
        ensureSpace();

        int bucket = getBucket(hash, t);
        if (bucket < 0) { // unique
            size++;
            assert data[~bucket] == null;
            data[~bucket] = t;
            return t;
        }

        return data[bucket];
    }

    public synchronized T removeElem(T t) {
        copyArray();
        modCount++;
        int bucket = getBucket(hash, t);
        return bucket < 0 ? null : doRemove(bucket);
    }

    public <K> Map<K,T> foreignKey(Hash<K,? super T> h) {
        return new FMap<K>(h);
    }

    // ---------------- Implementation

    /**
     * Additional mixing of hash
     */
    private int rehash(int h) {
        //return 54435761*h;
        return 1103515245*h >> 2;
    }

    /**
     * Finds a bucket for given key.
     *
     * @param key The key to compare
     * @return the bucket equivalent to the key or -(bucket) as an empty slot
     * where such an entry can be stored.
     */
    private <K> int getBucket(Hash<K,? super T> ha, K key) {
        T entry;
        int hcode = rehash(ha.getHashCode(key));
        int bucket = hcode & mask;
        while ((entry = data[bucket]) != null) {
            if (ha.equals(key, entry))
                return bucket;
            bucket = (bucket+1) & mask;
        }
        return ~bucket;
    }

    private T doRemove(int slot) {
        T t = data[slot];
        assert t != null;

        fillTheHole(slot); // fill the hole (or null it)
        size--;
        return t;
    }

    private void fillTheHole(int hole) {
        int bucket = (hole+1) & mask;
        T entry;

        while ((entry = data[bucket]) != null) {
            int right = rehash(hash.getHashCode(entry)) & mask;
            // if the entry should be in <hole+1,bucket-1> (circular-wise)
            // we can't move it. The move can be proved safe otherwise,
            // because the entry safely belongs to <previous_null+1,hole>
            if ((bucket < right && (right <= hole || hole <= bucket)) ||
                    (right <=hole && hole <= bucket)) {

                data[hole] = data[bucket];
                hole = bucket;
            }
            bucket = (bucket+1) & mask;
        }

        // no entry belongs here, just null out the slot
        data[hole] = null;
    }

    private void ensureSpace() {
        if (size > data.length*loadFactor) { // rehash
            T[] big = (T[]) new Object[data.length * 2];
            int nMask = big.length - 1;

            for (T o : data) {
                if (o == null) {
                    continue;
                }
                int bucket = rehash(hash.getHashCode(o)) & nMask;
                while (big[bucket] != null) {
                    bucket = (bucket+1) & nMask;
                }
                big[bucket] = o;
            }

            data = big;
            mask = nMask;
        }
    }

    // -------------- factories --------------------
    /**
     * A factory for default hash implementation.
     * @return a hash implementation that just delegates to object's own
     * hashCode and equals.
     */
    public static <O> Hash<O,O> defaultHash() {
        return new Hash<O,O>() {
            public int getHashCode(O t) {
                return t.hashCode();
            }
            public boolean equals(O t1, O t2) {
                return t1.equals(t2);
            }
        };
    }
    /*
    public static <O> Hash<O,O> identityHash() {
        return new Hash<O,O>() {
            public int getHashCode(O t) {
                return System.identityHashCode(t);
            }
            public boolean equals(O t1, O t2) {
                return t1 == t2;
            }
        };
    }
     */

    private class FMap<K> implements Map<K,T> {
        Hash<K,? super T> fHash;

        private FMap(Hash<K,? super T> h) {
            fHash = h;
        }

        public int size() {
            return Storage.this.size();
        }

        public boolean isEmpty() {
            return Storage.this.isEmpty();
        }

        public boolean containsKey(Object key) {
            int bucket = getBucket(fHash, (K)key);
            return bucket >= 0;
        }

        public boolean containsValue(Object value) {
            return Storage.this.contains(value);
        }

        public T get(Object key) {
            int bucket = getBucket(fHash, (K)key);
            return bucket < 0 ? null : data[bucket];
        }

        public T put(K key, T value) {
            if (!fHash.equals(key, value)) throw new IllegalArgumentException("inconsistent key");
            return Storage.this.put(value);
        }

        public T remove(Object key) {
            modCount++;
            int bucket = getBucket(fHash,(K)key);

            return bucket < 0 ? null : doRemove(bucket);
        }

        public void putAll(Map<? extends K, ? extends T> m) {
            if (m instanceof Storage.FMap) {
                Storage.this.addAll(((Storage.FMap)m).values());
            } else {
                for (Map.Entry<? extends K, ? extends T> e : m.entrySet()) {
                    put(e.getKey(), e.getValue());
                }
            }
        }

        public void clear() {
            Storage.this.clear();
        }

        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        public Collection<T> values() {
            return Storage.this;
        }

        public Set<Entry<K, T>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }

    private final class SafeReadonlyIter implements Iterator<T> {
        final T[] data;
        int slot = 0;

        SafeReadonlyIter(T[] data) {
            this.data = data;
        }

        public boolean hasNext() {
            align();
            return slot < data.length;
        }

        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            return data[slot++];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void align() {
            while (slot < data.length && data[slot] == null) {
                slot++;
            }
        }
    }


    private final class Iter implements Iterator<T> {
        private final int mods;
        int slot = 0;
        int removeSlot = -1;

        Iter() {
            mods = modCount;
        }

        public boolean hasNext() {
            align();
            return slot < data.length;
        }

        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            removeSlot = slot;
            return data[slot++];
        }

        public void remove() {
            if (removeSlot == -1) throw new IllegalStateException();

            doRemove(removeSlot);
            slot = removeSlot; // some entry might have been relocated here
            removeSlot = -1;
        }

        private void align() {
            if (mods != modCount)
                throw new ConcurrentModificationException();
            while (slot < data.length && data[slot] == null) {
                slot++;
            }
        }
    }

}
