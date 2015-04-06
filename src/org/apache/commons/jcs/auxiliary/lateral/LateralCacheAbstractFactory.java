package org.apache.commons.jcs.auxiliary.lateral;

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
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheListener;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;


/**
 * Particular lateral caches should define their own factory.  It is
 * not necessary to extend this base factory, but it can be useful.
 * <p>
 * The old factory tried to handle all types of laterals.  It was
 * gettting cluttered by ad hoc if statements.  Since the javagroups
 * lateral was jdk1.4 dependent it had to be moved.  As such, the
 * old factory could no longer import it.  This motivated the change.
 * <p>
 * This abstraction layer should keep things cleaner.
 * <p>
 * @author Aaron Smuts
 */
public abstract class LateralCacheAbstractFactory
	implements AuxiliaryCacheFactory
{
    /** The auxiliary name */
    private String name;

    /**
     * Creates a lateral cache.
     * <p>
     * @param attr
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return AuxiliaryCache
     */
    @Override
    public abstract <K, V> AuxiliaryCache<K, V> createCache(
            AuxiliaryCacheAttributes attr, ICompositeCacheManager cacheMgr,
            ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer );

    /**
     * Makes sure a listener gets created. It will get monitored as soon as it
     * is used.
     * <p>
     * This should be called by create cache.
     * <p>
     * @param lac  ILateralCacheAttributes
     * @param cacheMgr
     *
     * @return the listener if created, else null
     */
    public abstract <K, V>
        ILateralCacheListener<K, V> createListener( ILateralCacheAttributes lac, ICompositeCacheManager cacheMgr );

    /**
     * Gets the name attribute of the LateralCacheFactory object
     * <p>
     * @return The name value
     */
    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * Sets the name attribute of the LateralCacheFactory object
     * <p>
     * @param name
     *            The new name value
     */
    @Override
    public void setName( String name )
    {
        this.name = name;
    }
}
