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

wchar_t *ansi_to_utf16(const char *input);
wchar_t *utf8_to_utf16(const char *input);

char *strtrim(char* input, bool trim_left = true, bool trim_right = true);
wchar_t *wcstrim(wchar_t* input, bool trim_left = true, bool trim_right = true);

#ifdef UNICODE
	#define STRLEN wcslen	
	#define STRICMP _wcsicmp
	#define STRNCPY wcsncpy
	#define STRCHR wcschr
	#define STRTRIM wcstrim
	#define SNPRINTF _snwprintf
	#define SSCANF swscanf
	#define __T__(X) L##X
	#define T(X) __T__(X)
#else
	#define STRLEN strlen
	#define STRICMP _stricmp
	#define STRNCPY strncpy
	#define STRCHR strchr
	#define STRTRIM strtrim
	#define SNPRINTF _snprintf
	#define SSCANF sscanf
	#define T(X) X
#endif
