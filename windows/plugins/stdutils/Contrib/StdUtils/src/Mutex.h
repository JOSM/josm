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

#pragma once

#ifndef _INC_WINDOWS
#define WIN32_LEAN_AND_MEAN 1
#include <Windows.h>
#endif

/*RAII mutext locker class*/
class MutexLocker
{
public:
	MutexLocker(LPCRITICAL_SECTION mutex)
	:
		m_mutex(mutex)
	{
		EnterCriticalSection(m_mutex);
		m_locked = true;
	}
	~MutexLocker(void)
	{
		if(m_locked)
		{
			LeaveCriticalSection(m_mutex);
		}
	}
	void unlock(void)
	{
		if(!m_locked)
		{
			RaiseException(ERROR_POSSIBLE_DEADLOCK, EXCEPTION_NONCONTINUABLE, 0, NULL);
		}
		LeaveCriticalSection(m_mutex);
		m_locked = false;
	}
	void relock(void)
	{
		if(m_locked)
		{
			RaiseException(ERROR_POSSIBLE_DEADLOCK, EXCEPTION_NONCONTINUABLE, 0, NULL);
		}
		EnterCriticalSection(m_mutex);
		m_locked = true;
	}
private:
	LPCRITICAL_SECTION const m_mutex;
	volatile bool m_locked;
};
