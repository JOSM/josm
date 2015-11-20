package org.apache.commons.jcs.engine.stats;

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

import org.apache.commons.jcs.engine.stats.behavior.IStatElement;

/**
 * This is a stat data holder.
 */
public class StatElement<V>
    implements IStatElement<V>
{
    /** Don't change */
    private static final long serialVersionUID = -2982373725267618092L;

    /** name of the stat */
    private String name = null;

    /** the data */
    private V data = null;

    /**
     * Constructor
     *
     * @param name
     * @param data
     */
    public StatElement(String name, V data)
    {
        super();
        this.name = name;
        this.data = data;
    }

    /**
     * Get the name of the stat element, ex. HitCount
     * <p>
     * @return the stat element name
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     */
    @Override
    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Get the data, ex. for hit count you would get a value for some number.
     * <p>
     * @return data
     */
    @Override
    public V getData()
    {
        return data;
    }

    /**
     * Set the data for this element.
     * <p>
     * @param data
     */
    @Override
    public void setData( V data )
    {
        this.data = data;
    }

    /**
     * @return a readable string.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( name ).append(" = ").append( data );
        return buf.toString();
    }
}
