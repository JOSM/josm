package org.apache.commons.jcs.auxiliary.disk.block;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.jcs.auxiliary.disk.behavior.IDiskCacheAttributes.DiskLimitType;
import org.apache.commons.jcs.io.ObjectInputStreamClassLoaderAware;
import org.apache.commons.jcs.utils.struct.AbstractLRUMap;
import org.apache.commons.jcs.utils.struct.LRUMap;
import org.apache.commons.jcs.utils.timing.ElapsedTimer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is responsible for storing the keys.
 * <p>
 *
 * @author Aaron Smuts
 */
public class BlockDiskKeyStore<K>
{
    /** The logger */
    private static final Log log = LogFactory.getLog(BlockDiskKeyStore.class);

    /** Attributes governing the behavior of the block disk cache. */
    private final BlockDiskCacheAttributes blockDiskCacheAttributes;

    /** The key to block map */
    private Map<K, int[]> keyHash;

    /** The file where we persist the keys */
    private final File keyFile;

    /** The name to prefix log messages with. */
    protected final String logCacheName;

    /** Name of the file where we persist the keys */
    private final String fileName;

    /** The maximum number of keys to store in memory */
    private final int maxKeySize;

    /**
     * we need this so we can communicate free blocks to the data store when
     * keys fall off the LRU
     */
    protected final BlockDiskCache<K, ?> blockDiskCache;

    private DiskLimitType diskLimitType = DiskLimitType.COUNT;

    private int blockSize;

    /**
     * Set the configuration options.
     * <p>
     *
     * @param cacheAttributes
     * @param blockDiskCache
     *            used for freeing
     */
    public BlockDiskKeyStore(BlockDiskCacheAttributes cacheAttributes, BlockDiskCache<K, ?> blockDiskCache)
    {
        this.blockDiskCacheAttributes = cacheAttributes;
        this.logCacheName = "Region [" + this.blockDiskCacheAttributes.getCacheName() + "] ";
        this.fileName = this.blockDiskCacheAttributes.getCacheName();
        this.maxKeySize = cacheAttributes.getMaxKeySize();
        this.blockDiskCache = blockDiskCache;
        this.diskLimitType = cacheAttributes.getDiskLimitType();
        this.blockSize = cacheAttributes.getBlockSizeBytes();

        File rootDirectory = cacheAttributes.getDiskPath();

        if (log.isInfoEnabled())
        {
            log.info(logCacheName + "Cache file root directory [" + rootDirectory + "]");
        }

        this.keyFile = new File(rootDirectory, fileName + ".key");

        if (log.isInfoEnabled())
        {
            log.info(logCacheName + "Key File [" + this.keyFile.getAbsolutePath() + "]");
        }

        if (keyFile.length() > 0)
        {
            loadKeys();
            if (!verify())
            {
                log.warn(logCacheName + "Key File is invalid. Resetting file.");
                initKeyMap();
                reset();
            }
        }
        else
        {
            initKeyMap();
        }
    }

    /**
     * Saves key file to disk. This gets the LRUMap entry set and write the
     * entries out one by one after putting them in a wrapper.
     */
    protected void saveKeys()
    {
        try
        {
            ElapsedTimer timer = new ElapsedTimer();
            int numKeys = keyHash.size();
            if (log.isInfoEnabled())
            {
                log.info(logCacheName + "Saving keys to [" + this.keyFile.getAbsolutePath() + "], key count [" + numKeys + "]");
            }

            synchronized (keyFile)
            {
                FileOutputStream fos = new FileOutputStream(keyFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos, 65536);

                try (ObjectOutputStream oos = new ObjectOutputStream(bos))
                {
                    if (!verify())
                    {
                        throw new IOException("Inconsistent key file");
                    }
                    // don't need to synchronize, since the underlying
                    // collection makes a copy
                    for (Map.Entry<K, int[]> entry : keyHash.entrySet())
                    {
                        BlockDiskElementDescriptor<K> descriptor = new BlockDiskElementDescriptor<>();
                        descriptor.setKey(entry.getKey());
                        descriptor.setBlocks(entry.getValue());
                        // stream these out in the loop.
                        oos.writeUnshared(descriptor);
                    }
                }
            }

            if (log.isInfoEnabled())
            {
                log.info(logCacheName + "Finished saving keys. It took " + timer.getElapsedTimeString() + " to store " + numKeys
                        + " keys.  Key file length [" + keyFile.length() + "]");
            }
        }
        catch (IOException e)
        {
            log.error(logCacheName + "Problem storing keys.", e);
        }
    }

    /**
     * Resets the file and creates a new key map.
     */
    protected void reset()
    {
        synchronized (keyFile)
        {
            clearMemoryMap();
            saveKeys();
        }
    }

    /**
     * This is mainly used for testing. It leave the disk in tact, and just
     * clears memory.
     */
    protected void clearMemoryMap()
    {
        this.keyHash.clear();
    }

    /**
     * Create the map for keys that contain the index position on disk.
     */
    private void initKeyMap()
    {
        keyHash = null;
        if (maxKeySize >= 0)
        {
            if (this.diskLimitType == DiskLimitType.SIZE)
            {
                keyHash = new LRUMapSizeLimited(maxKeySize);
            }
            else
            {
                keyHash = new LRUMapCountLimited(maxKeySize);
            }
            if (log.isInfoEnabled())
            {
                log.info(logCacheName + "Set maxKeySize to: '" + maxKeySize + "'");
            }
        }
        else
        {
            // If no max size, use a plain map for memory and processing
            // efficiency.
            keyHash = new HashMap<>();
            // keyHash = Collections.synchronizedMap( new HashMap() );
            if (log.isInfoEnabled())
            {
                log.info(logCacheName + "Set maxKeySize to unlimited'");
            }
        }
    }

    /**
     * Loads the keys from the .key file. The keys are stored individually on
     * disk. They are added one by one to an LRUMap..
     */
    protected void loadKeys()
    {
        if (log.isInfoEnabled())
        {
            log.info(logCacheName + "Loading keys for " + keyFile.toString());
        }

        try
        {
            // create a key map to use.
            initKeyMap();

            HashMap<K, int[]> keys = new HashMap<>();

            synchronized (keyFile)
            {
                FileInputStream fis = new FileInputStream(keyFile);
                BufferedInputStream bis = new BufferedInputStream(fis, 65536);

                try (ObjectInputStream ois = new ObjectInputStreamClassLoaderAware(bis, null))
                {
                    while (true)
                    {
                        @SuppressWarnings("unchecked")
                        // Need to cast from Object
                        BlockDiskElementDescriptor<K> descriptor = (BlockDiskElementDescriptor<K>) ois.readObject();
                        if (descriptor != null)
                        {
                            keys.put(descriptor.getKey(), descriptor.getBlocks());
                        }
                    }
                }
                catch (EOFException eof)
                {
                    // nothing
                }
            }

            if (!keys.isEmpty())
            {
                keyHash.putAll(keys);

                if (log.isDebugEnabled())
                {
                    log.debug(logCacheName + "Found " + keys.size() + " in keys file.");
                }

                if (log.isInfoEnabled())
                {
                    log.info(logCacheName + "Loaded keys from [" + fileName + "], key count: " + keyHash.size() + "; up to "
                            + maxKeySize + " will be available.");
                }
            }
        }
        catch (Exception e)
        {
            log.error(logCacheName + "Problem loading keys for file " + fileName, e);
        }
    }

    /**
     * Gets the entry set.
     * <p>
     *
     * @return entry set.
     */
    public Set<Map.Entry<K, int[]>> entrySet()
    {
        return this.keyHash.entrySet();
    }

    /**
     * Gets the key set.
     * <p>
     *
     * @return key set.
     */
    public Set<K> keySet()
    {
        return this.keyHash.keySet();
    }

    /**
     * Gets the size of the key hash.
     * <p>
     *
     * @return the number of keys.
     */
    public int size()
    {
        return this.keyHash.size();
    }

    /**
     * gets the object for the key.
     * <p>
     *
     * @param key
     * @return Object
     */
    public int[] get(K key)
    {
        return this.keyHash.get(key);
    }

    /**
     * Puts a int[] in the keyStore.
     * <p>
     *
     * @param key
     * @param value
     */
    public void put(K key, int[] value)
    {
        this.keyHash.put(key, value);
    }

    /**
     * Remove by key.
     * <p>
     *
     * @param key
     * @return BlockDiskElementDescriptor if it was present, else null
     */
    public int[] remove(K key)
    {
        return this.keyHash.remove(key);
    }

    /**
     * Verify key store integrity
     *
     * @return true if key store is valid
     */
    private boolean verify()
    {
        Map<Integer, Set<K>> blockAllocationMap = new TreeMap<>();
        for (Entry<K, int[]> e : keyHash.entrySet())
        {
            for (int block : e.getValue())
            {
                Set<K> keys = blockAllocationMap.get(block);
                if (keys == null)
                {
                    keys = new HashSet<>();
                    blockAllocationMap.put(block, keys);
                }
                else if (!log.isDebugEnabled())
                {
                    // keys are not null, and no debug - fail fast
                    return false;
                }
                keys.add(e.getKey());
            }
        }
        boolean ok = true;
        if (log.isDebugEnabled())
        {
            for (Entry<Integer, Set<K>> e : blockAllocationMap.entrySet())
            {
                log.debug("Block " + e.getKey() + ":" + e.getValue());
                if (e.getValue().size() > 1)
                {
                    ok = false;
                }
            }
            return ok;
        }
        else
        {
            return ok;
        }
    }

    /**
     * Class for recycling and lru. This implements the LRU size overflow
     * callback, so we can mark the blocks as free.
     */
    public class LRUMapSizeLimited extends AbstractLRUMap<K, int[]>
    {
        /**
         * <code>tag</code> tells us which map we are working on.
         */
        public final static String TAG = "orig-lru-size";

        // size of the content in kB
        private AtomicInteger contentSize;
        private int maxSize;

        /**
         * Default
         */
        public LRUMapSizeLimited()
        {
            this(-1);
        }

        /**
         * @param maxSize
         *            maximum cache size in kB
         */
        public LRUMapSizeLimited(int maxSize)
        {
            super();
            this.maxSize = maxSize;
            this.contentSize = new AtomicInteger(0);
        }

        // keep the content size in kB, so 2^31 kB is reasonable value
        private void subLengthFromCacheSize(int[] value)
        {
            contentSize.addAndGet(value.length * blockSize / -1024 - 1);
        }

        // keep the content size in kB, so 2^31 kB is reasonable value
        private void addLengthToCacheSize(int[] value)
        {
            contentSize.addAndGet(value.length * blockSize / 1024 + 1);
        }

        @Override
        public int[] put(K key, int[] value)
        {
            int[] oldValue = null;

            try
            {
                oldValue = super.put(key, value);
            }
            finally
            {
                if (value != null)
                {
                    addLengthToCacheSize(value);
                }
                if (oldValue != null)
                {
                    subLengthFromCacheSize(oldValue);
                }
            }

            return oldValue;
        }

        @Override
        public int[] remove(Object key)
        {
            int[] value = null;

            try
            {
                value = super.remove(key);
                return value;
            }
            finally
            {
                if (value != null)
                {
                    subLengthFromCacheSize(value);
                }
            }
        }

        /**
         * This is called when the may key size is reached. The least recently
         * used item will be passed here. We will store the position and size of
         * the spot on disk in the recycle bin.
         * <p>
         *
         * @param key
         * @param value
         */
        @Override
        protected void processRemovedLRU(K key, int[] value)
        {
            blockDiskCache.freeBlocks(value);
            if (log.isDebugEnabled())
            {
                log.debug(logCacheName + "Removing key: [" + key + "] from key store.");
                log.debug(logCacheName + "Key store size: [" + super.size() + "].");
            }

            if (value != null)
            {
                subLengthFromCacheSize(value);
            }
        }

        @Override
        protected boolean shouldRemove()
        {
            return maxSize > 0 && contentSize.get() > maxSize && this.size() > 1;
        }
    }

    /**
     * Class for recycling and lru. This implements the LRU overflow callback,
     * so we can mark the blocks as free.
     */
    public class LRUMapCountLimited extends LRUMap<K, int[]>
    {
        /**
         * <code>tag</code> tells us which map we are working on.
         */
        public final static String TAG = "orig-lru-count";

        public LRUMapCountLimited(int maxKeySize)
        {
            super(maxKeySize);
        }

        /**
         * This is called when the may key size is reached. The least recently
         * used item will be passed here. We will store the position and size of
         * the spot on disk in the recycle bin.
         * <p>
         *
         * @param key
         * @param value
         */
        @Override
        protected void processRemovedLRU(K key, int[] value)
        {
            blockDiskCache.freeBlocks(value);
            if (log.isDebugEnabled())
            {
                log.debug(logCacheName + "Removing key: [" + key + "] from key store.");
                log.debug(logCacheName + "Key store size: [" + super.size() + "].");
            }
        }
    }
}
