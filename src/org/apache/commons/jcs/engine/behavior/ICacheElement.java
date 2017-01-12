package org.apache.commons.jcs.engine.behavior;

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

/**
 * Every item is the cache is wrapped in an ICacheElement. This contains
 * information about the element: the region name, the key, the value, and the
 * element attributes.
 * <p>
 * The element attributes have lots of useful information about each element,
 * such as when they were created, how long they have to live, and if they are
 * allowed to be spooled, etc.
 *
 */
public interface ICacheElement<K, V>
    extends Serializable
{

    /**
     * Gets the cacheName attribute of the ICacheElement&lt;K, V&gt; object. The cacheName
     * is also known as the region name.
     *
     * @return The cacheName value
     */
    String getCacheName();

    /**
     * Gets the key attribute of the ICacheElement&lt;K, V&gt; object
     *
     * @return The key value
     */
    K getKey();

    /**
     * Gets the val attribute of the ICacheElement&lt;K, V&gt; object
     *
     * @return The val value
     */
    V getVal();

    /**
     * Gets the attributes attribute of the ICacheElement&lt;K, V&gt; object
     *
     * @return The attributes value
     */
    IElementAttributes getElementAttributes();

    /**
     * Sets the attributes attribute of the ICacheElement&lt;K, V&gt; object
     *
     * @param attr
     *            The new attributes value
     */
    void setElementAttributes( IElementAttributes attr );
}
