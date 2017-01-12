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

import java.io.Serializable;

/**
 * Defines how an element event object should behave.
 */
public interface IElementEvent<T>
    extends Serializable
{
    /**
     * Gets the elementEvent attribute of the IElementEvent object. This code is Contained in the
     * IElememtEventConstants class.
     *<p>
     * @return The elementEvent value
     */
    ElementEventType getElementEvent();

    /**
     * @return the source of the event.
     */
    T getSource();
}
