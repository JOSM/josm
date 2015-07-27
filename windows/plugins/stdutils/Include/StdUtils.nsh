#################################################################################
# StdUtils plug-in for NSIS
# Copyright (C) 2004-2014 LoRd_MuldeR <MuldeR2@GMX.de>
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
#
# http://www.gnu.org/licenses/lgpl-2.1.txt
#################################################################################


#################################################################################
# FUNCTION DECLARTIONS
#################################################################################

!define StdUtils.Time             '!insertmacro _StdUtils_Time'          #time(), as in C standard library
!define StdUtils.GetMinutes       '!insertmacro _StdUtils_GetMinutes'    #GetSystemTimeAsFileTime(), returns the number of minutes
!define StdUtils.GetHours         '!insertmacro _StdUtils_GetHours'      #GetSystemTimeAsFileTime(), returns the number of hours
!define StdUtils.GetDays          '!insertmacro _StdUtils_GetDays'       #GetSystemTimeAsFileTime(), returns the number of days
!define StdUtils.Rand             '!insertmacro _StdUtils_Rand'          #rand(), as in C standard library
!define StdUtils.RandMax          '!insertmacro _StdUtils_RandMax'       #rand(), as in C standard library, with maximum value
!define StdUtils.RandMinMax       '!insertmacro _StdUtils_RandMinMax'    #rand(), as in C standard library, with minimum/maximum value
!define StdUtils.RandList         '!insertmacro _StdUtils_RandList'      #rand(), as in C standard library, with list support
!define StdUtils.FormatStr        '!insertmacro _StdUtils_FormatStr'     #sprintf(), as in C standard library, one '%d' placeholder
!define StdUtils.FormatStr2       '!insertmacro _StdUtils_FormatStr2'    #sprintf(), as in C standard library, two '%d' placeholders
!define StdUtils.FormatStr3       '!insertmacro _StdUtils_FormatStr3'    #sprintf(), as in C standard library, three '%d' placeholders
!define StdUtils.ScanStr          '!insertmacro _StdUtils_ScanStr'       #sscanf(), as in C standard library, one '%d' placeholder
!define StdUtils.ScanStr2         '!insertmacro _StdUtils_ScanStr2'      #sscanf(), as in C standard library, two '%d' placeholders
!define StdUtils.ScanStr3         '!insertmacro _StdUtils_ScanStr3'      #sscanf(), as in C standard library, three '%d' placeholders
!define StdUtils.TrimStr          '!insertmacro _StdUtils_TrimStr'       #Remove whitspaces from string, left and right
!define StdUtils.TrimStrLeft      '!insertmacro _StdUtils_TrimStrLeft'   #Remove whitspaces from string, left side only
!define StdUtils.TrimStrRight     '!insertmacro _StdUtils_TrimStrRight'  #Remove whitspaces from string, right side only
!define StdUtils.RevStr           '!insertmacro _StdUtils_RevStr'        #Reverse a string, e.g. "reverse me" <-> "em esrever"
!define StdUtils.SHFileMove       '!insertmacro _StdUtils_SHFileMove'    #SHFileOperation(), using the FO_MOVE operation
!define StdUtils.SHFileCopy       '!insertmacro _StdUtils_SHFileCopy'    #SHFileOperation(), using the FO_COPY operation
!define StdUtils.ExecShellAsUser  '!insertmacro _StdUtils_ExecShlUser'   #ShellExecute() as NON-elevated user from elevated installer
!define StdUtils.InvokeShellVerb  '!insertmacro _StdUtils_InvkeShlVrb'   #Invokes a "shell verb", e.g. for pinning items to the taskbar
!define StdUtils.ExecShellWaitEx  '!insertmacro _StdUtils_ExecShlWaitEx' #ShellExecuteEx(), returns the handle of the new process
!define StdUtils.WaitForProcEx    '!insertmacro _StdUtils_WaitForProcEx' #WaitForSingleObject(), e.g. to wait for a running process
!define StdUtils.GetParameter     '!insertmacro _StdUtils_GetParameter'  #Get the value of a specific command-line option
!define StdUtils.GetAllParameters '!insertmacro _StdUtils_GetAllParams'  #Get complete command-line, but without executable name
!define StdUtils.GetRealOSVersion '!insertmacro _StdUtils_GetRealOSVer'  #Get the *real* Windows version number, even on Windows 8.1+
!define StdUtils.GetRealOSBuildNo '!insertmacro _StdUtils_GetRealOSBld'  #Get the *real* Windows build number, even on Windows 8.1+
!define StdUtils.GetRealOSName    '!insertmacro _StdUtils_GetRealOSStr'  #Get the *real* Windows version, as a "friendly" name
!define StdUtils.GetOSEdition     '!insertmacro _StdUtils_GetOSEdition'  #Get the Windows edition, i.e. "workstation" or "server"
!define StdUtils.VerifyOSVersion  '!insertmacro _StdUtils_VrfyRealOSVer' #Compare *real* operating system to an expected version number
!define StdUtils.VerifyOSBuildNo  '!insertmacro _StdUtils_VrfyRealOSBld' #Compare *real* operating system to an expected build number
!define StdUtils.GetLibVersion    '!insertmacro _StdUtils_GetLibVersion' #Get the current StdUtils library version (for debugging)
!define StdUtils.SetVerbose       '!insertmacro _StdUtils_SetVerbose'    #Enable or disable "verbose" mode (for debugging)


#################################################################################
# MACRO DEFINITIONS
#################################################################################

!macro _StdUtils_Time out
	StdUtils::Time /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetMinutes out
	StdUtils::GetMinutes /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetHours out
	StdUtils::GetHours /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetDays out
	StdUtils::GetDays /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_Rand out
	StdUtils::Rand /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_RandMax out max
	push ${max}
	StdUtils::RandMax /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_RandMinMax out min max
	push ${min}
	push ${max}
	StdUtils::RandMinMax /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_RandList count max
	push ${max}
	push ${count}
	StdUtils::RandList /NOUNLOAD
!macroend

!macro _StdUtils_FormatStr out format val
	push '${format}'
	push ${val}
	StdUtils::FormatStr /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_FormatStr2 out format val1 val2
	push '${format}'
	push ${val1}
	push ${val2}
	StdUtils::FormatStr2 /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_FormatStr3 out format val1 val2 val3
	push '${format}'
	push ${val1}
	push ${val2}
	push ${val3}
	StdUtils::FormatStr3 /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_ScanStr out format input default
	push '${format}'
	push '${input}'
	push ${default}
	StdUtils::ScanStr /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_ScanStr2 out1 out2 format input default1 default2
	push '${format}'
	push '${input}'
	push ${default1}
	push ${default2}
	StdUtils::ScanStr2 /NOUNLOAD
	pop ${out1}
	pop ${out2}
!macroend

!macro _StdUtils_ScanStr3 out1 out2 out3 format input default1 default2 default3
	push '${format}'
	push '${input}'
	push ${default1}
	push ${default2}
	push ${default3}
	StdUtils::ScanStr3 /NOUNLOAD
	pop ${out1}
	pop ${out2}
	pop ${out3}
!macroend

!macro _StdUtils_TrimStr var
	push ${var}
	StdUtils::TrimStr /NOUNLOAD
	pop ${var}
!macroend

!macro _StdUtils_TrimStrLeft var
	push ${var}
	StdUtils::TrimStrLeft /NOUNLOAD
	pop ${var}
!macroend

!macro _StdUtils_TrimStrRight var
	push ${var}
	StdUtils::TrimStrRight /NOUNLOAD
	pop ${var}
!macroend

!macro _StdUtils_RevStr var
	push ${var}
	StdUtils::RevStr /NOUNLOAD
	pop ${var}
!macroend

!macro _StdUtils_SHFileMove out from to hwnd
	push '${from}'
	push '${to}'
	push ${hwnd}
	StdUtils::SHFileMove /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_SHFileCopy out from to hwnd
	push '${from}'
	push '${to}'
	push ${hwnd}
	StdUtils::SHFileCopy /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_ExecShlUser out file verb args
	push '${file}'
	push '${verb}'
	push '${args}'
	StdUtils::ExecShellAsUser /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_InvkeShlVrb out path file verb_id
	push "${path}"
	push "${file}"
	push ${verb_id}
	StdUtils::InvokeShellVerb /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_ExecShlWaitEx out_res out_val file verb args
	push '${file}'
	push '${verb}'
	push '${args}'
	StdUtils::ExecShellWaitEx /NOUNLOAD
	pop ${out_res}
	pop ${out_val}
!macroend

!macro _StdUtils_WaitForProcEx out handle
	push '${handle}'
	StdUtils::WaitForProcEx /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetParameter out name default
	push '${name}'
	push '${default}'
	StdUtils::GetParameter /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetAllParams out truncate
	push '${truncate}'
	StdUtils::GetAllParameters /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetRealOSVer out_major out_minor out_spack
	StdUtils::GetRealOsVersion /NOUNLOAD
	pop ${out_major}
	pop ${out_minor}
	pop ${out_spack}
!macroend

!macro _StdUtils_GetRealOSBld out
	StdUtils::GetRealOsBuildNo /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetRealOSStr out
	StdUtils::GetRealOsName /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_VrfyRealOSVer out major minor spack
	push '${major}'
	push '${minor}'
	push '${spack}'
	StdUtils::VerifyRealOsVersion /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_VrfyRealOSBld out build
	push '${build}'
	StdUtils::VerifyRealOsBuildNo /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetOSEdition out
	StdUtils::GetOsEdition /NOUNLOAD
	pop ${out}
!macroend

!macro _StdUtils_GetLibVersion out_ver out_tst
	StdUtils::GetLibVersion /NOUNLOAD
	pop ${out_ver}
	pop ${out_tst}
!macroend

!macro _StdUtils_SetVerbose on
	!if "${on}" != "0"
		StdUtils::EnableVerboseMode /NOUNLOAD
	!else
		StdUtils::DisableVerboseMode /NOUNLOAD
	!endif
!macroend


#################################################################################
# MAGIC NUMBERS
#################################################################################

!define StdUtils.Const.ISV_PinToTaskbar 5386
!define StdUtils.Const.ISV_UnpinFromTaskbar 5387
!define StdUtils.Const.ISV_PinToStartmenu 5381
!define StdUtils.Const.ISV_UnpinFromStartmenu 5382
