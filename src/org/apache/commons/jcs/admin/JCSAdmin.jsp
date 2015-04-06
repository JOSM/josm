<%--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
--%>
<%@page import="org.apache.commons.jcs.JCS"%>
<%@page import="org.apache.commons.jcs.access.CacheAccess" %>
<%@page import="org.apache.commons.jcs.admin.CacheElementInfo" %>
<%@page import="org.apache.commons.jcs.admin.CacheRegionInfo" %>
<%@page import="java.io.Serializable" %>
<%@page import="java.util.HashMap" %>

<jsp:useBean id="jcsBean" scope="request" class="org.apache.commons.jcs.admin.JCSAdminBean" />

<html>
<head>

<SCRIPT LANGUAGE="Javascript">
  function decision( message, url )
  {
    if( confirm(message) )
    {
      location.href = url;
    }
  }
</SCRIPT>

<title> JCS Admin Servlet </title>

</head>

<body>

<%
			String CACHE_NAME_PARAM = "cacheName";
			String ACTION_PARAM = "action";
		 	String CLEAR_ALL_REGIONS_ACTION = "clearAllRegions";
		 	String CLEAR_REGION_ACTION = "clearRegion";
		 	String REMOVE_ACTION = "remove";
		 	String DETAIL_ACTION = "detail";
		 	String REGION_SUMMARY_ACTION = "regionSummary";
		 	String ITEM_ACTION = "item";
			String KEY_PARAM = "key";
			String SILENT_PARAM = "silent";

     		String DEFAULT_TEMPLATE_NAME = "DEFAULT";
     		String REGION_DETAIL_TEMPLATE_NAME = "DETAIL";
     		String ITEM_TEMPLATE_NAME = "ITEM";
     		String REGION_SUMMARY_TEMPLATE_NAME = "SUMMARY";

			String templateName = DEFAULT_TEMPLATE_NAME;

			HashMap<String, Object> context = new HashMap<String, Object>();

			// Get cacheName for actions from request (might be null)
			String cacheName = request.getParameter( CACHE_NAME_PARAM );

			if ( cacheName != null )
			{
			    cacheName = cacheName.trim();
			}

			// If an action was provided, handle it
			String action = request.getParameter( ACTION_PARAM );

			if ( action != null )
			{
				if ( action.equals( CLEAR_ALL_REGIONS_ACTION ) )
				{
					jcsBean.clearAllRegions();
				}
				else if ( action.equals( CLEAR_REGION_ACTION ) )
				{
					if ( cacheName == null )
					{
						// Not Allowed
					}
					else
					{
						jcsBean.clearRegion( cacheName );
					}
				}
				else if ( action.equals( REMOVE_ACTION ) )
				{
					String[] keys = request.getParameterValues( KEY_PARAM );

					for ( int i = 0; i < keys.length; i++ )
					{
						jcsBean.removeItem( cacheName, keys[ i ] );
					}

					templateName = REGION_DETAIL_TEMPLATE_NAME;
				}
				else if ( action.equals( DETAIL_ACTION ) )
				{
					templateName = REGION_DETAIL_TEMPLATE_NAME;
				}
				else if ( action.equals( ITEM_ACTION ) )
				{
					templateName = ITEM_TEMPLATE_NAME;
				}
				else if ( action.equals( REGION_SUMMARY_ACTION ) )
				{
					templateName = REGION_SUMMARY_TEMPLATE_NAME;
				}
			}

			if ( request.getParameter( SILENT_PARAM ) != null )
			{
				// If silent parameter was passed, no output should be produced.
				//return null;
			}
			else
			{
				// Populate the context based on the template
				if ( templateName == REGION_DETAIL_TEMPLATE_NAME )
				{
					//context.put( "cacheName", cacheName );
					context.put( "elementInfoRecords", jcsBean.buildElementInfo( cacheName ) );
				}
				else if ( templateName == DEFAULT_TEMPLATE_NAME )
				{
					context.put( "cacheInfoRecords", jcsBean.buildCacheInfo() );
				}
			}

///////////////////////////////////////////////////////////////////////////////////
			//handle display

			if ( templateName == ITEM_TEMPLATE_NAME )
			{
			    String key = request.getParameter( KEY_PARAM );

			    if ( key != null )
			    {
			        key = key.trim();
			    }

			    CacheAccess<Serializable, Serializable> cache = JCS.getInstance( cacheName );
				org.apache.commons.jcs.engine.behavior.ICacheElement<?, ?> element = cache.getCacheElement( key );
%>
<h1> Item for key [<%=key%>] in region [<%=cacheName%>] </h1>

<a href="JCSAdmin.jsp?action=detail&cacheName=<%=cacheName%>">Region Detail</a>
| <a href="JCSAdmin.jsp">All Regions</a>

  <pre>
	<%=element%>
  </pre>
<%
			}
			else if ( templateName == REGION_SUMMARY_TEMPLATE_NAME )
			{
%>

<h1> Summary for region [<%=cacheName%>] </h1>

<a href="JCSAdmin.jsp">All Regions</a>

<%
    CacheAccess<?, ?> cache = JCS.getInstance( cacheName );
    String stats = cache.getStats();
%>

    <br>
<b> Stats for region [<%=cacheName%>] </b>

    <pre>
    	<%=stats%>
    </pre>

<%
			}
			else if ( templateName == REGION_DETAIL_TEMPLATE_NAME )
			{
%>

<h1> Detail for region [<%=cacheName%>] </h1>

<a href="JCSAdmin.jsp">All Regions</a>

<table border="1" cellpadding="5" >
    <tr>
        <th> Key </th>
        <th> Eternal? </th>
        <th> Create time </th>
        <th> Max Life (s) </th>
        <th> Till Expiration (s) </th>
    </tr>
<%
	CacheElementInfo[] list = (CacheElementInfo[]) context.get( "elementInfoRecords" );
    for (CacheElementInfo element : list)
    {
%>
        <tr>
            <td> <%=element.getKey()%> </td>
            <td> <%=element.isEternal()%> </td>
            <td> <%=element.getCreateTime()%> </td>
            <td> <%=element.getMaxLifeSeconds()%> </td>
            <td> <%=element.getExpiresInSeconds()%> </td>
            <td>
             <a href="JCSAdmin.jsp?action=item&cacheName=<%=cacheName%>&key=<%=element.getKey()%>"> View </a>
            | <a href="JCSAdmin.jsp?action=remove&cacheName=<%=cacheName%>&key=<%=element.getKey()%>"> Remove </a>
            </td>
        </tr>
<%
    }

    CacheAccess<?, ?> cache = JCS.getInstance( cacheName );
    String stats = cache.getStats();
%>
    </table>

    <br>
<b> Stats for region [<%=cacheName%>] </b>

    <pre>
    	<%=stats%>
    </pre>
<%
  }
  else
  {
%>

<h1> Cache Regions </h1>

<p>
These are the regions which are currently defined in the cache. 'Items' and
'Bytes' refer to the elements currently in memory (not spooled). You can clear
all items for a region by selecting 'Remove all' next to the desired region
below. You can also <a href="javascript:decision('Clicking OK will clear all the data from all regions!','JCSAdmin.jsp?action=clearAllRegions')">Clear all regions</a>
which empties the entire cache.
</p>
<p>
	<form action="JCSAdmin.jsp">
		<input type="hidden" name="action" value="item">
		Retrieve (key) <input type="text" name="key"> &nbsp;
		(region) <select name="cacheName">
<%
  CacheRegionInfo[] listSelect = (CacheRegionInfo[]) context.get( "cacheInfoRecords" );
  for (CacheRegionInfo record : listSelect)
  {
	%>
    <option value="<%=record.getCacheName()%>"><%=record.getCacheName()%></option>
	<%
  }
%>
				</select>
		<input type="submit">
	</form>
</p>

<table border="1" cellpadding="5" >
    <tr>
        <th> Cache Name </th>
        <th> Items </th>
        <th> Bytes </th>
        <th> Status </th>
        <th> Memory Hits </th>
        <th> Aux Hits </th>
        <th> Not Found Misses </th>
        <th> Expired Misses </th>
    </tr>

<%
	CacheRegionInfo[] list = (CacheRegionInfo[]) context.get( "cacheInfoRecords" );
    for (CacheRegionInfo record : listSelect)
    {
%>
        <tr>
            <td> <%=record.getCacheName()%> </td>
            <td> <%=record.getCacheSize()%> </td>
            <td> <%=record.getByteCount()%> </td>
            <td> <%=record.getCacheStatus()%> </td>
            <td> <%=record.getHitCountRam()%> </td>
            <td> <%=record.getHitCountAux()%> </td>
            <td> <%=record.getMissCountNotFound()%> </td>
            <td> <%=record.getMissCountExpired()%> </td>
            <td>
                <a href="JCSAdmin.jsp?action=regionSummary&cacheName=<%=record.getCacheName()%>"> Summary </a>
                | <a href="JCSAdmin.jsp?action=detail&cacheName=<%=record.getCacheName()%>"> Detail </a>
                | <a href="javascript:decision('Clicking OK will remove all the data from the region [<%=record.getCacheName()%>]!','JCSAdmin.jsp?action=clearRegion&cacheName=<%=record.getCacheName()%>')"> Clear </a>
            </td>
        </tr>
<%
    }
%>
    </table>
<%
  }
%>


</body>

</html>
