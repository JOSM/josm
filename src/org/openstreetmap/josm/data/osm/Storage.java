// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openstreetmap.josm.tools.Utils;

/**
 * A Set-like class that allows looking up equivalent preexising instance.
 * It is useful wherever one would use self-mapping construct like
 * <code>Map&lt;T,T&gt;.put(t,t)</code>, that is, for caches, uniqueness filters or similar.
 *
 * The semantics of equivalency can be external to the object, using the
 * {@link Hash} interface. The set also supports querying for entries using
 * different key type, in case you can provide a Hash implementation
 * that can resolve the equality.
 *
 * <h2>Examples</h2>
 * <ul><li>A String cache:
 * <pre>
 * Storage&lt;String&gt; cache = new Storage(); // use default Hash
 * for (String input : data) {
 *     String onlyOne = cache.putIfUnique(input);
 *     ....
 * }
 * </pre></li>
 * <li>Identity-based set:
 * <pre>
 * Storage&lt;Object&gt; identity = new Storage(new Hash&lt;Object,Object&gt; {
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
 * Storage&lt;Thing&gt; things = new Storage(new Hash&lt;Thing,Thing&gt;() {
 *     public int getHashCode(Thing t) {
 *         return t.id;
 *     }
 *     public boolean equals(Thing t1, Thing t2) {
 *         return t1 == t2;
 *     }
 *  });
 * Map&lt;Integer,Thing&gt; fk = things.foreignKey(new Hash&lt;Integer,Thing&gt;() {
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
 * </ul>
 *
 * @author nenik
 * @param <T> type of stored objects
 */
public class Storage<T> extends AbstractSet<T> {

    /**
     * Hash for {@link PrimitiveId}.
     */
    public static class PrimitiveIdHash implements Hash<PrimitiveId, PrimitiveId> {

        @Override
        public int getHashCode(PrimitiveId k) {
            return (int) k.getUniqueId() ^ k.getType().hashCode();
        }

        @Override
        public boolean equals(PrimitiveId key, PrimitiveId value) {
            if (key == null || value == null) return false;
            return key.getUniqueId() == value.getUniqueId() && key.getType() == value.getType();
        }
    }

    private final Hash<? super T, ? super T> hash;
    private T[] data;
    private int mask;
    private int size;
    private volatile int modCount;
    private static final double LOAD_FACTOR = 0.6d;
    private static final int DEFAULT_CAPACITY = 16;
    private final boolean safeIterator;
    private boolean arrayCopyNecessary;

    /**
     * Constructs a new {@code Storage} with default capacity (16).
     */
    public Storage() {
        this(Storage.<T>defaultHash(), DEFAULT_CAPACITY, false);
    }

    /**
     * Constructs a new {@code Storage} with given capacity.
     * @param capacity capacity
     */
    public Storage(int capacity) {
        this(Storage.<T>defaultHash(), capacity, false);
    }

    /**
     * Constructs a new {@code Storage} with given hash.
     * @param ha hash
     */
    public Storage(Hash<? super T, ? super T> ha) {
        this(ha, DEFAULT_CAPACITY, false);
    }

    /**
     * Constructs a new {@code Storage}.
     * @param safeIterator If set to false, you must not modify the Storage while iterating over it.
     * If set to true, you can safely modify, but the read-only iteration will happen on a copy of the unmodified Storage.
     * This is similar to CopyOnWriteArrayList.
     */
    public Storage(boolean safeIterator) {
        this(Storage.<T>defaultHash(), DEFAULT_CAPACITY, safeIterator);
    }

    /**
     * Constructs a new {@code Storage} with given capacity.
     * @param capacity capacity
     * @param safeIterator If set to false, you must not modify the Storage while iterating over it.
     * If set to true, you can safely modify, but the read-only iteration will happen on a copy of the unmodified Storage.
     * This is similar to CopyOnWriteArrayList.
     */
    public Storage(int capacity, boolean safeIterator) {
        this(Storage.<T>defaultHash(), capacity, safeIterator);
    }

    /**
     * Constructs a new {@code Storage} with given hash.
     * @param ha hash
     * @param safeIterator If set to false, you must not modify the Storage while iterating over it.
     * If set to true, you can safely modify, but the read-only iteration will happen on a copy of the unmodified Storage.
     * This is similar to CopyOnWriteArrayList.
     */
    public Storage(Hash<? super T, ? super T> ha, boolean safeIterator) {
        this(ha, DEFAULT_CAPACITY, safeIterator);
    }

    /**
     * Constructs a new {@code Storage} with given hash and capacity.
     * @param ha hash
     * @param capacity capacity
     */
    public Storage(Hash<? super T, ? super T> ha, int capacity) {
        this(ha, capacity, false);
    }

    /**
     * Constructs a new {@code Storage} with given hash and capacity.
     * @param ha hash
     * @param capacity capacity
     * @param safeIterator If set to false, you must not modify the Storage while iterating over it.
     * If set to true, you can safely modify, but the read-only iteration will happen on a copy of the unmodified Storage.
     * This is similar to CopyOnWriteArrayList.
     */
    public Storage(Hash<? super T, ? super T> ha, int capacity, boolean safeIterator) {
        this.hash = ha;
        int cap = 1 << (int) Math.ceil(Math.log(capacity/LOAD_FACTOR) / Math.log(2));
        @SuppressWarnings("unchecked")
        T[] newData = (T[]) new Object[cap];
        data = newData;
        mask = data.length - 1;
        this.safeIterator = safeIterator;
    }

    private void copyArray() {
        if (arrayCopyNecessary) {
            data = Utils.copyArray(data);
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
        @SuppressWarnings("unchecked")
        T t = (T) o;
        int bucket = getBucket(hash, t);
        return bucket >= 0;
    }

    @Override
    public synchronized boolean add(T t) {
        T orig = putUnique(t);
        return orig == t;
    }

    @Override
    public synchronized boolean remove(Object o) {
        @SuppressWarnings("unchecked")
        T t = (T) o;
        T tOrig = removeElem(t);
        return tOrig != null;
    }

    @Override
    public synchronized void clear() {
        copyArray();
        modCount++;
        size = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = null;
        }
    }

    @Override
    public synchronized int hashCode() {
        if (hash == null) {
            return 0;
        }
        return this.stream().mapToInt(hash::getHashCode).sum();
    }

    @Override
    @SuppressWarnings("EqualsUsingHashCode")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Storage<?> other = (Storage<?>) obj;
        return Arrays.equals(data, other.data)
                && hashCode() == obj.hashCode();
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

    public <K> Map<K, T> foreignKey(Hash<K, ? super T> h) {
        return new FMap<>(h);
    }

    // ---------------- Implementation

    /**
     * Additional mixing of hash
     * @param h hash
     * @return new hash
     */
    private static int rehash(int h) {
        return (1_103_515_245*h) >> 2;
    }

    /**
     * Finds a bucket for given key.
     * @param <K> type for hashCode and first equals parameter
     * @param ha hash function
     *
     * @param key The key to compare
     * @return the bucket equivalent to the key or -(bucket) as an empty slot
     * where such an entry can be stored.
     */
    private <K> int getBucket(Hash<K, ? super T> ha, K key) {
        T entry;
        int hcode = rehash(ha.getHashCode(key));
        int bucket = hcode & mask;
        while (bucket < data.length && (entry = data[bucket]) != null) {
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
                    (right <= hole && hole <= bucket)) {

                data[hole] = data[bucket];
                hole = bucket;
            }
            bucket = (bucket+1) & mask;
        }

        // no entry belongs here, just null out the slot
        data[hole] = null;
    }

    private void ensureSpace() {
        if (size > data.length*LOAD_FACTOR) { // rehash
            @SuppressWarnings("unchecked")
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
     * @param <O> type for hash
     * @return a hash implementation that just delegates to object's own hashCode and equals.
     */
    public static <O> Hash<O, O> defaultHash() {
        return new Hash<O, O>() {
            @Override
            public int getHashCode(O t) {
                return t.hashCode();
            }

            @Override
            public boolean equals(O t1, O t2) {
                return t1.equals(t2);
            }
        };
    }

    private final class FMap<K> implements Map<K, T> {
        private final Hash<K, ? super T> fHash;

        private FMap(Hash<K, ? super T> h) {
            fHash = h;
        }

        @Override
        public int size() {
            return Storage.this.size();
        }

        @Override
        public boolean isEmpty() {
            return Storage.this.isEmpty();
        }

        @Override
        public boolean containsKey(Object o) {
            @SuppressWarnings("unchecked")
            K key = (K) o;
            int bucket = getBucket(fHash, key);
            return bucket >= 0;
        }

        @Override
        public boolean containsValue(Object value) {
            return Storage.this.contains(value);
        }

        @Override
        public T get(Object o) {
            @SuppressWarnings("unchecked")
            K key = (K) o;
            int bucket = getBucket(fHash, key);
            return bucket < 0 ? null : data[bucket];
        }

        @Override
        public T put(K key, T value) {
            if (!fHash.equals(key, value)) throw new IllegalArgumentException("inconsistent key");
            return Storage.this.put(value);
        }

        @Override
        public T remove(Object o) {
            synchronized (Storage.this) {
                modCount++;
                @SuppressWarnings("unchecked")
                K key = (K) o;
                int bucket = getBucket(fHash, key);

                return bucket < 0 ? null : doRemove(bucket);
            }
        }

        @Override
        public void putAll(Map<? extends K, ? extends T> m) {
            if (m instanceof Storage.FMap) {
                Storage.this.addAll(m.values());
            } else {
                for (Map.Entry<? extends K, ? extends T> e : m.entrySet()) {
                    put(e.getKey(), e.getValue());
                }
            }
        }

        @Override
        public void clear() {
            Storage.this.clear();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<T> values() {
            return Storage.this;
        }

        @Override
        public Set<Entry<K, T>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }

    private abstract class AbstractIter implements Iterator<T> {
        protected int slot;

        protected final boolean doHasNext(T[] data) {
            if (data == null) return false;
            align(data);
            return slot < data.length;
        }

        protected void align(T[] data) {
            while (slot < data.length && data[slot] == null) {
                slot++;
            }
        }
    }

    private final class SafeReadonlyIter extends AbstractIter {
        private final T[] data;

        SafeReadonlyIter(T[] data) {
            this.data = data;
        }

        @Override
        public boolean hasNext() {
            return doHasNext(data);
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            return data[slot++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class Iter extends AbstractIter {
        private final int mods;
        private int removeSlot = -1;

        Iter() {
            mods = modCount;
        }

        @Override
        public boolean hasNext() {
            return doHasNext(data);
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            removeSlot = slot;
            return data[slot++];
        }

        @Override
        public void remove() {
            if (removeSlot == -1) throw new IllegalStateException();

            doRemove(removeSlot);
            slot = removeSlot; // some entry might have been relocated here
            removeSlot = -1;
        }

        @Override
        protected void align(T[] data) {
            if (mods != modCount)
                throw new ConcurrentModificationException();
            super.align(data);
        }
    }
}
