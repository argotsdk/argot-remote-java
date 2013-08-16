package com.argot.remote.transport.buffer;

import java.io.IOException;
import java.io.InputStream;

import com.argot.remote.transport.TypeEndPoint;
import com.argot.remote.transport.TypeEndPointBasic;
import com.argot.remote.transport.TypeTransport;
import com.argot.util.ChunkByteBuffer;

public class BufferTransport 
implements TypeTransport
{
	private ChunkByteBuffer _buffer;
	
	public BufferTransport()
	{
		_buffer = new ChunkByteBuffer();
	}
	
	@Override
	public TypeEndPoint openLink() 
	throws IOException 
	{
		return new TypeEndPointBasic( null,_buffer.getOutputStream());
	}

	@Override
	public void closeLink(TypeEndPoint endPoint) 
	{
		_buffer.close();
		
		try 
		{
			InputStream in = _buffer.getInputStream();
			int c;
			while (( c = in.read()) > -1)
			{
				System.out.print(c);
			}
		} 
		catch (IOException e) 
		{
			
			e.printStackTrace();
		}
		
	}

}
