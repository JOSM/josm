package org.apache.commons.jcs.auxiliary.lateral.behavior;

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

import org.apache.commons.jcs.engine.behavior.ICacheListener;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;

/**
 * Listens for lateral cache event notification.
 */
public interface ILateralCacheListener<K, V>
    extends ICacheListener<K, V>
{
    /**
     * Initialize this listener
     */
    void init();

    /**
     * @param cacheMgr
     *            The cacheMgr to set.
     */
    void setCacheManager( ICompositeCacheManager cacheMgr );

    /**
     * @return Returns the cacheMgr.
     */
    ICompositeCacheManager getCacheManager();

    /**
     * Dispose this listener
     */
    void dispose();
}
