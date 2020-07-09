#!/bin/bash

set -Eeuxo pipefail


if [ -z "$1" ]
then
    echo "Usage: $0 josm_revision"
else
    jpackage -n "JOSM" --input dist --main-jar josm-custom.jar \
        --main-class org.openstreetmap.josm.gui.MainApplication \
        --icon ./native/macosx/JOSM.icns --type app-image --dest dist \
        --java-options "-Xmx8192m" --app-version $1 \
        --copyright "JOSM, and all its integral parts, are released under the GNU General Public License v2 or later" \
        --vendor "https://josm.openstreetmap.de" \
        --file-associations native/macosx/bz2.properties \
        --file-associations native/macosx/geojson.properties \
        --file-associations native/macosx/gpx.properties \
        --file-associations native/macosx/gz.properties \
        --file-associations native/macosx/jos.properties \
        --file-associations native/macosx/joz.properties \
        --file-associations native/macosx/osm.properties \
        --file-associations native/macosx/zip.properties \
        --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.jfr,jdk.jsobject,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom
fi