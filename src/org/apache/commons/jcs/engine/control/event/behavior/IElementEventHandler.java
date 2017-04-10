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
 * This interface defines the behavior for event handler. Event handlers are
 * transient. They are not replicated and are not written to disk.
 * <p>
 * If you want an event handler by default for all elements in a region, then
 * you can add it to the default element attributes. This way it will get created
 * whenever an item gets put into the cache.
 *
 */
public interface IElementEventHandler
{
    /**
     * Handle events for this element. The events are typed.
     *
     * @param event
     *            The event created by the cache.
     */
    <T> void handleElementEvent( IElementEvent<T> event );
}
