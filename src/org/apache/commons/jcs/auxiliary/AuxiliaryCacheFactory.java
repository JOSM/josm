package org.apache.commons.jcs.auxiliary;

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

import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;

/**
 * All auxiliary caches must have a factory that the cache configurator can use to create instances.
 */
public interface AuxiliaryCacheFactory
{
    /**
     * Creates an auxiliary using the supplied attributes. Adds it to the composite cache manager.
     * 
     * @param attr
     * @param cacheMgr This allows auxiliaries to reference the manager without assuming that it is
     *            a singleton. This will allow JCS to be a non-singleton. Also, it makes it easier to
     *            test.
     * @param cacheEventLogger
     * @param elementSerializer
     * @return AuxiliaryCache
     * @throws Exception if cache instance could not be created
     */
    <K, V> AuxiliaryCache<K, V> createCache(
            AuxiliaryCacheAttributes attr, ICompositeCacheManager cacheMgr,
            ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
            throws Exception;

    /**
     * Initialize this factory
     */
    void initialize();

    /**
     * Dispose of this factory, clean up shared resources
     */
    void dispose();

    /**
     * Sets the name attribute of the AuxiliaryCacheFactory object
     * 
     * @param s The new name value
     */
    void setName( String s );

    /**
     * Gets the name attribute of the AuxiliaryCacheFactory object
     * 
     * @return The name value
     */
    String getName();
}
