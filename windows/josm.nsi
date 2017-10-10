;
; josm.nsi
;

; Set the compression mechanism first.
SetCompressor /SOLID lzma

; Load StdUtils plugin (ANSI until we switch to Unicode installer with NSIS 3)
!addplugindir plugins/stdutils/Plugins/Release_ANSI
!addincludedir plugins/stdutils/Include

!include "StdUtils.nsh"

; make sure the installer will get elevated rights on UAC-enabled system (Vista+)
RequestExecutionLevel admin

; Used to refresh the display of file association
!define SHCNE_ASSOCCHANGED 0x08000000
!define SHCNF_IDLIST 0

; Used to add associations between file extensions and JOSM
!define OSM_ASSOC "josm-file"

; ============================================================================
; Header configuration
; ============================================================================
; The name of the installer
Name "JOSM ${VERSION}"

; The file to write
OutFile "${DEST}-setup-${VERSION}.exe"

XPStyle on

Var /GLOBAL plugins

; ============================================================================
; Modern UI
; ============================================================================

!include "MUI2.nsh"

; Icon of installer and uninstaller
!define MUI_ICON "logo.ico"
!define MUI_UNICON "logo.ico"

!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_WELCOMEFINISHPAGE_BITMAP "josm-nsis-brand.bmp"
!define MUI_WELCOMEPAGE_TEXT $(JOSM_WELCOME_TEXT) 

!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION LaunchJOSM

; Function used to Launch JOSM in user (non-elevated) mode
Function LaunchJOSM
  ${StdUtils.ExecShellAsUser} $0 "$INSTDIR\${DEST}.exe" "open" ""
FunctionEnd

; ============================================================================
; MUI Pages
; ============================================================================

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "..\LICENSE"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_COMPONENTS
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

; ============================================================================
; MUI Languages
; ============================================================================

  ;Remember the installer language
  !define MUI_LANGDLL_REGISTRY_ROOT "HKLM" 
  !define MUI_LANGDLL_REGISTRY_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" 
  !define MUI_LANGDLL_REGISTRY_VALUENAME "Installer Language"
  
  ;; English goes first because its the default. The rest are
  ;; in alphabetical order (at least the strings actually displayed
  ;; will be).

  !insertmacro MUI_LANGUAGE "English"
  !insertmacro MUI_LANGUAGE "French"
  !insertmacro MUI_LANGUAGE "German"

;--------------------------------
;Translations

  !define JOSM_DEFAULT_LANGFILE "locale\english.nsh"

  !include "langmacros.nsh"
  
  !insertmacro JOSM_MACRO_INCLUDE_LANGFILE "ENGLISH" "locale\english.nsh"
  !insertmacro JOSM_MACRO_INCLUDE_LANGFILE "FRENCH" "locale\french.nsh"
  !insertmacro JOSM_MACRO_INCLUDE_LANGFILE "GERMAN" "locale\german.nsh"

; Uninstall stuff
!define MUI_UNCONFIRMPAGE_TEXT_TOP ${un.JOSM_UNCONFIRMPAGE_TEXT_TOP}

; ============================================================================
; Installation types
; ============================================================================

InstType "$(JOSM_FULL_INSTALL)"

InstType "un.$(un.JOSM_DEFAULT_UNINSTALL)"
InstType "un.$(un.JOSM_FULL_UNINSTALL)"

; ============================================================================
; Section macros
; ============================================================================
!include "Sections.nsh"

; ========= Macro to unselect and disable a section =========

!macro DisableSection SECTION

  Push $0
    SectionGetFlags "${SECTION}" $0
    IntOp $0 $0 & ${SECTION_OFF}
    IntOp $0 $0 | ${SF_RO}
    SectionSetFlags "${SECTION}" $0
  Pop $0

!macroend

; ========= Macro to enable (unreadonly) a section =========
!define SECTION_ENABLE   0xFFFFFFEF
!macro EnableSection SECTION

  Push $0
    SectionGetFlags "${SECTION}" $0
    IntOp $0 $0 & ${SECTION_ENABLE}
    SectionSetFlags "${SECTION}" $0
  Pop $0

!macroend

; ============================================================================
; Command Line
; ============================================================================
!include "FileFunc.nsh"

; ============================================================================
; Directory selection page configuration
; ============================================================================
; The text to prompt the user to enter a directory
DirText $(JOSM_DIR_TEXT)

; The default installation directory
InstallDir $PROGRAMFILES\JOSM\

; See if this is an upgrade; if so, use the old InstallDir as default
InstallDirRegKey HKEY_LOCAL_MACHINE SOFTWARE\JOSM "InstallDir"


; ============================================================================
; Install page configuration
; ============================================================================
ShowInstDetails show
ShowUninstDetails show

; ============================================================================
; Functions and macros
; ============================================================================

; update file extension icons
!macro UpdateIcons
	Push $R0
  	Push $R1
  	Push $R2

	!define UPDATEICONS_UNIQUE ${__LINE__}

	IfFileExists "$SYSDIR\shell32.dll" UpdateIcons.next1_${UPDATEICONS_UNIQUE} UpdateIcons.error1_${UPDATEICONS_UNIQUE}
UpdateIcons.next1_${UPDATEICONS_UNIQUE}:
	GetDllVersion "$SYSDIR\shell32.dll" $R0 $R1
	IntOp $R2 $R0 / 0x00010000
	IntCmp $R2 4 UpdateIcons.next2_${UPDATEICONS_UNIQUE} UpdateIcons.error2_${UPDATEICONS_UNIQUE}
UpdateIcons.next2_${UPDATEICONS_UNIQUE}:
	System::Call 'shell32.dll::SHChangeNotify(i, i, i, i) v (${SHCNE_ASSOCCHANGED}, ${SHCNF_IDLIST}, 0, 0)'
	Goto UpdateIcons.quit_${UPDATEICONS_UNIQUE}

UpdateIcons.error1_${UPDATEICONS_UNIQUE}:
	MessageBox MB_OK|MB_ICONSTOP $(JOSM_UPDATEICONS_ERROR1)
	Goto UpdateIcons.quit_${UPDATEICONS_UNIQUE}
UpdateIcons.error2_${UPDATEICONS_UNIQUE}:
	MessageBox MB_OK|MB_ICONINFORMATION $(JOSM_UPDATEICONS_ERROR2)
	Goto UpdateIcons.quit_${UPDATEICONS_UNIQUE}
UpdateIcons.quit_${UPDATEICONS_UNIQUE}:
	!undef UPDATEICONS_UNIQUE
	Pop $R2
	Pop $R1
  	Pop $R0

!macroend

; associate a file extension to an icon
Function Associate
	; $R0 should contain the prefix to associate to JOSM
	Push $R1

	ReadRegStr $R1 HKCR $R0 ""
	StrCmp $R1 "" Associate.doRegister
	Goto Associate.end
Associate.doRegister:
	;The extension is not associated to any program, we can do the link
	WriteRegStr HKCR $R0 "" ${OSM_ASSOC}
Associate.end:
	pop $R1
FunctionEnd

; disassociate a file extension from an icon
Function un.unlink
	; $R0 should contain the prefix to unlink
	Push $R1

	ReadRegStr $R1 HKCR $R0 ""
	StrCmp $R1 ${OSM_ASSOC} un.unlink.doUnlink
	Goto un.unlink.end
un.unlink.doUnlink:
	; The extension is associated with JOSM so, we must destroy this!
	DeleteRegKey HKCR $R0
un.unlink.end:
	pop $R1
FunctionEnd

Function .onInit
  !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Function un.onInit
  !insertmacro MUI_UNGETLANGUAGE
FunctionEnd

; ============================================================================
; Installation execution commands
; ============================================================================

Section "-Required"
;-------------------------------------------

;
; Install for every user
;
SectionIn 1 2 RO
SetShellVarContext current

SetOutPath $INSTDIR

; Write the uninstall keys for Windows
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "DisplayVersion" "${VERSION}"
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "DisplayName" "JOSM ${VERSION}"
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "UninstallString" '"$INSTDIR\uninstall.exe"'
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "Publisher" "OpenStreetMap JOSM team"
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "HelpLink" "mailto:josm-dev@openstreetmap.org."
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "URLInfoAbout" "https://josm.openstreetmap.de"
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "URLUpdateInfo" "https://josm.openstreetmap.de"
WriteRegDWORD HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "NoModify" 1
WriteRegDWORD HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM" "NoRepair" 1
WriteUninstaller "uninstall.exe"

; Write an entry for ShellExecute
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\App Paths\${DEST}.exe" "" '$INSTDIR\${DEST}.exe'
WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\App Paths\${DEST}.exe" "Path" '$INSTDIR'

SectionEnd ; "Required"


Section $(JOSM_SEC_JOSM) SecJosm
;-------------------------------------------
SectionIn 1
SetOutPath $INSTDIR
File "${DEST}.exe"
File "josm-tested.jar"

; XXX - should be provided/done by josm.jar itself and not here!
SetShellVarContext current
SetOutPath "$APPDATA\JOSM"

SectionEnd

SectionGroup $(JOSM_SEC_PLUGINS_GROUP) SecPluginsGroup

Section $(JOSM_SEC_IMAGERY_OFFSET_DB_PLUGIN) SecImageryOffsetDbPlugin
;-------------------------------------------
SectionIn 1 2
SetShellVarContext current
SetOutPath $APPDATA\JOSM\plugins
File "../../dist/imagery_offset_db.jar"
StrCpy $plugins "$plugins<entry value='imagery_offset_db'/>"
SectionEnd

Section $(JOSM_SEC_TURNRESTRICTIONS_PLUGIN) SecTurnrestrictionsPlugin
;-------------------------------------------
SectionIn 1 2
SetShellVarContext current
SetOutPath $APPDATA\JOSM\plugins
File "../../dist/turnrestrictions.jar"
StrCpy $plugins "$plugins<entry value='turnrestrictions'/>"
SectionEnd

SectionGroupEnd	; "Plugins"

Section $(JOSM_SEC_STARTMENU) SecStartMenu
;-------------------------------------------
SectionIn 1 2
; To quote "http://msdn.microsoft.com/library/default.asp?url=/library/en-us/dnwue/html/ch11d.asp":
; "Do not include Readme, Help, or Uninstall entries on the Programs menu."
CreateShortCut "$SMPROGRAMS\JOSM.lnk" "$INSTDIR\${DEST}.exe" "" "$INSTDIR\${DEST}.exe" 0 "" "" $(JOSM_LINK_TEXT)
SectionEnd

Section $(JOSM_SEC_DESKTOP_ICON) SecDesktopIcon
;-------------------------------------------
; Create desktop icon
; Desktop icon for a program should not be installed as default!
CreateShortCut "$DESKTOP\JOSM.lnk" "$INSTDIR\${DEST}.exe" "" "$INSTDIR\${DEST}.exe" 0 "" "" $(JOSM_LINK_TEXT)
SectionEnd

Section $(JOSM_SEC_QUICKLAUNCH_ICON) SecQuickLaunchIcon
;-------------------------------------------
; Create quick launch icon. Does not really exist as of Windows 7/8 but still advanced users use it.
; Only disable it by default, see #10241
CreateShortCut "$QUICKLAUNCH\JOSM.lnk" "$INSTDIR\${DEST}.exe" "" "$INSTDIR\${DEST}.exe" 0 "" "" $(JOSM_LINK_TEXT)
SectionEnd

Section $(JOSM_SEC_FILE_EXTENSIONS) SecFileExtensions
;-------------------------------------------
SectionIn 1 2
; Create File Extensions
WriteRegStr HKCR ${OSM_ASSOC} "" "OpenStreetMap data"
WriteRegStr HKCR "${OSM_ASSOC}\Shell\open\command" "" '"$INSTDIR\${DEST}.exe" "%1"'
WriteRegStr HKCR "${OSM_ASSOC}\DefaultIcon" "" '"$INSTDIR\${DEST}.exe",0'
push $R0
	StrCpy $R0 ".osm"
  	Call Associate
	StrCpy $R0 ".gpx"
  	Call Associate
; if somethings added here, add it also to the uninstall section
pop $R0
!insertmacro UpdateIcons
SectionEnd

Section "-PluginSetting"
;-------------------------------------------
SectionIn 1 2
;MessageBox MB_OK "PluginSetting!" IDOK 0
; XXX - should better be handled inside JOSM (recent plugin manager is going in the right direction)
SetShellVarContext current
!include LogicLib.nsh
IfFileExists "$APPDATA\JOSM\preferences" settings_exists
IfFileExists "$APPDATA\JOSM\preferences.xml" settings_exists
FileOpen $R0 "$APPDATA\JOSM\preferences.xml" w
FileWrite $R0 "<?xml version='1.0' encoding='UTF-8'?><preferences xmlns='http://josm.openstreetmap.de/preferences-1.0' version='4660'><list key='plugins'>$plugins</list></preferences>"
FileClose $R0
settings_exists:
SectionEnd

Section "un.$(un.JOSM_SEC_UNINSTALL)" un.SecUinstall
;-------------------------------------------

;
; UnInstall for every user
;
SectionIn 1 2
SetShellVarContext current

ClearErrors
Delete "$INSTDIR\josm-tested.jar"
IfErrors 0 NoJOSMErrorMsg
	MessageBox MB_OK $(un.JOSM_IN_USE_ERROR) IDOK 0 ;skipped if josm.jar removed
	Abort $(un.JOSM_IN_USE_ERROR)
NoJOSMErrorMsg:
Delete "$INSTDIR\${DEST}.exe"
Delete "$INSTDIR\imageformats\qjpeg4.dll"
RMDir "$INSTDIR\imageformats"
Delete "$INSTDIR\mingwm10.dll"
Delete "$INSTDIR\QtCore4.dll"
Delete "$INSTDIR\QtGui4.dll"
Delete "$INSTDIR\QtNetwork4.dll"
Delete "$INSTDIR\QtWebKit4.dll"
Delete "$INSTDIR\webkit-image.exe"
Delete "$INSTDIR\uninstall.exe"

DeleteRegKey HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\JOSM"
DeleteRegKey HKEY_LOCAL_MACHINE "Software\${DEST}.exe"
DeleteRegKey HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\App Paths\${DEST}.exe"

; Remove Language preference info
DeleteRegKey HKCU "Software/JOSM" ;${MUI_LANGDLL_REGISTRY_ROOT} ${MUI_LANGDLL_REGISTRY_KEY}

push $R0
	StrCpy $R0 ".osm"
  	Call un.unlink
	StrCpy $R0 ".gpx"
  	Call un.unlink
pop $R0

DeleteRegKey HKCR ${OSM_ASSOC}
DeleteRegKey HKCR "${OSM_ASSOC}\Shell\open\command"
DeleteRegKey HKCR "${OSM_ASSOC}\DefaultIcon"
!insertmacro UpdateIcons

Delete "$SMPROGRAMS\josm.lnk"
Delete "$DESKTOP\josm.lnk"
Delete "$QUICKLAUNCH\josm.lnk"

RMDir "$INSTDIR"

SectionEnd ; "Uninstall"

Section /o "un.$(un.JOSM_SEC_PERSONAL_SETTINGS)" un.SecPersonalSettings
;-------------------------------------------
SectionIn 2
SetShellVarContext current
Delete "$APPDATA\JOSM\plugins\turnrestrictions\*.*"
RMDir "$APPDATA\JOSM\plugins\turnrestrictions"
Delete "$APPDATA\JOSM\plugins\*.*"
RMDir "$APPDATA\JOSM\plugins"

Delete "$APPDATA\JOSM\motd.html"
Delete "$APPDATA\JOSM\preferences.xml"
RMDir "$APPDATA\JOSM"
SectionEnd

Section "-Un.Finally"
;-------------------------------------------
SectionIn 1 2
; this test must be done after all other things uninstalled (e.g. Global Settings)
IfSilent NoFinalErrorMsg
IfFileExists "$INSTDIR" 0 NoFinalErrorMsg
    MessageBox MB_OK $(un.JOSM_INSTDIR_ERROR) IDOK 0 ; skipped if dir doesn't exist
NoFinalErrorMsg:
SectionEnd

; ============================================================================
; PLEASE MAKE SURE, THAT THE DESCRIPTIVE TEXT FITS INTO THE DESCRIPTION FIELD!
; ============================================================================
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecJosm} $(JOSM_SECDESC_JOSM)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecPluginsGroup} $(JOSM_SECDESC_PLUGINS_GROUP)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecImageryOffsetDbPlugin} $(JOSM_SECDESC_IMAGERY_OFFSET_DB_PLUGIN)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTurnrestrictionsPlugin} $(JOSM_SECDESC_TURNRESTRICTIONS_PLUGIN)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecStartMenu} $(JOSM_SECDESC_STARTMENU)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktopIcon} $(JOSM_SECDESC_DESKTOP_ICON)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecQuickLaunchIcon} $(JOSM_SECDESC_QUICKLAUNCH_ICON) 
  !insertmacro MUI_DESCRIPTION_TEXT ${SecFileExtensions} $(JOSM_SECDESC_FILE_EXTENSIONS)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

!insertmacro MUI_UNFUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${un.SecUinstall} $(un.JOSM_SECDESC_UNINSTALL)
  !insertmacro MUI_DESCRIPTION_TEXT ${un.SecPersonalSettings} $(un.JOSM_SECDESC_PERSONAL_SETTINGS)
!insertmacro MUI_UNFUNCTION_DESCRIPTION_END

; ============================================================================
; Callback functions
; ============================================================================

