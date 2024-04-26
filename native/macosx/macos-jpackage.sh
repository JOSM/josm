#!/bin/bash

## Expected environment, passed from GitHub secrets:
# https://docs.github.com/en/free-pro-team@latest/actions/reference/encrypted-secrets
# APPLE_ID_PW     Password for the Apple ID
# CERT_MACOS_P12  Certificate used for code signing, base64 encoded
# CERT_MACOS_PW   Password for that certificate

set -Eeo pipefail

# Don't show one time passwords
set +x

IMPORT_AND_UNLOCK_KEYCHAIN=${IMPORT_AND_UNLOCK_KEYCHAIN:-1}

if [ -z "${1-}" ]
then
    echo "Usage: $0 josm_revision [other_arch_jdk]"
    exit 1
fi

echo "Building JOSM.app"

mkdir app

if [ -z "$CERT_MACOS_P12" ] || [ -z "$CERT_MACOS_PW" ] || [ -z "$APPLE_ID_PW" ] || [ -z "$APPLE_ID_TEAM" ]  || [ -z "$APPLE_ID" ]
then
    echo "CERT_MACOS_P12, CERT_MACOS_PW, APPLE_ID, APPLE_ID_PW, or APPLE_ID_TEAM are not set in the environment."
    echo "A JOSM.app will be created but not signed nor notarized."
    SIGNAPP=false
    KEYCHAINPATH=false
    JPACKAGEOPTIONS=""
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
    JPACKAGEOPTIONS="--mac-sign --mac-signing-keychain $KEYCHAINPATH"
fi

set -u

function do_jpackage() {
  echo "Building app (${JAVA_HOME})"
  # We specifically need the options to not be quoted -- we _want_ the word splitting.
  # shellcheck disable=SC2086
  "${JAVA_HOME}/bin/jpackage" $JPACKAGEOPTIONS -n "JOSM" --input dist --main-jar josm-custom.jar \
      --main-class org.openstreetmap.josm.gui.MainApplication \
      --icon ./native/macosx/JOSM.icns --type app-image --dest app \
      --java-options "--add-modules java.scripting,java.sql,javafx.controls,javafx.media,javafx.swing,javafx.web" \
      --java-options "--add-exports=java.base/sun.security.action=ALL-UNNAMED" \
      --java-options "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED" \
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
      --vendor "JOSM" \
      --mac-package-identifier de.openstreetmap.josm \
      --mac-package-signing-prefix de.openstreetmap.josm \
      --file-associations native/file-associations/bz2.properties \
      --file-associations native/file-associations/geojson.properties \
      --file-associations native/file-associations/gpx.properties \
      --file-associations native/file-associations/gz.properties \
      --file-associations native/file-associations/jos.properties \
      --file-associations native/file-associations/joz.properties \
      --file-associations native/file-associations/osm.properties \
      --file-associations native/file-associations/xz.properties \
      --file-associations native/file-associations/zip.properties \
      --add-modules java.compiler,java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.jfr,jdk.jsobject,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom,javafx.controls,javafx.media,javafx.swing,javafx.web
  echo "Building done (${JAVA_HOME})."
}
function do_signapp() {
  echo "Compressing app (${1})"
  ditto -c -k --zlibCompressionLevel 9 --keepParent "app/${1}.app" "app/${1}.zip"
  if $SIGNAPP; then
      echo "Signing app (${1})"
      echo "Preparing for notarization"
      echo "Uploading to Apple"
      xcrun notarytool submit --apple-id "$APPLE_ID" --password "$APPLE_ID_PW" --team-id "$APPLE_ID_TEAM" --wait "app/${1}.zip"
  fi
}

function merge() {
  if [ "$(command -v lipo)" ]; then
    lipo -create -output "${1}" "${2}" "${3}"
  elif [ "$(command -v llvm-lipo-15)" ]; then
    llvm-lipo-15 -create -output "${1}" "${2}" "${3}"
  fi
}

function copy() {
  # Trim the root path
  FILE="${1#*/}"
  if [ ! -e "${2}/${FILE}" ]; then
    # Only make directories if we aren't looking at the root files
    if [[ "${FILE}" == *"/"* ]]; then mkdir -p "${2}/${FILE%/*}"; fi
    if file "${1}" | grep -q 'Mach-O' ; then
      merge "${2}/${FILE}" "${3}/${FILE}" "${4}/${FILE}"
      if file "${1}" | grep -q 'executable'; then
        chmod 755 "${2}/${FILE}"
      fi
    else
      cp -a "${1}" "${2}/${FILE}"
    fi
  fi
}

function directory_iterate() {
  while IFS= read -r -d '' file
  do
    copy "${file}" "${2}" "${3}" "${4}" &
  done <   <(find "${1}" -type f,l -print0)
  wait
}

do_jpackage "${1}"
if [ -n "${2}" ]; then
  function get_name() {
    echo "$("${JAVA_HOME}/bin/java" --version | head -n1 | awk '{print $2}' | awk -F'.' '{print $1}')_$(file "${JAVA_HOME}/bin/java" | awk -F' executable ' '{print $2}')"
  }
  first="$(get_name)"
  JAVA_HOME="${2}" second="$(get_name)"
  mv app/JOSM.app "app/JOSM_${first}.app"
  JAVA_HOME="${2}" do_jpackage "${1}"
  mv app/JOSM.app "app/JOSM_${second}.app"
  mkdir app/JOSM.app
  (cd app
  directory_iterate "JOSM_${first}.app" "JOSM.app" "JOSM_${first}.app" "JOSM_${second}.app"
  directory_iterate "JOSM_${second}.app" "JOSM.app" "JOSM_${first}.app" "JOSM_${second}.app"
  )
  do_signapp "JOSM_${first}"
  do_signapp "JOSM_${second}"
  if [ "${KEYCHAINPATH}" != "false" ]; then
    function do_codesign() {
      codesign --sign "FOSSGIS e.V." \
        --force \
        --keychain "${KEYCHAINPATH}" \
        --timestamp \
        --prefix "de.openstreetmap.josm" \
        --identifier "${2}" \
        --options runtime \
        --entitlements "$(dirname "${BASH_SOURCE[0]}")/josm.entitlements" \
        --verbose=4 "${1}"
    }
    do_codesign app/JOSM.app/Contents/runtime "com.oracle.java.de.openstreetmap.josm"
    do_codesign app/JOSM.app/ "de.openstreetmap.josm"
  fi
fi
do_signapp JOSM
