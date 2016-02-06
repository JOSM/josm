package org.apache.commons.jcs.engine.behavior;

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
 * This interface defines the behavior of the serialized element wrapper.
 * <p>
 * The value is stored as a byte array. This should allow for a variety of serialization mechanisms.
 * <p>
 * This currently extends ICacheElement&lt;K, V&gt; for backward compatibility.
 *<p>
 * @author Aaron Smuts
 */
public interface ICacheElementSerialized<K, V>
    extends ICacheElement<K, V>
{
    /**
     * Gets the value attribute of the ICacheElementSerialized object. This is the value the client
     * cached serialized by some mechanism.
     *<p>
     * @return The serialized value
     */
    byte[] getSerializedValue();
}
