;;
;;  french.nsh
;;
;;  French language strings for the Windows JOSM NSIS installer.
;;  Windows Code page: 1252
;;
;;  Author: Vincent Privat <vprivat@openstreetmap.fr>, 2011.
;;

; Make sure to update the JOSM_MACRO_LANGUAGEFILE_END macro in
; langmacros.nsh when updating this file

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_WELCOME_TEXT "Cet assistant va vous guider à travers l'installation de l'éditeur Java OpenStreetMap (JOSM).$\r$\n$\r$\nAvant de lancer l'installation, assurez-vous que JOSM n'est pas déjà en cours d'exécution.$\r$\n$\r$\nVeuillez cliquer sur 'Suivant' pour continuer."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_DIR_TEXT "Veuillez choisir un dossier où installer JOSM."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_FULL_INSTALL "JOSM (installation complète)"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_JOSM "JOSM"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_PLUGINS_GROUP "Greffons"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_IMAGERY_OFFSET_DB_PLUGIN  "ImageryOffsetDatabase"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_TURNRESTRICTIONS_PLUGIN  "TurnRestrictions"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_STARTMENU  "Entrée dans le menu Démarrer"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_DESKTOP_ICON  "Icône sur le Bureau"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_QUICKLAUNCH_ICON  "Icône dans la barre de lancement rapide"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SEC_FILE_EXTENSIONS  "Extensions de fichier"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_JOSM "JOSM est l'éditeur Java OpenStreetMap pour les fichiers .osm."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_PLUGINS_GROUP "Une sélection de greffons utiles pour JOSM."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_IMAGERY_OFFSET_DB_PLUGIN  "Base de données de décalages d'imagerie: partager et acquérir des décalages d'imagerie avec un seul bouton."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_TURNRESTRICTIONS_PLUGIN  "Permet de saisir et de maintenir des informations sur les restrictions de tourner."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_STARTMENU  "Ajoute une entrée JOSM au menu démarrer."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_DESKTOP_ICON  "Ajoute une icône JOSM au Bureau."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_QUICKLAUNCH_ICON  "Ajoute une icône JOSM à la barre de lancement rapide."
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_SECDESC_FILE_EXTENSIONS  "Associe JOSM aux extensions de fichier .osm et .gpx."

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR1 "La bibliothèque 'shell32.dll' est introuvable. Impossible de mettre à jour les icônes"
!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_UPDATEICONS_ERROR2 "Vous devriez installer le complément gratuit 'Microsoft Layer for Unicode' pour mettre à jour les fichiers d'icônes de JOSM"

!insertmacro JOSM_MACRO_DEFAULT_STRING JOSM_LINK_TEXT "Éditeur Java OpenStreetMap"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_UNCONFIRMPAGE_TEXT_TOP "L'installation suivante de l'éditeur Java OpenStreetMap (JOSM) va être désinstallée. Veuillez cliquer sur 'Suivant' pour continuer."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_DEFAULT_UNINSTALL "Défaut (conserve les paramètres personnels et les greffons)"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_FULL_UNINSTALL "Tout (supprime l'intégralité des fichiers)"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_IN_USE_ERROR "Attention: JOSM n'a pas pu être retiré, il est probablement en utilisation !"
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_INSTDIR_ERROR "Attention: Le dossier $INSTDIR n'a pas pu être supprimé !"

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_UNINSTALL "JOSM" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PERSONAL_SETTINGS "Paramètres personnels" 
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SEC_PLUGINS "Greffons personnels" 

!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_UNINSTALL "Désinstaller JOSM."
!insertmacro JOSM_MACRO_DEFAULT_STRING un.JOSM_SECDESC_PERSONAL_SETTINGS  "Désinstaller les paramètres personnels de votre profil: $PROFILE."
