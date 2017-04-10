package org.apache.commons.jcs.engine.memory.lru;

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

import java.io.IOException;

import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.memory.AbstractDoubleLinkedListMemoryCache;
import org.apache.commons.jcs.engine.memory.util.MemoryElementDescriptor;

/**
 * A fast reference management system. The least recently used items move to the end of the list and
 * get spooled to disk if the cache hub is configured to use a disk cache. Most of the cache
 * bottlenecks are in IO. There are no io bottlenecks here, it's all about processing power.
 * <p>
 * Even though there are only a few adjustments necessary to maintain the double linked list, we
 * might want to find a more efficient memory manager for large cache regions.
 * <p>
 * The LRUMemoryCache is most efficient when the first element is selected. The smaller the region,
 * the better the chance that this will be the case. &lt; .04 ms per put, p3 866, 1/10 of that per get
 */
public class LRUMemoryCache<K, V>
    extends AbstractDoubleLinkedListMemoryCache<K, V>
{
    /**
     * Puts an item to the cache. Removes any pre-existing entries of the same key from the linked
     * list and adds this one first.
     * <p>
     * @param ce The cache element, or entry wrapper
     * @return MemoryElementDescriptor the new node
     * @throws IOException
     */
    @Override
    protected MemoryElementDescriptor<K, V> adjustListForUpdate( ICacheElement<K, V> ce )
        throws IOException
    {
        return addFirst( ce );
    }

    /**
     * Makes the item the first in the list.
     * <p>
     * @param me
     */
    @Override
    protected void adjustListForGet( MemoryElementDescriptor<K, V> me )
    {
        list.makeFirst( me );
    }
}
