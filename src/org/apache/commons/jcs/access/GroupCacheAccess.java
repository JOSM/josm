package org.apache.commons.jcs.access;

import java.io.IOException;
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

import org.apache.commons.jcs.access.behavior.IGroupCacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.access.exception.InvalidArgumentException;
import org.apache.commons.jcs.engine.CacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.group.GroupAttrName;
import org.apache.commons.jcs.engine.control.group.GroupId;

/**
 * Access for groups.
 */
public class GroupCacheAccess<K, V>
    extends AbstractCacheAccess<GroupAttrName<K>, V>
    implements IGroupCacheAccess<K, V>
{
    /**
     * Constructor for the GroupCacheAccess object
     * <p>
     * @param cacheControl
     */
    public GroupCacheAccess( CompositeCache<GroupAttrName<K>, V> cacheControl )
    {
        super(cacheControl);
    }

    /**
     * Gets an item out of the cache that is in a specified group.
     * <p>
     * @param name
     *            The key name.
     * @param group
     *            The group name.
     * @return The cached value, null if not found.
     */
    @Override
    public V getFromGroup( K name, String group )
    {
        ICacheElement<GroupAttrName<K>, V> element = this.getCacheControl().get( getGroupAttrName( group, name ) );
        return ( element != null ) ? element.getVal() : null;
    }

    /**
     * Internal method used for group functionality.
     * <p>
     * @param group
     * @param name
     * @return GroupAttrName
     */
    private GroupAttrName<K> getGroupAttrName( String group, K name )
    {
        GroupId gid = new GroupId( this.getCacheControl().getCacheName(), group );
        return new GroupAttrName<>( gid, name );
    }

    /**
     * Allows the user to put an object into a group within a particular cache
     * region. This method sets the object's attributes to the default for the
     * region.
     * <p>
     * @param name
     *            The key name.
     * @param groupName
     *            The group name.
     * @param value
     *            The object to cache
     * @throws CacheException
     */
    @Override
    public void putInGroup( K name, String groupName, V value )
        throws CacheException
    {
        putInGroup( name, groupName, value, null );
    }

    /**
     * Allows the user to put an object into a group within a particular cache
     * region. This method allows the object's attributes to be individually
     * specified.
     * <p>
     * @param name
     *            The key name.
     * @param groupName
     *            The group name.
     * @param value
     *            The object to cache
     * @param attr
     *            The objects attributes.
     * @throws CacheException
     */
    @Override
    public void putInGroup( K name, String groupName, V value, IElementAttributes attr )
        throws CacheException
    {
        if ( name == null )
        {
            throw new InvalidArgumentException( "Key must not be null" );
        }

        if ( value == null )
        {
            throw new InvalidArgumentException( "Value must not be null" );
        }

        // Create the element and update. This may throw an IOException which
        // should be wrapped by cache access.
        try
        {
            GroupAttrName<K> key = getGroupAttrName( groupName, name );
            CacheElement<GroupAttrName<K>, V> ce =
                new CacheElement<>( this.getCacheControl().getCacheName(), key, value );

            IElementAttributes attributes = (attr == null) ? this.getCacheControl().getElementAttributes() : attr;
            ce.setElementAttributes( attributes );

            this.getCacheControl().update( ce );
        }
        catch ( IOException e )
        {
            throw new CacheException( e );
        }

    }

    /**
     * @param name
     * @param group
     */
    @Override
    public void removeFromGroup( K name, String group )
    {
        GroupAttrName<K> key = getGroupAttrName( group, name );
        this.getCacheControl().remove( key );
    }

    /**
     * Gets the set of keys of objects currently in the group.
     * <p>
     * @param group
     * @return A Set of keys.
     */
    @Override
    public Set<K> getGroupKeys( String group )
    {
        GroupId groupId = new GroupId( this.getCacheControl().getCacheName(), group );

        return this.getCacheControl().getKeySet()
                .stream()
                .filter(gan -> gan.groupId.equals(groupId))
                .map(gan -> gan.attrName)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the set of group names in the cache
     * <p>
     * @return A Set of group names.
     */
    public Set<String> getGroupNames()
    {
        return this.getCacheControl().getKeySet()
                .stream()
                .map(gan -> gan.groupId.groupName)
                .collect(Collectors.toSet());
    }

    /**
     * Invalidates a group: remove all the group members
     * <p>
     * @param group
     *            The name of the group to invalidate
     */
    @Override
    public void invalidateGroup( String group )
    {
        this.getCacheControl().remove(getGroupAttrName(group, null));
    }
}
