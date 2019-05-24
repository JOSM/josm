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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache;
import org.apache.commons.jcs.engine.CacheConstants;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.behavior.IRequireScheduler;
import org.apache.commons.jcs.engine.control.group.GroupAttrName;
import org.apache.commons.jcs.engine.control.group.GroupId;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.Stats;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * There is one BlockDiskCache per region. It manages the key and data store.
 * <p>
 * @author Aaron Smuts
 */
public class BlockDiskCache<K, V>
    extends AbstractDiskCache<K, V>
    implements IRequireScheduler
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( BlockDiskCache.class );

    /** The name to prefix all log messages with. */
    private final String logCacheName;

    /** The name of the file to store data. */
    private final String fileName;

    /** The data access object */
    private BlockDisk dataFile;

    /** Attributes governing the behavior of the block disk cache. */
    private final BlockDiskCacheAttributes blockDiskCacheAttributes;

    /** The root directory for keys and data. */
    private final File rootDirectory;

    /** Store, loads, and persists the keys */
    private BlockDiskKeyStore<K> keyStore;

    /**
     * Use this lock to synchronize reads and writes to the underlying storage mechanism. We don't
     * need a reentrant lock, since we only lock one level.
     */
    private final ReentrantReadWriteLock storageLock = new ReentrantReadWriteLock();

    private ScheduledFuture<?> future;

    /**
     * Constructs the BlockDisk after setting up the root directory.
     * <p>
     * @param cacheAttributes
     */
    public BlockDiskCache( BlockDiskCacheAttributes cacheAttributes )
    {
        this( cacheAttributes, null );
    }

    /**
     * Constructs the BlockDisk after setting up the root directory.
     * <p>
     * @param cacheAttributes
     * @param elementSerializer used if supplied, the super's super will not set a null
     */
    public BlockDiskCache( BlockDiskCacheAttributes cacheAttributes, IElementSerializer elementSerializer )
    {
        super( cacheAttributes );
        setElementSerializer( elementSerializer );

        this.blockDiskCacheAttributes = cacheAttributes;
        this.logCacheName = "Region [" + getCacheName() + "] ";

        if ( log.isInfoEnabled() )
        {
            log.info( logCacheName + "Constructing BlockDiskCache with attributes " + cacheAttributes );
        }

        // Make a clean file name
        this.fileName = getCacheName().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        this.rootDirectory = cacheAttributes.getDiskPath();

        if ( log.isInfoEnabled() )
        {
            log.info( logCacheName + "Cache file root directory: [" + rootDirectory + "]");
        }

        try
        {
            if ( this.blockDiskCacheAttributes.getBlockSizeBytes() > 0 )
            {
                this.dataFile = new BlockDisk( new File( rootDirectory, fileName + ".data" ),
                                               this.blockDiskCacheAttributes.getBlockSizeBytes(),
                                               getElementSerializer() );
            }
            else
            {
                this.dataFile = new BlockDisk( new File( rootDirectory, fileName + ".data" ),
                                               getElementSerializer() );
            }

            keyStore = new BlockDiskKeyStore<>( this.blockDiskCacheAttributes, this );

            boolean alright = verifyDisk();

            if ( keyStore.size() == 0 || !alright )
            {
                this.reset();
            }

            // Initialization finished successfully, so set alive to true.
            setAlive(true);
            if ( log.isInfoEnabled() )
            {
                log.info( logCacheName + "Block Disk Cache is alive." );
            }
        }
        catch ( IOException e )
        {
            log.error( logCacheName + "Failure initializing for fileName: " + fileName + " and root directory: "
                + rootDirectory, e );
        }
    }

    /**
     * @see org.apache.commons.jcs.engine.behavior.IRequireScheduler#setScheduledExecutorService(java.util.concurrent.ScheduledExecutorService)
     */
    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutor)
    {
        // add this region to the persistence thread.
        // TODO we might need to stagger this a bit.
        if ( this.blockDiskCacheAttributes.getKeyPersistenceIntervalSeconds() > 0 )
        {
            future = scheduledExecutor.scheduleAtFixedRate(keyStore::saveKeys,
                    this.blockDiskCacheAttributes.getKeyPersistenceIntervalSeconds(),
                    this.blockDiskCacheAttributes.getKeyPersistenceIntervalSeconds(),
                    TimeUnit.SECONDS);
        }
    }

    /**
     * We need to verify that the file on disk uses the same block size and that the file is the
     * proper size.
     * <p>
     * @return true if it looks ok
     */
    protected boolean verifyDisk()
    {
        boolean alright = false;
        // simply try to read a few. If it works, then the file is probably ok.
        // TODO add more.

        storageLock.readLock().lock();

        try
        {
            this.keyStore.entrySet().stream()
                .limit(100)
                .forEach(entry -> {
                    try
                    {
                        Object data = this.dataFile.read(entry.getValue());
                        if ( data == null )
                        {
                            throw new IOException("Data is null");
                        }
                    }
                    catch (IOException | ClassNotFoundException e)
                    {
                        throw new RuntimeException(logCacheName
                                + " Couldn't find data for key [" + entry.getKey() + "]", e);
                    }
                });
            alright = true;
        }
        catch ( Exception e )
        {
            log.warn(logCacheName + " Problem verifying disk.", e);
            alright = false;
        }
        finally
        {
            storageLock.readLock().unlock();
        }

        return alright;
    }

    /**
     * Return the keys in this cache.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet() throws IOException
    {
        HashSet<K> keys = new HashSet<>();

        storageLock.readLock().lock();

        try
        {
            keys.addAll(this.keyStore.keySet());
        }
        finally
        {
            storageLock.readLock().unlock();
        }

        return keys;
    }

    /**
     * Gets matching items from the cache.
     * <p>
     * @param pattern
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache matching keys
     */
    @Override
    public Map<K, ICacheElement<K, V>> processGetMatching( String pattern )
    {
        Set<K> keyArray = null;
        storageLock.readLock().lock();
        try
        {
            keyArray = new HashSet<>(keyStore.keySet());
        }
        finally
        {
            storageLock.readLock().unlock();
        }

        Set<K> matchingKeys = getKeyMatcher().getMatchingKeysFromArray( pattern, keyArray );

        Map<K, ICacheElement<K, V>> elements = matchingKeys.stream()
            .collect(Collectors.toMap(
                    key -> key,
                    key -> processGet( key ))).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue()));

        return elements;
    }

    /**
     * Returns the number of keys.
     * <p>
     * (non-Javadoc)
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#getSize()
     */
    @Override
    public int getSize()
    {
        return this.keyStore.size();
    }

    /**
     * Gets the ICacheElement&lt;K, V&gt; for the key if it is in the cache. The program flow is as follows:
     * <ol>
     * <li>Make sure the disk cache is alive.</li> <li>Get a read lock.</li> <li>See if the key is
     * in the key store.</li> <li>If we found a key, ask the BlockDisk for the object at the
     * blocks..</li> <li>Release the lock.</li>
     * </ol>
     * @param key
     * @return ICacheElement
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#get(Object)
     */
    @Override
    protected ICacheElement<K, V> processGet( K key )
    {
        if ( !isAlive() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( logCacheName + "No longer alive so returning null for key = " + key );
            }
            return null;
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( logCacheName + "Trying to get from disk: " + key );
        }

        ICacheElement<K, V> object = null;


        try
        {
            storageLock.readLock().lock();
            try {
                int[] ded = this.keyStore.get( key );
                if ( ded != null )
                {
                    object = this.dataFile.read( ded );
                }
            } finally {
                storageLock.readLock().unlock();
            }

        }
        catch ( IOException ioe )
        {
            log.error( logCacheName + "Failure getting from disk--IOException, key = " + key, ioe );
            reset();
        }
        catch ( Exception e )
        {
            log.error( logCacheName + "Failure getting from disk, key = " + key, e );
        }
        return object;
    }

    /**
     * Writes an element to disk. The program flow is as follows:
     * <ol>
     * <li>Acquire write lock.</li> <li>See id an item exists for this key.</li> <li>If an item
     * already exists, add its blocks to the remove list.</li> <li>Have the Block disk write the
     * item.</li> <li>Create a descriptor and add it to the key map.</li> <li>Release the write
     * lock.</li>
     * </ol>
     * @param element
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#update(ICacheElement)
     */
    @Override
    protected void processUpdate( ICacheElement<K, V> element )
    {
        if ( !isAlive() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( logCacheName + "No longer alive; aborting put of key = " + element.getKey() );
            }
            return;
        }

        int[] old = null;

        // make sure this only locks for one particular cache region
        storageLock.writeLock().lock();

        try
        {
            old = this.keyStore.get( element.getKey() );

            if ( old != null )
            {
                this.dataFile.freeBlocks( old );
            }

            int[] blocks = this.dataFile.write( element );

            this.keyStore.put( element.getKey(), blocks );

            if ( log.isDebugEnabled() )
            {
                log.debug( logCacheName + "Put to file [" + fileName + "] key [" + element.getKey() + "]" );
            }
        }
        catch ( IOException e )
        {
            log.error( logCacheName + "Failure updating element, key: " + element.getKey() + " old: " + Arrays.toString(old), e );
        }
        finally
        {
            storageLock.writeLock().unlock();
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( logCacheName + "Storing element on disk, key: " + element.getKey() );
        }
    }

    /**
     * Returns true if the removal was successful; or false if there is nothing to remove. Current
     * implementation always result in a disk orphan.
     * <p>
     * @param key
     * @return true if removed anything
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#remove(Object)
     */
    @Override
    protected boolean processRemove( K key )
    {
        if ( !isAlive() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( logCacheName + "No longer alive so returning false for key = " + key );
            }
            return false;
        }

        boolean reset = false;
        boolean removed = false;

        storageLock.writeLock().lock();

        try
        {
            if (key instanceof String && key.toString().endsWith(CacheConstants.NAME_COMPONENT_DELIMITER))
            {
                removed = performPartialKeyRemoval((String) key);
            }
            else if (key instanceof GroupAttrName && ((GroupAttrName<?>) key).attrName == null)
            {
                removed = performGroupRemoval(((GroupAttrName<?>) key).groupId);
            }
            else
            {
                removed = performSingleKeyRemoval(key);
            }
        }
        catch ( Exception e )
        {
            log.error( logCacheName + "Problem removing element.", e );
            reset = true;
        }
        finally
        {
            storageLock.writeLock().unlock();
        }

        if ( reset )
        {
            reset();
        }

        return removed;
    }

    /**
     * Remove all elements from the group. This does not use the iterator to remove. It builds a
     * list of group elements and then removes them one by one.
     * <p>
     * This operates under a lock obtained in doRemove().
     * <p>
     *
     * @param key
     * @return true if an element was removed
     */
    private boolean performGroupRemoval(GroupId key)
    {
        // remove all keys of the same name group.
        List<K> itemsToRemove = keyStore.keySet()
                .stream()
                .filter(k -> k instanceof GroupAttrName && ((GroupAttrName<?>) k).groupId.equals(key))
                .collect(Collectors.toList());

        // remove matches.
        // Don't add to recycle bin here
        // https://issues.apache.org/jira/browse/JCS-67
        itemsToRemove.forEach(fullKey -> performSingleKeyRemoval(fullKey));
        // TODO this needs to update the remove count separately

        return !itemsToRemove.isEmpty();
    }

    /**
     * Iterates over the keyset. Builds a list of matches. Removes all the keys in the list. Does
     * not remove via the iterator, since the map impl may not support it.
     * <p>
     * This operates under a lock obtained in doRemove().
     * <p>
     *
     * @param key
     * @return true if there was a match
     */
    private boolean performPartialKeyRemoval(String key)
    {
        // remove all keys of the same name hierarchy.
        List<K> itemsToRemove = keyStore.keySet()
                .stream()
                .filter(k -> k instanceof String && k.toString().startsWith(key))
                .collect(Collectors.toList());

        // remove matches.
        // Don't add to recycle bin here
        // https://issues.apache.org/jira/browse/JCS-67
        itemsToRemove.forEach(fullKey -> performSingleKeyRemoval(fullKey));
        // TODO this needs to update the remove count separately

        return !itemsToRemove.isEmpty();
    }


	private boolean performSingleKeyRemoval(K key) {
		boolean removed;
		// remove single item.
		int[] ded = this.keyStore.remove( key );
		removed = ded != null;
		if ( removed )
		{
		    this.dataFile.freeBlocks( ded );
		}

		if ( log.isDebugEnabled() )
		{
		    log.debug( logCacheName + "Disk removal: Removed from key hash, key [" + key + "] removed = "
		        + removed );
		}
		return removed;
	}

    /**
     * Resets the keyfile, the disk file, and the memory key map.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#removeAll()
     */
    @Override
    protected void processRemoveAll()
    {
        reset();
    }

    /**
     * Dispose of the disk cache in a background thread. Joins against this thread to put a cap on
     * the disposal time.
     * <p>
     * TODO make dispose window configurable.
     */
    @Override
    public void processDispose()
    {
        Thread t = new Thread(this::disposeInternal, "BlockDiskCache-DisposalThread" );
        t.start();
        // wait up to 60 seconds for dispose and then quit if not done.
        try
        {
            t.join( 60 * 1000 );
        }
        catch ( InterruptedException ex )
        {
            log.error( logCacheName + "Interrupted while waiting for disposal thread to finish.", ex );
        }
    }

    /**
     * Internal method that handles the disposal.
     */
    protected void disposeInternal()
    {
        if ( !isAlive() )
        {
            log.error( logCacheName + "Not alive and dispose was called, filename: " + fileName );
            return;
        }
        storageLock.writeLock().lock();
        try
        {
            // Prevents any interaction with the cache while we're shutting down.
            setAlive(false);
            this.keyStore.saveKeys();

            if (future != null)
            {
                future.cancel(true);
            }

            try
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( logCacheName + "Closing files, base filename: " + fileName );
                }
                dataFile.close();
                // dataFile = null;

                // TOD make a close
                // keyFile.close();
                // keyFile = null;
            }
            catch ( IOException e )
            {
                log.error( logCacheName + "Failure closing files in dispose, filename: " + fileName, e );
            }
        }
        finally
        {
            storageLock.writeLock().unlock();
        }

        if ( log.isInfoEnabled() )
        {
            log.info( logCacheName + "Shutdown complete." );
        }
    }

    /**
     * Returns the attributes.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCache#getAuxiliaryCacheAttributes()
     */
    @Override
    public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
    {
        return this.blockDiskCacheAttributes;
    }

    /**
     * Reset effectively clears the disk cache, creating new files, recyclebins, and keymaps.
     * <p>
     * It can be used to handle errors by last resort, force content update, or removeall.
     */
    private void reset()
    {
        if ( log.isWarnEnabled() )
        {
            log.warn( logCacheName + "Resetting cache" );
        }

        try
        {
            storageLock.writeLock().lock();

            this.keyStore.reset();

            if ( dataFile != null )
            {
                dataFile.reset();
            }
        }
        catch ( IOException e )
        {
            log.error( logCacheName + "Failure resetting state", e );
        }
        finally
        {
            storageLock.writeLock().unlock();
        }
    }

    /**
     * Add these blocks to the emptyBlock list.
     * <p>
     * @param blocksToFree
     */
    protected void freeBlocks( int[] blocksToFree )
    {
        this.dataFile.freeBlocks( blocksToFree );
    }

    /**
     * Returns info about the disk cache.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.AuxiliaryCache#getStatistics()
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = new Stats();
        stats.setTypeName( "Block Disk Cache" );

        ArrayList<IStatElement<?>> elems = new ArrayList<>();

        elems.add(new StatElement<>( "Is Alive", Boolean.valueOf(isAlive()) ) );
        elems.add(new StatElement<>( "Key Map Size", Integer.valueOf(this.keyStore.size()) ) );

        if (this.dataFile != null)
        {
            try
            {
                elems.add(new StatElement<>( "Data File Length", Long.valueOf(this.dataFile.length()) ) );
            }
            catch ( IOException e )
            {
                log.error( e );
            }

            elems.add(new StatElement<>( "Block Size Bytes",
                    Integer.valueOf(this.dataFile.getBlockSizeBytes()) ) );
            elems.add(new StatElement<>( "Number Of Blocks",
                    Integer.valueOf(this.dataFile.getNumberOfBlocks()) ) );
            elems.add(new StatElement<>( "Average Put Size Bytes",
                    Long.valueOf(this.dataFile.getAveragePutSizeBytes()) ) );
            elems.add(new StatElement<>( "Empty Blocks",
                    Integer.valueOf(this.dataFile.getEmptyBlocks()) ) );
        }

        // get the stats from the super too
        IStats sStats = super.getStatistics();
        elems.addAll(sStats.getStatElements());

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * This is used by the event logging.
     * <p>
     * @return the location of the disk, either path or ip.
     */
    @Override
    protected String getDiskLocation()
    {
        return dataFile.getFilePath();
    }
}
