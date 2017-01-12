package org.apache.commons.jcs.auxiliary.remote.server.behavior;

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

import org.apache.commons.jcs.auxiliary.remote.behavior.ICommonRemoteCacheAttributes;

/**
 * This defines the minimal behavior for the objects that are used to configure
 * the remote cache server.
 */
public interface IRemoteCacheServerAttributes
    extends ICommonRemoteCacheAttributes
{
    /**
     * Gets the localPort attribute of the IRemoteCacheAttributes object.
     * <p>
     * @return The localPort value
     */
    int getServicePort();

    /**
     * Sets the localPort attribute of the IRemoteCacheAttributes object.
     * <p>
     * @param p
     *            The new localPort value
     */
    void setServicePort( int p );

    /**
     * Should we try to get remotely when the request does not come in from a
     * cluster. If local L1 asks remote server R1 for element A and R1 doesn't
     * have it, should R1 look remotely? The difference is between a local and a
     * remote update. The local update stays local. Normal updates, removes,
     * etc, stay local when they come from a client. If this is set to true,
     * then they can go remote.
     * <p>
     * @return The localClusterConsistency value
     */
    boolean isAllowClusterGet();

    /**
     * Should cluster updates be propagated to the locals.
     * <p>
     * @param r
     *            The new localClusterConsistency value
     */
    void setAllowClusterGet( boolean r );

    /**
     * Gets the ConfigFileName attribute of the IRemoteCacheAttributes object.
     * <p>
     * @return The clusterServers value
     */
    String getConfigFileName();

    /**
     * Sets the ConfigFileName attribute of the IRemoteCacheAttributes object.
     * <p>
     * @param s
     *            The new clusterServers value
     */
    void setConfigFileName( String s );

    /**
     * Should we try to keep the registry alive
     * <p>
     * @param useRegistryKeepAlive the useRegistryKeepAlive to set
     */
    void setUseRegistryKeepAlive( boolean useRegistryKeepAlive );

    /**
     * Should we start the registry
     * <p>
     * @param startRegistry the startRegistry to set
     */
    void setStartRegistry( boolean startRegistry );

    /**
     * Should we start the registry
     * <p>
     * @return the startRegistry
     */
    boolean isStartRegistry();

    /**
     * Should we try to keep the registry alive
     * <p>
     * @return the useRegistryKeepAlive
     */
    boolean isUseRegistryKeepAlive();

    /**
     * @param registryKeepAliveDelayMillis the registryKeepAliveDelayMillis to set
     */
    void setRegistryKeepAliveDelayMillis( long registryKeepAliveDelayMillis );

    /**
     * @return the registryKeepAliveDelayMillis
     */
    long getRegistryKeepAliveDelayMillis();
}
