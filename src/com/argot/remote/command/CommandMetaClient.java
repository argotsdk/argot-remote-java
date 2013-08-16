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

import java.io.IOException;

import com.argot.TypeException;
import com.argot.TypeInputStream;
import com.argot.TypeMap;
import com.argot.TypeOutputStream;
import com.argot.TypeReader;
import com.argot.TypeWriter;

import com.argot.remote.transport.TypeEndPoint;
import com.argot.remote.transport.TypeTransport;

public class CommandMetaClient
{
	private TypeTransport _back;
	private TypeMap _refMap;
	private TypeWriter _rpcMetaRequest;
	//private TypeReader _rpcMetaResponse;
	
	public CommandMetaClient( TypeTransport back, TypeMap map )
	throws TypeException
	{
		_back = back;
		_refMap = map;
		_rpcMetaRequest = map.getWriter(map.getStreamId(RpcMetaRequest.TYPENAME));
		//_rpcMetaResponse = map.getReader(map.getStreamId(RpcMetaResponse.TYPENAME));
	}

	public void process( RpcMetaMethodRequest methodRequest ) 
	throws RemoteException
	{		
		// Write out request to a stream.	
		TypeEndPoint endPoint;
		
		try 
		{
			endPoint = _back.openLink();
		} catch (IOException e) {
			throw new RemoteException( e.getMessage(), e );
		}
		
		try
		{	
			TypeOutputStream tmos = new TypeOutputStream( endPoint.getOutputStream(), _refMap );
			_rpcMetaRequest.write(tmos, methodRequest);
			tmos.getStream().flush();
		
			//TypeInputStream tmis = new TypeInputStream( endPoint.getInputStream(), _refMap );
			//RpcMetaMethodRequest methodResponse  = (RpcMetaMethodRequest) _rpcMetaResponse.read(tmis);
			//methodRequest.setResponse( methodResponse );
		}
		catch (TypeException e1)
		{
			throw new RemoteException( e1.getMessage(), e1 );
		}
		catch (IOException e1)
		{
			throw new RemoteException( e1.getMessage(), e1 );
		}
		finally
		{
			_back.closeLink(endPoint);
		}
	}



}
