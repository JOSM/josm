///////////////////////////////////////////////////////////////////////////////
// StdUtils plug-in for NSIS
// Copyright (C) 2004-2014 LoRd_MuldeR <MuldeR2@GMX.de>
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

#define WIN32_LEAN_AND_MEAN
#include <Windows.h>

#include <climits>

#include "UnicodeSupport.h"
#include "DetectOsVersion.h"

//Forward declaration
static bool verify_os_version(const DWORD major, const DWORD minor, const DWORD spack);
static bool verify_os_buildNo(const DWORD buildNo);

/*
 * Determine the *real* Windows version
 */
bool get_real_os_version(unsigned int *major, unsigned int *minor, unsigned int *spack, bool *pbOverride)
{
	static const DWORD MAX_VALUE = 1024;

	*major = *minor = *spack = 0;
	*pbOverride = false;
	
	//Initialize local variables
	OSVERSIONINFOEXW osvi;
	memset(&osvi, 0, sizeof(OSVERSIONINFOEXW));
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXW);

	//Try GetVersionEx() first
	if(GetVersionExW((LPOSVERSIONINFOW)&osvi) == FALSE)
	{
		/*fprintf(stderr, "GetVersionEx() has failed, cannot detect Windows version!\n");*/
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
	for(DWORD nextSpack = (*spack) + 1; nextSpack <= MAX_VALUE; nextSpack++)
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

	return true;
}

/*
 * Determine the *real* Windows build number
 */
bool get_real_os_buildNo(unsigned int *buildNo, bool *pbOverride)
{
	*buildNo = 0;
	*pbOverride = false;
	
	//Initialize local variables
	OSVERSIONINFOEXW osvi;
	memset(&osvi, 0, sizeof(OSVERSIONINFOEXW));
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXW);

	//Try GetVersionEx() first
	if(GetVersionExW((LPOSVERSIONINFOW)&osvi) == FALSE)
	{
		/*fprintf(stderr, "GetVersionEx() has failed, cannot detect Windows version!\n");*/
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

	return true;
}

/*
 * Get friendly OS version name
 */
const TCHAR *get_os_friendly_name(const DWORD major, const DWORD minor)
{
	static const size_t NAME_COUNT = 8;

	static const struct
	{
		const DWORD major;
		const DWORD minor;
		const TCHAR name[6];
	}
	s_names[NAME_COUNT] =
	{
		{ 4, 0, T("winnt") },
		{ 5, 0, T("win2k") },
		{ 5, 1, T("winxp") },
		{ 5, 2, T("xpx64") },
		{ 6, 0, T("vista") },
		{ 6, 1, T("win70") },
		{ 6, 2, T("win80") },
		{ 6, 3, T("win81") }
	};

	for(size_t i = 0; i < NAME_COUNT; i++)
	{
		if((s_names[i].major == major) && (s_names[i].minor == minor))
		{
			return &s_names[i].name[0];
		}
	}

	return T("unknown");
}

/*
 * Verify a specific Windows version
 */
static bool verify_os_version(const DWORD major, const DWORD minor, const DWORD spack)
{
	OSVERSIONINFOEXW osvi;
	DWORDLONG dwlConditionMask = 0;

	//Initialize the OSVERSIONINFOEX structure
	memset(&osvi, 0, sizeof(OSVERSIONINFOEXW));

	//Fille the OSVERSIONINFOEX structure
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXW);
	osvi.dwMajorVersion      = major;
	osvi.dwMinorVersion      = minor;
	osvi.wServicePackMajor   = spack;
	osvi.dwPlatformId        = VER_PLATFORM_WIN32_NT;

	//Initialize the condition mask
	VER_SET_CONDITION(dwlConditionMask, VER_MAJORVERSION,     VER_GREATER_EQUAL);
	VER_SET_CONDITION(dwlConditionMask, VER_MINORVERSION,     VER_GREATER_EQUAL);
	VER_SET_CONDITION(dwlConditionMask, VER_SERVICEPACKMAJOR, VER_GREATER_EQUAL);
	VER_SET_CONDITION(dwlConditionMask, VER_PLATFORMID,       VER_EQUAL);

	// Perform the test
	const BOOL ret = VerifyVersionInfoW(&osvi, VER_MAJORVERSION | VER_MINORVERSION | VER_SERVICEPACKMAJOR | VER_PLATFORMID, dwlConditionMask);

	//Error checking
	if(!ret)
	{
		if(GetLastError() != ERROR_OLD_WIN_VERSION)
		{
			/*fprintf(stderr, "VerifyVersionInfo() system call has failed!\n");*/
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
	memset(&osvi, 0, sizeof(OSVERSIONINFOEXW));

	//Fille the OSVERSIONINFOEX structure
	osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEXW);
	osvi.dwBuildNumber       = buildNo;

	//Initialize the condition mask
	VER_SET_CONDITION(dwlConditionMask, VER_BUILDNUMBER, VER_GREATER_EQUAL);

	// Perform the test
	const BOOL ret = VerifyVersionInfoW(&osvi, VER_BUILDNUMBER, dwlConditionMask);

	//Error checking
	if(!ret)
	{
		if(GetLastError() != ERROR_OLD_WIN_VERSION)
		{
			/*fprintf(stderr, "VerifyVersionInfo() system call has failed!\n");*/
		}
	}

	return (ret != FALSE);
}

/*eof*/
