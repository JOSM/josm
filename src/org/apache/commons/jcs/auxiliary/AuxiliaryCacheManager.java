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

/**
 * AuxiliaryCacheManager
 */
public interface AuxiliaryCacheManager
{
    /**
     * Return the appropriate auxiliary cache for this region.
     * <p>
     * @param cacheName
     * @return AuxiliaryCache
     */
    <K, V> AuxiliaryCache<K, V> getCache( String cacheName );

    /**
     * This allows the cache manager to be plugged into the auxiliary caches,
     * rather then having them get it themselves. Cache managers can be mocked
     * out and the auxiliaries will be easier to test.
     * <p>
     * @param cacheName
     * @param cacheManager
     * @return AuxiliaryCache
     */
    //AuxiliaryCache getCache( String cacheName, ICompositeCacheManager
    // cacheManager );
}
