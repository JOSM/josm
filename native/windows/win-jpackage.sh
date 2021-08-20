#!/bin/bash

set -Eeou pipefail

# Don't show one time passwords
set +x

if [ -z "${1-}" ]
then
    echo "Usage: $0 josm_revision"
    exit 1
fi

echo "Building JOSM.app"

mkdir app

JPACKAGEOPTIONS=""

echo "Building app"
    jpackage $JPACKAGEOPTIONS -n "JOSM" --input dist --main-jar josm-custom.jar \
    --main-class org.openstreetmap.josm.gui.MainApplication \
    --icon ./native/windows/logo.ico --type app-image --dest app \
    --java-options "--add-exports=java.base/sun.security.action=ALL-UNNAMED" \
    --java-options "--add-exports=java.desktop/com.sun.imageio.plugins.jpeg=ALL-UNNAMED" \
    --java-options "--add-exports=java.desktop/com.sun.imageio.spi=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/java.nio=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED" \
    --java-options "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED" \
    --java-options "--add-opens=java.desktop/javax.imageio.spi=ALL-UNNAMED" \
    --java-options "--add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED" \
    --java-options "--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED" \
    --app-version "$1" \
    --copyright "JOSM, and all its integral parts, are released under the GNU General Public License v2 or later" \
    --vendor "https://josm.openstreetmap.de" \
    --file-associations native/file-associations/bz2.properties \
    --file-associations native/file-associations/geojson.properties \
    --file-associations native/file-associations/gpx.properties \
    --file-associations native/file-associations/gz.properties \
    --file-associations native/file-associations/jos.properties \
    --file-associations native/file-associations/joz.properties \
    --file-associations native/file-associations/osm.properties \
    --file-associations native/file-associations/zip.properties \
    --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.jfr,jdk.jsobject,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom

echo "Building done."
