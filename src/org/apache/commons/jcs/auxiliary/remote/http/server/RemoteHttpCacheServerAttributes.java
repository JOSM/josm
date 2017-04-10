package org.apache.commons.jcs.auxiliary.remote.http.server;

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

/**
 * Configuration for the RemoteHttpCacheServer. Most of these properties are used only by the
 * service.
 */
public class RemoteHttpCacheServerAttributes
    extends AbstractAuxiliaryCacheAttributes
{
    /** Don't change. */
    private static final long serialVersionUID = -3987239306108780496L;

    /** Can a cluster remote put to other remotes */
    private boolean localClusterConsistency = true;

    /** Can a cluster remote get from other remotes */
    private boolean allowClusterGet = true;

    /**
     * Should cluster updates be propagated to the locals
     * <p>
     * @return The localClusterConsistency value
     */
    public boolean isLocalClusterConsistency()
    {
        return localClusterConsistency;
    }

    /**
     * Should cluster updates be propagated to the locals
     * <p>
     * @param r The new localClusterConsistency value
     */
    public void setLocalClusterConsistency( boolean r )
    {
        this.localClusterConsistency = r;
    }

    /**
     * Should gets from non-cluster clients be allowed to get from other remote auxiliaries.
     * <p>
     * @return The localClusterConsistency value
     */
    public boolean isAllowClusterGet()
    {
        return allowClusterGet;
    }

    /**
     * Should we try to get from other cluster servers if we don't find the items locally.
     * <p>
     * @param r The new localClusterConsistency value
     */
    public void setAllowClusterGet( boolean r )
    {
        allowClusterGet = r;
    }

    /**
     * @return String details
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nRemoteHttpCacheServiceAttributes" );
        buf.append( "\n cacheName = [" + this.getCacheName() + "]" );
        buf.append( "\n allowClusterGet = [" + this.isAllowClusterGet() + "]" );
        buf.append( "\n localClusterConsistency = [" + this.isLocalClusterConsistency() + "]" );
        buf.append( "\n eventQueueType = [" + this.getEventQueueType() + "]" );
        buf.append( "\n eventQueuePoolName = [" + this.getEventQueuePoolName() + "]" );
        return buf.toString();
    }
}
