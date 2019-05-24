package org.apache.commons.jcs.admin;

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

import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.auxiliary.remote.server.RemoteCacheServer;
import org.apache.commons.jcs.auxiliary.remote.server.RemoteCacheServerFactory;
import org.apache.commons.jcs.engine.CacheElementSerialized;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;
import org.apache.commons.jcs.engine.memory.behavior.IMemoryCache;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Set;

/**
 * A servlet which provides HTTP access to JCS. Allows a summary of regions to be viewed, and
 * removeAll to be run on individual regions or all regions. Also provides the ability to remove
 * items (any number of key arguments can be provided with action 'remove'). Should be initialized
 * with a properties file that provides at least a classpath resource loader.
 */
public class JCSAdminBean implements JCSJMXBean
{
    /** The cache manager. */
    private final CompositeCacheManager cacheHub;

    /**
     * Default constructor
     */
    public JCSAdminBean()
    {
        super();
        try
        {
            this.cacheHub = CompositeCacheManager.getInstance();
        }
        catch (CacheException e)
        {
            throw new RuntimeException("Could not retrieve cache manager instance", e);
        }
    }

    /**
     * Parameterized constructor
     *
	 * @param cacheHub the cache manager instance
	 */
	public JCSAdminBean(CompositeCacheManager cacheHub)
	{
		super();
		this.cacheHub = cacheHub;
	}

	/**
     * Builds up info about each element in a region.
     * <p>
     * @param cacheName
     * @return Array of CacheElementInfo objects
     * @throws Exception
     */
    @Override
    public CacheElementInfo[] buildElementInfo( String cacheName )
        throws Exception
    {
        CompositeCache<Serializable, Serializable> cache = cacheHub.getCache( cacheName );

        Serializable[] keys = cache.getMemoryCache().getKeySet().toArray(new Serializable[0]);

        // Attempt to sort keys according to their natural ordering. If that
        // fails, get the key array again and continue unsorted.
        try
        {
            Arrays.sort( keys );
        }
        catch ( Exception e )
        {
            keys = cache.getMemoryCache().getKeySet().toArray(new Serializable[0]);
        }

        LinkedList<CacheElementInfo> records = new LinkedList<>();

        ICacheElement<Serializable, Serializable> element;
        IElementAttributes attributes;
        CacheElementInfo elementInfo;

        DateFormat format = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );

        long now = System.currentTimeMillis();

        for (Serializable key : keys)
        {
            element = cache.getMemoryCache().getQuiet( key );

            attributes = element.getElementAttributes();

            elementInfo = new CacheElementInfo(
            		String.valueOf( key ),
            		attributes.getIsEternal(),
            		format.format(new Date(attributes.getCreateTime())),
            		attributes.getMaxLife(),
            		(now - attributes.getCreateTime() - attributes.getMaxLife() * 1000 ) / -1000);

            records.add( elementInfo );
        }

        return records.toArray(new CacheElementInfo[0]);
    }

    /**
     * Builds up data on every region.
     * <p>
     * TODO we need a most light weight method that does not count bytes. The byte counting can
     *       really swamp a server.
     * @return list of CacheRegionInfo objects
     * @throws Exception
     */
    @Override
    public CacheRegionInfo[] buildCacheInfo()
        throws Exception
    {
        String[] cacheNames = cacheHub.getCacheNames();

        Arrays.sort( cacheNames );

        LinkedList<CacheRegionInfo> cacheInfo = new LinkedList<>();

        CacheRegionInfo regionInfo;
        CompositeCache<?, ?> cache;

        for ( int i = 0; i < cacheNames.length; i++ )
        {
            cache = cacheHub.getCache( cacheNames[i] );

            regionInfo = new CacheRegionInfo(
                    cache.getCacheName(),
                    cache.getSize(),
                    cache.getStatus().toString(),
                    cache.getStats(),
                    cache.getHitCountRam(),
                    cache.getHitCountAux(),
                    cache.getMissCountNotFound(),
                    cache.getMissCountExpired(),
                    getByteCount( cache ));

            cacheInfo.add( regionInfo );
        }

        return cacheInfo.toArray(new CacheRegionInfo[0]);
    }


	/**
     * Tries to estimate how much data is in a region. This is expensive. If there are any non serializable objects in
     * the region or an error occurs, suppresses exceptions and returns 0.
     * <p>
     *
     * @return int The size of the region in bytes.
     */
	@Override
    public long getByteCount(String cacheName)
	{
		return getByteCount(cacheHub.getCache(cacheName));
	}

	/**
     * Tries to estimate how much data is in a region. This is expensive. If there are any non serializable objects in
     * the region or an error occurs, suppresses exceptions and returns 0.
     * <p>
     *
     * @return int The size of the region in bytes.
     */
    public <K, V> long getByteCount(CompositeCache<K, V> cache)
    {
        if (cache == null)
        {
            throw new IllegalArgumentException("The cache object specified was null.");
        }

        long size = 0;
        IMemoryCache<K, V> memCache = cache.getMemoryCache();

        for (K key : memCache.getKeySet())
        {
            ICacheElement<K, V> ice = null;
			try
			{
				ice = memCache.get(key);
			}
			catch (IOException e)
			{
                throw new RuntimeException("IOException while trying to get a cached element", e);
			}

			if (ice == null)
			{
				continue;
			}

			if (ice instanceof CacheElementSerialized)
            {
                size += ((CacheElementSerialized<K, V>) ice).getSerializedValue().length;
            }
            else
            {
                Object element = ice.getVal();

                //CountingOnlyOutputStream: Keeps track of the number of bytes written to it, but doesn't write them anywhere.
                CountingOnlyOutputStream counter = new CountingOnlyOutputStream();
                try (ObjectOutputStream out = new ObjectOutputStream(counter);)
                {
                    out.writeObject(element);
                }
                catch (IOException e)
                {
                    throw new RuntimeException("IOException while trying to measure the size of the cached element", e);
                }
                finally
                {
                	try
                	{
						counter.close();
					}
                	catch (IOException e)
                	{
                		// ignore
					}
                }

                // 4 bytes lost for the serialization header
                size = size + counter.getCount() - 4;
            }
        }

        if (size > Long.MAX_VALUE)
        {
            throw new IllegalStateException("The size of cache " + cache.getCacheName() + " (" + size + " bytes) is too large to be represented as an long integer.");
        }

        return size;
    }

    /**
     * Clears all regions in the cache.
     * <p>
     * If this class is running within a remote cache server, clears all regions via the <code>RemoteCacheServer</code>
     * API, so that removes will be broadcast to client machines. Otherwise clears all regions in the cache directly via
     * the usual cache API.
     */
    @Override
    public void clearAllRegions() throws IOException
    {
        if (RemoteCacheServerFactory.getRemoteCacheServer() == null)
        {
            // Not running in a remote cache server.
            // Remove objects from the cache directly, as no need to broadcast removes to client machines...

            String[] names = cacheHub.getCacheNames();

            for (int i = 0; i < names.length; i++)
            {
                cacheHub.getCache(names[i]).removeAll();
            }
        }
        else
        {
            // Running in a remote cache server.
            // Remove objects via the RemoteCacheServer API, so that removes will be broadcast to client machines...
            try
            {
                String[] cacheNames = cacheHub.getCacheNames();

                // Call remoteCacheServer.removeAll(String) for each cacheName...
                RemoteCacheServer<?, ?> remoteCacheServer = RemoteCacheServerFactory.getRemoteCacheServer();
                for (int i = 0; i < cacheNames.length; i++)
                {
                    String cacheName = cacheNames[i];
                    remoteCacheServer.removeAll(cacheName);
                }
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Failed to remove all elements from all cache regions: " + e, e);
            }
        }
    }

    /**
     * Clears a particular cache region.
     * <p>
     * If this class is running within a remote cache server, clears the region via the <code>RemoteCacheServer</code>
     * API, so that removes will be broadcast to client machines. Otherwise clears the region directly via the usual
     * cache API.
     */
    @Override
    public void clearRegion(String cacheName) throws IOException
    {
        if (cacheName == null)
        {
            throw new IllegalArgumentException("The cache name specified was null.");
        }
        if (RemoteCacheServerFactory.getRemoteCacheServer() == null)
        {
            // Not running in a remote cache server.
            // Remove objects from the cache directly, as no need to broadcast removes to client machines...
            cacheHub.getCache(cacheName).removeAll();
        }
        else
        {
            // Running in a remote cache server.
            // Remove objects via the RemoteCacheServer API, so that removes will be broadcast to client machines...
            try
            {
                // Call remoteCacheServer.removeAll(String)...
                RemoteCacheServer<?, ?> remoteCacheServer = RemoteCacheServerFactory.getRemoteCacheServer();
                remoteCacheServer.removeAll(cacheName);
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Failed to remove all elements from cache region [" + cacheName + "]: " + e, e);
            }
        }
    }

    /**
     * Removes a particular item from a particular region.
     * <p>
     * If this class is running within a remote cache server, removes the item via the <code>RemoteCacheServer</code>
     * API, so that removes will be broadcast to client machines. Otherwise clears the region directly via the usual
     * cache API.
     *
     * @param cacheName
     * @param key
     *
     * @throws IOException
     */
    @Override
    public void removeItem(String cacheName, String key) throws IOException
    {
        if (cacheName == null)
        {
            throw new IllegalArgumentException("The cache name specified was null.");
        }
        if (key == null)
        {
            throw new IllegalArgumentException("The key specified was null.");
        }
        if (RemoteCacheServerFactory.getRemoteCacheServer() == null)
        {
            // Not running in a remote cache server.
            // Remove objects from the cache directly, as no need to broadcast removes to client machines...
            cacheHub.getCache(cacheName).remove(key);
        }
        else
        {
            // Running in a remote cache server.
            // Remove objects via the RemoteCacheServer API, so that removes will be broadcast to client machines...
            try
            {
                Object keyToRemove = null;
                CompositeCache<?, ?> cache = CompositeCacheManager.getInstance().getCache(cacheName);

                // A String key was supplied, but to remove elements via the RemoteCacheServer API, we need the
                // actual key object as stored in the cache (i.e. a Serializable object). To find the key in this form,
                // we iterate through all keys stored in the memory cache until we find one whose toString matches
                // the string supplied...
                Set<?> allKeysInCache = cache.getMemoryCache().getKeySet();
                for (Object keyInCache : allKeysInCache)
                {
                    if (keyInCache.toString().equals(key))
                    {
                        if (keyToRemove == null)
                        {
                            keyToRemove = keyInCache;
                        }
                        else
                        {
                            // A key matching the one specified was already found...
                            throw new IllegalStateException("Unexpectedly found duplicate keys in the cache region matching the key specified.");
                        }
                    }
                }
                if (keyToRemove == null)
                {
                    throw new IllegalStateException("No match for this key could be found in the set of keys retrieved from the memory cache.");
                }
                // At this point, we have retrieved the matching K key.

                // Call remoteCacheServer.remove(String, Serializable)...
                RemoteCacheServer<Serializable, Serializable> remoteCacheServer = RemoteCacheServerFactory.getRemoteCacheServer();
                remoteCacheServer.remove(cacheName, key);
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Failed to remove element with key [" + key + ", " + key.getClass() + "] from cache region [" + cacheName + "]: " + e, e);
            }
        }
    }
}
