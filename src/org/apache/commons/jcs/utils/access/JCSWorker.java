package org.apache.commons.jcs.utils.access;

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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.GroupCacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to encapsulate doing a piece of work, and caching the results
 * in JCS. Simply construct this class with the region name for the Cache and
 * keep a static reference to it instead of the JCS itself. Then make a new
 * org.apache.commons.jcs.utils.access.AbstractJCSWorkerHelper and implement Object
 * doWork() and do the work in there, returning the object to be cached. Then
 * call .getResult() with the key and the AbstractJCSWorkerHelper to get the
 * result of the work. If the object isn't already in the Cache,
 * AbstractJCSWorkerHelper.doWork() will get called, and the result will be put
 * into the cache. If the object is already in cache, the cached result will be
 * returned instead.
 * <p>
 * As an added bonus, multiple JCSWorkers with the same region, and key won't do
 * the work multiple times: The first JCSWorker to get started will do the work,
 * and all subsequent workers with the same region, group, and key will wait on
 * the first one and use his resulting work instead of doing the work
 * themselves.
 * <p>
 * This is ideal when the work being done is a query to the database where the
 * results may take time to be retrieved.
 * <p>
 * For example:
 *
 * <pre>
 *      public static JCSWorker cachingWorker = new JCSWorker(&quot;example region&quot;);
 *   		public Object getSomething(Serializable aKey){
 *        JCSWorkerHelper helper = new AbstractJCSWorkerHelper(){
 *          public Object doWork(){
 *            // Do some (DB?) work here which results in a list
 *            // This only happens if the cache dosn't have a item in this region for aKey
 *            // Note this is especially useful with Hibernate, which will cache indiviual
 *            // Objects, but not entire query result sets.
 *            List results = query.list();
 *            // Whatever we return here get's cached with aKey, and future calls to
 *            // getResult() on a CachedWorker with the same region and key will return that instead.
 *            return results;
 *        };
 *        List result = worker.getResult(aKey, helper);
 *      }
 * </pre>
 *
 * This is essentially the same as doing:
 *
 * <pre>
 * JCS jcs = JCS.getInstance( &quot;exampleregion&quot; );
 * List results = (List) jcs.get( aKey );
 * if ( results != null )
 * {
 *     //do the work here
 *     results = query.list();
 *     jcs.put( aKey, results );
 * }
 * </pre>
 *
 * <p>
 * But has the added benefit of the work-load sharing; under normal
 * circumstances if multiple threads all tried to do the same query at the same
 * time, the same query would happen multiple times on the database, and the
 * resulting object would get put into JCS multiple times.
 * <p>
 * @author Travis Savo
 */
public class JCSWorker<K, V>
{
    /** The logger */
    private static final Log logger = LogFactory.getLog( JCSWorker.class );

    /** The cache we are working with */
    private CacheAccess<K, V> cache;

    /** The cache we are working with */
    private GroupCacheAccess<K, V> groupCache;

    /**
     * Map to hold who's doing work presently.
     */
    private volatile ConcurrentMap<String, JCSWorkerHelper<V>> map = new ConcurrentHashMap<>();

    /**
     * Region for the JCS cache.
     */
    private final String region;

    /**
     * Constructor which takes a region for the JCS cache.
     * @param aRegion
     *            The Region to use for the JCS cache.
     */
    public JCSWorker( final String aRegion )
    {
        region = aRegion;
        try
        {
            cache = JCS.getInstance( aRegion );
            groupCache = JCS.getGroupCacheInstance( aRegion );
        }
        catch ( CacheException e )
        {
            throw new RuntimeException( e.getMessage() );
        }
    }

    /**
     * Getter for the region of the JCS Cache.
     * @return The JCS region in which the result will be cached.
     */
    public String getRegion()
    {
        return region;
    }

    /**
     * Gets the cached result for this region/key OR does the work and caches
     * the result, returning the result. If the result has not been cached yet,
     * this calls doWork() on the JCSWorkerHelper to do the work and cache the
     * result. This is also an opportunity to do any post processing of the
     * result in your CachedWorker implementation.
     * @param aKey
     *            The key to get/put with on the Cache.
     * @param aWorker
     *            The JCSWorkerHelper implementing Object doWork(). This gets
     *            called if the cache get misses, and the result is put into
     *            cache.
     * @return The result of doing the work, or the cached result.
     * @throws Exception
     *             Throws an exception if anything goes wrong while doing the
     *             work.
     */
    public V getResult( K aKey, JCSWorkerHelper<V> aWorker )
        throws Exception
    {
        return run( aKey, null, aWorker );
    }

    /**
     * Gets the cached result for this region/key OR does the work and caches
     * the result, returning the result. If the result has not been cached yet,
     * this calls doWork() on the JCSWorkerHelper to do the work and cache the
     * result. This is also an opportunity to do any post processing of the
     * result in your CachedWorker implementation.
     * @param aKey
     *            The key to get/put with on the Cache.
     * @param aGroup
     *            The cache group to put the result in.
     * @param aWorker
     *            The JCSWorkerHelper implementing Object doWork(). This gets
     *            called if the cache get misses, and the result is put into
     *            cache.
     * @return The result of doing the work, or the cached result.
     * @throws Exception
     *             Throws an exception if anything goes wrong while doing the
     *             work.
     */
    public V getResult( K aKey, String aGroup, JCSWorkerHelper<V> aWorker )
        throws Exception
    {
        return run( aKey, aGroup, aWorker );
    }

    /**
     * Try and get the object from the cache, and if it's not there, do the work
     * and cache it. This also ensures that only one CachedWorker is doing the
     * work and subsequent calls to a CachedWorker with identical
     * region/key/group will wait on the results of this call. It will call the
     * JCSWorkerHelper.doWork() if the cache misses, and will put the result.
     * @param aKey
     * @param aGroup
     * @param aHelper
     * @return Either the result of doing the work, or the cached result.
     * @throws Exception
     *             If something goes wrong while doing the work, throw an
     *             exception.
     */
    private V run( K aKey, String aGroup, JCSWorkerHelper<V> aHelper )
        throws Exception
    {
        V result = null;
        // long start = 0;
        // long dbTime = 0;
        JCSWorkerHelper<V> helper = map.putIfAbsent(getRegion() + aKey, aHelper);

        if ( helper != null )
        {
            synchronized ( helper )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Found a worker already doing this work (" + getRegion() + ":" + aKey + ")." );
                }
                while ( !helper.isFinished() )
                {
                    try
                    {
                        helper.wait();
                    }
                    catch (InterruptedException e)
                    {
                        // expected
                    }
                }
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Another thread finished our work for us. Using those results instead. ("
                        + getRegion() + ":" + aKey + ")." );
                }
            }
        }
        // Do the work
        try
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( getRegion() + " is doing the work." );
            }

            // Try to get the item from the cache
            if ( aGroup != null )
            {
                result = groupCache.getFromGroup( aKey, aGroup );
            }
            else
            {
                result = cache.get( aKey );
            }
            // If the cache dosn't have it, do the work.
            if ( result == null )
            {
                result = aHelper.doWork();
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Work Done, caching: key:" + aKey + ", group:" + aGroup + ", result:" + result + "." );
                }
                // Stick the result of the work in the cache.
                if ( aGroup != null )
                {
                    groupCache.putInGroup( aKey, aGroup, result );
                }
                else
                {
                    cache.put( aKey, result );
                }
            }
            // return the result
            return result;
        }
        finally
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( getRegion() + ":" + aKey + " entered finally." );
            }

            // Remove ourselves as the worker.
            if ( helper == null )
            {
                map.remove( getRegion() + aKey );
            }
            synchronized ( aHelper )
            {
                aHelper.setFinished( true );
                // Wake everyone waiting on us
                aHelper.notifyAll();
            }
        }
    }
}
