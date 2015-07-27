package org.apache.commons.jcs.auxiliary;

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

import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.config.OptionConverter;
import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

/**
 * Configuration util for auxiliary caches. I plan to move the auxiliary configuration from the
 * composite cache configurator here.
 */
public class AuxiliaryCacheConfigurator
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( AuxiliaryCacheConfigurator.class );

    /** .attributes */
    public static final String ATTRIBUTE_PREFIX = ".attributes";

    /**
     * jcs.auxiliary.NAME.cacheeventlogger=CLASSNAME
     * <p>
     * jcs.auxiliary.NAME.cacheeventlogger.attributes.CUSTOMPROPERTY=VALUE
     */
    public static final String CACHE_EVENT_LOGGER_PREFIX = ".cacheeventlogger";

    /**
     * jcs.auxiliary.NAME.serializer=CLASSNAME
     * <p>
     * jcs.auxiliary.NAME.serializer.attributes.CUSTOMPROPERTY=VALUE
     */
    public static final String SERIALIZER_PREFIX = ".serializer";

    /**
     * Parses the event logger config, if there is any for the auxiliary.
     * <p>
     * @param props
     * @param auxPrefix - ex. AUXILIARY_PREFIX + auxName
     * @return cacheEventLogger
     */
    public static ICacheEventLogger parseCacheEventLogger( Properties props, String auxPrefix )
    {
        ICacheEventLogger cacheEventLogger = null;

        // auxFactory was not previously initialized.
        String eventLoggerClassName = auxPrefix + CACHE_EVENT_LOGGER_PREFIX;
        cacheEventLogger = OptionConverter.instantiateByKey( props, eventLoggerClassName, null );
        if ( cacheEventLogger != null )
        {
            String cacheEventLoggerAttributePrefix = auxPrefix + CACHE_EVENT_LOGGER_PREFIX + ATTRIBUTE_PREFIX;
            PropertySetter.setProperties( cacheEventLogger, props, cacheEventLoggerAttributePrefix + "." );
            if ( log.isInfoEnabled() )
            {
                log.info( "Using custom cache event logger [" + cacheEventLogger + "] for auxiliary [" + auxPrefix
                    + "]" );
            }
        }
        else
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "No cache event logger defined for auxiliary [" + auxPrefix + "]" );
            }
        }
        return cacheEventLogger;
    }

    /**
     * Parses the element config, if there is any for the auxiliary.
     * <p>
     * @param props
     * @param auxPrefix - ex. AUXILIARY_PREFIX + auxName
     * @return cacheEventLogger
     */
    public static IElementSerializer parseElementSerializer( Properties props, String auxPrefix )
    {
        // TODO take in the entire prop key
        IElementSerializer elementSerializer = null;

        // auxFactory was not previously initialized.
        String elementSerializerClassName = auxPrefix + SERIALIZER_PREFIX;
        elementSerializer = OptionConverter.instantiateByKey( props, elementSerializerClassName, null );
        if ( elementSerializer != null )
        {
            String attributePrefix = auxPrefix + SERIALIZER_PREFIX + ATTRIBUTE_PREFIX;
            PropertySetter.setProperties( elementSerializer, props, attributePrefix + "." );
            if ( log.isInfoEnabled() )
            {
                log.info( "Using custom element serializer [" + elementSerializer + "] for auxiliary [" + auxPrefix
                    + "]" );
            }
        }
        else
        {
            // use the default standard serializer
            elementSerializer = new StandardSerializer();
            if ( log.isInfoEnabled() )
            {
                log.info( "Using standard serializer [" + elementSerializer + "] for auxiliary [" + auxPrefix + "]" );
            }
        }
        return elementSerializer;
    }
}
