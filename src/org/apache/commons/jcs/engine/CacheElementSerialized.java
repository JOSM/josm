package org.apache.commons.jcs.engine;

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

import org.apache.commons.jcs.engine.behavior.ICacheElementSerialized;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;

import java.util.Arrays;

/** Either serialized value or the value should be null; */
public class CacheElementSerialized<K, V>
    extends CacheElement<K, V>
    implements ICacheElementSerialized<K, V>
{
    /** Don't change. */
    private static final long serialVersionUID = -7265084818647601874L;

    /** The serialized value. */
    private final byte[] serializedValue;

    /**
     * Constructs a usable wrapper.
     * <p>
     * @param cacheNameArg
     * @param keyArg
     * @param serializedValueArg
     * @param elementAttributesArg
     */
    public CacheElementSerialized( String cacheNameArg, K keyArg, byte[] serializedValueArg,
                                   IElementAttributes elementAttributesArg )
    {
        super(cacheNameArg, keyArg, null, elementAttributesArg);
        this.serializedValue = serializedValueArg;
    }

    /** @return byte[] */
    @Override
    public byte[] getSerializedValue()
    {
        return this.serializedValue;
    }

    /**
     * For debugging only.
     * <p>
     * @return debugging string.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\n CacheElementSerialized: " );
        buf.append( "\n CacheName = [" + getCacheName() + "]" );
        buf.append( "\n Key = [" + getKey() + "]" );
        buf.append( "\n SerializedValue = " + Arrays.toString(getSerializedValue()) );
        buf.append( "\n ElementAttributes = " + getElementAttributes() );
        return buf.toString();
    }

}
