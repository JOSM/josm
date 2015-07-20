package org.apache.commons.jcs.auxiliary.remote.http.client.behavior;

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

import org.apache.commons.jcs.auxiliary.remote.http.client.RemoteHttpCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICacheServiceNonLocal;

import java.io.IOException;


/**
 * It's not entirely clear that this interface is needed. I simply wanted the initialization method.
 * This could be added to the ICacheSerice method.
 */
public interface IRemoteHttpCacheClient<K, V>
    extends ICacheServiceNonLocal<K, V>
{
    /**
     * The provides an extension point. If you want to extend this and use a special dispatcher,
     * here is the place to do it.
     * <p>
     * @param attributes
     */
    void initialize( RemoteHttpCacheAttributes attributes );

    /**
     * Make and alive request.
     * <p>
     * @return true if we make a successful alive request.
     * @throws IOException
     */
    boolean isAlive()
        throws IOException;
}
