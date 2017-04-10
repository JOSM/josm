package org.apache.commons.jcs.engine;

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

import java.rmi.dgc.VMID;

/**
 * This is a static variable holder for the distribution auxiliaries that need something like a vmid.
 */
public final class CacheInfo
{
    /** shouldn't be instantiated */
    private CacheInfo()
    {
        super();
    }

    /**
     * Used to identify a client, so we can run multiple clients off one host.
     * Need since there is no way to identify a client other than by host in
     * rmi.
     * <p>
     * TODO: may have some trouble in failover mode if the cache keeps its old
     * id. We may need to reset this when moving into failover.
     */
    private static final VMID vmid = new VMID();

    /** By default this is the hashcode of the VMID */
    public static final long listenerId = vmid.hashCode();
}
