///////////////////////////////////////////////////////////////////////////////
// StdUtils plug-in for NSIS
// Copyright (C) 2004-2016 LoRd_MuldeR <MuldeR2@GMX.de>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
//
// http://www.gnu.org/licenses/lgpl-2.1.txt
///////////////////////////////////////////////////////////////////////////////

#define WIN32_LEAN_AND_MEAN 1
#include <Windows.h>

#include <climits>

#include "UnicodeSupport.h"
#include "DetectOsVersion.h"
#include "Mutex.h"
#include "msvc_utils.h"

//Forward declaration
static bool get_os_info(OSVERSIONINFOEXW *const osInfo);
static bool verify_os_version(const DWORD major, const DWORD minor, const WORD spack);
static bool verify_os_buildNo(const DWORD buildNo);

//External vars
extern bool g_bStdUtilsVerbose;
extern RTL_CRITICAL_SECTION g_pStdUtilsMutex;

//Lazy initialization vars
static volatile unsigned int g_os_version_major = 0;
static volatile unsigned int g_os_version_minor = 0;
static volatile unsigned int g_os_version_spack = 0;
static volatile unsigned int g_os_version_build = 0;
static volatile bool g_os_version_bOverride = false;

/*
 * Determine the *real* Windows version
 */
bool get_real_os_version(unsigned int *const major, unsigned int *const minor, unsigned int *const spack, bool *const pbOverride)
{
	static const DWORD MAX_VALUE = 0xFFFF;

	*major = *minor = *spack = 0;
	*pbOverride = false;
	
	//Enter critical section
	MutexLocker lock(&g_pStdUtilsMutex);

	//Already initialized?
	if((g_os_version_major != 0) || (g_os_version_minor != 0) || (g_os_version_spack != 0))
	{
		*major = g_os_version_major;
		*minor = g_os_version_minor;
		*spack = g_os_version_spack;
		*pbOverride = g_os_version_bOverride;
		return true;
	}

	//Temporary unlock
	lock.unlock();

	//Initialize local variables
	OSVERSIONINFOEXW osvi;

	//Try GetVersionEx() first
	if(get_os_info(&osvi) == FALSE)
	{
		if(g_bStdUtilsVerbose)
		{
			MessageBox(0, T("GetVersionEx() has failed, cannot detect Windows version!"), T("StdUtils::get_real_os_version"), MB_ICONERROR|MB_TOPMOST);
		}
		return false;
	}

	//Make sure we are running on NT
	if(osvi.dwPlatformId == VER_PLATFORM_WIN32_NT)
	{
		*major = osvi.dwMajorVersion;
		*minor = osvi.dwMinorVersion;
		*spack = osvi.wServicePackMajor;
	}
	else
	{
		//Workaround for Windows 9x comaptibility mode
		if(verify_os_version(4, 0, 0))
		{
			*pbOverride = true;
			*major = 4;
		}
		else
		{
			//Really not running on Windows NT
			return false;
		}
	}

	//Determine the real *major* version first
	for(DWORD nextMajor = (*major) + 1; nextMajor <= MAX_VALUE; nextMajor++)
	{
		if(verify_os_version(nextMajor, 0, 0))
		{
			*major = nextMajor;
			*minor = *spack = 0;
			*pbOverride = true;
			continue;
		}
		break;
	}

	//Now also determine the real *minor* version
	for(DWORD nextMinor = (*minor) + 1; nextMinor <= MAX_VALUE; nextMinor++)
	{
		if(verify_os_version((*major), nextMinor, 0))
		{
			*minor = nextMinor;
			*spack = 0;
			*pbOverride = true;
			continue;
		}
		break;
	}

	//Finally determine the real *servicepack* version
	for(WORD nextSpack = (*spack) + 1; nextSpack <= MAX_VALUE; nextSpack++)
	{
		if(verify_os_version((*major), (*minor), nextSpack))
		{
			*spack = nextSpack;
			*pbOverride = true;
			continue;
		}
		break;
	}

	//Overflow detected?
	if((*major >= MAX_VALUE) || (*minor >= MAX_VALUE) || (*spack >= MAX_VALUE))
	{
		return false;
	}

	//Enter critical section
	lock.relock();

	//Save the results
	if((g_os_version_major == 0) && (g_os_version_minor == 0) && (g_os_version_spack == 0))
	{
		g_os_version_major = *major;
		g_os_version_minor = *minor;
		g_os_version_spack = *spack;
		g_os_version_bOverride = g_os_version_bOverride || (*pbOverride);
	}
	else
	{
		*major = g_os_version_major;
		*minor = g_os_version_minor;
		*spack = g_os_version_spack;
		*pbOverride = g_os_version_bOverride;
	}

	//Done
	return true;
}

/*
 * Determine the *real* Windows build number
 */
bool get_real_os_buildNo(unsigned int *const buildNo, bool *const pbOverride)
{
	*buildNo = 0;
	*pbOverride = false;
	
	//Enter critical section
	MutexLocker lock(&g_pStdUtilsMutex);

	//Already initialized?
	if(g_os_version_build != 0)
	{
		*buildNo = g_os_version_build;
		*pbOverride = g_os_version_bOverride;
		return true;
	}

	//Temporary unlock
	lock.unlock();

	//Initialize local variables
	OSVERSIONINFOEXW osvi;
	memset(&osvi, 0, sizeof(OSVERSIONINFOEXW));
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXW);

	//Try GetVersionEx() first
	if(get_os_info(&osvi) == FALSE)
	{
		if(g_bStdUtilsVerbose)
		{
			MessageBox(0, T("GetVersionEx() has failed, cannot detect Windows version!"), T("StdUtils::get_real_os_buildNo"), MB_ICONERROR|MB_TOPMOST);
		}
		return false;
	}

	//Make sure we are running on NT
	if(osvi.dwPlatformId == VER_PLATFORM_WIN32_NT)
	{
		*buildNo = osvi.dwBuildNumber;
	}
	else
	{
		//Workaround for Windows 9x comaptibility mode
		if(verify_os_version(4, 0, 0))
		{
			*pbOverride = true;
			*buildNo = 1381;
		}
		else
		{
			//Really not running on Windows NT
			return false;
		}
	}

	//Determine the real build number
	DWORD stepSize = 4096;
	for(DWORD nextBuildNo = (*buildNo); nextBuildNo < INT_MAX; nextBuildNo = (*buildNo) + stepSize)
	{
		if(verify_os_buildNo(nextBuildNo))
		{
			*buildNo = nextBuildNo;
			*pbOverride = true;
			continue;
		}
		if(stepSize > 1)
		{
			stepSize = stepSize / 2;
			continue;
		}
		break;
	}

	//Enter critical section
	lock.relock();

	//Save the results
	if(g_os_version_build == 0)
	{
		g_os_version_build = *buildNo;
		g_os_version_bOverride = g_os_version_bOverride || (*pbOverride);
	}
	else
	{
		*buildNo = g_os_version_build;
		*pbOverride = g_os_version_bOverride;
	}

	//Done
	return true;
}

/*
 * Get friendly OS version name
 */
const TCHAR *get_os_friendly_name(const DWORD major, const DWORD minor)
{
	static const size_t NAME_COUNT = 9;

	static const struct
	{
		const DWORD major;
		const DWORD minor;
		const TCHAR name[20];
	}
	s_names[NAME_COUNT] =
	{
		{  4, 0, T("Windows NT 4.0"  ) },
		{  5, 0, T("Windows 2000"    ) },
		{  5, 1, T("Windows XP"      ) },
		{  5, 2, T("Windows XP (x64)") },
		{  6, 0, T("Windows Vista"   ) },
		{  6, 1, T("Windows 7"       ) },
		{  6, 2, T("Windows 8"       ) },
		{  6, 3, T("Windows 8.1"     ) },
		{ 10, 0, T("Windows 10"      ) }
	};

	for(size_t i = 0; i < NAME_COUNT; i++)
	{
		if((s_names[i].major == major) && (s_names[i].minor == minor))
		{
			return &s_names[i].name[0];
		}
	}

	if(g_bStdUtilsVerbose)
	{
		TCHAR buffer[256];
		SNPRINTF(buffer, 256, T("Running on an unknown windows version v%u.%u!"), major, minor);
		buffer[255] = '\0';
		MessageBox(0, buffer, T("StdUtils::get_os_friendly_name"), MB_ICONWARNING|MB_TOPMOST);
	}

	return T("unknown");
}

/*
 * Checks whether OS is a "server" (or "workstation") edition
 */
bool get_os_server_edition(bool &bIsServer)
{
	bIsServer = false;
	bool success = false;

	//Initialize local variables
	OSVERSIONINFOEXW osvi;
	memset(&osvi, 0, sizeof(OSVERSIONINFOEXW));
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXW);

	//Check for server/workstation edition
	if(get_os_info(&osvi) != FALSE)
	{
		switch(osvi.wProductType)
		{
		case VER_NT_SERVER:
		case VER_NT_DOMAIN_CONTROLLER:
			success = true;
			bIsServer = true;
			break;
		case VER_NT_WORKSTATION:
			success = true;
			bIsServer = false;
			break;
		}
	}

	return success;
}

//===========================================================================
// INTERNAL FUNCTIONS
//===========================================================================

typedef LONG(__stdcall *RtlGetVersion)(LPOSVERSIONINFOEXW);
typedef LONG(__stdcall *RtlVerifyVersionInfo)(LPOSVERSIONINFOEXW, ULONG, ULONGLONG);

static void initialize_os_version(OSVERSIONINFOEXW *const osInfo)
{
	memset(osInfo, 0, sizeof(OSVERSIONINFOEXW));
	osInfo->dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXW);
}

/*
 * Get OS version info
 */
static bool get_os_info(OSVERSIONINFOEXW *const osInfo)
{
	if (const HMODULE ntdll = GetModuleHandleW(L"ntdll"))
	{
		if (const RtlGetVersion pRtlGetVersion = (RtlGetVersion) GetProcAddress(ntdll, "RtlGetVersion"))
		{
			initialize_os_version(osInfo);
			if (pRtlGetVersion(osInfo) == 0)
			{
				return true;
			}
		}
	}

	//Fallback
	initialize_os_version(osInfo);
	return (GetVersionExW((LPOSVERSIONINFOW)osInfo) != FALSE);
}

/*
 * Verify OS version info
 */
static bool verify_os_info(OSVERSIONINFOEXW *const osInfo, const ULONG typeMask, const ULONGLONG condMask)
{
	if (const HMODULE ntdll = GetModuleHandleW(L"ntdll"))
	{
		if (const RtlVerifyVersionInfo pRtlVerifyVersionInfo = (RtlVerifyVersionInfo) GetProcAddress(ntdll, "RtlVerifyVersionInfo"))
		{
			if (pRtlVerifyVersionInfo(osInfo, typeMask, condMask) == 0)
			{
				return true;
			}
		}
	}

	//Fallback
	return (VerifyVersionInfoW(osInfo, typeMask, condMask) != FALSE);
}

/*
 * Verify a specific Windows version
 */
static bool verify_os_version(const DWORD major, const DWORD minor, const WORD spack)
{
	OSVERSIONINFOEXW osvi;
	DWORDLONG dwlConditionMask = 0;

	//Initialize the OSVERSIONINFOEX structure
	initialize_os_version(&osvi);

	//Fille the OSVERSIONINFOEX structure
	osvi.dwMajorVersion    = major;
	osvi.dwMinorVersion    = minor;
	osvi.wServicePackMajor = spack;
	osvi.dwPlatformId      = VER_PLATFORM_WIN32_NT;

	//Initialize the condition mask
	VER_SET_CONDITION(dwlConditionMask, VER_MAJORVERSION,     VER_GREATER_EQUAL);
	VER_SET_CONDITION(dwlConditionMask, VER_MINORVERSION,     VER_GREATER_EQUAL);
	VER_SET_CONDITION(dwlConditionMask, VER_SERVICEPACKMAJOR, VER_GREATER_EQUAL);
	VER_SET_CONDITION(dwlConditionMask, VER_PLATFORMID,       VER_EQUAL);

	// Perform the test
	const BOOL ret = verify_os_info(&osvi, VER_MAJORVERSION | VER_MINORVERSION | VER_SERVICEPACKMAJOR | VER_PLATFORMID, dwlConditionMask);

	//Error checking
	if(!ret)
	{
		if(GetLastError() != ERROR_OLD_WIN_VERSION)
		{
			if(g_bStdUtilsVerbose)
			{
				MessageBox(0, T("VerifyVersionInfo() has failed, cannot test Windows version!"), T("StdUtils::verify_os_version"), MB_ICONERROR|MB_TOPMOST);
			}
		}
	}

	return (ret != FALSE);
}

/*
 * Verify a specific Windows build
 */
static bool verify_os_buildNo(const DWORD buildNo)
{
	OSVERSIONINFOEXW osvi;
	DWORDLONG dwlConditionMask = 0;

	//Initialize the OSVERSIONINFOEX structure
	initialize_os_version(&osvi);

	//Fille the OSVERSIONINFOEX structure
	osvi.dwBuildNumber = buildNo;

	//Initialize the condition mask
	VER_SET_CONDITION(dwlConditionMask, VER_BUILDNUMBER, VER_GREATER_EQUAL);

	// Perform the test
	const BOOL ret = verify_os_info(&osvi, VER_BUILDNUMBER, dwlConditionMask);

	//Error checking
	if(!ret)
	{
		if(GetLastError() != ERROR_OLD_WIN_VERSION)
		{
			if(g_bStdUtilsVerbose)
			{
				MessageBox(0, T("VerifyVersionInfo() has failed, cannot test Windows version!"), T("StdUtils::verify_os_buildNo"), MB_ICONERROR|MB_TOPMOST);
			}
		}
	}

	return (ret != FALSE);
}

/*eof*/
