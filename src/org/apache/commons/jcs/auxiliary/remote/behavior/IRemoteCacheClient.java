package org.apache.commons.jcs.auxiliary.remote.behavior;

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
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;

/**
 * This defines the behavior expected of a remote cache client. This extends Auxiliary cache which
 * in turn extends ICache.
 * <p>
 * I'd like generalize this a bit.
 * <p>
 * @author Aaron Smuts
 */
public interface IRemoteCacheClient<K, V>
    extends AuxiliaryCache<K, V>
{
    /**
     * Replaces the current remote cache service handle with the given handle. If the current remote
     * is a Zombie, the propagate the events that may be queued to the restored service.
     * <p>
     * @param remote ICacheServiceNonLocal -- the remote server or proxy to the remote server
     */
    void fixCache( ICacheServiceNonLocal<?, ?> remote );

    /**
     * Gets the listenerId attribute of the RemoteCacheListener object.
     * <p>
     * All requests to the remote cache must include a listener id. This allows the server to avoid
     * sending updates the the listener associated with this client.
     * <p>
     * @return The listenerId value
     */
    long getListenerId();

    /**
     * This returns the listener associated with this remote cache. TODO we should try to get this
     * out of the interface.
     * <p>
     * @return IRemoteCacheListener
     */
    IRemoteCacheListener<K, V> getListener();
}
