package org.apache.commons.jcs.engine.control.event;

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

import java.util.EventObject;

import org.apache.commons.jcs.engine.control.event.behavior.ElementEventType;
import org.apache.commons.jcs.engine.control.event.behavior.IElementEvent;

/**
 * Element events will trigger the creation of Element Event objects. This is a wrapper around the
 * cache element that indicates the event triggered.
 */
public class ElementEvent<T>
    extends EventObject
    implements IElementEvent<T>
{
    /** Don't change */
    private static final long serialVersionUID = -5364117411457467056L;

    /** default event code */
    private ElementEventType elementEvent = ElementEventType.EXCEEDED_MAXLIFE_BACKGROUND;

    /**
     * Constructor for the ElementEvent object
     * <p>
     * @param source The Cache Element
     * @param elementEvent The event id defined in the enum class.
     */
    public ElementEvent( T source, ElementEventType elementEvent )
    {
        super( source );
        this.elementEvent = elementEvent;
    }

    /**
     * Gets the elementEvent attribute of the ElementEvent object
     * <p>
     * @return The elementEvent value. The List of values is defined in ElementEventType.
     */
    @Override
    public ElementEventType getElementEvent()
    {
        return elementEvent;
    }

    /**
     * @return the source of the event.
     */
    @SuppressWarnings("unchecked") // Generified
    @Override
    public T getSource()
    {
        return (T) super.getSource();

    }
}
