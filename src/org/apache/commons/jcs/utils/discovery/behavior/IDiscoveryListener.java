package org.apache.commons.jcs.utils.discovery.behavior;

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

import org.apache.commons.jcs.utils.discovery.DiscoveredService;

/**
 * Interface for things that want to listen to discovery events. This will allow discovery to be
 * used outside of the TCP lateral.
 */
public interface IDiscoveryListener
{
    /**
     * Add the service if needed. This does not necessarily mean that the service is not already
     * added. This can be called if there is a change in service information, such as the cacheNames.
     * <p>
     * @param service the service to add
     */
    void addDiscoveredService( DiscoveredService service );

    /**
     * Remove the service from the list.
     * <p>
     * @param service the service to remove
     */
    void removeDiscoveredService( DiscoveredService service );
}
