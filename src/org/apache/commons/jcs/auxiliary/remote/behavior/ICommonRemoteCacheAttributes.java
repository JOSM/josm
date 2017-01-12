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

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.RemoteLocation;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;

/**
 * This specifies what a remote cache configuration object should look like.
 */
public interface ICommonRemoteCacheAttributes
    extends AuxiliaryCacheAttributes
{
    /** The default timeout for the custom RMI socket factory */
    int DEFAULT_RMI_SOCKET_FACTORY_TIMEOUT_MILLIS = 10000;

    /**
     * Gets the remoteTypeName attribute of the IRemoteCacheAttributes object
     * <p>
     * @return The remoteTypeName value
     */
    String getRemoteTypeName();

    /**
     * Sets the remoteTypeName attribute of the IRemoteCacheAttributes object
     * <p>
     * @param s The new remoteTypeName value
     */
    void setRemoteTypeName( String s );

    /**
     * Gets the remoteType attribute of the IRemoteCacheAttributes object
     * <p>
     * @return The remoteType value
     */
    RemoteType getRemoteType();

    /**
     * Sets the remoteType attribute of the IRemoteCacheAttributes object
     * <p>
     * @param p The new remoteType value
     */
    void setRemoteType( RemoteType p );

    /**
     * Gets the remoteServiceName attribute of the IRemoteCacheAttributes object
     * <p>
     * @return The remoteServiceName value
     */
    String getRemoteServiceName();

    /**
     * Sets the remoteServiceName attribute of the IRemoteCacheAttributes object
     * <p>
     * @param s The new remoteServiceName value
     */
    void setRemoteServiceName( String s );

    /**
     * Sets the location attribute of the RemoteCacheAttributes object.
     * <p>
     * @param location The new location value
     */
    void setRemoteLocation( RemoteLocation location );

    /**
     * Sets the location attribute of the RemoteCacheAttributes object.
     * <p>
     * @param host The new remoteHost value
     * @param port The new remotePort value
     */
    void setRemoteLocation( String host, int port );

    /**
     * Gets the location attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The remote location value
     */
    public RemoteLocation getRemoteLocation();

    /**
     * Gets the clusterServers attribute of the IRemoteCacheAttributes object
     * <p>
     * @return The clusterServers value
     */
    String getClusterServers();

    /**
     * Sets the clusterServers attribute of the IRemoteCacheAttributes object
     * <p>
     * @param s The new clusterServers value
     */
    void setClusterServers( String s );

    /**
     * Gets the removeUponRemotePut attribute of the IRemoteCacheAttributes object
     * <p>
     * @return The removeUponRemotePut value
     */
    boolean getRemoveUponRemotePut();

    /**
     * Sets the removeUponRemotePut attribute of the IRemoteCacheAttributes object
     * <p>
     * @param r The new removeUponRemotePut value
     */
    void setRemoveUponRemotePut( boolean r );

    /**
     * Gets the getOnly attribute of the IRemoteCacheAttributes object
     * <p>
     * @return The getOnly value
     */
    boolean getGetOnly();

    /**
     * Sets the getOnly attribute of the IRemoteCacheAttributes object
     * <p>
     * @param r The new getOnly value
     */
    void setGetOnly( boolean r );

    /**
     * Should cluster updates be propagated to the locals
     * <p>
     * @return The localClusterConsistency value
     */
    boolean isLocalClusterConsistency();

    /**
     * Should cluster updates be propagated to the locals
     * <p>
     * @param r The new localClusterConsistency value
     */
    void setLocalClusterConsistency( boolean r );

    /**
     * This sets a general timeout on the rmi socket factory. By default the socket factory will
     * block forever.
     * <p>
     * We have a default setting. The default rmi behavior should never be used.
     * <p>
     * @return int milliseconds
     */
    int getRmiSocketFactoryTimeoutMillis();

    /**
     * This sets a general timeout on the RMI socket factory. By default the socket factory will
     * block forever.
     * <p>
     * @param rmiSocketFactoryTimeoutMillis
     */
    void setRmiSocketFactoryTimeoutMillis( int rmiSocketFactoryTimeoutMillis );
}
