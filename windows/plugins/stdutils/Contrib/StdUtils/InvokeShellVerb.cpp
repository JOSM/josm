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

///////////////////////////////////////////////////////////////////////////////
// The following code was strongly inspired by the "InvokeShellVerb" plug-in
// Copyright (c) 2011 Robert Strong
// For details see: http://nsis.sourceforge.net/Invoke_Shell_Verb_plugin
///////////////////////////////////////////////////////////////////////////////

#include "InvokeShellVerb.h"
#include "UnicodeSupport.h"
#include "msvc_utils.h"

#include <exdisp.h>
#include <Shobjidl.h>
#include <Shlwapi.h>
#include <SHLGUID.h>

///////////////////////////////////////////////////////////////////////////////

#include "ComUtils.h"

typedef struct
{
	const TCHAR *pcDirectoryName;
	const TCHAR *pcFileName;
	DWORD uiVerbId;
	int returnValue;
}
threadParam_t;

static const WCHAR *shell32 = L"shell32.dll";

///////////////////////////////////////////////////////////////////////////////

static unsigned __stdcall MyInvokeShellVerb_ThreadHelperProc(void* pArguments)
{
	HRESULT hr = CoInitialize(NULL);
	if((hr == S_OK) || (hr == S_FALSE))
	{
		if(threadParam_t *params = (threadParam_t*) pArguments)
		{
			params->returnValue = MyInvokeShellVerb(params->pcDirectoryName, params->pcFileName, params->uiVerbId, false);
		}
		DispatchPendingMessages(1000); //Required to avoid potential deadlock or crash on CoUninitialize() !!!
		CoUninitialize();
	}
	else
	{
		if(threadParam_t *params = (threadParam_t*) pArguments)
		{
			params->returnValue = -3;
		}
	}

	return EXIT_SUCCESS;
}

static int MyInvokeShellVerb_ShellDispatchProc(const TCHAR *pcDirectoryName, const TCHAR *pcFileName, const DWORD uiVerbId)
{
	int iSuccess = 0;

	bool bUnloadDll = false;
	HMODULE hShellDll = GetModuleHandleW(shell32); 
	if(hShellDll == NULL)
	{
		bUnloadDll = true;
		hShellDll = LoadLibraryW(shell32);
		if(hShellDll == NULL)
		{
			iSuccess = -3;
			return iSuccess;
		}
	}

	WCHAR pcVerbName[128];
	memset(pcVerbName, 0, sizeof(WCHAR) * 128);
	
	if(LoadStringW(hShellDll, uiVerbId, pcVerbName, 128) < 1)
	{
		if(bUnloadDll)
		{
			FreeLibrary(hShellDll);
			hShellDll = NULL;
		}
		iSuccess = -3;
		return iSuccess;
	}

	if(bUnloadDll)
	{
		FreeLibrary(hShellDll);
		hShellDll = NULL;
	}

	// ----------------------------------- //

	IShellDispatch *pShellDispatch = NULL;
	Folder *pFolder = NULL; FolderItem *pItem = NULL;
	
	HRESULT hr = CoCreateInstance(CLSID_Shell, NULL, CLSCTX_INPROC_SERVER, IID_IShellDispatch, (void**)&pShellDispatch);
	if(FAILED(hr) || (pShellDispatch ==  NULL))
	{
		iSuccess = -3;
		return iSuccess;
	}

	variant_t vaDirectory(pcDirectoryName);
	hr = pShellDispatch->NameSpace(vaDirectory, &pFolder);
	if(FAILED(hr) || (pFolder == NULL))
	{
		iSuccess = -3;
		pShellDispatch->Release();
		return iSuccess;
	}

	pShellDispatch->Release();
	pShellDispatch = NULL;

	variant_t vaFileName(pcFileName);
	hr = pFolder->ParseName(vaFileName, &pItem);
	if(FAILED(hr) || (pItem == NULL))
	{
		iSuccess = -3;
		pFolder->Release();
		return iSuccess;
	}

	pFolder->Release();
	pFolder = NULL;

	// ----------------------------------- //

	long iVerbCount = 0;
	FolderItemVerbs *pVerbs = NULL;
	
	hr = pItem->Verbs(&pVerbs);
	if(FAILED(hr) || (pVerbs == NULL))
	{
		iSuccess = -3;
		pItem->Release();
		return iSuccess;
	}

	pItem->Release();
	pItem = NULL;

	hr = pVerbs->get_Count(&iVerbCount);
	if(FAILED(hr) || (iVerbCount < 1))
	{
		iSuccess = -3;
		pVerbs->Release();
		return iSuccess;
	}

	DispatchPendingMessages(125);

	// ----------------------------------- //

	for(int i = 0; i < iVerbCount; i++)
	{
		variant_t vaVariantIndex(i);
		FolderItemVerb *pCurrentVerb = NULL;
		BSTR pcCurrentVerbName = NULL;

		hr = pVerbs->Item(vaVariantIndex, &pCurrentVerb);
		if (FAILED(hr) || (pCurrentVerb == NULL))
		{
			continue;
		}
		
		hr = pCurrentVerb->get_Name(&pcCurrentVerbName);
		if(FAILED(hr) || (pcCurrentVerbName == NULL))
		{
			pCurrentVerb->Release();
			continue;
		}

		if(_wcsicmp(pcCurrentVerbName, pcVerbName) == 0)
		{
			hr = pCurrentVerb->DoIt();
			if(!FAILED(hr))
			{
				iSuccess = 1;
			}
		}

		SysFreeString(pcCurrentVerbName);
		pCurrentVerb->Release();
	}

	pVerbs->Release();
	pVerbs = NULL;

	// ----------------------------------- //
	
	return iSuccess;
}

int MyInvokeShellVerb(const TCHAR *pcDirectoryName, const TCHAR *pcFileName, const DWORD uiVerbId, const bool threaded)
{
	int iSuccess = -1;

	OSVERSIONINFO osVersion;
	memset(&osVersion, 0, sizeof(OSVERSIONINFO));
	osVersion.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
	
	if(GetVersionEx(&osVersion))
	{
		if((osVersion.dwPlatformId == VER_PLATFORM_WIN32_NT) && ((osVersion.dwMajorVersion > 6) || ((osVersion.dwMajorVersion == 6) && (osVersion.dwMinorVersion >= 1))))
		{
			if(threaded)
			{
				threadParam_t threadParams = {pcDirectoryName, pcFileName, uiVerbId, 0};
				HANDLE hThread = (HANDLE) _beginthreadex(NULL, 0, MyInvokeShellVerb_ThreadHelperProc, &threadParams, 0, NULL);
				if((hThread != NULL) && (hThread != INVALID_HANDLE_VALUE))
				{
					DWORD status = WaitForSingleObject(hThread, 30000);
					if(status == WAIT_OBJECT_0)
					{
						iSuccess = threadParams.returnValue;
					}
					else if(status == WAIT_TIMEOUT)
					{
						iSuccess = -2;
						TerminateThread(hThread, EXIT_FAILURE);
					}
					CloseHandle(hThread);
					return iSuccess;
				}
			}
			else
			{
				iSuccess = MyInvokeShellVerb_ShellDispatchProc(pcDirectoryName, pcFileName, uiVerbId);
			}
		}
	}

	return iSuccess;
}
