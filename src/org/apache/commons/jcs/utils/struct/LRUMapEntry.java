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

import java.io.Serializable;
import java.util.Map.Entry;

/**
 * Entry for the LRUMap.
 * <p>
 * @author Aaron Smuts
 */
public class LRUMapEntry<K, V>
    implements Entry<K, V>, Serializable
{
    /** Don't change */
    private static final long serialVersionUID = -8176116317739129331L;

    /** key */
    private final K key;

    /** value */
    private V value;

    /**
     * S
     * @param key
     * @param value
     */
    public LRUMapEntry(K key, V value)
    {
        this.key = key;
        this.value = value;
    }

    /**
     * @return key
     */
    @Override
    public K getKey()
    {
        return this.key;
    }

    /**
     * @return value
     */
    @Override
    public V getValue()
    {
        return this.value;
    }

    /**
     * @param valueArg
     * @return the old value
     */
    @Override
    public V setValue(V valueArg)
    {
        V old = this.value;
        this.value = valueArg;
        return old;
    }
}
