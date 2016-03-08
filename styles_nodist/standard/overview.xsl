<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:html="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="html"
>
 
    <xsl:output
        method="xml"
		indent="yes"
		encoding="UTF-8"
    />


<xsl:variable name="column-width">2</xsl:variable>
<xsl:variable name="xscale">10</xsl:variable>
<xsl:variable name="yscale">10</xsl:variable>
<xsl:variable name="areaoffset">4</xsl:variable>
<xsl:variable name="ymax">41</xsl:variable>
<xsl:variable name="ynextcol">29</xsl:variable>


<xsl:template name="node-attributes">
	<xsl:param name="id" />
	<xsl:param name="x" />
	<xsl:param name="y" />

	
	<xsl:variable name="lon">
		<xsl:choose>
			<xsl:when test="$x &gt;= 1000"><xsl:value-of select="$x" /></xsl:when>
			<xsl:when test="$x &gt;= 100">0<xsl:value-of select="$x" /></xsl:when>
			<xsl:when test="$x &gt;= 10">00<xsl:value-of select="$x" /></xsl:when>
			<xsl:otherwise>000<xsl:value-of select="$x" /></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<xsl:variable name="lat">
		<xsl:choose>
			<xsl:when test="$y &gt;= 1000"><xsl:value-of select="$y" /></xsl:when>
			<xsl:when test="$y &gt;= 100">0<xsl:value-of select="$y" /></xsl:when>
			<xsl:when test="$y &gt;= 10">00<xsl:value-of select="$y" /></xsl:when>
			<xsl:otherwise>000<xsl:value-of select="$y" /></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	

	<xsl:attribute name="id"><xsl:value-of select="$id" /></xsl:attribute>
	<xsl:attribute name="user">overview-creator</xsl:attribute>
	<xsl:attribute name="visible">true</xsl:attribute>
	<xsl:attribute name="lat">-0.0<xsl:value-of select="$lat" /></xsl:attribute>
	<xsl:attribute name="lon">0.0<xsl:value-of select="$lon" /></xsl:attribute>

	<!-- debugging
	<xsl:element name="tag">
		<xsl:attribute name="k">x</xsl:attribute>
		<xsl:attribute name="v"><xsl:value-of select="$x" /></xsl:attribute>
	</xsl:element>
	
	<xsl:element name="tag">
		<xsl:attribute name="k">y</xsl:attribute>
		<xsl:attribute name="v"><xsl:value-of select="$y" /></xsl:attribute>
	</xsl:element>
	-->

	<!-- debugging
	<xsl:element name="tag">
		<xsl:attribute name="k">lat</xsl:attribute>
		<xsl:attribute name="v"><xsl:value-of select="$lat" /></xsl:attribute>
	</xsl:element>
	
	<xsl:element name="tag">
		<xsl:attribute name="k">lon</xsl:attribute>
		<xsl:attribute name="v"><xsl:value-of select="$lon" /></xsl:attribute>
	</xsl:element>
	-->
	
</xsl:template>

	
	
<xsl:template name="rule">
	<xsl:param name="index" />
	<xsl:param name="xpos" />
	<xsl:param name="ypos" />

	
	<xsl:variable name="xoffset"><xsl:value-of select="number( ($xpos) * ($xscale + $column-width*$xscale) )" /></xsl:variable>
	<xsl:variable name="yoffset"><xsl:value-of select="number($ypos*$yscale)" /></xsl:variable>
	<xsl:variable name="idbase"><xsl:value-of select="number($xpos * 20000 + $ypos * 200)" /></xsl:variable>

	

	<!-- ICON example -->
	<xsl:element name="node">
		<xsl:call-template name="node-attributes">
			<xsl:with-param name="id" select="-number($idbase + 0)"/>
			<xsl:with-param name="x" select="number( 0*$xscale + $xoffset )"/>
			<xsl:with-param name="y" select="$yoffset"/>
		</xsl:call-template>

		<xsl:if test="rule[$index]/condition/@v">
			<xsl:element name="tag">
				<xsl:attribute name="k"><xsl:value-of select = "rule[$index]/condition/@k" /></xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@v" /></xsl:attribute>
			</xsl:element>
			<xsl:element name="tag">
				<xsl:attribute name="k">name</xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@v" /></xsl:attribute>
			</xsl:element>
		</xsl:if>
		<xsl:if test="rule[$index]/condition/@b">
			<xsl:element name="tag">
				<xsl:attribute name="k"><xsl:value-of select = "rule[$index]/condition/@k" /></xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@b" /></xsl:attribute>
			</xsl:element>
			<xsl:element name="tag">
				<xsl:attribute name="k">name</xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@b" /></xsl:attribute>
			</xsl:element>
		</xsl:if>
		<xsl:if test="not(rule[$index]/condition/@b) and not(rule[$index]/condition/@v)">
			<xsl:element name="tag">
				<xsl:attribute name="k"><xsl:value-of select = "rule[$index]/condition/@k" /></xsl:attribute>
				<xsl:attribute name="v">any</xsl:attribute>
			</xsl:element>
			<xsl:element name="tag">
				<xsl:attribute name="k">name</xsl:attribute>
				<xsl:attribute name="v">(any)</xsl:attribute>
			</xsl:element>
		</xsl:if>
	</xsl:element>


	<!-- AREA / LINE example -->
	
	<xsl:element name="node">
		<xsl:call-template name="node-attributes">
			<xsl:with-param name="id" select="-number($idbase + 1)"/>
			<xsl:with-param name="x" select="number($xoffset - $areaoffset)"/>
			<xsl:with-param name="y" select="number($yoffset - $areaoffset)"/>
		</xsl:call-template>
		<!--<xsl:element name="tag">
			<xsl:attribute name="k">pos</xsl:attribute>
			<xsl:attribute name="v">nw</xsl:attribute>
		</xsl:element>-->
	</xsl:element>
	
	<xsl:element name="node">
		<xsl:call-template name="node-attributes">
			<xsl:with-param name="id" select="-number($idbase + 2)"/>
			<xsl:with-param name="x" select="number($xoffset + $areaoffset + 2*$xscale)"/>
			<xsl:with-param name="y" select="number($yoffset - $areaoffset)"/>
		</xsl:call-template>
		<!--<xsl:element name="tag">
			<xsl:attribute name="k">pos</xsl:attribute>
			<xsl:attribute name="v">ne</xsl:attribute>
		</xsl:element>-->
	</xsl:element>
	
	<xsl:element name="node">
		<xsl:call-template name="node-attributes">
			<xsl:with-param name="id" select="-number($idbase + 3)"/>
			<xsl:with-param name="x" select="number($xoffset + $areaoffset + 2*$xscale)"/>
			<xsl:with-param name="y" select="number($yoffset + $areaoffset)"/>
		</xsl:call-template>
		<!--<xsl:element name="tag">
			<xsl:attribute name="k">pos</xsl:attribute>
			<xsl:attribute name="v">se</xsl:attribute>
		</xsl:element>-->
	</xsl:element>
	
	<xsl:element name="node">
		<xsl:call-template name="node-attributes">
			<xsl:with-param name="id" select="-number($idbase + 4)"/>
			<xsl:with-param name="x" select="number($xoffset - $areaoffset)"/>
			<xsl:with-param name="y" select="number($yoffset + $areaoffset)"/>
		</xsl:call-template>
		<!--<xsl:element name="tag">
			<xsl:attribute name="k">pos</xsl:attribute>
			<xsl:attribute name="v">sw</xsl:attribute>
		</xsl:element>-->
	</xsl:element>
	
	
	<xsl:element name="way">
		<xsl:attribute name="id"><xsl:value-of select="-number($idbase + 4)" /></xsl:attribute>
		<xsl:attribute name="user">overview-creator</xsl:attribute>
		<xsl:attribute name="visible">true</xsl:attribute>

		<xsl:element name="nd">
			<xsl:attribute name="ref"><xsl:value-of select = "-number($idbase + 1)" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="nd">
			<xsl:attribute name="ref"><xsl:value-of select = "-number($idbase + 2)" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="nd">
			<xsl:attribute name="ref"><xsl:value-of select = "-number($idbase + 3)" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="nd">
			<xsl:attribute name="ref"><xsl:value-of select = "-number($idbase + 4)" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="nd">
			<xsl:attribute name="ref"><xsl:value-of select = "-number($idbase + 1)" /></xsl:attribute>
		</xsl:element>
		
		<xsl:if test="rule[$index]/condition/@v">
			<xsl:element name="tag">
				<xsl:attribute name="k"><xsl:value-of select = "rule[$index]/condition/@k" /></xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@v" /></xsl:attribute>
			</xsl:element>
			<xsl:element name="tag">
				<xsl:attribute name="k">name</xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@v" /></xsl:attribute>
			</xsl:element>
		</xsl:if>
		<xsl:if test="rule[$index]/condition/@b">
			<xsl:element name="tag">
				<xsl:attribute name="k"><xsl:value-of select = "rule[$index]/condition/@k" /></xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@b" /></xsl:attribute>
			</xsl:element>
			<xsl:element name="tag">
				<xsl:attribute name="k">name</xsl:attribute>
				<xsl:attribute name="v"><xsl:value-of select = "rule[$index]/condition/@b" /></xsl:attribute>
			</xsl:element>
		</xsl:if>
		<xsl:if test="not(rule[$index]/condition/@b) and not(rule[$index]/condition/@v)">
			<xsl:element name="tag">
				<xsl:attribute name="k"><xsl:value-of select = "rule[$index]/condition/@k" /></xsl:attribute>
				<xsl:attribute name="v">any</xsl:attribute>
			</xsl:element>
			<xsl:element name="tag">
				<xsl:attribute name="k">name</xsl:attribute>
				<xsl:attribute name="v">any</xsl:attribute>
			</xsl:element>
		</xsl:if>
	</xsl:element>
</xsl:template>


<xsl:template name="posed_rules">

	<xsl:param name="key"/>
	<xsl:param name="index"/>
	<xsl:param name="xpos"/>
	<xsl:param name="ypos"/>
	
	<xsl:if test="rule[$index]/condition/@k=$key">
		<xsl:call-template name="rule">
			<xsl:with-param name="index" select="$index"/>
			<xsl:with-param name="xpos" select="$xpos"/>
			<xsl:with-param name="ypos" select="$ypos"/>
		</xsl:call-template>

		<xsl:choose>
			<xsl:when test="$ypos &lt; $ymax">
				<!-- recursive call - increasing index and output counters -->
			    <xsl:call-template name="posed_rules">
					<xsl:with-param name="key" select="$key"/>
					<xsl:with-param name="index" select="$index + 1"/>
					<xsl:with-param name="xpos" select="$xpos"/>
					<xsl:with-param name="ypos" select="$ypos + 1"/>
			    </xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<!-- recursive call - increasing index and output counters -->
			    <xsl:call-template name="posed_rules">
					<xsl:with-param name="key" select="$key"/>
					<xsl:with-param name="index" select="$index + 1"/>
					<xsl:with-param name="xpos" select="$xpos + 1"/>
					<xsl:with-param name="ypos" select="$ynextcol"/>
			    </xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:if>

	<xsl:if test="rule[$index]/condition/@k!=$key">
		<!-- recursive call - only increasing the index counter -->
	    <xsl:call-template name="posed_rules">
			<xsl:with-param name="key" select="$key"/>
			<xsl:with-param name="index" select="$index + 1"/>
			<xsl:with-param name="xpos" select="$xpos"/>
			<xsl:with-param name="ypos" select="$ypos"/>
	    </xsl:call-template>
	</xsl:if>
	
</xsl:template>


<xsl:template name="topic">

	<xsl:param name="key"/>
	<xsl:param name="row"/>
	<xsl:param name="column"/>


	<xsl:variable name="xoffset"><xsl:value-of select="number( ($column) * ($xscale + $column-width*$xscale) )" /></xsl:variable>
	<xsl:variable name="yoffset"><xsl:value-of select="number($row*$yscale)" /></xsl:variable>
	<xsl:variable name="idbase"><xsl:value-of select="number($column * 20000 + $row * 200)" /></xsl:variable>

	
	<!-- header item -->
	<xsl:element name="node">
		<xsl:call-template name="node-attributes">
			<xsl:with-param name="id" select="-number($idbase)"/>
			<xsl:with-param name="x" select="number($xoffset)"/>
			<xsl:with-param name="y" select="number($yoffset)"/>
		</xsl:call-template>
		
		<xsl:element name="tag">
			<xsl:attribute name="k"><xsl:value-of select = "'tourism'" /></xsl:attribute>
			<xsl:attribute name="v"><xsl:value-of select = "'information'" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="tag">
			<xsl:attribute name="k">name</xsl:attribute>
			<xsl:attribute name="v"><xsl:value-of select = "$key" /></xsl:attribute>
		</xsl:element>
	</xsl:element>
	
	<!-- key related items -->
    <xsl:call-template name="posed_rules">
		<xsl:with-param name="key" select="$key"/>
		<xsl:with-param name="index" select="1"/>
		<xsl:with-param name="xpos" select="$column"/>
		<xsl:with-param name="ypos" select="number($row + 1)"/>
    </xsl:call-template>
</xsl:template>
	

<xsl:template match="rules">

	<xsl:comment>DO NOT EDIT! THIS FILE IS GENERATED!!!</xsl:comment>
	<xsl:element name="osm">
	<xsl:attribute name="version">0.6</xsl:attribute>
	<xsl:attribute name="generator">overview-creator.xslt</xsl:attribute>
	
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'highway'"/>
		<xsl:with-param name="column" select="1"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>

    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'cycleway'"/>
		<xsl:with-param name="column" select="2"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'tracktype'"/>
		<xsl:with-param name="column" select="2"/>
		<xsl:with-param name="row" select="8"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'direction'"/>
		<xsl:with-param name="column" select="2"/>
		<xsl:with-param name="row" select="15"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'junction'"/>
		<xsl:with-param name="column" select="2"/>
		<xsl:with-param name="row" select="17"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'restriction'"/>
		<xsl:with-param name="column" select="2"/>
		<xsl:with-param name="row" select="20"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'tunnel'"/>
		<xsl:with-param name="column" select="2"/>
		<xsl:with-param name="row" select="36"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'mountain_pass'"/>
		<xsl:with-param name="column" select="2"/>
		<xsl:with-param name="row" select="40"/>
    </xsl:call-template>
    
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'barrier'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="28"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'access'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'bicycle'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="3"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'foot'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="6"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'goods'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="10"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'hgv'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="12"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'horse'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="14"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'motorcycle'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="17"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'motorcar'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="19"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'psv'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="21"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'motorboat'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="23"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'boat'"/>
		<xsl:with-param name="column" select="3"/>
		<xsl:with-param name="row" select="25"/>
    </xsl:call-template>
	
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'noexit'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'maxweight'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="3"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'maxheight'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="5"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'maxwidth'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="7"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'maxlength'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="9"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'maxspeed'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="11"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'minspeed'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="13"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'maxstay'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="15"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'toll'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="17"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'oneway'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="19"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'traffic_sign'"/>
		<xsl:with-param name="column" select="4"/>
		<xsl:with-param name="row" select="21"/>
    </xsl:call-template>
		
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'railway'"/>
		<xsl:with-param name="column" select="5"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'service'"/>
		<xsl:with-param name="column" select="5"/>
		<xsl:with-param name="row" select="21"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'bridge'"/>
		<xsl:with-param name="column" select="5"/>
		<xsl:with-param name="row" select="26"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'route'"/>
		<xsl:with-param name="column" select="5"/>
		<xsl:with-param name="row" select="33"/>
    </xsl:call-template>
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'aeroway'"/>
		<xsl:with-param name="column" select="6"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'aerialway'"/>
		<xsl:with-param name="column" select="6"/>
		<xsl:with-param name="row" select="11"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'piste:difficulty'"/>
		<xsl:with-param name="column" select="6"/>
		<xsl:with-param name="row" select="18"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'waterway'"/>
		<xsl:with-param name="column" select="6"/>
		<xsl:with-param name="row" select="26"/>
    </xsl:call-template>

	
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'amenity'"/>
		<xsl:with-param name="column" select="8"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'leisure'"/>
		<xsl:with-param name="column" select="9"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'parking'"/>
		<xsl:with-param name="column" select="9"/>
		<xsl:with-param name="row" select="22"/>
    </xsl:call-template>
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'religion'"/>
		<xsl:with-param name="column" select="10"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
<xsl:call-template name="topic">
	<xsl:with-param name="key" select="'information'"/>
	<xsl:with-param name="column" select="11"/>
	<xsl:with-param name="row" select="33"/>
</xsl:call-template>
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'man_made'"/>
		<xsl:with-param name="column" select="11"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
<xsl:call-template name="topic">
	<xsl:with-param name="key" select="'power'"/>
	<xsl:with-param name="column" select="10"/>
	<xsl:with-param name="row" select="20"/>
</xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'power_source'"/>
		<xsl:with-param name="column" select="11"/>
		<xsl:with-param name="row" select="20"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'vending'"/>
		<xsl:with-param name="column" select="11"/>
		<xsl:with-param name="row" select="40"/>
    </xsl:call-template>

    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'shop'"/>
		<xsl:with-param name="column" select="12"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'tourism'"/>
		<xsl:with-param name="column" select="13"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>		
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'historic'"/>
		<xsl:with-param name="column" select="13"/>
		<xsl:with-param name="row" select="19"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'military'"/>
		<xsl:with-param name="column" select="13"/>
		<xsl:with-param name="row" select="36"/>
    </xsl:call-template>
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'sport'"/>
		<xsl:with-param name="column" select="14"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>



    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'landuse'"/>
		<xsl:with-param name="column" select="16"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'building'"/>
		<xsl:with-param name="column" select="16"/>
		<xsl:with-param name="row" select="28"/>
    </xsl:call-template>	
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'boundary'"/>
		<xsl:with-param name="column" select="16"/>
		<xsl:with-param name="row" select="34"/>
    </xsl:call-template>
	
    <xsl:call-template name="topic">
		<xsl:with-param name="key" select="'place'"/>
		<xsl:with-param name="column" select="17"/>
		<xsl:with-param name="row" select="22"/>
    </xsl:call-template>
	<xsl:call-template name="topic">
		<xsl:with-param name="key" select="'natural'"/>
		<xsl:with-param name="column" select="17"/>
		<xsl:with-param name="row" select="1"/>
    </xsl:call-template>
	
	</xsl:element>
</xsl:template>
 
</xsl:stylesheet>
