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

package com.argot.peer;

/**
 * PeerTypeMapper is under development and does not currently work.
 */

import java.io.IOException;

import com.argot.TypeLocation;
import com.argot.TypeMap;
import com.argot.network.TypeTriple;

/**
 * This is the main protocol code for the peer to peer type resolution system.
 * It is the same on both the client and server however one side must be designated
 * the server.
 * 
 * @author David Ryan
 *
 */

public class TypePeerCommunication
{
	
	public void initialise(TypeMap map)
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Confirms that the MetaDictionary used between each side is the same.
	 * If both client and server send simultaneous requests both requests
	 * can complete independent of each other.  
	 * 
	 * 
	 * @param localMetaDictionary
	 * @return
	 * @throws IOException
	 */
	
	/*
	public boolean checkMetaDictionary(byte[] metaDictionary)
	throws IOException
	{
		TypePeerEndPoint endPoint = openLink();
		
		
		
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
		return false;
	}
*/
	public int reserveType(TypeLocation location)
	throws IOException	
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int resolveType(TypeLocation location, byte[] definition)
	throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public TypeTriple resolveDefault(TypeLocation location)
	throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public TypeTriple resolveReverse(int id)
	throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}


	
}
