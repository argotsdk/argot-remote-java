/*
 * Copyright (c) 2003-2010, Live Media Pty. Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this list of
 *     conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice, this list of
 *     conditions and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *  3. Neither the name of Live Media nor the names of its contributors may be used to endorse
 *     or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.argot.remote;

import java.io.IOException;

import com.argot.TypeException;
import com.argot.TypeLibraryWriter;
import com.argot.TypeMap;
import com.argot.TypeOutputStream;
import com.argot.TypeWriter;
import com.argot.common.Int32;
import com.argot.common.U8Ascii;

public class MetaRemoteStackTraceElement
{
	public static final String TYPENAME = "remote.stack_trace_element";
	public static final String VERSION = "1.3";
	
	private String _class;
	private String _method;
	private String _file;
	private int _line;
	
	public MetaRemoteStackTraceElement( String clss, String method, String file, int line )
	{
		_class = clss;
		_method = method;
		_file = file;
		_line = (int) line;
	}
	
	public String getClassName()
	{
		return _class;
	}
	
	public String getMethodName()
	{
		return _method;
	}
	
	public int getLineNumber()
	{
		return _line;
	}
	
	public String getFileName()
	{
		return _file;
	}
	
	public String toString()
	{
		return _class + "." + _method + "(" + _file + ":" + _line + ")";
	}

	public static class MetaRemoteStackTraceElementWriter
	implements TypeLibraryWriter,TypeWriter
	{
		public void write(TypeOutputStream out, Object o)
		throws TypeException, IOException
		{
			MetaRemoteStackTraceElement e = (MetaRemoteStackTraceElement) o;
			out.writeObject( U8Ascii.TYPENAME, e.getClassName() );
			out.writeObject( U8Ascii.TYPENAME, e.getMethodName() );
			out.writeObject( U8Ascii.TYPENAME, e.getFileName() );
			out.writeObject( Int32.TYPENAME, new Integer( e.getLineNumber() ));
		}
		
		public TypeWriter getWriter(TypeMap map) 
		throws TypeException 
		{
			return this;
		}		
	}
}
