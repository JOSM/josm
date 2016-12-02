package org.apache.commons.jcs.auxiliary.disk;

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

import org.apache.commons.jcs.utils.struct.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extension of LRUMap for logging of removals. Can switch this back to a HashMap easily. This
 * provides some abstraction. It also makes it easy to log overflow.
 */
public class LRUMapJCS<K, V>
    extends LRUMap<K, V>
{
    /** The logger */
    private static final Log log = LogFactory.getLog( LRUMapJCS.class );

    /**
     * This creates an unbounded version.
     */
    public LRUMapJCS()
    {
        super();
    }

    /**
     * This creates a list bounded by the max key size argument. The Boundary is enforces by an LRU
     * eviction policy.
     * <p>
     * This is used in the Disk cache to store keys and purgatory elements if a boundary is
     * requested.
     * <p>
     * The LRU memory cache uses its own LRU implementation.
     * <p>
     * @param maxKeySize
     */
    public LRUMapJCS( int maxKeySize )
    {
        super( maxKeySize );
    }

    /**
     * This is called when an item is removed from the LRU. We just log some information.
     * <p>
     * @param key
     * @param value
     */
    @Override
    protected void processRemovedLRU(K key, V value)
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Removing key [" + key + "] from key store, value [" + value + "]" );
            log.debug( "Key store size [" + this.size() + "]" );
        }
    }
}
