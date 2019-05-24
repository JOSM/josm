package org.apache.commons.jcs;

import java.util.Properties;

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

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.GroupCacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;
import org.apache.commons.jcs.engine.control.group.GroupAttrName;

/**
 * Simple class for using JCS. To use JCS in your application, you can use the static methods of
 * this class to get access objects (instances of this class) for your cache regions. One CacheAccess
 * object should be created for each region you want to access. If you have several regions, then
 * get instances for each. For best performance the getInstance call should be made in an
 * initialization method.
 */
public abstract class JCS
{
    /** cache.ccf alternative. */
    private static String configFilename = null;

    /** alternative configuration properties */
    private static Properties configProps = null;

    /** Cache manager use by the various forms of defineRegion and getAccess */
    private static CompositeCacheManager cacheMgr;

    /**
     * Set the filename that the cache manager will be initialized with. Only matters before the
     * instance is initialized.
     * <p>
     * @param configFilename
     */
    public static void setConfigFilename( String configFilename )
    {
        JCS.configFilename = configFilename;
    }

    /**
     * Set the properties that the cache manager will be initialized with. Only
     * matters before the instance is initialized.
     *
     * @param configProps
     */
    public static void setConfigProperties( Properties configProps )
    {
        JCS.configProps = configProps;
    }

    /**
     * Shut down the cache manager and set the instance to null
     */
    public static void shutdown()
    {
        synchronized ( JCS.class )
        {
            if ( cacheMgr != null && cacheMgr.isInitialized())
            {
            	cacheMgr.shutDown();
            }

            cacheMgr = null;
        }
    }

    /**
     * Helper method which checks to make sure the cacheMgr class field is set, and if not requests
     * an instance from CacheManagerFactory.
     *
     * @throws CacheException if the configuration cannot be loaded
     */
    private static CompositeCacheManager getCacheManager() throws CacheException
    {
        synchronized ( JCS.class )
        {
            if ( cacheMgr == null || !cacheMgr.isInitialized())
            {
                if ( configProps != null )
                {
                    cacheMgr = CompositeCacheManager.getUnconfiguredInstance();
                    cacheMgr.configure( configProps );
                }
                else if ( configFilename != null )
                {
                    cacheMgr = CompositeCacheManager.getUnconfiguredInstance();
                    cacheMgr.configure( configFilename );
                }
                else
                {
                    cacheMgr = CompositeCacheManager.getInstance();
                }
            }

            return cacheMgr;
        }
    }

    /**
     * Get a CacheAccess which accesses the provided region.
     * <p>
     * @param region Region that return CacheAccess will provide access to
     * @return A CacheAccess which provides access to a given region.
     * @throws CacheException
     */
    public static <K, V> CacheAccess<K, V> getInstance( String region )
        throws CacheException
    {
        CompositeCache<K, V> cache = getCacheManager().getCache( region );
        return new CacheAccess<>( cache );
    }

    /**
     * Get a CacheAccess which accesses the provided region.
     * <p>
     * @param region Region that return CacheAccess will provide access to
     * @param icca CacheAttributes for region
     * @return A CacheAccess which provides access to a given region.
     * @throws CacheException
     */
    public static <K, V> CacheAccess<K, V> getInstance( String region, ICompositeCacheAttributes icca )
        throws CacheException
    {
        CompositeCache<K, V> cache = getCacheManager().getCache( region, icca );
        return new CacheAccess<>( cache );
    }

    /**
     * Get a CacheAccess which accesses the provided region.
     * <p>
     * @param region Region that return CacheAccess will provide access to
     * @param icca CacheAttributes for region
     * @param eattr ElementAttributes for the region
     * @return A CacheAccess which provides access to a given region.
     * @throws CacheException
     */
    public static <K, V> CacheAccess<K, V> getInstance( String region, ICompositeCacheAttributes icca,  IElementAttributes eattr )
        throws CacheException
    {
        CompositeCache<K, V> cache = getCacheManager().getCache( region, icca, eattr );
        return new CacheAccess<>( cache );
    }

    /**
     * Get a GroupCacheAccess which accesses the provided region.
     * <p>
     * @param region Region that return GroupCacheAccess will provide access to
     * @return A GroupCacheAccess which provides access to a given region.
     * @throws CacheException
     */
    public static <K, V> GroupCacheAccess<K, V> getGroupCacheInstance( String region )
        throws CacheException
    {
        CompositeCache<GroupAttrName<K>, V> cache = getCacheManager().getCache( region );
        return new GroupCacheAccess<>( cache );
    }

    /**
     * Get a GroupCacheAccess which accesses the provided region.
     * <p>
     * @param region Region that return GroupCacheAccess will provide access to
     * @param icca CacheAttributes for region
     * @return A GroupCacheAccess which provides access to a given region.
     * @throws CacheException
     */
    public static <K, V> GroupCacheAccess<K, V> getGroupCacheInstance( String region, ICompositeCacheAttributes icca )
        throws CacheException
    {
        CompositeCache<GroupAttrName<K>, V> cache = getCacheManager().getCache( region, icca );
        return new GroupCacheAccess<>( cache );
    }

    /**
     * Get a GroupCacheAccess which accesses the provided region.
     * <p>
     * @param region Region that return CacheAccess will provide access to
     * @param icca CacheAttributes for region
     * @param eattr ElementAttributes for the region
     * @return A GroupCacheAccess which provides access to a given region.
     * @throws CacheException
     */
    public static <K, V> GroupCacheAccess<K, V> getGroupCacheInstance( String region, ICompositeCacheAttributes icca,  IElementAttributes eattr )
        throws CacheException
    {
        CompositeCache<GroupAttrName<K>, V> cache = getCacheManager().getCache( region, icca, eattr );
        return new GroupCacheAccess<>( cache );
    }
}
