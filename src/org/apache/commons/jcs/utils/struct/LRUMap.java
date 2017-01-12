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
 *
 * @author Wiktor Niesiobędzki
 *
 *         Simple LRUMap implementation that keeps the number of the objects below or equal maxObjects
 *
 * @param <K>
 * @param <V>
 */
public class LRUMap<K, V> extends AbstractLRUMap<K, V>
{
    /** if the max is less than 0, there is no limit! */
    private int maxObjects = -1;

    public LRUMap()
    {
        super();
    }

    /**
     *
     * @param maxObjects
     *            maximum number to keep in the map
     */
    public LRUMap(int maxObjects)
    {
        this();
        this.maxObjects = maxObjects;
    }

    @Override
    public boolean shouldRemove()
    {
        return maxObjects > 0 && this.size() > maxObjects;
    }
}
