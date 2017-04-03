Caption "StdUtils Test-Suite"

!addincludedir  "..\..\Include"

!ifdef NSIS_UNICODE
	!addplugindir "..\..\Plugins\Release_Unicode"
	OutFile "InvokeShellVerb-Unicode.exe"
!else
	!addplugindir "..\..\Plugins\Release_ANSI"
	OutFile "InvokeShellVerb-ANSI.exe"
!endif

!include 'StdUtils.nsh'

RequestExecutionLevel user ;no elevation needed for this test
ShowInstDetails show

Section
	IfFileExists "$SYSDIR\mspaint.exe" +3
	MessageBox MB_ICONSTOP 'File does not exist:$\n"$SYSDIR\mspaint.exe"$\n$\nExample cannot run!'
	Quit
	MessageBox MB_OK "Please make sure Paint isn't pinned to your Taskbar right now.$\nThen press 'OK' to begin test..."
SectionEnd

Section
	DetailPrint "Going to pin MSPaint..."
	
	DetailPrint  'InvokeShellVerb: "$SYSDIR" "mspaint.exe" ${StdUtils.Const.ISV_PinToTaskbar}'
	${StdUtils.InvokeShellVerb} $0 "$SYSDIR" "mspaint.exe" ${StdUtils.Const.ISV_PinToTaskbar}
	DetailPrint "Result: $0"

	StrCmp "$0" "ok" 0 +3
	MessageBox MB_TOPMOST "Paint should have been pinned to Taskbar now!"
	Goto +2
	MessageBox MB_TOPMOST "Failed to pin, see log for details!"

	DetailPrint "--------------"
SectionEnd

Section
	DetailPrint "Going to un-pin MSPaint..."
	
	DetailPrint  'InvokeShellVerb: "$SYSDIR" "mspaint.exe" ${StdUtils.Const.ISV_UnpinFromTaskbar}'
	${StdUtils.InvokeShellVerb} $0 "$SYSDIR" "mspaint.exe" ${StdUtils.Const.ISV_UnpinFromTaskbar}
	DetailPrint "Result: $0"
	
	StrCmp "$0" "ok" 0 +3
	MessageBox MB_TOPMOST "Paint should have been un-pinned from Taskbar now!"
	Goto +2
	MessageBox MB_TOPMOST "Failed to un-pin, see log for details!"

	DetailPrint "--------------"
SectionEnd
