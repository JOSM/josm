
all: overview.osm

overview.osm: makefile elemstyles.xml overview.xsl
	xsltproc --nonet overview.xsl elemstyles.xml > overview.osm
	