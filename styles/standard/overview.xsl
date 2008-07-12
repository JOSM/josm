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


<xsl:variable name="xscale">20</xsl:variable>
<xsl:variable name="yscale">10</xsl:variable>

	
	
<xsl:template name="rule">
	<xsl:param name="inpos" />
	<xsl:param name="xpos" />
	<xsl:param name="ypos" />

	
	<xsl:variable name="xoutpos">
		<xsl:choose>
			<xsl:when test="$xpos &gt;= 100"><xsl:value-of select="$xpos" /></xsl:when>
			<xsl:when test="$xpos &gt;= 10">0<xsl:value-of select="$xpos" /></xsl:when>
			<xsl:otherwise>00<xsl:value-of select="$xpos" /></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:variable name="youtpos">
		<xsl:choose>
			<xsl:when test="$ypos &gt;= 100"><xsl:value-of select="$ypos" /></xsl:when>
			<xsl:when test="$ypos &gt;= 10">0<xsl:value-of select="$ypos" /></xsl:when>
			<xsl:otherwise>00<xsl:value-of select="$ypos" /></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:element name="node">
		<xsl:attribute name="id">-<xsl:value-of select="$xpos" />000<xsl:value-of select="$ypos" /></xsl:attribute>
		<xsl:attribute name="user">overview-creator</xsl:attribute>
		<xsl:attribute name="visible">true</xsl:attribute>
		<xsl:attribute name="lat">-0.0<xsl:value-of select="$youtpos" /></xsl:attribute>
		<xsl:attribute name="lon">0.0<xsl:value-of select="$xoutpos" /></xsl:attribute>

		<xsl:element name="tag">
			<xsl:attribute name="k"><xsl:value-of select = "rule[$inpos]/condition/@k" /></xsl:attribute>
			<xsl:attribute name="v"><xsl:value-of select = "rule[$inpos]/condition/@v" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="tag">
			<xsl:attribute name="k">name</xsl:attribute>
			<xsl:attribute name="v"><xsl:value-of select = "rule[$inpos]/condition/@v" /></xsl:attribute>
		</xsl:element>
	</xsl:element>
</xsl:template>


<xsl:template name="posed_rules">

	<xsl:param name="key"/>
	<xsl:param name="inpos"/>
	<xsl:param name="xpos"/>
	<xsl:param name="ypos"/>
	
	<xsl:if test="rule[$inpos]/condition/@k=$key">
		<xsl:call-template name="rule">
			<xsl:with-param name="inpos" select="$inpos"/>
			<xsl:with-param name="xpos" select="$xpos"/>
			<xsl:with-param name="ypos" select="$ypos"/>
		</xsl:call-template>

		<!-- recursive call - increasing in- and output counters -->
	    <xsl:call-template name="posed_rules">
			<xsl:with-param name="key" select="$key"/>
			<xsl:with-param name="inpos" select="$inpos + 1"/>
			<xsl:with-param name="xpos" select="$xpos"/>
			<xsl:with-param name="ypos" select="$ypos + $yscale"/>
	    </xsl:call-template>
	</xsl:if>

	<xsl:if test="rule[$inpos]/condition/@k!=$key">
		<!-- recursive call - only increasing the in-counter -->
	    <xsl:call-template name="posed_rules">
			<xsl:with-param name="key" select="$key"/>
			<xsl:with-param name="inpos" select="$inpos + 1"/>
			<xsl:with-param name="xpos" select="$xpos"/>
			<xsl:with-param name="ypos" select="$ypos"/>
	    </xsl:call-template>
	</xsl:if>
	
</xsl:template>


<xsl:template name="key_rules">
	<xsl:param name="key"/>
	<xsl:param name="xpos"/>


	<xsl:variable name="xoffset"><xsl:value-of select="number($xpos*$xscale)" /></xsl:variable>

	
	<!-- header item -->
	<xsl:variable name="xoutpos">
		<xsl:choose>
			<xsl:when test="$xoffset &gt;= 100"><xsl:value-of select="$xoffset" /></xsl:when>
			<xsl:when test="$xoffset &gt;= 10">0<xsl:value-of select="$xoffset" /></xsl:when>
			<xsl:otherwise>00<xsl:value-of select="$xoffset" /></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:element name="node">
		<xsl:attribute name="id">-<xsl:value-of select="$xoffset" />0000</xsl:attribute>
		<xsl:attribute name="user">overview-creator</xsl:attribute>
		<xsl:attribute name="visible">true</xsl:attribute>
		<xsl:attribute name="lat">-0.0</xsl:attribute>
		<xsl:attribute name="lon">0.0<xsl:value-of select="$xoutpos" /></xsl:attribute>
		
		<xsl:element name="tag">
			<xsl:attribute name="k"><xsl:value-of select = "'tourism'" /></xsl:attribute>
			<xsl:attribute name="v"><xsl:value-of select = "'attraction'" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="tag">
			<xsl:attribute name="k">name</xsl:attribute>
			<xsl:attribute name="v"><xsl:value-of select = "$key" /></xsl:attribute>
		</xsl:element>
	</xsl:element>
	
	<!-- key related items -->
    <xsl:call-template name="posed_rules">
		<xsl:with-param name="key" select="$key"/>
		<xsl:with-param name="inpos" select="1"/>
		<xsl:with-param name="xpos" select="$xoffset"/>
		<xsl:with-param name="ypos" select="$yscale"/>
    </xsl:call-template>
</xsl:template>
	

<xsl:template match="rules">

	<xsl:element name="osm">
	<xsl:attribute name="version">0.5</xsl:attribute>
	<xsl:attribute name="generator">overview-creator.xslt</xsl:attribute>
	
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'highway'"/>
		<xsl:with-param name="xpos" select="1"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'cycleway'"/>
		<xsl:with-param name="xpos" select="2"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'tracktype'"/>
		<xsl:with-param name="xpos" select="3"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'waterway'"/>
		<xsl:with-param name="xpos" select="4"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'railway'"/>
		<xsl:with-param name="xpos" select="5"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'aeroway'"/>
		<xsl:with-param name="xpos" select="6"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'aerialway'"/>
		<xsl:with-param name="xpos" select="7"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'power'"/>
		<xsl:with-param name="xpos" select="8"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'man_made'"/>
		<xsl:with-param name="xpos" select="9"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'leisure'"/>
		<xsl:with-param name="xpos" select="10"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'amenity'"/>
		<xsl:with-param name="xpos" select="11"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'shop'"/>
		<xsl:with-param name="xpos" select="12"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'tourism'"/>
		<xsl:with-param name="xpos" select="13"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'historic'"/>
		<xsl:with-param name="xpos" select="14"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'landuse'"/>
		<xsl:with-param name="xpos" select="15"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'military'"/>
		<xsl:with-param name="xpos" select="16"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'natural'"/>
		<xsl:with-param name="xpos" select="17"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'route'"/>
		<xsl:with-param name="xpos" select="18"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'boundary'"/>
		<xsl:with-param name="xpos" select="19"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'sport'"/>
		<xsl:with-param name="xpos" select="20"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'abutters'"/>
		<xsl:with-param name="xpos" select="21"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'access'"/>
		<xsl:with-param name="xpos" select="22"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'bridge'"/>
		<xsl:with-param name="xpos" select="23"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'junction'"/>
		<xsl:with-param name="xpos" select="24"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'oneway'"/>
		<xsl:with-param name="xpos" select="24"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'place'"/>
		<xsl:with-param name="xpos" select="25"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'route'"/>
		<xsl:with-param name="xpos" select="26"/>
    </xsl:call-template>
	
    <xsl:call-template name="key_rules">
		<xsl:with-param name="key" select="'surface'"/>
		<xsl:with-param name="xpos" select="27"/>
    </xsl:call-template>
	
	</xsl:element>
</xsl:template>
 
</xsl:stylesheet>
