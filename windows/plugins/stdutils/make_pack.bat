@echo off
REM -------------------------------------------------------------------------
set "PATH_MSVC=D:\Microsoft Visual Studio 10.0\VC"
REM -------------------------------------------------------------------------
call "%PATH_MSVC%\vcvarsall.bat" x86
if "%VCINSTALLDIR%"=="" (
	pause
	exit
)
REM -------------------------------------------------------------------------
set "ISO_DATE="
if not exist "%~dp0\Contrib\StdUtils\utils\Date.exe" GOTO:EOF
for /F "tokens=1,2 delims=:" %%a in ('"%~dp0\Contrib\StdUtils\utils\Date.exe" +ISODATE:%%Y-%%m-%%d') do (
	if "%%a"=="ISODATE" set "ISO_DATE=%%b"
)
if "%ISO_DATE%"=="" (
	pause
	exit
)
REM -------------------------------------------------------------------------
if exist "%~dp0\StdUtils.%ISO_DATE%.zip" (
	attrib -r "%~dp0\StdUtils.%ISO_DATE%.zip"
	del "%~dp0\StdUtils.%ISO_DATE%.zip"
)
if exist "%~dp0\StdUtils.%ISO_DATE%.zip" (
	pause
	exit
)
REM -------------------------------------------------------------------------
MSBuild.exe /property:Configuration=Release_ANSI    /property:Platform=Win32 /target:Rebuild /verbosity:normal "%~dp0\Contrib\StdUtils\StdUtils.sln"
if not "%ERRORLEVEL%"=="0" (
	pause
	exit
)
REM -------------------------------------------------------------------------
MSBuild.exe /property:Configuration=Release_Unicode /property:Platform=Win32 /target:Rebuild /verbosity:normal "%~dp0\Contrib\StdUtils\StdUtils.sln"
if not "%ERRORLEVEL%"=="0" (
	pause
	exit
)
REM -------------------------------------------------------------------------
echo StdUtils plug-in for NSIS > "%~dp0\BUILD.tag"
echo Copyright (C) 2004-2014 LoRd_MuldeR ^<MuldeR2@GMX.de^> >> "%~dp0\BUILD.tag"
echo. >> "%~dp0\BUILD.tag"
echo Built on %DATE%, at %TIME%. >> "%~dp0\BUILD.tag"
REM -------------------------------------------------------------------------
pushd "%~dp0"
set "EXCLUDE_MASK=make_pack.* *.exe *.zip *.7z *.user *.old *.sdf *examples/*.exe */obj/* */ipch/*"
"%~dp0\Contrib\StdUtils\utils\Zip.exe" -r -9 -z "%~dp0\StdUtils.%ISO_DATE%.zip" "*.*" -x %EXCLUDE_MASK% < "%~dp0\BUILD.tag"
popd
attrib +r "%~dp0\StdUtils.%ISO_DATE%.zip" 
del "%~dp0\BUILD.tag"
REM -------------------------------------------------------------------------
pause
