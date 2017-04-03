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

#include <Windows.h>
#include "UnicodeSupport.h"

static bool parse_parameter(const TCHAR *str, const size_t len, const TCHAR *arg_name, bool *first, TCHAR *dest_buff, size_t dest_size)
{
	if(*first)
	{
		*first = false;
		return false;
	}

	bool bSuccess = false;
	
	if((len > 1) && (str[0] == T('/')))
	{
		TCHAR *buffer = new TCHAR[len];
		memset(buffer, 0, sizeof(TCHAR) * len);
		STRNCPY(buffer, &str[1], len-1);

		TCHAR *offset = STRCHR(buffer, T('='));
		if(offset != NULL)
		{
			offset[0] = T('\0');
			if(STRICMP(buffer, arg_name) == 0)
			{
				bSuccess = true;
				STRNCPY(dest_buff, &offset[1], dest_size);
				dest_buff[dest_size-1] = T('\0');
			}
		}
		else
		{
			if(STRICMP(buffer, arg_name) == 0)
			{
				bSuccess = true;
				dest_buff[0] = T('\0');
			}
		}
	
		delete [] buffer;
	}

	return bSuccess;
}

bool parse_commandline(const TCHAR *arg_name, TCHAR *dest_buff, size_t dest_size)
{
	bool bSuccess = false;
	TCHAR *cmd = GetCommandLine();
	
	if(cmd)
	{
		bool first = true;
		size_t cmd_len = STRLEN(cmd);
		size_t tok_len = 0;
		TCHAR *tok_pos = NULL;
		bool flag = false;
		for(size_t i = 0; i < cmd_len; i++)
		{
			if(cmd[i] == T('\"'))
			{
				if(tok_pos != NULL)
				{
					if(parse_parameter(tok_pos, tok_len, arg_name, &first, dest_buff, dest_size))
					{
						bSuccess = true;
					}
				}
				tok_len = 0;
				tok_pos = NULL;
				flag = !flag;
				continue;
			}
			if((cmd[i] == L' ') && (flag == false))
			{
				if(tok_pos != NULL)
				{
					if(parse_parameter(tok_pos, tok_len, arg_name, &first, dest_buff, dest_size))
					{
						bSuccess = true;
					}
				}
				tok_len = 0;
				tok_pos = NULL;
				continue;
			}
			if(tok_pos == NULL)
			{
				tok_pos = &cmd[i];
			}
			tok_len++;
		}
		if(tok_pos != NULL)
		{
			if(parse_parameter(tok_pos, tok_len, arg_name, &first, dest_buff, dest_size))
			{
				bSuccess = true;
			}
		}
	}

	return bSuccess;
}

const TCHAR *get_commandline_arguments(void)
{
	TCHAR *cmd = GetCommandLine();
	static const TCHAR *error = T("error");

	if(cmd)
	{
		size_t i = 0;
		while(cmd[i] == T(' ')) i++;
		if(cmd[i] == T('\"'))
		{
			i++;
			while((cmd[i] != T('\0')) && (cmd[i] != T('\"'))) i++;
			if(cmd[i] == T('\"')) i++;
		}
		else
		{
			while((cmd[i] != T('\0')) && (cmd[i] != T(' ')) && (cmd[i] != T('\"'))) i++;
		}
		while(cmd[i] == T(' ')) i++;
		return &cmd[i];
	}
	else
	{
		return error;
	}
}