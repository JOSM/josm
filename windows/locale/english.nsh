;;
;;  english.nsh
;;
;;  Default language strings for the Windows JOSM NSIS installer.
;;  Windows Code page: 1252
;;
;;  Note: If translating this file, replace "!insertmacro JOSM_MACRO_DEFAULT_STRING"
;;  with "!define".

; Make sure to update the JOSM_MACRO_LANGUAGEFILE_END macro in
; langmacros.nsh when updating this file

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_WELCOME_TEXT "This wizard will guide you through the installation of the Java OpenStreetMap Editor (JOSM).$\r$\n$\r$\nBefore starting the installation, make sure any JOSM applications are not running.$\r$\n$\r$\nClick 'Next' to continue."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_DIR_TEXT "Choose a directory in which to install JOSM."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_FULL_INSTALL "JOSM (full install)"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_JOSM "JOSM"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_PLUGINS_GROUP "Plugins"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_TURNRESTRICTIONS_PLUGIN  "TurnRestrictions"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_STARTMENU  "Start Menu Entry"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_DESKTOP_ICON  "Desktop Icon"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_QUICKLAUNCH_ICON  "Quick Launch Icon"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_FILE_EXTENSIONS  "File Extensions"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_JOSM "JOSM is the Java OpenStreetMap editor for .osm files."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_PLUGINS_GROUP "An assortment of useful JOSM plugins."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_IMAGERY_OFFSET_DB_PLUGIN  "Database of imagery offsets: share and aquire imagery offsets with one button."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_TURNRESTRICTIONS_PLUGIN  "Allows to enter and maintain information about turn restrictions."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_STARTMENU  "Add a JOSM start menu entry."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_DESKTOP_ICON  "Add a JOSM desktop icon."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_QUICKLAUNCH_ICON  "Add a JOSM icon to the quick launch bar."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_FILE_EXTENSIONS  "Add JOSM file extensions for .osm and .gpx files."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR1 "Can't find 'shell32.dll' library. Impossible to update icons"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR2 "You should install the free 'Microsoft Layer for Unicode' to update JOSM file icons"

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_LINK_TEXT "Java OpenStreetMap - Editor"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_UNCONFIRMPAGE_TEXT_TOP "The following Java OpenStreetMap editor (JOSM) installation will be uninstalled. Click 'Next' to continue."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_DEFAULT_UNINSTALL "Default (keep Personal Settings and plugins)"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_FULL_UNINSTALL "All (remove all)"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_IN_USE_ERROR "Please note: josm could not be removed, it's probably in use!"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_INSTDIR_ERROR "Please note: The directory $INSTDIR could not be removed!"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_UNINSTALL "JOSM" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PERSONAL_SETTINGS "Personal settings" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PLUGINS "Personal plugins" 

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_UNINSTALL "Uninstall JOSM."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_PERSONAL_SETTINGS  "Uninstall personal settings from your profile: $PROFILE."
