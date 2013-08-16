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
import java.util.HashMap;

import com.argot.TypeException;
import com.argot.TypeLibrary;
import com.argot.TypeMap;
import com.argot.TypeMapper;
import com.argot.TypeMapperCore;
import com.argot.network.DynamicClientTypeMapper;
import com.argot.network.IProxyFactory;
import com.argot.network.TypeClient;
import com.argot.remote.transport.TypeTransport;



public class CommandClient
{
	private boolean _isBound;
	private TypeTransport _transport;
	private HashMap _factoryMap;

	private String _hostName;
	//private ColonyEnvironment _environment;
	private TypeLibrary _library;
	private DynamicClientTypeMapper _typeClientMapper;
	private boolean _disableMetaDictionaryCheck;
	private TypeClient _typeClient;
	private TypeMap _clientTypeMap;
	private CommandMetaClient _rpcMetaClient;
	
	public CommandClient( TypeLibrary library, TypeTransport transport )
	{
		_isBound = false;
		_library = library;
		_transport = transport;
		_factoryMap = new HashMap();
		_disableMetaDictionaryCheck = false;
	}
	
	public void disableMetaDictionaryCheck()
	{
		_disableMetaDictionaryCheck = true;
	}

	public TypeLibrary getTypeLibrary()
	{
		return _library;
	}

	public Object getFront( Class clss ) 
	throws RemoteException, TypeException
	{
		return getFactory(clss).getProxy();
	}
	
	public String getHost()
	{
		return _hostName;
	}

	public IProxyFactory getFactory( Class clss ) 
	throws TypeException
	{
		IProxyFactory factory = (IProxyFactory) _factoryMap.get(clss);
		if (factory == null )
		{
			int ids[] = _library.getId(clss);
			if (ids.length != 1) throw new TypeException("class bound to multiple types");
			MetaCommand metaInterface = (MetaCommand) _library.getReader(ids[0]);
			factory = new CommandStubFactory( _rpcMetaClient, metaInterface, clss );
			setFactory( clss, factory );
		}
		return factory;
	}

	public void setFactory( Class clss, IProxyFactory factory )
	{
		_factoryMap.put(clss,factory);
	}
	
	public void bind( ) 
	throws RemoteException, TypeException, IOException
	{
		if ( _isBound ) throw new RemoteException( "Client already bound" );
		_isBound = true;
		
		_typeClient = new TypeClient( _library, _transport );
		_typeClientMapper = new DynamicClientTypeMapper(  _typeClient );
		if ( _disableMetaDictionaryCheck )
		{
			_typeClientMapper.disableMetaDictionaryCheck();
		}
		TypeMapper dynMapper = new TypeMapperCore( _typeClientMapper );
		_clientTypeMap = new TypeMap( _library, dynMapper );
		_clientTypeMap.setReference(TypeMap.REFERENCE_MAP, _clientTypeMap);
		_rpcMetaClient = new CommandMetaClient( _typeClient, _clientTypeMap );		
	}

	public boolean isBound()
	{
		return _isBound;
	}


}
