package org.apache.commons.jcs.admin.servlet;

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

import org.apache.commons.jcs.admin.JCSAdminBean;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.VelocityViewServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A servlet which provides HTTP access to JCS. Allows a summary of regions to
 * be viewed, and removeAll to be run on individual regions or all regions. Also
 * provides the ability to remove items (any number of key arguments can be
 * provided with action 'remove'). Should be initialized with a properties file
 * that provides at least a classpath resource loader. Since this extends
 * VelocityServlet, which uses the singleton model for velocity, it will share
 * configuration with any other Velocity in the same JVM.
 * <p>
 * Initialization in a webapp will look something like this:
 * <p>
 *
 * <pre>
 *
 *    [servlet]
 *        [servlet-name]JCSAdminServlet[/servlet-name]
 *        [servlet-class]org.apache.commons.jcs.admin.servlet.JCSAdminServlet[/servlet-class]
 *        [init-param]
 *            [param-name]properties[/param-name]
 *            [param-value]WEB-INF/conf/JCSAdminServlet.velocity.properties[/param-value]
 *        [/init-param]
 *    [/servlet]
 *
 * </pre>
 *
 * <p>
 * FIXME: It would be nice to use the VelocityEngine model so this can be truly
 * standalone. Right now if you run it in the same container as, say, turbine,
 * turbine must be run first to ensure it's config takes precedence.
 * <p>
 */
public class JCSAdminServlet
    extends VelocityViewServlet
{
    private static final long serialVersionUID = -5519844149238645275L;

    private static final String DEFAULT_TEMPLATE_NAME = "/org/apache/jcs/admin/servlet/JCSAdminServletDefault.vm";

    private static final String REGION_DETAIL_TEMPLATE_NAME = "/org/apache/jcs/admin/servlet/JCSAdminServletRegionDetail.vm";

    // Keys for parameters

    private static final String CACHE_NAME_PARAM = "cacheName";

    private static final String ACTION_PARAM = "action";

    private static final String KEY_PARAM = "key";

    private static final String SILENT_PARAM = "silent";

    // Possible values for 'action' parameter

    private static final String CLEAR_ALL_REGIONS_ACTION = "clearAllRegions";

    private static final String CLEAR_REGION_ACTION = "clearRegion";

    private static final String REMOVE_ACTION = "remove";

    private static final String DETAIL_ACTION = "detail";

    /**
     * Velocity based admin servlet.
     * <p>
     * @param request
     * @param response
     * @param context
     * @return Template
     *
     */
    @Override
    protected Template handleRequest( HttpServletRequest request, HttpServletResponse response, Context context )
    {
        JCSAdminBean admin = new JCSAdminBean();

        String templateName = DEFAULT_TEMPLATE_NAME;

        // Get cacheName for actions from request (might be null)

        String cacheName = request.getParameter( CACHE_NAME_PARAM );

        // If an action was provided, handle it

        String action = request.getParameter( ACTION_PARAM );

        try
        {
			if ( action != null )
			{
			    if ( action.equals( CLEAR_ALL_REGIONS_ACTION ) )
			    {
			        admin.clearAllRegions();
			    }
			    else if ( action.equals( CLEAR_REGION_ACTION ) )
			    {
			        if ( cacheName != null )
			        {
			            admin.clearRegion( cacheName );
			        }
			    }
			    else if ( action.equals( REMOVE_ACTION ) )
			    {
			        String[] keys = request.getParameterValues( KEY_PARAM );

			        for ( int i = 0; i < keys.length; i++ )
			        {
			            admin.removeItem( cacheName, keys[i] );
			        }

			        templateName = REGION_DETAIL_TEMPLATE_NAME;
			    }
			    else if ( action.equals( DETAIL_ACTION ) )
			    {
			        templateName = REGION_DETAIL_TEMPLATE_NAME;
			    }
			}
		}
        catch (IOException e)
        {
        	getLog().error("Could not execute action.", e);
        	return null;
		}

        if ( request.getParameter( SILENT_PARAM ) != null )
        {
            // If silent parameter was passed, no output should be produced.

            return null;
        }
        // Populate the context based on the template

        try
        {
			if ( templateName == REGION_DETAIL_TEMPLATE_NAME )
			{
			    context.put( "cacheName", cacheName );
			    context.put( "elementInfoRecords", admin.buildElementInfo( cacheName ) );
			}
			else if ( templateName == DEFAULT_TEMPLATE_NAME )
			{
			    context.put( "cacheInfoRecords", admin.buildCacheInfo() );
			}
		}
        catch (Exception e)
        {
        	getLog().error("Could not populate context.", e);
		}

        return getTemplate( templateName );
    }
}
