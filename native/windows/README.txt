This is the Win32 installer generator for JOSM, to create a Windows installer.
This should ease installation for Windows users.

Currently josm, the Java runtime and JavaFX are included in the installer.


install
-------
simply execute josm-setup-latest.exe

uninstall
---------
use "control panel / software" to uninstall


current state of the art
------------------------
The installer will currently add:
- josm and java/javafx runtime into "%CSIDL_LOCAL_APPDATA%\JOSM" (or the directory chosen by user)
- josm icons to the desktop
- josm file associations


build the installer
-------------------
1.) You will need to download JavaFX modules from:
- https://gluonhq.com/products/javafx/

2.) Extract the modules to %JAVA_HOME%/jmods

3.) Start a cygwin shell and call ./native/windows/win-jpackage.sh

4.) The JOSM.exe and JOSM.msi files can then be found in ./app/

how the installer is built
--------------------------
See jpackage documentation to understand what it does and how it works:
- https://docs.oracle.com/en/java/javase/17/jpackage/preface.html

known issues
------------
- no translation

build the installer under Linux / Debian
----------------------------------------
It's likely not possible to build the installer under Linux because of the WiX transitive dependency.
