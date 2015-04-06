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

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;

/** Common disk cache methods and properties. */
public abstract class AbstractDiskCacheManager
    implements AuxiliaryCacheManager
{
    /** The event logger. */
    private ICacheEventLogger cacheEventLogger;

    /** The serializer. */
    private IElementSerializer elementSerializer;

    /**
     * @param cacheEventLogger the cacheEventLogger to set
     */
    public void setCacheEventLogger( ICacheEventLogger cacheEventLogger )
    {
        this.cacheEventLogger = cacheEventLogger;
    }

    /**
     * @return the cacheEventLogger
     */
    public ICacheEventLogger getCacheEventLogger()
    {
        return cacheEventLogger;
    }

    /**
     * @param elementSerializer the elementSerializer to set
     */
    public void setElementSerializer( IElementSerializer elementSerializer )
    {
        this.elementSerializer = elementSerializer;
    }

    /**
     * @return the elementSerializer
     */
    public IElementSerializer getElementSerializer()
    {
        return elementSerializer;
    }
}
