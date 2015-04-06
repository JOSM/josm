package org.apache.commons.jcs.auxiliary.disk.jdbc;

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

import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheFactory;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;

/**
 * This factory should create mysql disk caches.
 * <p>
 * @author Aaron Smuts
 */
public class JDBCDiskCacheFactory
    implements AuxiliaryCacheFactory
{
    /** Name of the factory */
    private String name = "JDBCDiskCacheFactory";

    /**
     * This factory method should create an instance of the mysqlcache.
     * <p>
     * @param rawAttr
     * @param compositeCacheManager
     * @param cacheEventLogger
     * @param elementSerializer
     * @return AuxiliaryCache
     */
    @Override
    public <K, V> AuxiliaryCache<K, V> createCache( AuxiliaryCacheAttributes rawAttr, ICompositeCacheManager compositeCacheManager,
                                       ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        JDBCDiskCacheManager diskCacheManager = JDBCDiskCacheManager.getInstance( (JDBCDiskCacheAttributes) rawAttr,
                                                                                  compositeCacheManager,
                                                                                  cacheEventLogger, elementSerializer );
        return diskCacheManager.getCache( (JDBCDiskCacheAttributes) rawAttr );
    }

    /**
     * The name of the factory.
     * <p>
     * @param nameArg
     */
    @Override
    public void setName( String nameArg )
    {
        name = nameArg;
    }

    /**
     * Returns the display name
     * <p>
     * @return String
     */
    @Override
    public String getName()
    {
        return name;
    }
}
