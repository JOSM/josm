package org.apache.commons.jcs.auxiliary.lateral;

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
 * Enumeration of the available lateral commands
 */
public enum LateralCommand
{
    /** The command for updates */
    UPDATE,

    /** The command for removes */
    REMOVE,

    /** The command instructing us to remove all */
    REMOVEALL,

    /** The command for disposing the cache. */
    DISPOSE,

    /** Command to return an object. */
    GET,

    /** Command to return an object. */
    GET_MATCHING,

    /** Command to get all keys */
    GET_KEYSET
}
