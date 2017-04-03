@echo off
setlocal EnableDelayedExpansion
REM ----------------------------------------------------------------------
set "NSIS_ANSI=C:\Program Files (x86)\NSIS\ANSI"
set "NSIS_Unicode=C:\Program Files (x86)\NSIS\Unicode"
REM ----------------------------------------------------------------------
set "NSIS_PROJECTS=StdUtilsTest,SHFileOperation,ShellExecAsUser,InvokeShellVerb,ShellExecWait,GetParameters,AppendToFile,HashFunctions,TimerCreate"
REM ----------------------------------------------------------------------
REM
for %%i in (%NSIS_PROJECTS%) do (
	del "%~dp0\%%i-ANSI.exe"
	del "%~dp0\%%i-Unicode.exe"
	if exist "%~dp0\%%i-ANSI.exe" (
		pause && exit
	)
	if exist "%~dp0\%%i-Unicode.exe" (
		pause && exit
	)
)
REM ----------------------------------------------------------------------
for %%i in (%NSIS_PROJECTS%) do (
	"%NSIS_ANSI%\makensis.exe" "%~dp0\%%i.nsi"
	if not "!ERRORLEVEL!"=="0" pause && exit
	if not exist "%~dp0\%%i-ANSI.exe" pause && exit
	
	"%NSIS_Unicode%\makensis.exe" "%~dp0\%%i.nsi"
	if not "!ERRORLEVEL!"=="0" pause && exit
	if not exist "%~dp0\%%i-Unicode.exe" pause && exit
)
REM ----------------------------------------------------------------------
pause
