package org.apache.commons.jcs.auxiliary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.CacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.engine.match.KeyMatcherPatternImpl;
import org.apache.commons.jcs.engine.match.behavior.IKeyMatcher;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;

/** This holds convenience methods used by most auxiliary caches. */
public abstract class AbstractAuxiliaryCache<K, V>
    implements AuxiliaryCache<K, V>
{
    /** An optional event logger */
    private ICacheEventLogger cacheEventLogger;

    /** The serializer. Uses a standard serializer by default. */
    private IElementSerializer elementSerializer = new StandardSerializer();

    /** Key matcher used by the getMatching API */
    private IKeyMatcher<K> keyMatcher = new KeyMatcherPatternImpl<>();

    /**
     * Gets multiple items from the cache based on the given set of keys.
     *
     * @param keys
     * @return a map of K key to ICacheElement&lt;K, V&gt; element, or an empty map if there is no
     *         data in cache for any of these keys
     */
    protected Map<K, ICacheElement<K, V>> processGetMultiple(Set<K> keys) throws IOException
    {
        if (keys != null)
        {
            return keys.stream()
                .map(key -> {
                    try
                    {
                        return get(key);
                    }
                    catch (IOException e)
                    {
                        return null;
                    }
                })
                .filter(element -> element != null)
                .collect(Collectors.toMap(
                        element -> element.getKey(),
                        element -> element));
        }

        return new HashMap<>();
    }

    /**
     * Gets the item from the cache.
     *
     * @param key
     * @return ICacheElement, a wrapper around the key, value, and attributes
     * @throws IOException
     */
    @Override
    public abstract ICacheElement<K, V> get( K key ) throws IOException;

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param item
     * @param eventName
     * @return ICacheEvent
     */
    protected ICacheEvent<K> createICacheEvent( ICacheElement<K, V> item, String eventName )
    {
        if ( cacheEventLogger == null )
        {
            return new CacheEvent<>();
        }
        String diskLocation = getEventLoggingExtraInfo();
        String regionName = null;
        K key = null;
        if ( item != null )
        {
            regionName = item.getCacheName();
            key = item.getKey();
        }
        return cacheEventLogger.createICacheEvent( getAuxiliaryCacheAttributes().getName(), regionName, eventName,
                                                   diskLocation, key );
    }

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param regionName
     * @param key
     * @param eventName
     * @return ICacheEvent
     */
    protected <T> ICacheEvent<T> createICacheEvent( String regionName, T key, String eventName )
    {
        if ( cacheEventLogger == null )
        {
            return new CacheEvent<>();
        }
        String diskLocation = getEventLoggingExtraInfo();
        return cacheEventLogger.createICacheEvent( getAuxiliaryCacheAttributes().getName(), regionName, eventName,
                                                   diskLocation, key );

    }

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param cacheEvent
     */
    protected <T> void logICacheEvent( ICacheEvent<T> cacheEvent )
    {
        if ( cacheEventLogger != null )
        {
            cacheEventLogger.logICacheEvent( cacheEvent );
        }
    }

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param source
     * @param eventName
     * @param optionalDetails
     */
    protected void logApplicationEvent( String source, String eventName, String optionalDetails )
    {
        if ( cacheEventLogger != null )
        {
            cacheEventLogger.logApplicationEvent( source, eventName, optionalDetails );
        }
    }

    /**
     * Logs an event if an event logger is configured.
     * <p>
     * @param source
     * @param eventName
     * @param errorMessage
     */
    protected void logError( String source, String eventName, String errorMessage )
    {
        if ( cacheEventLogger != null )
        {
            cacheEventLogger.logError( source, eventName, errorMessage );
        }
    }

    /**
     * Gets the extra info for the event log.
     * <p>
     * @return IP, or disk location, etc.
     */
    public abstract String getEventLoggingExtraInfo();

    /**
     * Allows it to be injected.
     * <p>
     * @param cacheEventLogger
     */
    @Override
    public void setCacheEventLogger( ICacheEventLogger cacheEventLogger )
    {
        this.cacheEventLogger = cacheEventLogger;
    }

    /**
     * Allows it to be injected.
     * <p>
     * @return cacheEventLogger
     */
    public ICacheEventLogger getCacheEventLogger()
    {
        return this.cacheEventLogger;
    }

    /**
     * Allows you to inject a custom serializer. A good example would be a compressing standard
     * serializer.
     * <p>
     * Does not allow you to set it to null.
     * <p>
     * @param elementSerializer
     */
    @Override
    public void setElementSerializer( IElementSerializer elementSerializer )
    {
        if ( elementSerializer != null )
        {
            this.elementSerializer = elementSerializer;
        }
    }

    /**
     * Allows it to be injected.
     * <p>
     * @return elementSerializer
     */
    public IElementSerializer getElementSerializer()
    {
        return this.elementSerializer;
    }

    /**
     * Sets the key matcher used by get matching.
     * <p>
     * @param keyMatcher
     */
    @Override
    public void setKeyMatcher( IKeyMatcher<K> keyMatcher )
    {
        if ( keyMatcher != null )
        {
            this.keyMatcher = keyMatcher;
        }
    }

    /**
     * Returns the key matcher used by get matching.
     * <p>
     * @return keyMatcher
     */
    public IKeyMatcher<K> getKeyMatcher()
    {
        return this.keyMatcher;
    }
}
