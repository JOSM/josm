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
 * InvalidGroupException
 */
public class InvalidGroupException
    extends CacheException
{
    /** Don't change. */
    private static final long serialVersionUID = -5219807114008843480L;

    /** Constructor for the InvalidGroupException object */
    public InvalidGroupException()
    {
        super();
    }

    /**
     * Constructor for the InvalidGroupException object
     * <p>
     * @param message
     */
    public InvalidGroupException( String message )
    {
        super( message );
    }

}
