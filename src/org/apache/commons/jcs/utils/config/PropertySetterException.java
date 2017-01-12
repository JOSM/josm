package org.apache.commons.jcs.utils.config;

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
 * This class is based on the log4j class org.apache.log4j.config.PropertySetter that was made by
 * Anders Kristensen
 * <p>
 * Thrown when an error is encountered whilst attempting to set a property using the
 * {@link PropertySetter}utility class.
 */
public class PropertySetterException
    extends Exception
{
    /** DOn't change */
    private static final long serialVersionUID = -210271658004609028L;

    /** Description of the Field */
    private final Throwable rootCause;

    /**
     * Constructor for the PropertySetterException object
     * <p>
     * @param msg
     */
    public PropertySetterException( String msg )
    {
        super( msg );
        this.rootCause = null;
    }

    /**
     * Constructor for the PropertySetterException object
     * <p>
     * @param rootCause
     */
    public PropertySetterException( Throwable rootCause )
    {
        super();
        this.rootCause = rootCause;
    }

    /**
     * Returns descriptive text on the cause of this exception.
     * <p>
     * @return The message value
     */
    @Override
    public String getMessage()
    {
        String msg = super.getMessage();
        if ( msg == null && rootCause != null )
        {
            msg = rootCause.getMessage();
        }
        return msg;
    }
}
