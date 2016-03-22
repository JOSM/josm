package org.apache.commons.jcs.auxiliary.remote.http.client;

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

import org.apache.commons.jcs.auxiliary.remote.AbstractRemoteCacheListener;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;

/** Does nothing */
public class RemoteHttpClientListener<K, V>
    extends AbstractRemoteCacheListener<K, V>
{
    /**
     * Only need one since it does work for all regions, just reference by multiple region names.
     * <p>
     * The constructor exports this object, making it available to receive incoming calls. The
     * callback port is anonymous unless a local port value was specified in the configuration.
     * <p>
     * @param irca cache configuration
     * @param cacheMgr the cache hub
     * @param elementSerializer a custom serializer
     */
    public RemoteHttpClientListener( IRemoteCacheAttributes irca, ICompositeCacheManager cacheMgr, IElementSerializer elementSerializer )
    {
        super( irca, cacheMgr, elementSerializer );
    }

    /** Nothing */
    @Override
    public void dispose()
    {
        // noop
    }
}
