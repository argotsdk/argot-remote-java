package com.argot.remote.transport.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import com.argot.remote.command.RemoteException;
import com.argot.remote.transport.TypeEndPoint;
import com.argot.remote.transport.TypeEndPointBasic;
import com.argot.remote.transport.TypeTransport;
import com.argot.remote.transport.pipe.Pipe;

public class SerialTransport 
implements SerialPortEventListener,TypeTransport
{
	SerialPort serialPort;
     
	/** The port we're normally going to use. */

	private InputStream input;
	private OutputStream output;

	private String[] portNames;
	private int timeOut;
	private int dataRate;
	
	public SerialTransport( String[] portNames, int dataRate, int timeOut) 
	throws RemoteException
	{
		this.portNames = portNames;
		this.timeOut = timeOut;
		this.dataRate = dataRate;
		
		try
		{
			initialize();
		}
		catch (Exception ex)
		{
			throw new RemoteException("Failed to open serial port", ex);
		}
	}
	
	@Override
	public TypeEndPoint openLink() 
	throws IOException 
	{
		pipeIn = new Pipe();
		pipeOut = pipeIn.outputStream();
		TypeEndPointBasic endPoint = new TypeEndPointBasic(pipeIn.inputStream(),output);
		return endPoint;
	}

	@Override
	public void closeLink(TypeEndPoint endPoint) 
	{	
		close();
	}
	
	Pipe pipeIn;
	OutputStream pipeOut;
	
	private void initialize() 
	throws PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException 
	{
		CommPortIdentifier portId = null;
		
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portEnum = (Enumeration<CommPortIdentifier>) CommPortIdentifier.getPortIdentifiers();

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : portNames) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			throw new IOException("Could not open comm port");
		}


		// open serial port, and use class name for the appName.
		serialPort = (SerialPort) portId.open(this.getClass().getName(), timeOut);

		// set port parameters
		serialPort.setSerialPortParams(dataRate,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

		// open the streams
		input = serialPort.getInputStream();
		output = serialPort.getOutputStream();

		// add event listeners
		serialPort.addEventListener(this);
		serialPort.notifyOnDataAvailable(true);
		serialPort.enableReceiveTimeout(100);

	}

	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent) 
	{
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) 
		{
			try 
			{
				int c = -1;
				while ((c=input.read()) > -1)
				{
					if (pipeOut!=null)
					{
						pipeOut.write(c);
						pipeOut.flush();
					}
				}
				
			} 
			catch (Exception e) 
			{
				System.err.println(e.toString());
			}
		}
		// Ignore all the other eventTypes, but you should consider the other ones.
	}



}
