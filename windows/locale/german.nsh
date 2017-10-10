;;
;;  german.nsh
;;
;;  German language strings for the Windows JOSM NSIS installer.
;;  Windows Code page: 1252
;;
;;  Author: Bjoern Voigt <bjoern@cs.tu-berlin.de>, 2003.
;;  Version 2

; Make sure to update the JOSM_MACRO_LANGUAGEFILE_END macro in
; langmacros.nsh when updating this file

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_WELCOME_TEXT "Diese Installationshilfe wird Sie durch den Installationsvorgang des JAVA OpenStreetMap Editors (JOSM) führen.$\r$\n$\r$\nBevor Sie die Installation starten, stellen Sie bitte sicher das JOSM nicht bereits läuft.$\r$\n$\r$\nAuf 'Weiter' klicken um fortzufahren."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_DIR_TEXT "Bitte das Verzeichnis auswählen, in das JOSM installiert werden soll."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_FULL_INSTALL "JOSM (Komplettinstallation)"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_JOSM "JOSM"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_PLUGINS_GROUP "Plugins"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_TURNRESTRICTIONS_PLUGIN  "TurnRestrictions"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_STARTMENU  "Startmenü Eintrag"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_DESKTOP_ICON  "Desktop Icon"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_QUICKLAUNCH_ICON  "Schnellstartleiste Icon"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_FILE_EXTENSIONS  "Dateiendungen"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_JOSM "JOSM ist der JAVA OpenStreetMap Editor für .osm Dateien."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_PLUGINS_GROUP "Eine Auswahl an nützlichen JOSM Plugins."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_IMAGERY_OFFSET_DB_PLUGIN  "Datenbank der Bildversätze: Teilen und übernehmen Sie Bildversätze mit nur einer Schaltfläche."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_TURNRESTRICTIONS_PLUGIN  "Erleichtert die Eingabe und Pflege von Informationen zu Abbiegebeschränkungen."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_STARTMENU  "Fügt JOSM zum Startmenü hinzu."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_DESKTOP_ICON  "Fügt ein JOSM Icon zum Desktop hinzu."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_QUICKLAUNCH_ICON  "Fügt ein JOSM Icon zur Schnellstartleiste (Quick Launch) hinzu."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_FILE_EXTENSIONS  "Fügt JOSM Dateiendungen für .osm and .gpx Dateien hinzu."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR1 "Kann die Bibliothek 'shell32.dll' nicht finden. Das Update der Icons ist nicht möglich"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR2 "Sie sollten die kostenlose 'Microsoft Layer for Unicode' installieren um die Icons updaten zu können"

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_LINK_TEXT "JAVA OpenStreetMap - Editor"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_UNCONFIRMPAGE_TEXT_TOP "Die folgende JAVA OpenStreetMap editor (JOSM) Installation wird deinstalliert. Auf 'Weiter' klicken um fortzufahren."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_DEFAULT_UNINSTALL "Default (persönliche Einstellungen und Plugins behalten)"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_FULL_UNINSTALL "Alles (alles entfernen)"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_IN_USE_ERROR "Achtung: josm konnte nicht entfernt werden, möglicherweise wird es noch benutzt!"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_INSTDIR_ERROR "Achtung: Das Verzeichnis $INSTDIR konnte nicht entfernt werden!"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_UNINSTALL "JOSM" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PERSONAL_SETTINGS "Persönliche Einstellungen" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PLUGINS "Persönliche Plugins" 

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_UNINSTALL "Deinstalliere JOSM."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_PERSONAL_SETTINGS  "Deinstalliere persönliche Einstellungen von Ihrem Profil: $PROFILE."
