package org.apache.commons.jcs.engine.stats.behavior;

import java.io.Serializable;

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
 * IAuxiliaryCacheStats will hold these IStatElements.
 */
public interface IStatElement<V> extends Serializable
{
    /**
     * Get the name of the stat element, ex. HitCount
     * <p>
     * @return the stat element name
     */
    String getName();

    /**
     * @param name
     */
    void setName( String name );

    /**
     * Get the data, ex. for hit count you would get a value for some number.
     * <p>
     * @return data
     */
    V getData();

    /**
     * Set the data for this element.
     * <p>
     * @param data
     */
    void setData( V data );
}
