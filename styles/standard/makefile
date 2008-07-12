
all: overview.osm

overview.osm: makefile elemstyles.xml overview.xsl
	xsltproc --stringparam  use.id.as.filename 1 --nonet overview.xsl elemstyles.xml > overview.osm
	cat overview.osm
	

	