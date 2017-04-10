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

import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.behavior.ICacheListener;

import java.io.IOException;
import java.rmi.Remote;

/**
 * Listens for remote cache event notification ( rmi callback ).
 */
public interface IRemoteCacheListener<K, V>
    extends ICacheListener<K, V>, Remote
{
    /**
     * Get the id to be used by this manager.
     * <p>
     * @return long
     * @throws IOException
     */
    @Override
    long getListenerId()
        throws IOException;

    /**
     * Set the id to be used by this manager. The remote cache server identifies clients by this id.
     * The value will be set by the server through the remote cache listener.
     * <p>
     * @param id
     * @throws IOException
     */
    @Override
    void setListenerId( long id )
        throws IOException;

    /**
     * Gets the remoteType attribute of the IRemoteCacheListener object
     * <p>
     * @return The remoteType value
     * @throws IOException
     */
    RemoteType getRemoteType()
        throws IOException;

    /**
     * This is for debugging. It allows the remote cache server to log the address of any listeners
     * that register.
     * <p>
     * @return the local host address.
     * @throws IOException
     */
    String getLocalHostAddress()
        throws IOException;

    /**
     * Deregisters itself.
     * <p>
     * @throws IOException
     */
    void dispose()
        throws IOException;
}
