package org.apache.commons.jcs.engine.logging.behavior;

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
 * This defines the behavior for event logging. Auxiliaries will send events to injected event
 * loggers.
 * <p>
 * In general all ICache interface methods should call the logger if one is configured. This will be
 * done on an ad hoc basis for now. Various auxiliaries may have additional events.
 */
public interface ICacheEventLogger
{
	// TODO: Use enum
    /** ICache update */
    String UPDATE_EVENT = "update";

    /** ICache get */
    String GET_EVENT = "get";

    /** ICache getMultiple */
    String GETMULTIPLE_EVENT = "getMultiple";

    /** ICache getMatching */
    String GETMATCHING_EVENT = "getMatching";

    /** ICache remove */
    String REMOVE_EVENT = "remove";

    /** ICache removeAll */
    String REMOVEALL_EVENT = "removeAll";

    /** ICache dispose */
    String DISPOSE_EVENT = "dispose";

    /** ICache enqueue. The time in the queue. */
    //String ENQUEUE_EVENT = "enqueue";
    /**
     * Creates an event.
     * <p>
     * @param source - e.g. RemoteCacheServer
     * @param region - the name of the region
     * @param eventName - e.g. update, get, put, remove
     * @param optionalDetails - any extra message
     * @param key - the cache key
     * @return ICacheEvent
     */
    <T> ICacheEvent<T> createICacheEvent( String source, String region,
            String eventName, String optionalDetails, T key );

    /**
     * Logs an event.
     * <p>
     * @param event - the event created in createICacheEvent
     */
    <T> void logICacheEvent( ICacheEvent<T> event );

    /**
     * Logs an event. These are internal application events that do not correspond to ICache calls.
     * <p>
     * @param source - e.g. RemoteCacheServer
     * @param eventName - e.g. update, get, put, remove
     * @param optionalDetails - any extra message
     */
    void logApplicationEvent( String source, String eventName, String optionalDetails );

    /**
     * Logs an error.
     * <p>
     * @param source - e.g. RemoteCacheServer
     * @param eventName - e.g. update, get, put, remove
     * @param errorMessage - any error message
     */
    void logError( String source, String eventName, String errorMessage );
}
