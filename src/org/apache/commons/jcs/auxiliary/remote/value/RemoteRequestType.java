package org.apache.commons.jcs.auxiliary.remote.value;

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
 * The different types of requests
 */
public enum RemoteRequestType
{
    /** Alive check request type. */
    ALIVE_CHECK,

    /** Get request type. */
    GET,

    /** Get Multiple request type. */
    GET_MULTIPLE,

    /** Get Matching request type. */
    GET_MATCHING,

    /** Update request type. */
    UPDATE,

    /** Remove request type. */
    REMOVE,

    /** Remove All request type. */
    REMOVE_ALL,

    /** Get keys request type. */
    GET_KEYSET,

    /** Dispose request type. */
    DISPOSE,
}
