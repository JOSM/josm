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

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheFactory;

/**
 * Base class for auxiliary cache factories.
 */
public abstract class AbstractAuxiliaryCacheFactory
    implements AuxiliaryCacheFactory
{
    /** The auxiliary name. The composite cache manager keeps this in a map, keyed by name. */
    private String name = this.getClass().getSimpleName();

    /**
     * Initialize this factory
     */
    @Override
    public void initialize()
    {
        // empty
    }

    /**
     * Dispose of this factory, clean up shared resources
     */
    @Override
    public void dispose()
    {
        // empty
    }

    /**
     * Gets the name attribute of the DiskCacheFactory object
     * <p>
     * @return The name value
     */
    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * Sets the name attribute of the DiskCacheFactory object
     * <p>
     * @param name The new name value
     */
    @Override
    public void setName( String name )
    {
        this.name = name;
    }
}
