package org.apache.commons.jcs.access.exception;

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

/** Thrown if there is some severe configuration problem that makes the cache nonfunctional. */
public class ConfigurationException
    extends CacheException
{
    /** Don't change. */
    private static final long serialVersionUID = 6881044536186097055L;

    /** Constructor for the ConfigurationException object */
    public ConfigurationException()
    {
        super();
    }

    /**
     * Constructor for the ConfigurationException object.
     * <p>
     * @param message
     */
    public ConfigurationException( String message )
    {
        super( message );
    }
}
