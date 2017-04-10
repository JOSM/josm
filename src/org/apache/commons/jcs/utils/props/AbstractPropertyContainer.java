package org.apache.commons.jcs.utils.props;

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

import org.apache.commons.jcs.access.exception.ConfigurationException;

import java.util.Properties;

/**
 * Provides a mechanism to load properties into objects.
 * <p>
 * Functions that depend on properties should call ensureProperties() before it uses any properties.
 */
public abstract class AbstractPropertyContainer
{
    /** File, db, etc */
    private static final PropertiesFactory DEFAULT_PROPERTIES_FACTORY = new PropertiesFactoryFileImpl();

    /**
     * A property group is a subsection of properties. It's sent to the properties factory to
     * specify which group of properties to pull back. This will probably mean different things to
     * different property factories. For PropertiesFactoryFileImpl, the propertiesGroup maps to a
     * filename.
     */
    private String propertiesGroup;

    /**
     * The property heading is used to specify a starting point in the properties object. This is
     * used so that settings can be relative to this propertiesHeading, as opposed to being
     * statically coded. There's no enforcement of this, but users are encouraged to call
     * getProperties().get( getPropertiesHeading() + ".foo" );
     */
    private String propertiesHeading;

    /** The factory to use. */
    private PropertiesFactory propertiesFactory;

    /** The loaded properties. */
    private Properties properties;

    /**
     * Makes sure an AbstractPropertyClass has all the properties it needs.
     * <p>
     * Synchronized mutators so multiple threads cannot cause problems. We wouldn't want the
     * properties heading to get changed as we were processing the properties.
     * <p>
     * @throws ConfigurationException on configuration failure
     */
    public synchronized void ensureProperties()
        throws ConfigurationException
    {
        if ( getProperties() == null )
        {
            initializeProperties();
        }
    }

    /**
     * Loads the properties and then calls handleProperties. Typically, you don't need to call this.
     * This is primarily intended for reinitialization.
     * <p>
     * If the properties object is null, when you call ensureProperties initialize will be called.
     * <p>
     * @throws ConfigurationException on configuration failure
     */
    public synchronized void initializeProperties()
        throws ConfigurationException
    {
        loadProperties();

        handleProperties();
    }

    /**
     * This loads the properties regardless of whether or not they have already been loaded.
     * <p>
     * @throws ConfigurationException on configuration failure
     */
    private void loadProperties()
        throws ConfigurationException
    {
        if ( getPropertiesGroup() == null )
        {
            throw new ConfigurationException( "Properties group is null and it shouldn't be" );
        }

        if ( getPropertiesHeading() == null )
        {
            throw new ConfigurationException( "Properties heading is null and it shouldn't be" );
        }

        if ( getPropertiesFactory() == null )
        {
            setProperties( DEFAULT_PROPERTIES_FACTORY.getProperties( getPropertiesGroup() ) );
        }
        else
        {
            setProperties( getPropertiesFactory().getProperties( getPropertiesGroup() ) );
        }
    }

    /**
     * Sets fields for properties, and verifies that all necessary properties are there.
     * <p>
     * @throws ConfigurationException on configuration failure
     */
    protected abstract void handleProperties()
        throws ConfigurationException;

    /**
     * @return Returns the properties.
     */
    public synchronized Properties getProperties()
    {
        return properties;
    }

    /**
     * @param properties The properties to set.
     */
    public synchronized void setProperties( Properties properties )
    {
        this.properties = properties;
    }

    /**
     * @return Returns the propertiesHeading.
     */
    public synchronized String getPropertiesHeading()
    {
        return propertiesHeading;
    }

    /**
     * @param propertiesHeading The propertiesHeading to set.
     */
    public synchronized void setPropertiesHeading( String propertiesHeading )
    {
        this.propertiesHeading = propertiesHeading;
    }

    /**
     * @return Returns the propertiesFactory.
     */
    public PropertiesFactory getPropertiesFactory()
    {
        return propertiesFactory;
    }

    /**
     * @param propertiesFactory The propertiesFactory to set.
     */
    public void setPropertiesFactory( PropertiesFactory propertiesFactory )
    {
        this.propertiesFactory = propertiesFactory;
    }

    /**
     * @return Returns the propertiesGroup.
     */
    public String getPropertiesGroup()
    {
        return propertiesGroup;
    }

    /**
     * @param propertiesGroup The propertiesGroup to set.
     */
    public void setPropertiesGroup( String propertiesGroup )
    {
        this.propertiesGroup = propertiesGroup;
    }
}
