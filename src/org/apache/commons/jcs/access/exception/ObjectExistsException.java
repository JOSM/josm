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

/**
 * The putSafe method on the JCS convenience class throws this exception if the object is already
 * present in the cache.
 * <p>
 * I'm removing this exception from normal use.
 * <p>
 * The overhead of throwing exceptions and the cumbersomeness of coding around exceptions warrants
 * removal. Exceptions like this don't make sense to throw in the course of normal operations to
 * signify a normal and expected condition. Returning null if an object isn't found is sufficient.
 */
public class ObjectExistsException
    extends CacheException
{
    /** Don't change. */
    private static final long serialVersionUID = -3779745827993383872L;

    /** Constructor for the ObjectExistsException object */
    public ObjectExistsException()
    {
        super();
    }

    /**
     * Constructor for the ObjectExistsException object
     * @param message
     */
    public ObjectExistsException( String message )
    {
        super( message );
    }

}
