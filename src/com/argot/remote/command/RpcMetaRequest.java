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
import com.argot.TypeInputStream;
import com.argot.TypeLibraryReader;
import com.argot.TypeLibraryWriter;
import com.argot.TypeMap;
import com.argot.TypeOutputStream;
import com.argot.TypeReader;
import com.argot.TypeWriter;
import com.argot.common.UInt8;
import com.argot.common.UInt16;
import com.argot.common.UVInt28;
import com.argot.meta.MetaReference;

public class RpcMetaRequest 
implements TypeLibraryReader, TypeLibraryWriter
{
	public static final String TYPENAME = "remote.rpc.request";
/*
	private static class RpcMetaRequestReader
	implements TypeReader
	{
		private TypeReader _metaLocation;
		private TypeReader _uint16;
		private TypeReader _uint8;
		private HashMap _methodCache;
		
		public RpcMetaRequestReader( TypeMap map )
		throws TypeException
		{
			//_metaLocation = map.getReader(map.getStreamId(MetaLocation.TYPENAME));
			_uint16 = map.getReader(map.getStreamId(UInt16.TYPENAME));
			_uint8 = map.getReader(map.getStreamId(UInt8.TYPENAME));
			_methodCache = new HashMap();
		}
		
		public Object read(TypeInputStream in) 
		throws TypeException, IOException 
		{
			MetaLocation mObject = (MetaLocation) _metaLocation.read(in);
			
			Integer mId = (Integer) _uint16.read(in);
			MetaMethod mMethod = (MetaMethod) _methodCache.get(mId);
			if (mMethod==null)
			{
				int systemMid = in.getTypeMap().getDefinitionId( mId.intValue() );
				mMethod = (MetaMethod) in.getTypeMap().getLibrary().getStructure( systemMid );
				_methodCache.put(mId, mMethod);
			}
			
			MetaParameter[] responseTypes = mMethod.getResponseTypes();
			
			Short size = (Short) _uint8.read(in);
			Object[] objs = new Object[ size.intValue() ];
			for ( int x=0; x<size.intValue(); x++ )
			{
				Integer id = (Integer) _uint16.read(in);
				int sId = in.getTypeMap().getDefinitionId( id.intValue() );
				if ( sId != responseTypes[x].getParamType() )
				{
					// the type we expected wasn't there.
				}
				objs[x] = in.readObject( id.intValue() );
			}
			return new RpcMetaMethodRequest( mObject, mMethod, objs );
		}
	}
	*/
	public TypeReader getReader(TypeMap map) 
	throws TypeException 
	{
		//return new RpcMetaRequestReader(map);
		throw new TypeException("Not implemented");
	}

	private static class RpcMetaRequestWriter
	implements TypeWriter
	{
		//private TypeWriter _metaLocation;
		//private TypeWriter _uint8;
		private TypeWriter _uvint28;
		
		public RpcMetaRequestWriter(TypeMap map)
		throws TypeException
		{
			//_metaLocation = map.getWriter(map.getStreamId(MetaLocation.TYPENAME));
			//_uint8 = map.getWriter(map.getStreamId(UInt8.TYPENAME));
			_uvint28 = map.getWriter(map.getStreamId(UVInt28.TYPENAME));
		}
		
		public void write(TypeOutputStream out, Object o) 
		throws TypeException, IOException 
		{
			RpcMetaMethodRequest request = (RpcMetaMethodRequest) o;
			
			//_metaLocation.write(out, request.getLocation());
			
			//int mappedMethodId =  out.getTypeMap().getStreamId( request.getMetaMethod().getMemberTypeId());
			//_uint16.write(out, new Integer( mappedMethodId ));
			
			//MetaParameter[] requestTypes = request.getMetaMethod().getRequestTypes();
			Object[] args = request.getArguments();
			
			_uvint28.write(out,request.getSequence().getTypeId());
			
			MetaReference[] references = request.getMetaMethod().getReferences();
			//_uint8.write(out, new Integer( requestTypes.length ));
			
			for( int x=0; x< references.length; x++ )
			{
				int mappedId = out.getTypeMap().getStreamId( references[x].getType() );
				//_uint16.write(out, new Integer( mappedId ) );
				
				out.writeObject( mappedId, args[x] );
			}
		}
	}

	public TypeWriter getWriter(TypeMap map) 
	throws TypeException 
	{
		return new RpcMetaRequestWriter(map);
	}

}
