package org.apache.commons.jcs.engine.memory.util;

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
import org.apache.commons.jcs.utils.struct.DoubleLinkedListNode;

/**
 * This wrapper is needed for double linked lists.
 */
public class MemoryElementDescriptor<K, V>
    extends DoubleLinkedListNode<ICacheElement<K, V>>
{
    /** Don't change */
    private static final long serialVersionUID = -1905161209035522460L;

    /**
     * Constructs a usable MemoryElementDescriptor.
     * <p>
     * @param ce
     */
    public MemoryElementDescriptor( ICacheElement<K, V> ce )
    {
        super( ce );
    }

    /**
     * Get the cache element
     *
     * @return the ce
     */
    public ICacheElement<K, V> getCacheElement()
    {
        return getPayload();
    }
}
