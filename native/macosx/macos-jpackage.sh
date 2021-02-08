#!/bin/bash

## Expected environment, passed from GitHub secrets:
# https://docs.github.com/en/free-pro-team@latest/actions/reference/encrypted-secrets
# APPLE_ID_PW     Password for the Apple ID
# CERT_MACOS_P12  Certificate used for code signing, base64 encoded
# CERT_MACOS_PW   Password for that certificate

set -Eeou pipefail

# Don't show one time passwords
set +x

APPLE_ID="thomas.skowron@fossgis.de"
IMPORT_AND_UNLOCK_KEYCHAIN=${IMPORT_AND_UNLOCK_KEYCHAIN:-1}

if [ -z "${1-}" ]
then
    echo "Usage: $0 josm_revision"
    exit 1
fi

echo "Building JOSM.app"

mkdir app

if [ -z "$CERT_MACOS_P12" ] || [ -z "$CERT_MACOS_PW" ] || [ -z "$APPLE_ID_PW" ]
then
    echo "CERT_MACOS_P12, CERT_MACOS_PW and APPLE_ID_PW are not set in the environment."
    echo "I will create a JOSM.app but I won't attempt to sign and notarize it."
    SIGNAPP=false
else
    echo "Preparing certificates/keychain for signing…"

    KEYCHAIN=build.keychain
    KEYCHAINPATH=~/Library/Keychains/$KEYCHAIN-db
    KEYCHAIN_PW=$(head /dev/urandom | base64 | head -c 20)
    CERTIFICATE_P12=certificate.p12

    echo "$CERT_MACOS_P12" | base64 --decode > $CERTIFICATE_P12
    security create-keychain -p "$KEYCHAIN_PW" $KEYCHAIN
    security default-keychain -s $KEYCHAIN
    security unlock-keychain -p "$KEYCHAIN_PW" $KEYCHAIN
    security import $CERTIFICATE_P12 -k $KEYCHAIN -P "$CERT_MACOS_PW" -T /usr/bin/codesign
    security set-key-partition-list -S apple-tool:,apple: -s -k "$KEYCHAIN_PW" $KEYCHAIN
    rm $CERTIFICATE_P12
    SIGNAPP=true
    echo "Signing preparation done."
fi

if $SIGNAPP; then
  JPACKAGEOPTIONS="--mac-sign --mac-signing-keychain $KEYCHAINPATH"
else
  JPACKAGEOPTIONS=""
fi

echo "Building and signin app"
    jpackage $JPACKAGEOPTIONS -n "JOSM" --input dist --main-jar josm-custom.jar \
    --main-class org.openstreetmap.josm.gui.MainApplication \
    --icon ./native/macosx/JOSM.icns --type app-image --dest app \
    --java-options "-Xmx8192m" \
    --app-version "$1" \
    --copyright "JOSM, and all its integral parts, are released under the GNU General Public License v2 or later" \
    --vendor "https://josm.openstreetmap.de" \
    --mac-sign \
    --mac-package-identifier de.openstreetmap.josm \
    --mac-package-signing-prefix de.openstreetmap.josm \
    --mac-signing-keychain $KEYCHAINPATH \
    --file-associations native/macosx/bz2.properties \
    --file-associations native/macosx/geojson.properties \
    --file-associations native/macosx/gpx.properties \
    --file-associations native/macosx/gz.properties \
    --file-associations native/macosx/jos.properties \
    --file-associations native/macosx/joz.properties \
    --file-associations native/macosx/osm.properties \
    --file-associations native/macosx/zip.properties \
    --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.jfr,jdk.jsobject,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom

echo "Building done."

if $SIGNAPP; then
    echo "Preparing for notarization"
    ditto -c -k --zlibCompressionLevel 9 --keepParent app/JOSM.app app/JOSM.zip

    echo "Uploading to Apple"
    xcrun altool --notarize-app -f app/JOSM.zip -p "$APPLE_ID_PW" -u "$APPLE_ID" --primary-bundle-id de.openstreetmap.josm
fi