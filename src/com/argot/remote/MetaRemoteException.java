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

import com.argot.TypeBound;
import com.argot.TypeConstructor;
import com.argot.TypeElement;
import com.argot.TypeException;
import com.argot.TypeLibrary;
import com.argot.TypeLibraryReader;
import com.argot.TypeLibraryWriter;
import com.argot.TypeMap;
import com.argot.TypeOutputStream;
import com.argot.TypeReader;
import com.argot.TypeWriter;
import com.argot.auto.TypeConstructorAuto;
import com.argot.auto.TypeReaderAuto;
import com.argot.common.U8Ascii;
import com.argot.common.UInt16;
import com.argot.meta.MetaAbstract;

/**
 * This is a remote exception with stack trace.  It allows an exception and full stack
 * trace to be sent back over the wire.  Any local exception will have its
 * full stack trace copied.
 */
public class MetaRemoteException
{
	public static final String TYPENAME = "remote.exception";
	public static final String VERSION = "1.3";

	public static class ExceptionConstructor
	implements TypeConstructor
	{
		TypeConstructorAuto _autoConstructor;
		
		public ExceptionConstructor(Class<?> clazz)
		throws TypeException
		{
			_autoConstructor = new TypeConstructorAuto(clazz);
		}
		
		public Object construct(TypeElement sequence, Object[] parameters) 
		throws TypeException 
		{
			// the elements are wrapped in a sequence array.
			Object[] stackTrace = (Object[]) parameters[0];

			// To keep a local exception, create another exception in the chain
			// and swap the stack trace.  Local stack trace first, then remote.
			Throwable localException = new Exception("Remote", (Throwable) stackTrace[1]);
			StackTraceElement[] localStack = localException.getStackTrace();
			
			Object[] exceptionParams = new Object[2];
			exceptionParams[0] = stackTrace[0];
			exceptionParams[1] = localException;
			
			Throwable ex = (Throwable) _autoConstructor.construct(sequence, exceptionParams);
			ex.setStackTrace(localStack);
			
			localException.setStackTrace(convertStackTrace((Object[]) stackTrace[2]));
			
			
			return ex;
		}
		
		private StackTraceElement[] convertStackTrace( Object[] remoteStack )
		{
			StackTraceElement[] localTrace = new StackTraceElement[ remoteStack.length ];
			for ( int x = 0 ; x < remoteStack.length; x++ )
			{
				MetaRemoteStackTraceElement element = (MetaRemoteStackTraceElement) remoteStack[x];
				localTrace[x] = new StackTraceElement( element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber());
			}
			return localTrace;
		}
		
	}
	
	public static class Reader
	implements TypeLibraryReader,TypeBound
	{
		private TypeReaderAuto _autoReader;
		
		public Reader(Class<?> clazz) 
		throws TypeException
		{
			if (clazz.isAssignableFrom(Exception.class))
			{
				throw new TypeException("RemoteExceptionBasicReader class must extend Exception");
			}
			_autoReader = new TypeReaderAuto(new ExceptionConstructor(clazz));
		}

		public void bind(TypeLibrary library, int definitionId, TypeElement definition) 
		throws TypeException 
		{
			_autoReader.bind(library, definitionId, definition);
		}
		
		public TypeReader getReader(TypeMap map) 
		throws TypeException 
		{
			return _autoReader.getReader(map);
		}
	}

	public static boolean isWrapRequired(TypeLibrary library, Throwable cause)
	{
		try {
			int[] ids = library.getId(cause.getClass());
			if ( ids.length<=1)
			{
				throw new TypeException("Exception mapped to multiple types");
			}
			MetaAbstract metaAbstract = (MetaAbstract) library.getStructure( library.getDefinitionId("remote.exception","1.3"));
			return !metaAbstract.isMapped(ids[0]);
		} catch (TypeException e) {
			return false;
		}
	}
	
	public static class Writer
	implements TypeLibraryWriter,TypeWriter
	{
		public void write(TypeOutputStream out, Object o)
		throws TypeException, IOException
		{
			Throwable e = (Throwable) o;
			
			// cut the message off at 255 characters.
			String message = e.getMessage();
			message = (message==null)?"":message.substring(0, (message.length()>255)?255:message.length());
			out.writeObject( U8Ascii.TYPENAME, message );
			
			// if the cause is null simply write out the empty exception type.
			// if the cause is not null check if the cause is defined in the
			// type library.  If it is defined and is mapped to remote.exception
			// it can be written using remote.exception.  If its any other situation
			// then we would like to have the details, so wrap it in
			// a MetaRemoteException and send the details.
			Throwable cause = e.getCause();
			if (cause!=null)
			{
				try {
					TypeLibrary library = out.getTypeMap().getLibrary();
					int[] ids = library.getId(cause.getClass());
					if (ids.length != 1)
					{
						if (ids.length>1)
							throw new TypeException("Class bound to multiple system types:" +o.getClass().getName());
						if (ids.length==0)
							throw new TypeException("Class not bound to any mapped type:"+o.getClass().getName());
					}
					
					MetaAbstract metaAbstract = (MetaAbstract) library.getStructure( library.getDefinitionId("remote.exception","1.3"));
					if (metaAbstract.isMapped(ids[0]))
					{
						out.writeObject( "remote.exception", cause);
						return;
					}
				} catch (TypeException ex) {
					// exception not in type library.
				}
				WrappedRemoteException wrapped = new WrappedRemoteException(cause);
				out.writeObject("remote.exception", wrapped);
			} else {
				out.writeObject( "uvint28", new Integer( out.getTypeMap().getStreamId("empty")));
			}
			
			// write out the stack trace array.
			StackTraceElement[] elements = e.getStackTrace();
			out.writeObject( UInt16.TYPENAME, new Integer( elements.length ));			
			for ( int x = 0 ; x < elements.length ; x++ )
			{
				MetaRemoteStackTraceElement trace = new MetaRemoteStackTraceElement( elements[x].getClassName(), elements[x].getMethodName(), elements[x].getFileName(), elements[x].getLineNumber());
				out.writeObject( MetaRemoteStackTraceElement.TYPENAME, trace );
			}
		}

		public TypeWriter getWriter(TypeMap map) 
		throws TypeException 
		{
			return this;
		}
	}
}
