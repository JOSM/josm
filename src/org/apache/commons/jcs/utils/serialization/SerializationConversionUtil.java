package org.apache.commons.jcs.utils.serialization;

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

import org.apache.commons.jcs.engine.CacheElement;
import org.apache.commons.jcs.engine.CacheElementSerialized;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheElementSerialized;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * This uses a supplied Serializer to convert to and from cache elements.
 * <p>
 * @author Aaron Smuts
 */
public class SerializationConversionUtil
{
    /** The logger */
    private static final Log log = LogFactory.getLog( SerializationConversionUtil.class );

    /**
     * This returns a wrapper that has a serialized version of the value instead
     * of the value.
     * <p>
     * @param element
     * @param elementSerializer
     *            the serializer to be used.
     * @return null for null;
     * @throws IOException
     */
    public static <K, V> ICacheElementSerialized<K, V> getSerializedCacheElement( ICacheElement<K, V> element,
                                                                    IElementSerializer elementSerializer )
        throws IOException
    {
        if ( element == null )
        {
            return null;
        }

        byte[] serializedValue = null;

        // if it has already been serialized, don't do it again.
        if ( element instanceof ICacheElementSerialized )
        {
            serializedValue = ( (ICacheElementSerialized<K, V>) element ).getSerializedValue();
        }
        else
        {
            if ( elementSerializer != null )
            {
                try
                {
                    serializedValue = elementSerializer.serialize(element.getVal());

                    // update size in bytes
                    element.getElementAttributes().setSize(serializedValue.length);
                }
                catch ( IOException e )
                {
                    log.error( "Problem serializing object.", e );
                    throw e;
                }
            }
            else
            {
                // we could just use the default.
                log.warn( "ElementSerializer is null.  Could not serialize object." );
                throw new IOException( "Could not serialize object.  The ElementSerializer is null." );
            }
        }
        ICacheElementSerialized<K, V> serialized = new CacheElementSerialized<>(
                element.getCacheName(), element.getKey(), serializedValue, element.getElementAttributes() );

        return serialized;
    }

    /**
     * This returns a wrapper that has a de-serialized version of the value
     * instead of the serialized value.
     * <p>
     * @param serialized
     * @param elementSerializer
     *            the serializer to be used.
     * @return null for null;
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static <K, V> ICacheElement<K, V> getDeSerializedCacheElement( ICacheElementSerialized<K, V> serialized,
                                                            IElementSerializer elementSerializer )
        throws IOException, ClassNotFoundException
    {
        if ( serialized == null )
        {
            return null;
        }

        V deSerializedValue = null;

        if ( elementSerializer != null )
        {
            try
            {
                try
                {
                    deSerializedValue = elementSerializer.deSerialize( serialized.getSerializedValue(), null );
                }
                catch ( ClassNotFoundException e )
                {
                    log.error( "Problem de-serializing object.", e );
                    throw e;
                }
            }
            catch ( IOException e )
            {
                log.error( "Problem de-serializing object.", e );
                throw e;
            }
        }
        else
        {
            // we could just use the default.
            log.warn( "ElementSerializer is null.  Could not serialize object." );
        }
        ICacheElement<K, V> deSerialized = new CacheElement<>( serialized.getCacheName(), serialized.getKey(), deSerializedValue );
        deSerialized.setElementAttributes( serialized.getElementAttributes() );

        return deSerialized;
    }
}
