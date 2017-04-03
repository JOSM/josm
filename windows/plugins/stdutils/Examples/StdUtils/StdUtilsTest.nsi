Caption "StdUtils Test-Suite"

!addincludedir  "..\..\Include"

!ifdef NSIS_UNICODE
	!addplugindir "..\..\Plugins\Release_Unicode"
	OutFile "StdUtilsTest-Unicode.exe"
!else
	!addplugindir "..\..\Plugins\Release_ANSI"
	OutFile "StdUtilsTest-ANSI.exe"
!endif

!macro NextTest
	Section
		DetailPrint "--------------"
	SectionEnd
!macroend

!include 'StdUtils.nsh'

RequestExecutionLevel user
ShowInstDetails show

# -----------------------------------------
# GetRealOSVersion
# -----------------------------------------

Section
	${StdUtils.GetLibVersion} $1 $2
	DetailPrint "Testing StdUtils library v$1"
	DetailPrint "Library built: $2"
SectionEnd

!insertmacro NextTest

# -----------------------------------------
# GetRealOSVersion
# -----------------------------------------

Section
	${StdUtils.GetRealOSVersion} $1 $2 $3
	DetailPrint "Real Windows NT Version: $1,$2 (Service Pack: $3)"

	${StdUtils.GetRealOSBuildNo} $1
	DetailPrint "Real Windows NT BuildNo: $1"

	${StdUtils.GetRealOSName} $1
	DetailPrint "Real Windows NT Friendly Name: $1"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.VerifyOSVersion} $1 5 1 0
	DetailPrint "Check for Windows XP (RTM): $1"

	${StdUtils.VerifyOSVersion} $1 5 1 1
	DetailPrint "Check for Windows XP (SP1): $1"

	${StdUtils.VerifyOSVersion} $1 5 1 2
	DetailPrint "Check for Windows XP (SP2): $1"

	${StdUtils.VerifyOSVersion} $1 5 1 3
	DetailPrint "Check for Windows XP (SP3): $1"

	${StdUtils.VerifyOSVersion} $1 6 0 0
	DetailPrint "Check for Windows Vista (RTM): $1"

	${StdUtils.VerifyOSVersion} $1 6 0 1
	DetailPrint "Check for Windows Vista (SP1): $1"

	${StdUtils.VerifyOSVersion} $1 6 1 0
	DetailPrint "Check for Windows 7 (RTM): $1"

	${StdUtils.VerifyOSVersion} $1 6 1 1
	DetailPrint "Check for Windows 7 (SP1): $1"

	${StdUtils.VerifyOSVersion} $1 6 2 0
	DetailPrint "Check for Windows 8.0: $1"

	${StdUtils.VerifyOSVersion} $1 6 3 0
	DetailPrint "Check for Windows 8.1: $1"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.VerifyOSBuildNo} $1 2600
	DetailPrint "Check for Build #2600, Windows XP: $1"

	${StdUtils.VerifyOSBuildNo} $1 7600
	DetailPrint "Check for Build #7600, Windows 7 (RTM): $1"

	${StdUtils.VerifyOSBuildNo} $1 7601
	DetailPrint "Check for Build #7601, Windows 7 (SP1): $1"

	${StdUtils.VerifyOSBuildNo} $1 9600
	DetailPrint "Check for Build #9600, Windows 8.1: $1"
SectionEnd

!insertmacro NextTest

# -----------------------------------------
# Time functions
# -----------------------------------------

Section
	${StdUtils.Time} $1
	DetailPrint "Time: $1"
	Sleep 500
	${StdUtils.Time} $1
	DetailPrint "Time: $1"
	Sleep 500
	${StdUtils.Time} $1
	DetailPrint "Time: $1"
SectionEnd

Section
	${StdUtils.GetMinutes} $1
	DetailPrint "UTC time in minutes: $1"
	${StdUtils.GetHours} $1
	DetailPrint "UTC time in hours: $1"
	${StdUtils.GetDays} $1
	DetailPrint "UTC time in days: $1"
SectionEnd

!insertmacro NextTest

# -----------------------------------------
# PRNG functions
# -----------------------------------------

Section
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
	${StdUtils.Rand} $1
	DetailPrint "Random: $1"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
	${StdUtils.RandMax} $1 42
	DetailPrint "Random Max: $1"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 -4 -2
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 20 21
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 20 21
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 20 21
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 20 21
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 20 21
	DetailPrint "Random Min/Max: $1"
	${StdUtils.RandMinMax} $1 20 21
	DetailPrint "Random Min/Max: $1"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.FormatStr} $1 "Hello World is %05d woha!" 89
	DetailPrint "FormatStr: $1"
	${StdUtils.FormatStr2} $1 "Hello World is %05d and %05d woha!" 89 384
	DetailPrint "FormatStr: $1"
	${StdUtils.FormatStr3} $1 "Hello World is %05d and %05d or even %05d woha!" 89 384 2384
	DetailPrint "FormatStr: $1"
	${StdUtils.FormatStr} $1 "Hello World is %09000d." 89
	DetailPrint "FormatStr: $1"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.RandList} 50 100
	Pop $1
	StrCmp $1 EOL +3
	DetailPrint "RandList: $1"
	Goto -3
SectionEnd

!insertmacro NextTest

# -----------------------------------------
# SHFileCopy function
# -----------------------------------------

Section
	InitPluginsDir
	SetOutPath "$PLUGINSDIR\TestDirA"
	File "${NSISDIR}\Contrib\Graphics\Checks\*.*"
	SetOutPath "$PLUGINSDIR\TestDirA\SubDir"
	File "${NSISDIR}\Contrib\Graphics\Header\*.*"
	CreateDirectory "$PLUGINSDIR\SubDirX"
	CreateDirectory "$PLUGINSDIR\SubDirY"
	
	${StdUtils.SHFileCopy} $0 "$PLUGINSDIR\TestDirA" "$PLUGINSDIR\SubDirX\TestDirB" $HWNDPARENT
	DetailPrint "SHFileCopy: $0"
	${StdUtils.SHFileMove} $0 "$PLUGINSDIR\TestDirA" "$PLUGINSDIR\SubDirY\TestDirC" $HWNDPARENT
	DetailPrint "SHFileMove: $0"
	ExecShell "explore" "$PLUGINSDIR"
SectionEnd

!insertmacro NextTest

Section
	MessageBox MB_ICONINFORMATION "The next three operations are going to fail!$\nBut only one will be verbose..."

	${StdUtils.SHFileCopy} $0 "$PLUGINSDIR\TestDirXYZ" "$PLUGINSDIR\SubDirX\TestDirZ" $HWNDPARENT
	DetailPrint "SHFileCopy: $0"
	
	${StdUtils.SetVerbose} 1
	${StdUtils.SHFileCopy} $0 "$PLUGINSDIR\TestDirXYZ" "$PLUGINSDIR\SubDirX\TestDirZ" $HWNDPARENT
	DetailPrint "SHFileCopy: $0"
	
	${StdUtils.SetVerbose} 0
	${StdUtils.SHFileCopy} $0 "$PLUGINSDIR\TestDirXYZ" "$PLUGINSDIR\SubDirX\TestDirZ" $HWNDPARENT
	DetailPrint "SHFileCopy: $0"
SectionEnd

!insertmacro NextTest

# -----------------------------------------
# String functions
# -----------------------------------------

Section
	${StdUtils.ScanStr} $0 "Der Test sagt %d ist toll!" "Der Test sagt 571 ist toll!" 42
	DetailPrint "ScanStr: $0"
	${StdUtils.ScanStr} $0 "Der Hund sagt %d ist toll!" "Der Test sagt 571 ist toll!" 42
	DetailPrint "ScanStr: $0"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.ScanStr2} $0 $1 "Der Test sagt %d sowie %d ist toll!" "Der Test sagt 571 sowie 831 ist toll!" 42 43
	DetailPrint "ScanStr2: $0, $1"
	${StdUtils.ScanStr2} $0 $1 "Der Test sagt %d sowie %d ist toll!" "Der Test sagt 571 horch 831 ist toll!" 42 43
	DetailPrint "ScanStr2: $0, $1"
	${StdUtils.ScanStr2} $0 $1 "Der Test sagt %d sowie %d ist toll!" "Der Hund sagt 571 horch 831 ist toll!" 42 43
	DetailPrint "ScanStr2: $0, $1"
SectionEnd

!insertmacro NextTest

Section
	${StdUtils.ScanStr3} $0 $1 $2 "Der Test sagt %d sowie %d ist toll! Und %d." "Der Test sagt 571 sowie 831 ist toll! Und 325" 42 43 44
	DetailPrint "ScanStr3: $0, $1, $2"
	${StdUtils.ScanStr3} $0 $1 $2 "Der Test sagt %d sowie %d ist toll! Und %d." "Der Test sagt 571 sowie 831 ist toll! OMG 325" 42 43 44
	DetailPrint "ScanStr3: $0, $1, $2"
	${StdUtils.ScanStr3} $0 $1 $2 "Der Test sagt %d sowie %d ist toll! Und %d." "Der Test sagt 571 horch 831 ist toll! OMG 325" 42 43 44
	DetailPrint "ScanStr3: $0, $1, $2"
	${StdUtils.ScanStr3} $0 $1 $2 "Der Test sagt %d sowie %d ist toll! Und %d." "Der Hund sagt 571 horch 831 ist toll! OMG 325" 42 43 44
	DetailPrint "ScanStr3: $0, $1, $2"
SectionEnd

!insertmacro NextTest

Section
	StrCpy $1 "        Some Text        "
	DetailPrint "String: '$1'"
	StrCpy $0 $1
	${StdUtils.TrimStr} $0
	DetailPrint "TrimStr: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrLeft} $0
	DetailPrint "TrimStrLeft: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrRight} $0
	DetailPrint "TrimStrRight: '$0'"
	
	StrCpy $1 "Some Text"
	DetailPrint "String: '$1'"
	StrCpy $0 $1
	${StdUtils.TrimStr} $0
	DetailPrint "TrimStr: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrLeft} $0
	DetailPrint "TrimStrLeft: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrRight} $0
	DetailPrint "TrimStrRight: '$0'"

	StrCpy $1 ""
	DetailPrint "String: '$1'"
	StrCpy $0 $1
	${StdUtils.TrimStr} $0
	DetailPrint "TrimStr: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrLeft} $0
	DetailPrint "TrimStrLeft: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrRight} $0
	DetailPrint "TrimStrRight: '$0'"
	
	StrCpy $1 "   "
	DetailPrint "String: '$1'"
	StrCpy $0 $1
	${StdUtils.TrimStr} $0
	DetailPrint "TrimStr: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrLeft} $0
	DetailPrint "TrimStrLeft: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrRight} $0
	DetailPrint "TrimStrRight: '$0'"

	StrCpy $1 "$\tFoobar$\r$\n"
	DetailPrint "String: '$1'"
	StrCpy $0 $1
	${StdUtils.TrimStr} $0
	DetailPrint "TrimStr: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrLeft} $0
	DetailPrint "TrimStrLeft: '$0'"
	StrCpy $0 $1
	${StdUtils.TrimStrRight} $0
	DetailPrint "TrimStrRight: '$0'"
SectionEnd

!insertmacro NextTest

Section
	StrCpy $0 "ABC"
	DetailPrint "String: $0"
	${StdUtils.RevStr} $0
	DetailPrint "RevStr: $0"
	
	StrCpy $0 "ABCD"
	DetailPrint "String: $0"
	${StdUtils.RevStr} $0
	DetailPrint "RevStr: $0"

	StrCpy $0 "Just a very long text with no specific meaning at all!"
	DetailPrint "String: $0"
	${StdUtils.RevStr} $0
	DetailPrint "RevStr: $0"
SectionEnd

!insertmacro NextTest
