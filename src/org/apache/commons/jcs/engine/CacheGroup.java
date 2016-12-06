package org.apache.commons.jcs.engine;

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

import org.apache.commons.jcs.engine.behavior.IElementAttributes;

/**
 * Holder for attributes specific to a group. The grouping functionality is on
 * the way out.
 */
public class CacheGroup
{
    /** Element configuration. */
    private IElementAttributes attr;

    /** Constructor for the CacheGroup object */
    public CacheGroup()
    {
        super();
    }

    /**
     * Sets the attributes attribute of the CacheGroup object
     * <p>
     * @param attr
     *            The new attributes value
     */
    public void setElementAttributes( IElementAttributes attr )
    {
        this.attr = attr;
    }

    /**
     * Gets the attrributes attribute of the CacheGroup object
     * <p>
     * @return The attrributes value
     */
    public IElementAttributes getElementAttrributes()
    {
        return attr;
    }
}
