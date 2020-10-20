#!/bin/bash

set -Eeou pipefail

# Don't show one time passwords
set +x

SIGNING_KEY_NAME="Developer ID Application: FOSSGIS e.V. (P8AAAGN2AM)"
IMPORT_AND_UNLOCK_KEYCHAIN=${IMPORT_AND_UNLOCK_KEYCHAIN:-1}

if [ -z "${1-}" ]
then
    echo "Usage: $0 josm_revision"
    exit 1
fi

echo "Building JOSM.app"

mkdir app

if [[ $IMPORT_AND_UNLOCK_KEYCHAIN == 1 ]]; then
    if [ -z "$CERT_MACOS_P12" ]
    then
        echo "CERT_MACOS_P12 must be set in the environment. Won't sign app."
        exit 1
    fi


    if [ -z "$CERT_MACOS_PW" ]
    then
        echo "CERT_MACOS_P12 must be set in the environment. Won't sign app."
        exit 1
    fi

    echo "Preparing certificates/keychain for signing…"

    KEYCHAIN=build.keychain
    KEYCHAINPATH=~/Library/Keychains/$KEYCHAIN-db
    KEYCHAIN_PW=`head /dev/urandom | base64 | head -c 20`
    CERTIFICATE_P12=certificate.p12

    echo $CERT_MACOS_P12 | base64 --decode > $CERTIFICATE_P12
    security create-keychain -p $KEYCHAIN_PW $KEYCHAIN
    security default-keychain -s $KEYCHAIN
    security unlock-keychain -p $KEYCHAIN_PW $KEYCHAIN
    security import $CERTIFICATE_P12 -k $KEYCHAIN -P $CERT_MACOS_PW -T /usr/bin/codesign
    security set-key-partition-list -S apple-tool:,apple: -s -k $KEYCHAIN_PW $KEYCHAIN
    rm $CERTIFICATE_P12

    echo "Signing preparation done."
fi

echo "Building and signin app"
    jpackage -n "JOSM" --input dist --main-jar josm-custom.jar \
    --main-class org.openstreetmap.josm.gui.MainApplication \
    --icon ./native/macosx/JOSM.icns --type app-image --dest app \
    --java-options "-Xmx8192m" --app-version $1 \
    --copyright "JOSM, and all its integral parts, are released under the GNU General Public License v2 or later" \
    --vendor "https://josm.openstreetmap.de" \
    --mac-sign \
    --mac-package-identifier de.openstreetmap.josm \
    --mac-package-signing-prefix de.openstreetmap.josm \
    --mac-signing-key-user-name "$SIGNING_KEY_NAME" \
    --mac-signing-keychain $KEYCHAIN \
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

echo "Signing App Bundle…"

# codesign -vvv --timestamp --options runtime --deep --force --sign "$SIGNING_KEY_NAME" \
#     app/JOSM.app/Contents/MacOS/JOSM \
#     app/JOSM.app/Contents/runtime/Contents/Home/lib/*.jar \
#     app/JOSM.app/Contents/runtime/Contents/Home/lib/*.dylib \
#     app/JOSM.app/Contents/runtime/Contents/MacOS/libjli.dylib

# codesign -vvv --timestamp --entitlements native/macosx/josm.entitlements --options runtime --force --sign "$SIGNING_KEY_NAME" app/JOSM.app

# codesign -vvv app/JOSM.app

echo "Preparing for notarization"
ditto -c -k --zlibCompressionLevel 9 --keepParent app/JOSM.app app/JOSM.zip

echo "Uploading to Apple"
xcrun altool --notarize-app -f app/JOSM.zip -p "$APPLE_ID_PW" -u "thomas.skowron@fossgis.de" --primary-bundle-id de.openstreetmap.josm
