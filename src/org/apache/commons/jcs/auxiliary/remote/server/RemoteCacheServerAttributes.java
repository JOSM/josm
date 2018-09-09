package org.apache.commons.jcs.auxiliary.remote.server;

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

import org.apache.commons.jcs.auxiliary.remote.CommonRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.IRemoteCacheServerAttributes;

/**
 * These attributes are used to configure the remote cache server.
 */
public class RemoteCacheServerAttributes
    extends CommonRemoteCacheAttributes
    implements IRemoteCacheServerAttributes
{
    /** Don't change */
    private static final long serialVersionUID = -2741662082869155365L;

    /** port the server will listen to */
    private int servicePort = 0;

    /** Can a cluster remote get from other remotes */
    private boolean allowClusterGet = true;

    /** The config file, the initialization is multistage. Remote cache then composite cache. */
    private String configFileName = "";

    /** Should we start the registry */
    private boolean DEFAULT_START_REGISTRY = true;

    /** Should we start the registry */
    private boolean startRegistry = DEFAULT_START_REGISTRY;

    /** Should we try to keep the registry alive */
    private boolean DEFAULT_USE_REGISTRY_KEEP_ALIVE = true;

    /** Should we try to keep the registry alive */
    private boolean useRegistryKeepAlive = DEFAULT_USE_REGISTRY_KEEP_ALIVE;

    /** The delay between runs */
    private long registryKeepAliveDelayMillis = 15 * 1000;

    /** Default constructor for the RemoteCacheAttributes object */
    public RemoteCacheServerAttributes()
    {
        super();
    }

    /**
     * Gets the localPort attribute of the RemoteCacheAttributes object
     * <p>
     * @return The localPort value
     */
    @Override
    public int getServicePort()
    {
        return this.servicePort;
    }

    /**
     * Sets the localPort attribute of the RemoteCacheAttributes object
     * <p>
     * @param p The new localPort value
     */
    @Override
    public void setServicePort( int p )
    {
        this.servicePort = p;
    }

    /**
     * Should gets from non-cluster clients be allowed to get from other remote auxiliaries.
     * <p>
     * @return The localClusterConsistency value
     */
    @Override
    public boolean isAllowClusterGet()
    {
        return allowClusterGet;
    }

    /**
     * Should we try to get from other cluster servers if we don't find the items locally.
     * <p>
     * @param r The new localClusterConsistency value
     */
    @Override
    public void setAllowClusterGet( boolean r )
    {
        allowClusterGet = r;
    }

    /**
     * Gets the ConfigFileName attribute of the IRemoteCacheAttributes object
     * <p>
     * @return The clusterServers value
     */
    @Override
    public String getConfigFileName()
    {
        return configFileName;
    }

    /**
     * Sets the ConfigFileName attribute of the IRemoteCacheAttributes object
     * <p>
     * @param s The new clusterServers value
     */
    @Override
    public void setConfigFileName( String s )
    {
        configFileName = s;
    }

    /**
     * Should we try to keep the registry alive
     * <p>
     * @param useRegistryKeepAlive the useRegistryKeepAlive to set
     */
    @Override
    public void setUseRegistryKeepAlive( boolean useRegistryKeepAlive )
    {
        this.useRegistryKeepAlive = useRegistryKeepAlive;
    }

    /**
     * Should we start the registry
     * <p>
     * @param startRegistry the startRegistry to set
     * @deprecated Always true, to be removed
     */
    @Override
    public void setStartRegistry( boolean startRegistry )
    {
        this.startRegistry = startRegistry;
    }

    /**
     * Should we start the registry
     * <p>
     * @return the startRegistry
     * @deprecated Always true, to be removed
     */
    @Override
    public boolean isStartRegistry()
    {
        return startRegistry;
    }

    /**
     * Should we try to keep the registry alive
     * <p>
     * @return the useRegistryKeepAlive
     */
    @Override
    public boolean isUseRegistryKeepAlive()
    {
        return useRegistryKeepAlive;
    }

    /**
     * @param registryKeepAliveDelayMillis the registryKeepAliveDelayMillis to set
     */
    @Override
    public void setRegistryKeepAliveDelayMillis( long registryKeepAliveDelayMillis )
    {
        this.registryKeepAliveDelayMillis = registryKeepAliveDelayMillis;
    }

    /**
     * @return the registryKeepAliveDelayMillis
     */
    @Override
    public long getRegistryKeepAliveDelayMillis()
    {
        return registryKeepAliveDelayMillis;
    }

    /**
     * @return String details
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.append( "\n servicePort = [" + this.getServicePort() + "]" );
        buf.append( "\n allowClusterGet = [" + this.isAllowClusterGet() + "]" );
        buf.append( "\n configFileName = [" + this.getConfigFileName() + "]" );
        buf.append( "\n rmiSocketFactoryTimeoutMillis = [" + this.getRmiSocketFactoryTimeoutMillis() + "]" );
        buf.append( "\n useRegistryKeepAlive = [" + this.isUseRegistryKeepAlive() + "]" );
        buf.append( "\n registryKeepAliveDelayMillis = [" + this.getRegistryKeepAliveDelayMillis() + "]" );
        buf.append( "\n eventQueueType = [" + this.getEventQueueType() + "]" );
        buf.append( "\n eventQueuePoolName = [" + this.getEventQueuePoolName() + "]" );
        return buf.toString();
    }
}
