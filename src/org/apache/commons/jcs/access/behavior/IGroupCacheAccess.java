package org.apache.commons.jcs.access.behavior;

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
import org.apache.commons.jcs.engine.behavior.IElementAttributes;

import java.util.Set;

/**
 * IGroupCacheAccess defines group specific behavior for the client access
 * classes.
 */
public interface IGroupCacheAccess<K, V>
    extends ICacheAccessManagement
{
    /**
     * Gets the g attribute of the IGroupCacheAccess object
     * <p>
     * @param name
     * @param group
     *            the name of the group to associate this with.
     * @return The object that is keyed by the name in the group
     */
    V getFromGroup( K name, String group );

    /**
     * Puts an item in the cache associated with this group.
     * <p>
     * @param key
     * @param group
     * @param obj
     * @throws CacheException
     */
    void putInGroup( K key, String group, V obj )
        throws CacheException;

    /**
     * Put in the cache associated with this group using these attributes.
     * <p>
     * @param key
     * @param group
     * @param obj
     * @param attr
     * @throws CacheException
     */
    void putInGroup( K key, String group, V obj, IElementAttributes attr )
        throws CacheException;

    /**
     * Remove the item from this group in this region by this name.
     * <p>
     * @param name
     * @param group
     */
    void removeFromGroup( K name, String group );

    /**
     * Gets the set of keys of objects currently in the group
     * <p>
     * @param group
     * @return the set of group keys.
     */
    Set<K> getGroupKeys( String group );

    /**
     * Invalidates a group
     * <p>
     * @param group
     */
    void invalidateGroup( String group );
}
