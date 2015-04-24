package org.apache.commons.jcs.utils.struct;

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

/**
 * This is a node in the double linked list. It is stored as the value in the underlying map used by
 * the LRUMap class.
 */
public class LRUElementDescriptor<K, V>
    extends DoubleLinkedListNode<V>
{
    /** Don't change. */
    private static final long serialVersionUID = 8249555756363020156L;

    /** The key value */
    private K key;

    /**
     * @param key
     * @param payloadP
     */
    public LRUElementDescriptor(K key, V payloadP)
    {
        super(payloadP);
        this.setKey(key);
    }

    /**
     * @param key The key to set.
     */
    public void setKey(K key)
    {
        this.key = key;
    }

    /**
     * @return Returns the key.
     */
    public K getKey()
    {
        return key;
    }
}
