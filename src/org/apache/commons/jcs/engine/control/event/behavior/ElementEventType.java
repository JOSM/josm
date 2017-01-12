package org.apache.commons.jcs.engine.control.event.behavior;

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
 * This describes the events that an item can encounter.
 */
public enum ElementEventType
{
    /** Background expiration */
    EXCEEDED_MAXLIFE_BACKGROUND,

    /*** Expiration discovered on request */
    EXCEEDED_MAXLIFE_ONREQUEST,

    /** Background expiration */
    EXCEEDED_IDLETIME_BACKGROUND,

    /** Expiration discovered on request */
    EXCEEDED_IDLETIME_ONREQUEST,

    /** Moving from memory to disk (what if no disk?) */
    SPOOLED_DISK_AVAILABLE,

    /** Moving from memory to disk (what if no disk?) */
    SPOOLED_DISK_NOT_AVAILABLE,

    /** Moving from memory to disk, but item is not spoolable */
    SPOOLED_NOT_ALLOWED //,

    /** Removed actively by a remove command. (Could distinguish between local and remote) */
    //REMOVED,
    /**
     * Element was requested from cache. Not sure we ever want to implement this.
     */
    //GET
}
