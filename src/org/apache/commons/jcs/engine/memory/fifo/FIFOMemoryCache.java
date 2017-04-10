package org.apache.commons.jcs.engine.memory.fifo;

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
 * The items are spooled in the order they are added. No adjustments to the list are made on get.
 */
public class FIFOMemoryCache<K, V>
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
     * Does nothing.
     * <p>
     * @param me
     */
    @Override
    protected void adjustListForGet( MemoryElementDescriptor<K, V> me )
    {
        // DO NOTHING
    }
}
