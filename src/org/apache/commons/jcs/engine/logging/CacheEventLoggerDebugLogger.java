package org.apache.commons.jcs.engine.logging;

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

import org.apache.commons.jcs.engine.logging.behavior.ICacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This implementation simple logs to a commons logger at debug level, for all events. It's mainly
 * for testing. It isn't very useful otherwise.
 */
public class CacheEventLoggerDebugLogger
    implements ICacheEventLogger
{
    /** This is the name of the category. */
    private String logCategoryName = CacheEventLoggerDebugLogger.class.getName();

    /** The logger. This is recreated on set logCategoryName */
    private Log log = LogFactory.getLog( logCategoryName );

    /**
     * @param source
     * @param region
     * @param eventName
     * @param optionalDetails
     * @param key
     * @return ICacheEvent
     */
    @Override
    public <T> ICacheEvent<T> createICacheEvent( String source, String region, String eventName,
            String optionalDetails, T key )
    {
        ICacheEvent<T> event = new CacheEvent<>();
        event.setSource( source );
        event.setRegion( region );
        event.setEventName( eventName );
        event.setOptionalDetails( optionalDetails );
        event.setKey( key );

        return event;
    }

    /**
     * @param source
     * @param eventName
     * @param optionalDetails
     */
    @Override
    public void logApplicationEvent( String source, String eventName, String optionalDetails )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( source + " | " + eventName + " | " + optionalDetails );
        }
    }

    /**
     * @param source
     * @param eventName
     * @param errorMessage
     */
    @Override
    public void logError( String source, String eventName, String errorMessage )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( source + " | " + eventName + " | " + errorMessage );
        }
    }

    /**
     * @param event
     */
    @Override
    public <T> void logICacheEvent( ICacheEvent<T> event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( event );
        }
    }

    /**
     * @param logCategoryName
     */
    public synchronized void setLogCategoryName( String logCategoryName )
    {
        if ( logCategoryName != null && !logCategoryName.equals( this.logCategoryName ) )
        {
            this.logCategoryName = logCategoryName;
            log = LogFactory.getLog( logCategoryName );
        }
    }
}
