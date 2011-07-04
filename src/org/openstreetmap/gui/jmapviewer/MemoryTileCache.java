package org.openstreetmap.gui.jmapviewer;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.util.Hashtable;
import java.util.logging.Logger;

import org.openstreetmap.gui.jmapviewer.interfaces.TileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;

/**
 * {@link TileCache} implementation that stores all {@link Tile} objects in
 * memory up to a certain limit ({@link #getCacheSize()}). If the limit is
 * exceeded the least recently used {@link Tile} objects will be deleted.
 *
 * @author Jan Peter Stotz
 */
public class MemoryTileCache implements TileCache {

    protected static final Logger log = Logger.getLogger(MemoryTileCache.class.getName());

    /**
     * Default cache size
     */
    protected int cacheSize = 200;

    protected Hashtable<String, CacheEntry> hashtable;

    /**
     * List of all tiles in their last recently used order
     */
    protected CacheLinkedListElement lruTiles;

    public MemoryTileCache() {
        hashtable = new Hashtable<String, CacheEntry>(cacheSize);
        lruTiles = new CacheLinkedListElement();
    }

    public void addTile(Tile tile) {
        CacheEntry entry = createCacheEntry(tile);
        hashtable.put(tile.getKey(), entry);
        lruTiles.addFirst(entry);
        if (hashtable.size() > cacheSize)
            removeOldEntries();
    }

    public Tile getTile(TileSource source, int x, int y, int z) {
        CacheEntry entry = hashtable.get(Tile.getTileKey(source, x, y, z));
        if (entry == null)
            return null;
        // We don't care about placeholder tiles and hourglass image tiles, the
        // important tiles are the loaded ones
        if (entry.tile.isLoaded())
            lruTiles.moveElementToFirstPos(entry);
        return entry.tile;
    }

    /**
     * Removes the least recently used tiles
     */
    protected void removeOldEntries() {
        synchronized (lruTiles) {
            try {
                while (lruTiles.getElementCount() > cacheSize) {
                    removeEntry(lruTiles.getLastElement());
                }
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
        }
    }

    protected void removeEntry(CacheEntry entry) {
        hashtable.remove(entry.tile.getKey());
        lruTiles.removeEntry(entry);
    }

    protected CacheEntry createCacheEntry(Tile tile) {
        return new CacheEntry(tile);
    }

    /**
     * Clears the cache deleting all tiles from memory
     */
    public void clear() {
        synchronized (lruTiles) {
            hashtable.clear();
            lruTiles.clear();
        }
    }

    public int getTileCount() {
        return hashtable.size();
    }

    public int getCacheSize() {
        return cacheSize;
    }

    /**
     * Changes the maximum number of {@link Tile} objects that this cache holds.
     *
     * @param cacheSize
     *            new maximum number of tiles
     */
    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
        if (hashtable.size() > cacheSize)
            removeOldEntries();
    }

    /**
     * Linked list element holding the {@link Tile} and links to the
     * {@link #next} and {@link #prev} item in the list.
     */
    protected static class CacheEntry {
        Tile tile;

        CacheEntry next;
        CacheEntry prev;

        protected CacheEntry(Tile tile) {
            this.tile = tile;
        }

        public Tile getTile() {
            return tile;
        }

        public CacheEntry getNext() {
            return next;
        }

        public CacheEntry getPrev() {
            return prev;
        }

    }

    /**
     * Special implementation of a double linked list for {@link CacheEntry}
     * elements. It supports element removal in constant time - in difference to
     * the Java implementation which needs O(n).
     *
     * @author Jan Peter Stotz
     */
    protected static class CacheLinkedListElement {
        protected CacheEntry firstElement = null;
        protected CacheEntry lastElement;
        protected int elementCount;

        public CacheLinkedListElement() {
            clear();
        }

        public synchronized void clear() {
            elementCount = 0;
            firstElement = null;
            lastElement = null;
        }

        /**
         * Add the element to the head of the list.
         *
         * @param new element to be added
         */
        public synchronized void addFirst(CacheEntry element) {
            if (elementCount == 0) {
                firstElement = element;
                lastElement = element;
                element.prev = null;
                element.next = null;
            } else {
                element.next = firstElement;
                firstElement.prev = element;
                element.prev = null;
                firstElement = element;
            }
            elementCount++;
        }

        /**
         * Removes the specified elemntent form the list.
         *
         * @param element
         *            to be removed
         */
        public synchronized void removeEntry(CacheEntry element) {
            if (element.next != null) {
                element.next.prev = element.prev;
            }
            if (element.prev != null) {
                element.prev.next = element.next;
            }
            if (element == firstElement)
                firstElement = element.next;
            if (element == lastElement)
                lastElement = element.prev;
            element.next = null;
            element.prev = null;
            elementCount--;
        }

        public synchronized void moveElementToFirstPos(CacheEntry entry) {
            if (firstElement == entry)
                return;
            removeEntry(entry);
            addFirst(entry);
        }

        public int getElementCount() {
            return elementCount;
        }

        public CacheEntry getLastElement() {
            return lastElement;
        }

        public CacheEntry getFirstElement() {
            return firstElement;
        }

    }
}
