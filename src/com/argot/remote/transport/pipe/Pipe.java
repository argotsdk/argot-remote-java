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

package com.argot.remote.transport.pipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *  The Pipe class provides a single class which acts as a Pipe between two threads.
 *  It returns an MimeInputStream and MimeOutputStream for each side of the Pipe.  
 *  The Mime streams offer additional methods to support
 */
public class Pipe
{
    private final int BUFFERSIZE = 4096;
    private byte[] _buffer;
    private int    _head;
    private int    _tail;
    private boolean _waiting;
    private boolean _full;
    private boolean _closed;
    private boolean _written;
    private int _flush;

    private OutputStream _out;
    private InputStream _in;

    private Throwable _error;

    /**
     *  Create a Pipe.  The Pipe class holds a buffer which can be written to and read
     *  from using the PipeInputStream and PipeOutputStream which can be accessed via
     *  inputStream() and outputStream() methods.
     */

    public Pipe()
    {
       _buffer = new byte[ BUFFERSIZE ];
       _head = 0;
       _tail = 0;
       _waiting = false;
       _closed = false;
       _written = false;
       _flush = -1;

       _in = new PipeInputStream( this );
       _out = new PipeOutputStream( this );
       _error = null;
    }


    protected void close()
       throws IOException
    {
       synchronized( this )
       {
          // return silently if we're already closed.
          if ( _closed ) return;
          
          if ( _full ) throw new IOException( "Thread waiting for write" );
  
          _closed = true;
          if ( _waiting ) notifyAll();

       }
    }

    protected void setError( Throwable error )
       throws IOException
    {
       synchronized( this )
       {
           if ( _closed ) throw new IOException( "Pipe Closed" );
           _error = error;
           close();
       }
    }

	 /*
	  * If the output stream requests flush,  the current 
	  * data _head position is recorded.  The input stream
	  * checks if the _tail is ever equal to the _flush point.
	  * if it is, it resets the _flush point and returns the
	  * currently read data.
	  * 
	  * if setFlush is called multiple times before a read
	  * is complete, only the latest flush point is recorded.
	  */
	 protected void setFlush()
	 {
	 	synchronized( this )
	 	{
	 		
			/*if ( _written == false )
			{
				System.out.println("MimePipe: flush written is true");
				_written = true;
				notifyAll();
			}
			else*/
			{
				//System.out.println("MimePipe: _flush = " + _head );
				_flush = _head;
			}
	 	}
	 }
	 

    protected void write( int b )
       throws IOException
    {
       synchronized( this )
       {
          try 
          {
             if ( _closed == true ) throw new IOException( "Pipe Closed" );

             while ( _head+1 == _tail || (_head == (BUFFERSIZE-1) && _tail == 0) )
             {
                if ( _full == true ) throw new IOException( "Thread already writing" );
                _full = true;
                wait();
                _full = false;
             }
          }
          catch( InterruptedException ex )
          {
             throw new IOException( "Thread interrupted" );
          }

          _buffer[ _head++ ] = (byte) b;
          if ( _head >= BUFFERSIZE ) _head = 0;

          if ( _waiting )
          {
             notifyAll();
          }
       }
    }
    
    /* This function is used for the purpose of waiting for data to be written before
       attempting to read the headers set.
     */
    protected void waitWrite()
    {
       synchronized( this )
       {
          if ( _written == true )
             return;

          if ( _error != null )
          {
             _written = true;
             return;
          }

          try
          {
             while ( _head == _tail )
             {
                if ( _closed == true ) 
                {
                   _written = true;
                   return;
                }
                _waiting = true;
                wait();
                _waiting = false;
                
                // if flush has been called its as good
                // as if data has been written.
                if ( _written == true )
                	return;
             }
          }
          catch( InterruptedException ex )
          {
          }
          _written = true;

       }
    }

    protected int read()
       throws IOException
    {
       synchronized( this )
       {
       	

          try
          {
          	 // Check if pipe is empty.
             while ( _head == _tail )
             {
                // Theres been an upstream error.  Throw it.
		          if ( _error != null )
		          {
		              IOException e = new IOException( "Upstream Error" );
		              e.initCause( _error );
		              throw e;
		          }

             	 // maybe the output closed the pipe.
                if ( _closed == true ) return -1;


                if ( _waiting == true ) throw new IOException( "Thread already reading" );

                // Pipe is open.. wait for input.                
                _waiting = true;
                wait();
                _waiting = false;
             }
          }
          catch( InterruptedException ex )
          {
             throw new IOException( "Thread interrupted" );
          }

          int value = _buffer[ _tail++ ];
          if ( _tail >= BUFFERSIZE ) _tail = 0;
          if ( value < 0 ) value = (value & 0x7F) + 128;

          if ( _full )
          {
             notifyAll();
          }
	  return value;
       }
    }

    protected boolean hasBytes()
    {
       synchronized( this )
       {
          if ( _head != _tail )
             return true;
          return false;
       }
    }


    public OutputStream outputStream()
    {
       return _out;
    }

    public InputStream inputStream()
    {
       return _in;
    }

    public class PipeOutputStream
    extends OutputStream
    {
       private Pipe _pipe;

       protected PipeOutputStream( Pipe pipe )
       {
           _pipe = pipe;
       }

       public void write( int b )
          throws IOException
       {
    	   //System.out.println("MimePipe: writing byte" );
          _pipe.write( b );
       }

       public void close()
          throws IOException
       {
          _pipe.close();
       }

       public void setError( Throwable error )
          throws IOException
       {
          _pipe.setError( error );
       }

		 public void flush()
		 {
			 //System.out.println("MimePipe: OUtput flush");
		 	_pipe.setFlush();
		 }
    }


   public class PipeInputStream 
   extends InputStream
   {
      Pipe _pipe;

      protected PipeInputStream( Pipe pipe )
      {
         _pipe = pipe;
      }


      public void close()
         throws IOException
      {
         _pipe.close();
      }

      public int read()
         throws IOException
      {
         return _pipe.read();
      }
      
      
      /*
       * This will operate like the normal read except
       * that if the OutputStream of the pipe requests
       * a flush.  This will return a buffer that is not full.
       * 
       * read setFlush for more details.
       */
      public int read( byte[] b, int off, int len )
      throws IOException
      {
      	int x;
      	
      	for ( x = 0 ; x<len ; )
      	{
      		int c = -1;
      		
				try
				{
					//System.out.println("MImePipe: reading a byte");
					//  Read a character.
					c = _pipe.read();
				}
				catch (IOException e)
				{
					// If we have read some chars already.
					// Return them.
					
					if ( x > 0 )
						return x;
				}
				
				
				// If we found eof, return eof or bytes already read.
      		if ( c < 0 )
      		{
      			if ( x == 0 )
      			 	return c;
      			else
      				return x;
      		}
      		
      		// record the last byte read.
      		b[ off+x ] = (byte) c;

				// increment bytes read.
				x++;
      		
      		// if we've been asked to flush.  return bytes read.
			//System.out.println("MImePipe: flush " + _pipe._flush + " " + _pipe._tail );
      		if ( _pipe._flush == _pipe._tail )
      		{
      			_pipe._flush = -1;
      			return x;
      		}
      	}
      	//System.out.println("MimePipe: Read returning " + x );
      	return x;
      }
      
      public int read( byte[] b )
      throws IOException
      {
      	return read( b, 0, b.length );
      }

   }

}