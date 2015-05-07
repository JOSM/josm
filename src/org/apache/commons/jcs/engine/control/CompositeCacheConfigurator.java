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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This class configures JCS based on a properties object.
 * <p>
 * This class is based on the log4j class org.apache.log4j.PropertyConfigurator which was made by:
 * "Luke Blanshard" <Luke@quiq.com>"Mark DONSZELMANN" <Mark.Donszelmann@cern.ch>"Anders Kristensen"
 * <akristensen@dynamicsoft.com>
 */
public class CompositeCacheConfigurator
{
    /** The logger */
    private static final Log log = LogFactory.getLog( CompositeCacheConfigurator.class );

    /** default region prefix */
    static final String DEFAULT_REGION = "jcs.default";

    /** normal region prefix */
    static final String REGION_PREFIX = "jcs.region.";

    /** system region prefix. might not be used */
    static final String SYSTEM_REGION_PREFIX = "jcs.system.";

    /** auxiliary prefix */
    static final String AUXILIARY_PREFIX = "jcs.auxiliary.";

    /** .attributes */
    static final String ATTRIBUTE_PREFIX = ".attributes";

    /** .cacheattributes */
    static final String CACHE_ATTRIBUTE_PREFIX = ".cacheattributes";

    /** .elementattributes */
    static final String ELEMENT_ATTRIBUTE_PREFIX = ".elementattributes";

    /**
     * jcs.auxiliary.NAME.keymatcher=CLASSNAME
     * <p>
     * jcs.auxiliary.NAME.keymatcher.attributes.CUSTOMPROPERTY=VALUE
     */
    public static final String KEY_MATCHER_PREFIX = ".keymatcher";

    /** Can't operate on the interface. */
    private final CompositeCacheManager compositeCacheManager;

    /**
     * Constructor for the CompositeCacheConfigurator object
     *<p>
     * @param ccMgr
     */
    public CompositeCacheConfigurator( CompositeCacheManager ccMgr )
    {
        this.compositeCacheManager = ccMgr;
    }

    /**
     * Configure cached for file name.
     * <p>
     * This is only used for testing. The manager handles the translation of a file into a
     * properties object.
     * <p>
     * @param configFileName
     */
    protected void doConfigure( String configFileName )
    {
        Properties props = new Properties();
        FileInputStream istream = null;

        try
        {
            istream = new FileInputStream( configFileName );
            props.load( istream );
        }
        catch ( IOException e )
        {
            log.error( "Could not read configuration file, ignored: " + configFileName, e );
            return;
        }
        finally
        {
            if (istream != null)
            {
                try
                {
                    istream.close();
                }
                catch (IOException e)
                {
                    log.error( "Could not close configuration file " + configFileName, e );
                }
            }
        }

        // If we reach here, then the config file is alright.
        doConfigure( props );
    }

    /**
     * Configure cache for properties object.
     * <p>
     * This method proceeds in several steps:
     * <ul>
     * <li>Store props for use by non configured caches.
     * <li>Set default value list
     * <li>Set default cache attr
     * <li>Set default element attr
     * <li>Setup system caches to be used
     * <li>Setup preconfigured caches
     * </ul>
     * @param properties
     */
    public void doConfigure( Properties properties )
    {
        long start = System.currentTimeMillis();

        // store props for use by non configured caches
        compositeCacheManager.setConfigurationProperties( properties );

        // set default value list
        setDefaultAuxValues( properties );

        // set default cache attr
        setDefaultCompositeCacheAttributes( properties );

        // set default element attr
        setDefaultElementAttributes( properties );

        // set up system caches to be used by non system caches
        // need to make sure there is no circularity of reference
        parseSystemRegions( properties );

        // setup preconfigured caches
        parseRegions( properties );

        long end = System.currentTimeMillis();
        if ( log.isInfoEnabled() )
        {
            log.info( "Finished configuration in " + ( end - start ) + " ms." );
        }

    }

    /**
     * Set the default aux list for new caches.
     * <p>
     * @param props
     */
    protected void setDefaultAuxValues( Properties props )
    {
        String value = OptionConverter.findAndSubst( DEFAULT_REGION, props );
        compositeCacheManager.setDefaultAuxValues(value);

        if ( log.isInfoEnabled() )
        {
            log.info( "Setting default auxiliaries to " + value );
        }
    }

    /**
     * Set the default CompositeCacheAttributes for new caches.
     *<p>
     * @param props
     */
    protected void setDefaultCompositeCacheAttributes( Properties props )
    {
        ICompositeCacheAttributes icca = parseCompositeCacheAttributes( props, "",
                                                                        CompositeCacheConfigurator.DEFAULT_REGION );
        compositeCacheManager.setDefaultCacheAttributes( icca );

        log.info( "setting defaultCompositeCacheAttributes to " + icca );
    }

    /**
     * Set the default ElementAttributes for new caches.
     *<p>
     * @param props
     */
    protected void setDefaultElementAttributes( Properties props )
    {
        IElementAttributes iea = parseElementAttributes( props, "", CompositeCacheConfigurator.DEFAULT_REGION );
        compositeCacheManager.setDefaultElementAttributes( iea );

        log.info( "setting defaultElementAttributes to " + iea );
    }

    /**
     * Create caches used internally. System status gives them creation priority.
     *<p>
     * @param props
     */
    protected <K, V> void parseSystemRegions( Properties props )
    {
        Enumeration<?> en = props.propertyNames();
        while ( en.hasMoreElements() )
        {
            String key = (String) en.nextElement();
            if ( key.startsWith( SYSTEM_REGION_PREFIX ) && key.indexOf( "attributes" ) == -1 )
            {
                String regionName = key.substring( SYSTEM_REGION_PREFIX.length() );
                String value = OptionConverter.findAndSubst( key, props );
                ICache<K, V> cache;
                synchronized ( regionName )
                {
                    cache = parseRegion( props, regionName, value, null, SYSTEM_REGION_PREFIX );
                }

                compositeCacheManager.addCache( regionName, cache );
            }
        }
    }

    /**
     * Parse region elements.
     *<p>
     * @param props
     */
    protected <K, V> void parseRegions( Properties props )
    {
        List<String> regionNames = new ArrayList<String>();

        Enumeration<?> en = props.propertyNames();
        while ( en.hasMoreElements() )
        {
            String key = (String) en.nextElement();
            if ( key.startsWith( REGION_PREFIX ) && key.indexOf( "attributes" ) == -1 )
            {
                String regionName = key.substring( REGION_PREFIX.length() );

                regionNames.add( regionName );

                String auxiliaryList = OptionConverter.findAndSubst( key, props );
                ICache<K, V> cache;
                synchronized ( regionName )
                {
                    cache = parseRegion( props, regionName, auxiliaryList );
                }
                compositeCacheManager.addCache( regionName, cache );
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
     * @param props
     * @param regName
     * @param value
     * @return CompositeCache
     */
    protected <K, V> CompositeCache<K, V> parseRegion(
            Properties props, String regName, String value )
    {
        return parseRegion( props, regName, value, null, REGION_PREFIX );
    }

    /**
     * Get all the properties for a region and configure its cache.
     * <p>
     * This method tells the other parse method the name of the region prefix.
     *<p>
     * @param props
     * @param regName
     * @param value
     * @param cca
     * @return CompositeCache
     */
    protected <K, V> CompositeCache<K, V> parseRegion(
            Properties props, String regName, String value, ICompositeCacheAttributes cca )
    {
        return parseRegion( props, regName, value, cca, REGION_PREFIX );
    }

    /**
     * Get all the properties for a region and configure its cache.
     *<p>
     * @param props
     * @param regName
     * @param value
     * @param cca
     * @param regionPrefix
     * @return CompositeCache
     */
    protected <K, V> CompositeCache<K, V> parseRegion(
            Properties props, String regName, String value,
            ICompositeCacheAttributes cca, String regionPrefix )
    {
        // First, create or get the cache and element attributes, and create
        // the cache.
        IElementAttributes ea = parseElementAttributes( props, regName, regionPrefix );

        CompositeCache<K, V> cache = ( cca == null )
            ? new CompositeCache<K, V>( parseCompositeCacheAttributes( props, regName, regionPrefix ), ea )
            : new CompositeCache<K, V>( cca, ea );

        // Inject scheduler service
        cache.setScheduledExecutorService(compositeCacheManager.getScheduledExecutorService());

        // Inject element event queue
        cache.setElementEventQueue(compositeCacheManager.getElementEventQueue());

        if (cache.getMemoryCache() instanceof IRequireScheduler)
        {
            ((IRequireScheduler)cache.getMemoryCache()).setScheduledExecutorService(
                    compositeCacheManager.getScheduledExecutorService());
        }

        if (value != null)
        {
            // Next, create the auxiliaries for the new cache
            List<AuxiliaryCache<K, V>> auxList = new ArrayList<AuxiliaryCache<K, V>>();

            if ( log.isDebugEnabled() )
            {
                log.debug( "Parsing region name '" + regName + "', value '" + value + "'" );
            }

            // We must skip over ',' but not white space
            StringTokenizer st = new StringTokenizer( value, "," );

            // If value is not in the form ", appender.." or "", then we should set
            // the priority of the category.

            if ( !( value.startsWith( "," ) || value.equals( "" ) ) )
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

                auxCache = parseAuxiliary( props, auxName, regName );

                if ( auxCache != null )
                {
                    if (auxCache instanceof IRequireScheduler)
                    {
                        ((IRequireScheduler)auxCache).setScheduledExecutorService(
                                compositeCacheManager.getScheduledExecutorService());
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

    /**
     * Get an ICompositeCacheAttributes for the listed region.
     *<p>
     * @param props
     * @param regName
     * @return ICompositeCacheAttributes
     */
    protected ICompositeCacheAttributes parseCompositeCacheAttributes( Properties props, String regName )
    {
        return parseCompositeCacheAttributes( props, regName, REGION_PREFIX );
    }

    /**
     * Get the main attributes for a region.
     *<p>
     * @param props
     * @param regName
     * @param regionPrefix
     * @return ICompositeCacheAttributes
     */
    protected ICompositeCacheAttributes parseCompositeCacheAttributes( Properties props, String regName,
                                                                       String regionPrefix )
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

            ICompositeCacheAttributes ccAttr2 = compositeCacheManager.getDefaultCacheAttributes();
            ccAttr = ccAttr2.copy();
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
     * @param props
     * @param regName
     * @param regionPrefix
     * @return IElementAttributes
     */
    protected IElementAttributes parseElementAttributes( Properties props, String regName, String regionPrefix )
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

            IElementAttributes eAttr2 = compositeCacheManager.getDefaultElementAttributes();
            eAttr = eAttr2.copy();
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
     * @param auxName the name of the auxiliary cache
     * @param regName the name of the region.
     * @return AuxiliaryCache
     */
    protected <K, V> AuxiliaryCache<K, V> parseAuxiliary( Properties props, String auxName, String regName )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "parseAuxiliary " + auxName );
        }

        // GET CACHE
        @SuppressWarnings("unchecked") // Common map for all caches
        AuxiliaryCache<K, V> auxCache = (AuxiliaryCache<K, V>)compositeCacheManager.getAuxiliaryCache(auxName, regName);

        if (auxCache == null)
        {
            // GET FACTORY
            AuxiliaryCacheFactory auxFac = compositeCacheManager.registryFacGet( auxName );
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
                auxFac.initialize();

                compositeCacheManager.registryFacPut( auxFac );
            }

            // GET ATTRIBUTES
            AuxiliaryCacheAttributes auxAttr = compositeCacheManager.registryAttrGet( auxName );
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
                compositeCacheManager.registryAttrPut( auxAttr );
            }

            auxAttr = auxAttr.copy();

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
            ICacheEventLogger cacheEventLogger = AuxiliaryCacheConfigurator.parseCacheEventLogger( props, auxPrefix );

            // CONFIGURE THE ELEMENT SERIALIZER
            IElementSerializer elementSerializer = AuxiliaryCacheConfigurator.parseElementSerializer( props, auxPrefix );

            // CONFIGURE THE KEYMATCHER
            //IKeyMatcher keyMatcher = parseKeyMatcher( props, auxPrefix );
            // TODO add to factory interface

            // Consider putting the compositeCache back in the factory interface
            // since the manager may not know about it at this point.
            // need to make sure the manager already has the cache
            // before the auxiliary is created.
            try
            {
                auxCache = auxFac.createCache( auxAttr, compositeCacheManager, cacheEventLogger, elementSerializer );
            }
            catch (Exception e)
            {
                log.error( "Could not instantiate auxiliary cache named \"" + regName + "\"." );
                return null;
            }

            compositeCacheManager.addAuxiliaryCache(auxName, regName, auxCache);
        }

        return auxCache;
    }

    /**
     * Creates a custom key matcher if one is defined.  Else, it uses the default.
     * <p>
     * @param props
     * @param auxPrefix - ex. AUXILIARY_PREFIX + auxName
     * @return IKeyMatcher
     */
    public static <K> IKeyMatcher<K> parseKeyMatcher( Properties props, String auxPrefix )
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
            keyMatcher = new KeyMatcherPatternImpl<K>();
            if ( log.isInfoEnabled() )
            {
                log.info( "Using standard key matcher [" + keyMatcher + "] for auxiliary [" + auxPrefix + "]" );
            }
        }
        return keyMatcher;
    }
}
