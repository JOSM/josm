package org.apache.commons.jcs.engine.control;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheConfigurator;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheFactory;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.IElementAttributes;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.behavior.IRequireScheduler;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.engine.match.KeyMatcherPatternImpl;
import org.apache.commons.jcs.engine.match.behavior.IKeyMatcher;
import org.apache.commons.jcs.utils.config.OptionConverter;
import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class configures JCS based on a properties object.
 * <p>
 * This class is based on the log4j class org.apache.log4j.PropertyConfigurator which was made by:
 * "Luke Blanshard" &lt;Luke@quiq.com&gt;"Mark DONSZELMANN" &lt;Mark.Donszelmann@cern.ch&gt;"Anders Kristensen"
 * &lt;akristensen@dynamicsoft.com&gt;
 */
public class CompositeCacheConfigurator
{
    /** The logger */
    private static final Log log = LogFactory.getLog( CompositeCacheConfigurator.class );

    /** The prefix of relevant system properties */
    protected static final String SYSTEM_PROPERTY_KEY_PREFIX = "jcs";

    /** normal region prefix */
    protected static final String REGION_PREFIX = "jcs.region.";

    /** system region prefix. might not be used */
    protected static final String SYSTEM_REGION_PREFIX = "jcs.system.";

    /** auxiliary prefix */
    protected static final String AUXILIARY_PREFIX = "jcs.auxiliary.";

    /** .attributes */
    protected static final String ATTRIBUTE_PREFIX = ".attributes";

    /** .cacheattributes */
    protected static final String CACHE_ATTRIBUTE_PREFIX = ".cacheattributes";

    /** .elementattributes */
    protected static final String ELEMENT_ATTRIBUTE_PREFIX = ".elementattributes";

    /**
     * jcs.auxiliary.NAME.keymatcher=CLASSNAME
     * <p>
     * jcs.auxiliary.NAME.keymatcher.attributes.CUSTOMPROPERTY=VALUE
     */
    public static final String KEY_MATCHER_PREFIX = ".keymatcher";

    /**
     * Constructor for the CompositeCacheConfigurator object
     */
    public CompositeCacheConfigurator()
    {
        // empty
    }

    /**
     * Create caches used internally. System status gives them creation priority.
     *<p>
     * @param props Configuration properties
     * @param ccm Cache hub
     */
    protected void parseSystemRegions( Properties props, CompositeCacheManager ccm )
    {
        for (String key : props.stringPropertyNames() )
        {
            if ( key.startsWith( SYSTEM_REGION_PREFIX ) && key.indexOf( "attributes" ) == -1 )
            {
                String regionName = key.substring( SYSTEM_REGION_PREFIX.length() );
                String auxiliaries = OptionConverter.findAndSubst( key, props );
                ICache<?, ?> cache;
                synchronized ( regionName )
                {
                    cache = parseRegion( props, ccm, regionName, auxiliaries, null, SYSTEM_REGION_PREFIX );
                }
                ccm.addCache( regionName, cache );
            }
        }
    }

    /**
     * Parse region elements.
     *<p>
     * @param props Configuration properties
     * @param ccm Cache hub
     */
    protected void parseRegions( Properties props, CompositeCacheManager ccm )
    {
        List<String> regionNames = new ArrayList<>();

        for (String key : props.stringPropertyNames() )
        {
            if ( key.startsWith( REGION_PREFIX ) && key.indexOf( "attributes" ) == -1 )
            {
                String regionName = key.substring( REGION_PREFIX.length() );
                regionNames.add( regionName );
                String auxiliaries = OptionConverter.findAndSubst( key, props );
                ICache<?, ?> cache;
                synchronized ( regionName )
                {
                    cache = parseRegion( props, ccm, regionName, auxiliaries );
                }
                ccm.addCache( regionName, cache );
            }
        }

        if ( log.isInfoEnabled() )
        {
            log.info( "Parsed regions " + regionNames );
        }
    }

    /**
     * Create cache region.
     *<p>
     * @param props Configuration properties
     * @param ccm Cache hub
     * @param regName Name of the cache region
     * @param auxiliaries Comma separated list of auxiliaries
     *
     * @return CompositeCache
     */
    protected <K, V> CompositeCache<K, V> parseRegion(
            Properties props, CompositeCacheManager ccm, String regName, String auxiliaries )
    {
        return parseRegion( props, ccm, regName, auxiliaries, null, REGION_PREFIX );
    }

    /**
     * Get all the properties for a region and configure its cache.
     * <p>
     * This method tells the other parse method the name of the region prefix.
     *<p>
     * @param props Configuration properties
     * @param ccm Cache hub
     * @param regName Name of the cache region
     * @param auxiliaries Comma separated list of auxiliaries
     * @param cca Cache configuration
     *
     * @return CompositeCache
     */
    protected <K, V> CompositeCache<K, V> parseRegion(
            Properties props, CompositeCacheManager ccm, String regName, String auxiliaries,
            ICompositeCacheAttributes cca )
    {
        return parseRegion( props, ccm, regName, auxiliaries, cca, REGION_PREFIX );
    }

    /**
     * Get all the properties for a region and configure its cache.
     *<p>
     * @param props Configuration properties
     * @param ccm Cache hub
     * @param regName Name of the cache region
     * @param auxiliaries Comma separated list of auxiliaries
     * @param cca Cache configuration
     * @param regionPrefix Prefix for the region
     *
     * @return CompositeCache
     */
    protected <K, V> CompositeCache<K, V> parseRegion(
            Properties props, CompositeCacheManager ccm, String regName, String auxiliaries,
            ICompositeCacheAttributes cca, String regionPrefix )
    {
        // First, create or get the cache and element attributes, and create
        // the cache.
        IElementAttributes ea = parseElementAttributes( props, regName,
                ccm.getDefaultElementAttributes(), regionPrefix );

        ICompositeCacheAttributes instantiationCca = cca == null
                ? parseCompositeCacheAttributes(props, regName, ccm.getDefaultCacheAttributes(), regionPrefix)
                : cca;
        CompositeCache<K, V> cache = newCache(instantiationCca, ea);

        // Inject cache manager
        cache.setCompositeCacheManager(ccm);

        // Inject scheduler service
        cache.setScheduledExecutorService(ccm.getScheduledExecutorService());

        // Inject element event queue
        cache.setElementEventQueue(ccm.getElementEventQueue());

        if (cache.getMemoryCache() instanceof IRequireScheduler)
        {
            ((IRequireScheduler)cache.getMemoryCache()).setScheduledExecutorService(
                    ccm.getScheduledExecutorService());
        }

        if (auxiliaries != null)
        {
            // Next, create the auxiliaries for the new cache
            List<AuxiliaryCache<K, V>> auxList = new ArrayList<>();

            if ( log.isDebugEnabled() )
            {
                log.debug( "Parsing region name '" + regName + "', value '" + auxiliaries + "'" );
            }

            // We must skip over ',' but not white space
            StringTokenizer st = new StringTokenizer( auxiliaries, "," );

            // If value is not in the form ", appender.." or "", then we should set
            // the priority of the category.

            if ( !( auxiliaries.startsWith( "," ) || auxiliaries.equals( "" ) ) )
            {
                // just to be on the safe side...
                if ( !st.hasMoreTokens() )
                {
                    return null;
                }
            }

            AuxiliaryCache<K, V> auxCache;
            String auxName;
            while ( st.hasMoreTokens() )
            {
                auxName = st.nextToken().trim();
                if ( auxName == null || auxName.equals( "," ) )
                {
                    continue;
                }
                log.debug( "Parsing auxiliary named \"" + auxName + "\"." );

                auxCache = parseAuxiliary( props, ccm, auxName, regName );

                if ( auxCache != null )
                {
                    if (auxCache instanceof IRequireScheduler)
                    {
                        ((IRequireScheduler) auxCache).setScheduledExecutorService(
                                ccm.getScheduledExecutorService());
                    }

                    auxList.add( auxCache );
                }
            }

            // Associate the auxiliaries with the cache
            @SuppressWarnings("unchecked") // No generic arrays in java
            AuxiliaryCache<K, V>[] auxArray = auxList.toArray( new AuxiliaryCache[0] );
            cache.setAuxCaches( auxArray );
        }

        // Return the new cache
        return cache;
    }

    protected <K, V> CompositeCache<K, V> newCache(
            ICompositeCacheAttributes cca, IElementAttributes ea)
    {
        return new CompositeCache<>( cca, ea );
    }

    /**
     * Get an ICompositeCacheAttributes for the listed region.
     *<p>
     * @param props Configuration properties
     * @param regName the region name
     * @param defaultCCAttr the default cache attributes
     *
     * @return ICompositeCacheAttributes
     */
    protected ICompositeCacheAttributes parseCompositeCacheAttributes( Properties props,
            String regName, ICompositeCacheAttributes defaultCCAttr )
    {
        return parseCompositeCacheAttributes( props, regName, defaultCCAttr, REGION_PREFIX );
    }

    /**
     * Get the main attributes for a region.
     *<p>
     * @param props Configuration properties
     * @param regName the region name
     * @param defaultCCAttr the default cache attributes
     * @param regionPrefix the region prefix
     *
     * @return ICompositeCacheAttributes
     */
    protected ICompositeCacheAttributes parseCompositeCacheAttributes( Properties props,
            String regName, ICompositeCacheAttributes defaultCCAttr, String regionPrefix )
    {
        ICompositeCacheAttributes ccAttr;

        String attrName = regionPrefix + regName + CACHE_ATTRIBUTE_PREFIX;

        // auxFactory was not previously initialized.
        // String prefix = regionPrefix + regName + ATTRIBUTE_PREFIX;
        ccAttr = OptionConverter.instantiateByKey( props, attrName, null );

        if ( ccAttr == null )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "No special CompositeCacheAttributes class defined for key [" + attrName
                    + "], using default class." );
            }

            ccAttr = defaultCCAttr;
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "Parsing options for '" + attrName + "'" );
        }

        PropertySetter.setProperties( ccAttr, props, attrName + "." );
        ccAttr.setCacheName( regName );

        if ( log.isDebugEnabled() )
        {
            log.debug( "End of parsing for \"" + attrName + "\"." );
        }

        // GET CACHE FROM FACTORY WITH ATTRIBUTES
        ccAttr.setCacheName( regName );
        return ccAttr;
    }

    /**
     * Create the element attributes from the properties object for a cache region.
     *<p>
     * @param props Configuration properties
     * @param regName the region name
     * @param defaultEAttr the default element attributes
     * @param regionPrefix the region prefix
     *
     * @return IElementAttributes
     */
    protected IElementAttributes parseElementAttributes( Properties props, String regName,
            IElementAttributes defaultEAttr, String regionPrefix )
    {
        IElementAttributes eAttr;

        String attrName = regionPrefix + regName + CompositeCacheConfigurator.ELEMENT_ATTRIBUTE_PREFIX;

        // auxFactory was not previously initialized.
        // String prefix = regionPrefix + regName + ATTRIBUTE_PREFIX;
        eAttr = OptionConverter.instantiateByKey( props, attrName, null );
        if ( eAttr == null )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "No special ElementAttribute class defined for key [" + attrName + "], using default class." );
            }

            eAttr = defaultEAttr;
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "Parsing options for '" + attrName + "'" );
        }

        PropertySetter.setProperties( eAttr, props, attrName + "." );
        // eAttr.setCacheName( regName );

        if ( log.isDebugEnabled() )
        {
            log.debug( "End of parsing for \"" + attrName + "\"." );
        }

        // GET CACHE FROM FACTORY WITH ATTRIBUTES
        // eAttr.setCacheName( regName );
        return eAttr;
    }

    /**
     * Get an aux cache for the listed aux for a region.
     *<p>
     * @param props the configuration properties
     * @param ccm Cache hub
     * @param auxName the name of the auxiliary cache
     * @param regName the name of the region.
     * @return AuxiliaryCache
     */
    protected <K, V> AuxiliaryCache<K, V> parseAuxiliary( Properties props, CompositeCacheManager ccm,
            String auxName, String regName )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "parseAuxiliary " + auxName );
        }

        // GET CACHE
        @SuppressWarnings("unchecked") // Common map for all caches
        AuxiliaryCache<K, V> auxCache = (AuxiliaryCache<K, V>) ccm.getAuxiliaryCache(auxName, regName);

        if (auxCache == null)
        {
            // GET FACTORY
            AuxiliaryCacheFactory auxFac = ccm.registryFacGet( auxName );
            if ( auxFac == null )
            {
                // auxFactory was not previously initialized.
                String prefix = AUXILIARY_PREFIX + auxName;
                auxFac = OptionConverter.instantiateByKey( props, prefix, null );
                if ( auxFac == null )
                {
                    log.error( "Could not instantiate auxFactory named \"" + auxName + "\"." );
                    return null;
                }

                auxFac.setName( auxName );

                if ( auxFac instanceof IRequireScheduler)
                {
                	((IRequireScheduler)auxFac).setScheduledExecutorService(ccm.getScheduledExecutorService());
                }

                auxFac.initialize();
                ccm.registryFacPut( auxFac );
            }

            // GET ATTRIBUTES
            AuxiliaryCacheAttributes auxAttr = ccm.registryAttrGet( auxName );
            String attrName = AUXILIARY_PREFIX + auxName + ATTRIBUTE_PREFIX;
            if ( auxAttr == null )
            {
                // auxFactory was not previously initialized.
                String prefix = AUXILIARY_PREFIX + auxName + ATTRIBUTE_PREFIX;
                auxAttr = OptionConverter.instantiateByKey( props, prefix, null );
                if ( auxAttr == null )
                {
                    log.error( "Could not instantiate auxAttr named '" + attrName + "'" );
                    return null;
                }
                auxAttr.setName( auxName );
                ccm.registryAttrPut( auxAttr );
            }

            auxAttr = auxAttr.clone();

            if ( log.isDebugEnabled() )
            {
                log.debug( "Parsing options for '" + attrName + "'" );
            }

            PropertySetter.setProperties( auxAttr, props, attrName + "." );
            auxAttr.setCacheName( regName );

            if ( log.isDebugEnabled() )
            {
                log.debug( "End of parsing for '" + attrName + "'" );
            }

            // GET CACHE FROM FACTORY WITH ATTRIBUTES
            auxAttr.setCacheName( regName );

            String auxPrefix = AUXILIARY_PREFIX + auxName;

            // CONFIGURE THE EVENT LOGGER
            ICacheEventLogger cacheEventLogger =
                    AuxiliaryCacheConfigurator.parseCacheEventLogger( props, auxPrefix );

            // CONFIGURE THE ELEMENT SERIALIZER
            IElementSerializer elementSerializer =
                    AuxiliaryCacheConfigurator.parseElementSerializer( props, auxPrefix );

            // CONFIGURE THE KEYMATCHER
            //IKeyMatcher keyMatcher = parseKeyMatcher( props, auxPrefix );
            // TODO add to factory interface

            // Consider putting the compositeCache back in the factory interface
            // since the manager may not know about it at this point.
            // need to make sure the manager already has the cache
            // before the auxiliary is created.
            try
            {
                auxCache = auxFac.createCache( auxAttr, ccm, cacheEventLogger, elementSerializer );
            }
            catch (Exception e)
            {
                log.error( "Could not instantiate auxiliary cache named \"" + regName + "\"." );
                return null;
            }

            ccm.addAuxiliaryCache(auxName, regName, auxCache);
        }

        return auxCache;
    }

    /**
     * Any property values will be replaced with system property values that match the key.
     * <p>
     * @param props
     */
    protected static void overrideWithSystemProperties( Properties props )
    {
        // override any setting with values from the system properties.
        Properties sysProps = System.getProperties();
        for (String key : sysProps.stringPropertyNames())
        {
            if ( key.startsWith( SYSTEM_PROPERTY_KEY_PREFIX ) )
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "Using system property [[" + key + "] [" + sysProps.getProperty( key ) + "]]" );
                }
                props.setProperty( key, sysProps.getProperty( key ) );
            }
        }
    }

    /**
     * Creates a custom key matcher if one is defined.  Else, it uses the default.
     * <p>
     * @param props
     * @param auxPrefix - ex. AUXILIARY_PREFIX + auxName
     * @return IKeyMatcher
     */
    protected <K> IKeyMatcher<K> parseKeyMatcher( Properties props, String auxPrefix )
    {

        // auxFactory was not previously initialized.
        String keyMatcherClassName = auxPrefix + KEY_MATCHER_PREFIX;
        IKeyMatcher<K> keyMatcher = OptionConverter.instantiateByKey( props, keyMatcherClassName, null );
        if ( keyMatcher != null )
        {
            String attributePrefix = auxPrefix + KEY_MATCHER_PREFIX + ATTRIBUTE_PREFIX;
            PropertySetter.setProperties( keyMatcher, props, attributePrefix + "." );
            if ( log.isInfoEnabled() )
            {
                log.info( "Using custom key matcher [" + keyMatcher + "] for auxiliary [" + auxPrefix
                    + "]" );
            }
        }
        else
        {
            // use the default standard serializer
            keyMatcher = new KeyMatcherPatternImpl<>();
            if ( log.isInfoEnabled() )
            {
                log.info( "Using standard key matcher [" + keyMatcher + "] for auxiliary [" + auxPrefix + "]" );
            }
        }
        return keyMatcher;
    }
}
