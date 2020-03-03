This is the Win32 installer generator for JOSM, to create a Windows 
like installer. This should ease installation and provides a reasonable set of 
default preferences for Windows users.

Currently only josm and a small assortment of josm plugins is included in the 
installer.

As other osm related applications like osmarender and mapnik have a lot more
UNIX related dependencies that will make them complicated to install, only JOSM
is currently installed "the easy way".


install
-------
simply execute josm-setup-latest.exe

uninstall
---------
use "control panel / software" to uninstall


current state of the art
------------------------
The installer will currently add:
- josm into "C:\Program Files\JOSM" (or the corresponding international dir)
- josm icons to the desktop and quick launch bar
- josm file associations to .osm and .gpx files
- some assorted plugins (more to follow?)
- default preferences to the current user profile (if not already existing)

When the installed josm.exe is executed, it should ask the user to download 
Java runtime if it's not already installed.

build the installer
-------------------
1.) You will need to download and install the following on your machine:
- cygwin bash and wget
- launch4j - http://launch4j.sourceforge.net/ (currently 3.6, older 2.x will NOT work!)
- NSIS - http://nsis.sourceforge.net/ (any recent version should do)

2.) Edit the two absolute paths in the file josm-setup-unix.sh (in the calls 
to launch4jc and makensis)

3.) Start a cygwin shell and call ./josm-setup-unix.sh

how the installer is build
--------------------------
First, wget will download the required files (e.g. the josm plugins) into the 
downloads subdir. Then jaunch4j wraps the josm.jar into a josm.exe, which 
makes registration of file extensions a lot easier. Then NSIS is called to 
create the actual josm-setup-latest.exe.

known issues
------------
- absolute paths in josm-setup-unix.sh
- josm should support "global settings" instead of only the personal profile
- install all josm plugins by default and only enable them according to user wishes?

build the installer under Linux / Debian
----------------------------------------
It's possible to build the installer under Linux. Beside the things mentioned above, 
the NSIS Debian package currently has an incomplete System.dll (in the plugins directory).
This dll needs to be replaced by the System.dll included in the NSIS zip package, e.g. from
http://nsis.sourceforge.net/Development_Files.
More details can be found in the following NSIS forum thread:
http://forums.winamp.com/showthread.php?s=bb7fa5bf8173e31c05e083340ca2c242&postid=2211132 
