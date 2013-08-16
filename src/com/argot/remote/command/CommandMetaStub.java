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

package com.argot.remote.command;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.argot.meta.MetaSequence;


public class CommandMetaStub
implements InvocationHandler
{
	MetaCommand _mClass;
	CommandMetaClient _client;
	
	public CommandMetaStub( CommandMetaClient client, MetaCommand mClass )
	{
		_client = client;
		_mClass = mClass;
	}

	public Object invoke(Object proxy, Method method, Object[] args)
	throws Throwable
	{
		// Find the MetaMethod information.
		MetaSequence mMethod = _mClass.getTypeId( method );
		if ( mMethod == null )
		{
			throw new RemoteException("method not available");
		}
		
		// Create and process the request.
		RpcMetaMethodRequest request = new RpcMetaMethodRequest( _mClass, mMethod, args );
		
		_client.process( request );
		
		return null;

	}

	/*
	public String type()
	{
		return _mClass.getTypeName();
	}*/


}
