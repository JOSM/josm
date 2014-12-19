#!/bin/bash

# Creates an josm-setup-xy.exe File
#
# for working on a debian-unix system install the nsis package with
# apt-get install nsis
# replace the  /usr/share/nsis/Plugins/System.dll with the Version from the nsis .zip File
# The one coming with the debian package is missing the Call:: Function
# See also /usr/share/doc/nsis/README.Debian 
#
# Then download launch4j from http://launch4j.sourceforge.net/ 
# wget http://softlayer-ams.dl.sourceforge.net/project/launch4j/launch4j-3/3.4/launch4j-3.4-linux.tgz
# and unpack it to /usr/share/launch4j

## settings ##

# trying to find launch4j
if [ -s "/cygdrive/c/Program Files (x86)/Launch4j/launch4jc.exe" ]; then
    # Windows under cygwin or MobaXterm
    LAUNCH4J="/cygdrive/c/Program Files (x86)/Launch4j/launch4jc.exe"
elif [ -s /usr/share/launch4j/launch4j.jar ]; then
    # as described above
    LAUNCH4J="java -jar /usr/share/launch4j/launch4j.jar"
elif [ -s ../launch4j/launch4j.jar ]; then
    LAUNCH4J="java -jar ../launch4j/launch4j.jar"
elif [ -s $HOME/launch4j/launch4j.jar ]; then
    LAUNCH4J="java -jar $HOME/launch4j/launch4j.jar"
else
    # launch4j installed locally under this nsis folder
    LAUNCH4J="java -jar ./launch4j/launch4j.jar"
fi
echo Using launch4j: $LAUNCH4J

# trying to find makensis
if [ -s "/cygdrive/c/Program Files (x86)/NSIS/makensis.exe" ]; then
    # Windows under cygwin or MobaXterm
    MAKENSIS="/cygdrive/c/Program Files (x86)/NSIS/makensis.exe"
else
    # UNIX like
    MAKENSIS=/usr/bin/makensis
fi
echo Using NSIS: $MAKENSIS

if [ -n "$1" ]; then
  export VERSION=$1
  export JOSM_BUILD="no"
  export WEBKIT_DOWNLAD="no"
  export JOSM_FILE="/home/josm/www/download/josm-tested.jar"
else
  svncorerevision=`svnversion -n ../core`
  #svnpluginsrevision=`svnversion -n ../plugins`
  #svnrevision="$svncorerevision-$svnpluginsrevision"

  #export VERSION=custom-${svnrevision}
  export VERSION=`echo ${svncorerevision} | sed -e 's/M//g' -e 's/S//g' -e 's/P//g'`
  export JOSM_BUILD="yes"
  export WEBKIT_DOWNLOAD="yes"
  export JOSM_FILE="..\core\dist\josm-custom.jar"
fi

echo "Creating Windows Installer for josm-$VERSION"

echo 
echo "##################################################################"
echo "### Download and unzip the webkit stuff"
if [ "x$WEBKIT_DOWNLOAD" == "xyes" ]; then
    wget --continue --timestamping http://josm.openstreetmap.de/download/windows/webkit-image.zip
else
    if ! [ -f webkit-image.zip ]; then
      ln -s /home/josm/www/download/windows/webkit-image.zip .
    fi
fi
#mkdir -p webkit-image
#cd webkit-image
unzip -o webkit-image.zip
#cd ..

echo 
echo "##################################################################"
echo "### Build the Complete josm + Plugin Stuff"
if [ "x$JOSM_BUILD" == "xyes" ]; then
    (
	echo "Build the Complete josm Stuff"
	
	echo "Compile Josm"
	cd ../core
	ant -q clean
	ant -q compile || exit -1
	cd ..
	
	echo "Compile Josm Plugins"
	cd plugins
	ant -q clean
	ant -q dist || exit -1
	) || exit -1
fi

/bin/cp $JOSM_FILE josm-tested.jar

function build_exe {

	export TARGET=$1	# josm / josm64. Used in file name of launcher and installer
	
	/bin/rm -f "launch4j_${TARGET}.xml"
	/bin/sed -e "s/%TARGET%/$1/" -e "s/%RTBITS%/$2/" -e "s/%INIHEAP%/$3/" -e "s/%MAXHEAP%/$4/" -e "s/%VERSION%/$VERSION/" "launch4j.xml" > "launch4j_${TARGET}.xml"
	
	echo 
	echo "##################################################################"
	echo "### convert jar to ${TARGET}.exe with launch4j"
	# (an exe file makes attaching to file extensions a lot easier)
	# launch4j - http://launch4j.sourceforge.net/
	# delete old exe file first
	/bin/rm -f ${TARGET}.exe
	$LAUNCH4J "launch4j_${TARGET}.xml"
	# comment previous line and uncomment next one on Windows
	#"$LAUNCH4J" "launch4j_${TARGET}.xml"

	if ! [ -s ${TARGET}.exe ]; then
		echo "NO ${TARGET}.exe File Created"
		exit -1
	fi

	/bin/rm -f "launch4j_${TARGET}.xml"

	echo 
	echo "##################################################################"
	echo "### create the ${TARGET}-installer-${VERSION}.exe with makensis"
	# NSIS - http://nsis.sourceforge.net/Main_Page
	# apt-get install nsis
	"$MAKENSIS" -V2 -DVERSION=$VERSION -DDEST=$TARGET josm.nsi

	# keep the intermediate file, for debugging
	/bin/rm -f ${TARGET}-intermediate.exe 2>/dev/null >/dev/null
	/bin/mv ${TARGET}.exe ${TARGET}-intermediate.exe 2>/dev/null >/dev/null
}

build_exe "josm" "64\/32" 128 1024
# 64-bit binary generation commented until possible with launch4j / nsis
# build_exe "josm64"  "64" 256 2048

/bin/rm -f josm-tested.jar 2>/dev/null >/dev/null
