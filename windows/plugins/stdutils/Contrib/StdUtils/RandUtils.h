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

typedef BOOLEAN (__stdcall *secure_rand_t)(PVOID RandomBuffer, ULONG RandomBufferLength);

static bool s_secure_rand_init = false;
static secure_rand_t s_secure_rand = NULL;

/*RAII mutext locker class*/
class MutexLocker
{
public:
	MutexLocker(LPCRITICAL_SECTION mutex)
	:
		m_mutex(mutex)
	{
		EnterCriticalSection(m_mutex);
	}
	~MutexLocker(void)
	{
		LeaveCriticalSection(m_mutex);
	}
private:
	LPCRITICAL_SECTION const m_mutex;
};

/* Robert Jenkins' 96 bit Mix Function */
static unsigned int mix_function(const unsigned int x, const unsigned int y, const unsigned int z)
{
	unsigned int a = x;
	unsigned int b = y;
	unsigned int c = z;
	
	a=a-b;  a=a-c;  a=a^(c >> 13);
	b=b-c;  b=b-a;  b=b^(a << 8); 
	c=c-a;  c=c-b;  c=c^(b >> 13);
	a=a-b;  a=a-c;  a=a^(c >> 12);
	b=b-c;  b=b-a;  b=b^(a << 16);
	c=c-a;  c=c-b;  c=c^(b >> 5);
	a=a-b;  a=a-c;  a=a^(c >> 3);
	b=b-c;  b=b-a;  b=b^(a << 10);
	c=c-a;  c=c-b;  c=c^(b >> 15);

	return c;
}

static void init_rand(void)
{
	MutexLocker locker(&g_mutex);

	if(!s_secure_rand_init)
	{
		srand(static_cast<unsigned int>(time(NULL)));

		HMODULE advapi32 = GetModuleHandle(_T("Advapi32.dll"));
		if(advapi32)
		{
			s_secure_rand = reinterpret_cast<secure_rand_t>(GetProcAddress(advapi32, "SystemFunction036"));
		}

		s_secure_rand_init = true;
	}
}

static unsigned int next_rand(void)
{
	init_rand();

	if(s_secure_rand)
	{
		unsigned int rnd;
		if(s_secure_rand(&rnd, sizeof(unsigned int)))
		{
			return rnd;
		}
	}

	unsigned int x = (RAND_MAX * static_cast<unsigned int>(rand())) + static_cast<unsigned int>(rand());
	unsigned int y = (RAND_MAX * static_cast<unsigned int>(rand())) + static_cast<unsigned int>(rand());
	unsigned int z = (RAND_MAX * static_cast<unsigned int>(rand())) + static_cast<unsigned int>(rand());
	
	return mix_function(x, y, z);
}
