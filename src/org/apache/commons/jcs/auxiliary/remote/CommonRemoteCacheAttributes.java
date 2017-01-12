package org.apache.commons.jcs.auxiliary.remote;

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

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.ICommonRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheConstants;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;

/**
 * Attributes common to remote cache client and server.
 */
public class CommonRemoteCacheAttributes
    extends AbstractAuxiliaryCacheAttributes
    implements ICommonRemoteCacheAttributes
{
    /** Don't change */
    private static final long serialVersionUID = -1555143736942374000L;

    /** The service name */
    private String remoteServiceName = IRemoteCacheConstants.REMOTE_CACHE_SERVICE_VAL;

    /** server host and port */
    private RemoteLocation location;

    /** Cluster chain */
    private String clusterServers = "";

    /** THe type of remote cache, local or cluster */
    private RemoteType remoteType = RemoteType.LOCAL;

    /** Should we issue a local remove if we get a put from a remote server */
    private boolean removeUponRemotePut = true;

    /** Can we receive from or put to the remote. this probably shouldn't be used. Use receive. */
    private boolean getOnly = false;

    /** Should we put and get from the clusters. */
    private boolean localClusterConsistency = false;

    /** read and connect timeout */
    private int rmiSocketFactoryTimeoutMillis = DEFAULT_RMI_SOCKET_FACTORY_TIMEOUT_MILLIS;

    /** Default constructor for the RemoteCacheAttributes object */
    public CommonRemoteCacheAttributes()
    {
        super();
    }

    /**
     * Gets the remoteTypeName attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The remoteTypeName value
     */
    @Override
    public String getRemoteTypeName()
    {
        return remoteType != null ? remoteType.toString() : RemoteType.LOCAL.toString();
    }

    /**
     * Sets the remoteTypeName attribute of the RemoteCacheAttributes object.
     * <p>
     * @param s The new remoteTypeName value
     */
    @Override
    public void setRemoteTypeName( String s )
    {
        RemoteType rt = RemoteType.valueOf(s);
        if (rt != null)
        {
            this.remoteType = rt;
        }
    }

    /**
     * Gets the remoteType attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The remoteType value
     */
    @Override
    public RemoteType getRemoteType()
    {
        return remoteType;
    }

    /**
     * Sets the remoteType attribute of the RemoteCacheAttributes object.
     * <p>
     * @param p The new remoteType value
     */
    @Override
    public void setRemoteType( RemoteType p )
    {
        this.remoteType = p;
    }

    /**
     * Gets the remoteServiceName attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The remoteServiceName value
     */
    @Override
    public String getRemoteServiceName()
    {
        return this.remoteServiceName;
    }

    /**
     * Sets the remoteServiceName attribute of the RemoteCacheAttributes object.
     * <p>
     * @param s The new remoteServiceName value
     */
    @Override
    public void setRemoteServiceName( String s )
    {
        this.remoteServiceName = s;
    }

    /**
     * Sets the location attribute of the RemoteCacheAttributes object.
     * <p>
     * @param location The new location value
     */
    @Override
    public void setRemoteLocation( RemoteLocation location )
    {
        this.location = location;
    }

    /**
     * Sets the location attribute of the RemoteCacheAttributes object.
     * <p>
     * @param host The new remoteHost value
     * @param port The new remotePort value
     */
    @Override
    public void setRemoteLocation( String host, int port )
    {
        this.location = new RemoteLocation(host, port);
    }

    /**
     * Gets the location attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The remote location value
     */
    @Override
    public RemoteLocation getRemoteLocation()
    {
        return this.location;
    }

    /**
     * Gets the clusterServers attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The clusterServers value
     */
    @Override
    public String getClusterServers()
    {
        return this.clusterServers;
    }

    /**
     * Sets the clusterServers attribute of the RemoteCacheAttributes object.
     * <p>
     * @param s The new clusterServers value
     */
    @Override
    public void setClusterServers( String s )
    {
        this.clusterServers = s;
    }

    /**
     * Gets the removeUponRemotePut attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The removeUponRemotePut value
     */
    @Override
    public boolean getRemoveUponRemotePut()
    {
        return this.removeUponRemotePut;
    }

    /**
     * Sets the removeUponRemotePut attribute of the RemoteCacheAttributes object.
     * <p>
     * @param r The new removeUponRemotePut value
     */
    @Override
    public void setRemoveUponRemotePut( boolean r )
    {
        this.removeUponRemotePut = r;
    }

    /**
     * Gets the getOnly attribute of the RemoteCacheAttributes object.
     * <p>
     * @return The getOnly value
     */
    @Override
    public boolean getGetOnly()
    {
        return this.getOnly;
    }

    /**
     * Sets the getOnly attribute of the RemoteCacheAttributes object
     * @param r The new getOnly value
     */
    @Override
    public void setGetOnly( boolean r )
    {
        this.getOnly = r;
    }

    /**
     * Should cluster updates be propagated to the locals.
     * <p>
     * @return The localClusterConsistency value
     */
    @Override
    public boolean isLocalClusterConsistency()
    {
        return localClusterConsistency;
    }

    /**
     * Should cluster updates be propagated to the locals.
     * <p>
     * @param r The new localClusterConsistency value
     */
    @Override
    public void setLocalClusterConsistency( boolean r )
    {
        this.localClusterConsistency = r;
    }

    /**
     * @param rmiSocketFactoryTimeoutMillis The rmiSocketFactoryTimeoutMillis to set.
     */
    @Override
    public void setRmiSocketFactoryTimeoutMillis( int rmiSocketFactoryTimeoutMillis )
    {
        this.rmiSocketFactoryTimeoutMillis = rmiSocketFactoryTimeoutMillis;
    }

    /**
     * @return Returns the rmiSocketFactoryTimeoutMillis.
     */
    @Override
    public int getRmiSocketFactoryTimeoutMillis()
    {
        return rmiSocketFactoryTimeoutMillis;
    }

    /**
     * @return String, all the important values that can be configured
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\n RemoteCacheAttributes " );
        if (this.location != null)
        {
            buf.append( "\n remoteHost = [" + this.location.getHost() + "]" );
            buf.append( "\n remotePort = [" + this.location.getPort() + "]" );
        }
        buf.append( "\n cacheName = [" + getCacheName() + "]" );
        buf.append( "\n remoteType = [" + remoteType + "]" );
        buf.append( "\n removeUponRemotePut = [" + this.removeUponRemotePut + "]" );
        buf.append( "\n getOnly = [" + getOnly + "]" );
        return buf.toString();
    }
}
