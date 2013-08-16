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
package com.argot.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.argot.TypeException;
import com.argot.TypeInputStream;
import com.argot.TypeLocation;
import com.argot.TypeMap;
import com.argot.TypeOutputStream;
import com.argot.TypeLibrary;
import com.argot.TypeWriter;
import com.argot.common.Int32;
import com.argot.common.UInt8;
import com.argot.common.U8Boolean;
import com.argot.meta.DictionaryLocation;
import com.argot.remote.MetaObject;
import com.argot.remote.transport.TypeEndPoint;
import com.argot.remote.transport.TypeTransport;
import com.argot.util.ChunkByteBuffer;

public class TypeClient
implements TypeTransport
{
	private TypeTransport _link;
	private TypeMap _typeMap;
	private TypeMap _linkMap;
	private TypeWriter _uint8;

	public TypeClient( TypeLibrary library, TypeTransport link )
	throws TypeException
	{
		_link = link;
		_typeMap = new TypeMap( library, new ProtocolTypeMapper() );
		_uint8 = library.getWriter(library.getDefinitionId("uint8","1.3")).getWriter(_typeMap);
	}

	public void initialise(TypeMap map)
	throws TypeException
	{
		_typeMap.setReference( TypeMap.REFERENCE_MAP,map);
		_linkMap = map;
	}
	
	/*
	 * The idea behind TypeClient implementing TypeTransport is to allow
	 * higher level user protocols running on the stack.  This protocol
	 * is a request response system.  To provide a similar programmer
	 * API, the openLink returns an EndPoint which buffers the request.
	 * When the user starts to read from the input, it triggers a send of
	 * the full request to the server.  The response is also buffered and
	 * provided to the client.
	 * 
	 * It might be possible to offer a true pipe later on which gives the
	 * client correct asynchronous communications.  That can happen later though.
	 */
	public TypeEndPoint openLink() throws IOException
	{
		TypeEndPoint ep = _link.openLink();
		
		try {
			TypeOutputStream tmos = new TypeOutputStream( ep.getOutputStream(), _typeMap );
			_uint8.write(tmos, new Short(ProtocolTypeMapper.MSG));
		} catch (TypeException e) {
			// Java 1.4 only has simple IOException constructors.
			throw new IOException(e.toString());
		}
		
		return ep;
	}

	public void closeLink( TypeEndPoint endPoint )
	{
		_link.closeLink( endPoint );		
	}
	
	public class TypeClientInputStream
	extends InputStream
	{
		private TypeEndPoint _transport;
		private ChunkByteBuffer _buffer;
		private InputStream _stream;
		private boolean _reading;
		private boolean _error;
		
		public TypeClientInputStream( TypeEndPoint transport, ChunkByteBuffer buffer )
		{
			_transport = transport;
			_buffer = buffer;
			_reading = false;
			_error = false;
		}

		public int read() 
		throws IOException
		{
			if ( !_reading )
			{
				_reading = true;
				performRequest();
			}
			
			if ( _error ) return -1;
			
			return _stream.read();
		}
		
		public int read( byte[] buffer, int start, int end ) 
		throws IOException
		{
			if ( !_reading )
			{
				_reading = true;
				performRequest();
			}
			
			if ( _error ) return -1;

			return _stream.read(buffer, start, end);
		}		
		
		private void performRequest() 
		throws IOException
		{
						
			try
			{
				_buffer.close();
				OutputStream out = _transport.getOutputStream();
				TypeOutputStream tmos = new TypeOutputStream( out, _typeMap );
				tmos.writeObject( "u8", new Short(ProtocolTypeMapper.MSG) );
				tmos.writeObject( "u32binary", _buffer );		
				
				InputStream in = _transport.getInputStream();
				TypeInputStream tmis = new TypeInputStream( in, _typeMap );
				Short type = (Short) tmis.readObject( UInt8.TYPENAME );
				if ( type.intValue() != ProtocolTypeMapper.MSG )throw new IOException("Bad Protocol Error"); 
				_buffer = (ChunkByteBuffer) tmis.readObject( "u32binary" );
				_stream = _buffer.getInputStream();
			}
			catch (TypeException e)
			{
				_error = true;
				// Java 1.4 only has simple IOException constructors.
				throw new IOException( e.toString() );
			}
			catch (IOException e)
			{
				_error = true;
				throw e;
			}				
			
		}

		
	}


	
	public boolean checkMetaDictionary( byte[] metaDictionary )
	throws IOException, TypeException
	{
		TypeEndPoint endPoint = _link.openLink();
		
		try
		{
			// Write the name and definition to the request body.	
			OutputStream out = endPoint.getOutputStream();
			TypeOutputStream tmos = new TypeOutputStream( out, _typeMap );
			tmos.writeObject( "uint8", new Short(ProtocolTypeMapper.CHECK_CORE ) );
			tmos.writeObject( "u16binary", metaDictionary );		
			tmos.getStream().flush();
			
			InputStream in = endPoint.getInputStream();
			TypeInputStream tmis = new TypeInputStream( in, _typeMap );
			Short type = (Short) tmis.readObject( UInt8.TYPENAME );
			if ( type.intValue() != ProtocolTypeMapper.CHECK_CORE )throw new TypeException("Bad Protocol Error"); 
			Boolean value = (Boolean) tmis.readObject( U8Boolean.TYPENAME );

			return value.booleanValue();
		}
		finally
		{
			_link.closeLink( endPoint );
		}
		
	}
	
	private byte[] getLocationBytes(TypeLocation location) 
	throws TypeException, IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TypeOutputStream tmos = new TypeOutputStream( out, _typeMap );
		tmos.writeObject( DictionaryLocation.TYPENAME, location);
		return out.toByteArray();
	}	
	
	public int resolveType( TypeLocation location, byte[] definition )
	throws IOException, TypeException
	{
		TypeEndPoint endPoint = _link.openLink();
		
		try
		{
			// Write the name and definition to the request body.	
			OutputStream out = endPoint.getOutputStream();
			TypeOutputStream tmos = new TypeOutputStream( out, _typeMap );
			tmos.writeObject( "uint8", new Short(ProtocolTypeMapper.MAP) );
			tmos.writeObject( "u16binary", getLocationBytes( location ) );
			//tmos.writeObject( DictionaryLocation.TYPENAME, location );
			tmos.writeObject( "u16binary", definition );		
			tmos.getStream().flush();
			
			InputStream in = endPoint.getInputStream();
			TypeInputStream tmis = new TypeInputStream( in, _typeMap );
			Short type = (Short) tmis.readObject( UInt8.TYPENAME );
			if ( type.intValue() != ProtocolTypeMapper.MAP )throw new TypeException("Bad Protocol Error"); 
			Integer value = (Integer) tmis.readObject( Int32.TYPENAME );

			return value.intValue();
		}
		finally
		{
			_link.closeLink( endPoint );
		}
	}
	
	
	// This is for TypeLocationNamed types.
	public TypeTriple resolveDefault( TypeLocation location )
	throws IOException, TypeException
	{		
		TypeEndPoint endPoint = _link.openLink();
		
		try
		{
			// Write the name and definition to the request body.	
			TypeOutputStream tmos = new TypeOutputStream( endPoint.getOutputStream(), _typeMap );
			tmos.writeObject( "uint8", new Short(ProtocolTypeMapper.MAPDEF) );
			tmos.writeObject( "u16binary", getLocationBytes( location ) );
			//tmos.writeObject( DictionaryLocation.TYPENAME, location );
			tmos.getStream().flush();
			
			TypeInputStream tmis = new TypeInputStream( endPoint.getInputStream(), _typeMap );
			Short type = (Short) tmis.readObject( UInt8.TYPENAME );
			if ( type.intValue() != ProtocolTypeMapper.MAPDEF )
			{
				int id = _typeMap.getLibrary().getTypeId(location);
				throw new TypeException("Failed to map - " + _typeMap.getLibrary().getName(id).getFullName());		
			}
			Integer mapId = (Integer) tmis.readObject( Int32.TYPENAME );
			//Integer definitionId = (Integer) tmis.readObject( Int32.TYPENAME );
			TypeLocation locationDefinition = (TypeLocation) tmis.readObject( DictionaryLocation.TYPENAME );
			byte[] definition = (byte[]) tmis.readObject( "u16binary" );
			
			return new TypeTriple( mapId.intValue(), TypeTriple.fixLocation(locationDefinition, _linkMap), definition );
		}
		finally
		{
			_link.closeLink( endPoint );
		}
	}
	

	public int reserveType( TypeLocation location )
	throws IOException, TypeException
	{		
		TypeEndPoint endPoint = _link.openLink();
		
		try
		{
			// Write the name and definition to the request body.	
			TypeOutputStream tmos = new TypeOutputStream( endPoint.getOutputStream(), _typeMap );
			tmos.writeObject( "uint8", new Short(ProtocolTypeMapper.MAPRES) );		
			tmos.writeObject( DictionaryLocation.TYPENAME, location );
			tmos.getStream().flush();
			
			TypeInputStream tmis = new TypeInputStream( endPoint.getInputStream(), _typeMap );
			Short type = (Short) tmis.readObject( UInt8.TYPENAME );
			if ( type.intValue() != ProtocolTypeMapper.MAPRES )throw new TypeException("Bad Protocol Error");		
			Integer value = (Integer) tmis.readObject( Int32.TYPENAME );
	
			return value.intValue();
		}
		finally
		{
			_link.closeLink( endPoint );
		}
	}

	
	
	public TypeTriple resolveReverse( int id )
	throws IOException, TypeException
	{		
		TypeEndPoint endPoint = _link.openLink();
		
		try
		{
			// Write the name and definition to the request body.
			TypeOutputStream tmos = new TypeOutputStream( endPoint.getOutputStream(), _typeMap );
			tmos.writeObject( "uint8", new Short(ProtocolTypeMapper.MAPREV) );		
			tmos.writeObject( Int32.TYPENAME, new Integer( id ) );
			tmos.getStream().flush();
			
			TypeInputStream tmis = new TypeInputStream( endPoint.getInputStream(), _typeMap );
			Short type = (Short) tmis.readObject( UInt8.TYPENAME );
			if ( type.intValue() != ProtocolTypeMapper.MAPREV )
			{
				throw new TypeException("Unable to resolve reverse id: " + id + " result:" + type);		
			}
			TypeLocation location = (TypeLocation) tmis.readObject( DictionaryLocation.TYPENAME );
			byte[] definition = (byte[]) tmis.readObject( "u16binary" );
			
			return new TypeTriple( id, TypeTriple.fixLocation(location, _linkMap), definition );
		}
		finally
		{
			_link.closeLink( endPoint );
		}
	}
	
	public MetaObject getBaseObject( TypeMap map )
	throws IOException, TypeException
	{		
		TypeEndPoint endPoint = _link.openLink();
		
		try
		{
			// Write the name and definition to the request body.
			TypeOutputStream tmos = new TypeOutputStream( endPoint.getOutputStream(), _typeMap );
			tmos.writeObject( "uint8", new Short(ProtocolTypeMapper.BASE) );		
			tmos.getStream().flush();
			
			TypeInputStream tmis = new TypeInputStream( endPoint.getInputStream(), map );
			Short type = (Short) tmis.readObject( UInt8.TYPENAME );
			if ( type.intValue() != ProtocolTypeMapper.BASE )throw new TypeException("Bad Protocol Error");		
			Boolean value = (Boolean) tmis.readObject( U8Boolean.TYPENAME );
			if ( !value.booleanValue() )
			{
				return null;
			}
			MetaObject object = (MetaObject) tmis.readObject( MetaObject.TYPENAME );
			
			return object;
		}
		finally
		{
			_link.closeLink( endPoint );
		}
	}
}
