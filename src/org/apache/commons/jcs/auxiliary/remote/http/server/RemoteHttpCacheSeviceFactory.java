package org.apache.commons.jcs.auxiliary.remote.http.server;

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

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheConfigurator;
import org.apache.commons.jcs.auxiliary.remote.http.behavior.IRemoteHttpCacheConstants;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.config.PropertySetter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Properties;

/** Creates the server. */
public class RemoteHttpCacheSeviceFactory
{
    /** The logger */
    private static final Log log = LogFactory.getLog( RemoteHttpCacheSeviceFactory.class );

    /**
     * Configures the attributes and the event logger and constructs a service.
     * <p>
     * @param cacheManager
     * @return RemoteHttpCacheService
     */
    public static <K extends Serializable, V extends Serializable> RemoteHttpCacheService<K, V> createRemoteHttpCacheService( ICompositeCacheManager cacheManager )
    {
        Properties props = cacheManager.getConfigurationProperties();
        ICacheEventLogger cacheEventLogger = configureCacheEventLogger( props );
        RemoteHttpCacheServerAttributes attributes = configureRemoteHttpCacheServerAttributes( props );

        RemoteHttpCacheService<K, V> service = new RemoteHttpCacheService<K, V>( cacheManager, attributes, cacheEventLogger );
        if ( log.isInfoEnabled() )
        {
            log.info( "Created new RemoteHttpCacheService " + service );
        }
        return service;
    }

    /**
     * Tries to get the event logger.
     * <p>
     * @param props
     * @return ICacheEventLogger
     */
    protected static ICacheEventLogger configureCacheEventLogger( Properties props )
    {
        ICacheEventLogger cacheEventLogger = AuxiliaryCacheConfigurator
            .parseCacheEventLogger( props, IRemoteHttpCacheConstants.HTTP_CACHE_SERVER_PREFIX );

        return cacheEventLogger;
    }

    /**
     * Configure.
     * <p>
     * jcs.remotehttpcache.serverattributes.ATTRIBUTENAME=ATTRIBUTEVALUE
     * <p>
     * @param prop
     * @return RemoteCacheServerAttributesconfigureRemoteCacheServerAttributes
     */
    protected static RemoteHttpCacheServerAttributes configureRemoteHttpCacheServerAttributes( Properties prop )
    {
        RemoteHttpCacheServerAttributes rcsa = new RemoteHttpCacheServerAttributes();

        // configure automatically
        PropertySetter.setProperties( rcsa, prop,
                                      IRemoteHttpCacheConstants.HTTP_CACHE_SERVER_ATTRIBUTES_PROPERTY_PREFIX + "." );

        return rcsa;
    }
}
