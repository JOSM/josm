package org.apache.commons.jcs.auxiliary.disk.block;

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

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates disk cache instances.
 */
public class BlockDiskCacheFactory
    extends AbstractAuxiliaryCacheFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( BlockDiskCacheFactory.class );

    /**
     * Create an instance of the BlockDiskCache.
     * <p>
     * @param iaca the cache attributes for this cache
     * @param cacheMgr This allows auxiliaries to reference the manager without assuming that it is
     *            a singleton. This will allow JCS to be a non-singleton. Also, it makes it easier
     *            to test.
     * @param cacheEventLogger
     * @param elementSerializer
     * @return BlockDiskCache
     */
    @Override
    public <K, V> BlockDiskCache<K, V> createCache( AuxiliaryCacheAttributes iaca, ICompositeCacheManager cacheMgr,
                                       ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        BlockDiskCacheAttributes idca = (BlockDiskCacheAttributes) iaca;
        if ( log.isDebugEnabled() )
        {
            log.debug( "Creating DiskCache for attributes = " + idca );
        }

        BlockDiskCache<K, V> cache = new BlockDiskCache<K, V>( idca );
        cache.setCacheEventLogger( cacheEventLogger );
        cache.setElementSerializer( elementSerializer );

        return cache;
    }
}
