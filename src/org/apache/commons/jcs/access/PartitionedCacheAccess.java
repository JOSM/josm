package org.apache.commons.jcs.access;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.access.exception.ConfigurationException;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.stats.behavior.ICacheStats;
import org.apache.commons.jcs.utils.props.AbstractPropertyContainer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO:  Add new methods that will allow you to provide a partition indicator for all major calls.  Add an interface as well.
 * <p>
 * This handles dividing puts and gets.
 * <p>
 * There are two required properties.
 * <p>
 * <ol>
 * <li>.numberOfPartitions</li>
 * <li>.partitionRegionNamePrefix</li>
 * </ol>
 * System properties will override values in the properties file.
 * <p>
 * We use a JCS region name for each partition that looks like this: partitionRegionNamePrefix + "_"
 * + partitionNumber. The number is 0 indexed based.
 * <p>
 * @author Aaron Smuts
 */
public class PartitionedCacheAccess<K, V>
    extends AbstractPropertyContainer
    implements ICacheAccess<K, V>
{
    /** the logger. */
    private static final Log log = LogFactory.getLog( PartitionedCacheAccess.class );

    /** The number of partitions. */
    private int numberOfPartitions = 1;

    /**
     * We use a JCS region name for each partition that looks like this: partitionRegionNamePrefix +
     * "_" + partitionNumber
     */
    private String partitionRegionNamePrefix;

    /** An array of partitions built during initialization. */
    private ICacheAccess<K, V>[] partitions;

    /** Is the class initialized. */
    private boolean initialized = false;

    /** Sets default properties heading and group. */
    public PartitionedCacheAccess()
    {
        setPropertiesHeading( "PartitionedCacheAccess" );
        setPropertiesGroup( "cache" );
    }

    /**
     * Puts the value into the appropriate cache partition.
     * <p>
     * @param key key
     * @param object object
     * @throws CacheException on configuration problem
     */
    @Override
    public void put( K key, V object )
        throws CacheException
    {
        if ( key == null || object == null )
        {
            log.warn( "Bad input key [" + key + "].  Cannot put null into the cache." );
            return;
        }

        if (!ensureInit())
        {
            return;
        }

        int partition = getPartitionNumberForKey( key );
        try
        {
            partitions[partition].put( key, object );
        }
        catch ( CacheException e )
        {
            log.error( "Problem putting value for key [" + key + "] in cache [" + partitions[partition] + "]" );
            throw e;
        }
    }

    /**
     * Puts in cache if an item does not exist with the name in that region.
     * <p>
     * @param key
     * @param object
     * @throws CacheException
     */
    @Override
    public void putSafe( K key, V object )
        throws CacheException
    {
        if ( key == null || object == null )
        {
            log.warn( "Bad input key [" + key + "].  Cannot putSafe null into the cache." );
        }

        if (!ensureInit())
        {
            return;
        }

        int partition = getPartitionNumberForKey( key );
        partitions[partition].putSafe( key, object );
    }

    /**
     * Puts the value into the appropriate cache partition.
     * <p>
     * @param key key
     * @param object object
     * @param attr
     * @throws CacheException on configuration problem
     */
    @Override
    public void put( K key, V object, IElementAttributes attr )
        throws CacheException
    {
        if ( key == null || object == null )
        {
            log.warn( "Bad input key [" + key + "].  Cannot put null into the cache." );
            return;
        }

        if (!ensureInit())
        {
            return;
        }

        int partition = getPartitionNumberForKey( key );

        try
        {
            partitions[partition].put( key, object, attr );
        }
        catch ( CacheException e )
        {
            log.error( "Problem putting value for key [" + key + "] in cache [" + partitions[partition] + "]" );
            throw e;
        }
    }

    /**
     * Gets the object for the key from the desired partition.
     * <p>
     * @param key key
     * @return result, null if not found.
     */
    @Override
    public V get( K key )
    {
        if ( key == null )
        {
            log.warn( "Input key is null." );
            return null;
        }

        if (!ensureInit())
        {
            return null;
        }

        int partition = getPartitionNumberForKey( key );

        return partitions[partition].get( key );
    }

    /**
     * Retrieve an object from the cache region this instance provides access to.
     * If the object cannot be found in the cache, it will be retrieved by
     * calling the supplier and subsequently storing it in the cache.
     * <p>
     * @param name
     * @param supplier supplier to be called if the value is not found
     * @return Object.
     */
    @Override
    public V get(K name, Supplier<V> supplier)
    {
        V value = get(name);

        if (value == null)
        {
            value = supplier.get();
            put(name, value);
        }

        return value;
    }

    /**
     * Gets the ICacheElement&lt;K, V&gt; (the wrapped object) for the key from the desired partition.
     * <p>
     * @param key key
     * @return result, null if not found.
     */
    @Override
    public ICacheElement<K, V> getCacheElement( K key )
    {
        if ( key == null )
        {
            log.warn( "Input key is null." );
            return null;
        }

        if (!ensureInit())
        {
            return null;
        }

        int partition = getPartitionNumberForKey( key );

        return partitions[partition].getCacheElement( key );
    }

    /**
     * This is a getMultiple. We try to group the keys so that we make as few calls as needed.
     * <p>
     * @param names
     * @return Map of keys to ICacheElement
     */
    @Override
    public Map<K, ICacheElement<K, V>> getCacheElements( Set<K> names )
    {
        if ( names == null )
        {
            log.warn( "Bad input names cannot be null." );
            return Collections.emptyMap();
        }

        if (!ensureInit())
        {
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked") // No generic arrays in java
        Set<K>[] dividedNames = new Set[this.getNumberOfPartitions()];

        for (K key : names)
        {
            int partition = getPartitionNumberForKey( key );
            if ( dividedNames[partition] == null )
            {
                dividedNames[partition] = new HashSet<>();
            }
            dividedNames[partition].add( key );
        }

        Map<K, ICacheElement<K, V>> result = new HashMap<>();
        for ( int i = 0; i < partitions.length; i++ )
        {
            if ( dividedNames[i] != null && !dividedNames[i].isEmpty() )
            {
                result.putAll( partitions[i].getCacheElements( dividedNames[i] ) );
            }
        }
        return result;
    }

    /**
     * This is tricky. Do we need to get from all the partitions?
     * <p>
     * If this interface took an object, we could use the hashcode to determine the partition. Then
     * we could use the toString for the pattern.
     * <p>
     * @param pattern
     * @return HashMap key to value
     */
    @Override
    public Map<K, V> getMatching( String pattern )
    {
        if ( pattern == null )
        {
            log.warn( "Input pattern is null." );
            return null;
        }

        if (!ensureInit())
        {
            return null;
        }

        Map<K, V> result = new HashMap<>();
        for (ICacheAccess<K, V> partition : partitions)
        {
            result.putAll( partition.getMatching( pattern ) );
        }

        return result;
    }

    /**
     * This is tricky. Do we need to get from all the partitions?
     * <p>
     * @param pattern
     * @return HashMap key to ICacheElement
     */
    @Override
    public Map<K, ICacheElement<K, V>> getMatchingCacheElements( String pattern )
    {
        if ( pattern == null )
        {
            log.warn( "Input pattern is null." );
            return null;
        }

        if (!ensureInit())
        {
            return null;
        }

        Map<K, ICacheElement<K, V>> result = new HashMap<>();
        for (ICacheAccess<K, V> partition : partitions)
        {
            result.putAll( partition.getMatchingCacheElements( pattern ) );
        }
        return result;
    }

    /**
     * Removes the item from the appropriate partition.
     * <p>
     * @param key
     * @throws CacheException
     */
    @Override
    public void remove( K key )
        throws CacheException
    {
        if ( key == null )
        {
            log.warn( "Input key is null. Cannot remove null from the cache." );
            return;
        }

        if (!ensureInit())
        {
            return;
        }

        int partition = getPartitionNumberForKey( key );
        try
        {
            partitions[partition].remove( key );
        }
        catch ( CacheException e )
        {
            log.error( "Problem removing value for key [" + key + "] in cache [" + partitions[partition] + "]" );
            throw e;
        }
    }

    /**
     * Calls free on each partition.
     * <p>
     * @param numberToFree
     * @return number removed
     * @throws CacheException
     */
    @Override
    public int freeMemoryElements( int numberToFree )
        throws CacheException
    {
        if (!ensureInit())
        {
            return 0;
        }

        int count = 0;
        for (ICacheAccess<K, V> partition : partitions)
        {
            count += partition.freeMemoryElements( numberToFree );
        }
        return count;
    }

    /**
     * @return ICompositeCacheAttributes from the first partition.
     */
    @Override
    public ICompositeCacheAttributes getCacheAttributes()
    {
        if (!ensureInit())
        {
            return null;
        }

        if ( partitions.length == 0 )
        {
            return null;
        }

        return partitions[0].getCacheAttributes();
    }

    /**
     * @return IElementAttributes from the first partition.
     * @throws CacheException
     */
    @Override
    public IElementAttributes getDefaultElementAttributes()
        throws CacheException
    {
        if (!ensureInit())
        {
            return null;
        }

        if ( partitions.length == 0 )
        {
            return null;
        }

        return partitions[0].getDefaultElementAttributes();
    }

    /**
     * This is no more efficient than simply getting the cache element.
     * <p>
     * @param key
     * @return IElementAttributes
     * @throws CacheException
     */
    @Override
    public IElementAttributes getElementAttributes( K key )
        throws CacheException
    {
        if ( key == null )
        {
            log.warn( "Input key is null. Cannot getElementAttributes for null from the cache." );
            return null;
        }

        if (!ensureInit())
        {
            return null;
        }

        int partition = getPartitionNumberForKey( key );

        return partitions[partition].getElementAttributes( key );
    }

    /**
     * Resets the attributes for this item. This has the same effect as an update, in most cases.
     * None of the auxiliaries are optimized to do this more efficiently than a simply update.
     * <p>
     * @param key
     * @param attributes
     * @throws CacheException
     */
    @Override
    public void resetElementAttributes( K key, IElementAttributes attributes )
        throws CacheException
    {
        if ( key == null )
        {
            log.warn( "Input key is null. Cannot resetElementAttributes for null." );
            return;
        }

        if (!ensureInit())
        {
            return;
        }

        int partition = getPartitionNumberForKey( key );

        partitions[partition].resetElementAttributes( key, attributes );
    }

    /**
     * Sets the attributes on all the partitions.
     * <p>
     * @param cattr
     */
    @Override
    public void setCacheAttributes( ICompositeCacheAttributes cattr )
    {
        if (!ensureInit())
        {
            return;
        }

        for (ICacheAccess<K, V> partition : partitions)
        {
            partition.setCacheAttributes( cattr );
        }
    }

    /**
     * Removes all of the elements from a region.
     * <p>
     * @throws CacheException
     */
    @Override
    public void clear()
        throws CacheException
    {
        if (!ensureInit())
        {
            return;
        }

        for (ICacheAccess<K, V> partition : partitions)
        {
            partition.clear();
        }
    }

    /**
     * This method is does not reset the attributes for items already in the cache. It could
     * potentially do this for items in memory, and maybe on disk (which would be slow) but not
     * remote items. Rather than have unpredictable behavior, this method just sets the default
     * attributes. Items subsequently put into the cache will use these defaults if they do not
     * specify specific attributes.
     * <p>
     * @param attr the default attributes.
     * @throws CacheException if something goes wrong.
     */
    @Override
    public void setDefaultElementAttributes( IElementAttributes attr )
        throws CacheException
    {
        if (!ensureInit())
        {
            return;
        }

        for (ICacheAccess<K, V> partition : partitions)
        {
            partition.setDefaultElementAttributes(attr);
        }
    }

    /**
     * This returns the ICacheStats object with information on this region and its auxiliaries.
     * <p>
     * This data can be formatted as needed.
     * <p>
     * @return ICacheStats
     */
    @Override
    public ICacheStats getStatistics()
    {
        if (!ensureInit())
        {
            return null;
        }

        if ( partitions.length == 0 )
        {
            return null;
        }

        return partitions[0].getStatistics();
    }

    /**
     * @return A String version of the stats.
     */
    @Override
    public String getStats()
    {
        if (!ensureInit())
        {
            return "";
        }

        StringBuilder stats = new StringBuilder();
        for (ICacheAccess<K, V> partition : partitions)
        {
            stats.append(partition.getStats());
            stats.append("\n");
        }

        return stats.toString();
    }

    /**
     * Dispose this region. Flushes objects to and closes auxiliary caches. This is a shutdown
     * command!
     * <p>
     * To simply remove all elements from the region use clear().
     */
    @Override
    public synchronized void dispose()
    {
        if (!ensureInit())
        {
            return;
        }

        for (ICacheAccess<K, V> partition : partitions)
        {
            partition.dispose();
        }

        initialized = false;
    }

    /**
     * This expects a numeric key. If the key cannot be converted into a number, we will return 0.
     * TODO we could md5 it or get the hashcode.
     * <p>
     * We determine the partition by taking the mod of the number of partitions.
     * <p>
     * @param key key
     * @return the partition number.
     */
    protected int getPartitionNumberForKey( K key )
    {
        if ( key == null )
        {
            return 0;
        }

        long keyNum = getNumericValueForKey( key );

        int partition = (int) ( keyNum % getNumberOfPartitions() );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Using partition [" + partition + "] for key [" + key + "]" );
        }

        return partition;
    }

    /**
     * This can be overridden for special purposes.
     * <p>
     * @param key key
     * @return long
     */
    public long getNumericValueForKey( K key )
    {
        String keyString = key.toString();
        long keyNum = -1;
        try
        {
            keyNum = Long.parseLong( keyString );
        }
        catch ( NumberFormatException e )
        {
            // THIS IS UGLY, but I can't think of a better failsafe right now.
            keyNum = key.hashCode();
            log.warn( "Couldn't convert [" + key + "] into a number.  Will use hashcode [" + keyNum + "]" );
        }
        return keyNum;
    }

    /**
     * Initialize if we haven't already.
     * <p>
     * @throws ConfigurationException on configuration problem
     */
    protected synchronized boolean ensureInit()
    {
        if ( !initialized )
        {
            try
            {
                initialize();
            }
            catch ( ConfigurationException e )
            {
                log.error( "Couldn't configure partioned access.", e );
                return false;
            }
        }

        return true;
    }

    /**
     * Use the partition prefix and the number of partitions to get JCS regions.
     * <p>
     * @throws ConfigurationException on configuration problem
     */
    protected synchronized void initialize()
        throws ConfigurationException
    {
        ensureProperties();

        @SuppressWarnings("unchecked") // No generic arrays in java
        ICacheAccess<K, V>[] tempPartitions = new ICacheAccess[this.getNumberOfPartitions()];
        for ( int i = 0; i < this.getNumberOfPartitions(); i++ )
        {
            String regionName = this.getPartitionRegionNamePrefix() + "_" + i;
            try
            {
                tempPartitions[i] = JCS.getInstance( regionName );
            }
            catch ( CacheException e )
            {
                log.error( "Problem getting cache for region [" + regionName + "]" );
            }
        }

        partitions = tempPartitions;
        initialized = true;
    }

    /**
     * Loads in the needed configuration settings. System properties are checked first. A system
     * property will override local property value.
     * <p>
     * Loads the following JCS Cache specific properties:
     * <ul>
     * <li>heading.numberOfPartitions</li>
     * <li>heading.partitionRegionNamePrefix</li>
     * </ul>
     * @throws ConfigurationException on configuration problem
     */
    @Override
    protected void handleProperties()
        throws ConfigurationException
    {
        // Number of Partitions.
        String numberOfPartitionsPropertyName = this.getPropertiesHeading() + ".numberOfPartitions";
        String numberOfPartitionsPropertyValue = getPropertyForName( numberOfPartitionsPropertyName, true );
        try
        {
            this.setNumberOfPartitions( Integer.parseInt( numberOfPartitionsPropertyValue ) );
        }
        catch ( NumberFormatException e )
        {
            String message = "Could not convert [" + numberOfPartitionsPropertyValue + "] into a number for ["
                + numberOfPartitionsPropertyName + "]";
            log.error( message );
            throw new ConfigurationException( message );
        }

        // Partition Name Prefix.
        String prefixPropertyName = this.getPropertiesHeading() + ".partitionRegionNamePrefix";
        String prefix = getPropertyForName( prefixPropertyName, true );
        this.setPartitionRegionNamePrefix( prefix );
    }

    /**
     * Checks the system properties before the properties.
     * <p>
     * @param propertyName name
     * @param required is it required?
     * @return the property value if one is found
     * @throws ConfigurationException thrown if it is required and not found.
     */
    protected String getPropertyForName( String propertyName, boolean required )
        throws ConfigurationException
    {
        String propertyValue = null;
        propertyValue = System.getProperty( propertyName );
        if ( propertyValue != null )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Found system property override: Name [" + propertyName + "] Value [" + propertyValue + "]" );
            }
        }
        else
        {
            propertyValue = this.getProperties().getProperty( propertyName );
            if ( required && propertyValue == null )
            {
                String message = "Could not find required property [" + propertyName + "] in propertiesGroup ["
                    + this.getPropertiesGroup() + "]";
                log.error( message );
                throw new ConfigurationException( message );
            }
            else
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "Name [" + propertyName + "] Value [" + propertyValue + "]" );
                }
            }
        }
        return propertyValue;
    }

    /**
     * @param numberOfPartitions The numberOfPartitions to set.
     */
    protected void setNumberOfPartitions( int numberOfPartitions )
    {
        this.numberOfPartitions = numberOfPartitions;
    }

    /**
     * @return Returns the numberOfPartitions.
     */
    protected int getNumberOfPartitions()
    {
        return numberOfPartitions;
    }

    /**
     * @param partitionRegionNamePrefix The partitionRegionNamePrefix to set.
     */
    protected void setPartitionRegionNamePrefix( String partitionRegionNamePrefix )
    {
        this.partitionRegionNamePrefix = partitionRegionNamePrefix;
    }

    /**
     * @return Returns the partitionRegionNamePrefix.
     */
    protected String getPartitionRegionNamePrefix()
    {
        return partitionRegionNamePrefix;
    }

    /**
     * @param partitions The partitions to set.
     */
    protected void setPartitions( ICacheAccess<K, V>[] partitions )
    {
        this.partitions = partitions;
    }

    /**
     * @return Returns the partitions.
     */
    protected ICacheAccess<K, V>[] getPartitions()
    {
        return partitions;
    }
}
