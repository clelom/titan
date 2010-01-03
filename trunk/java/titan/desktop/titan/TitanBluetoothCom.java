/*
    This file is part of Titan.

    Titan is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Titan is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Titan. If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * TitanBluetoothCom.java
 * 
 * This Class implements the mobile phones Bluetooth interface to be able to talk to the Bluetooth to IEEE 802.15.4 Gateway.
 * 
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package titan;

import gnu.io.*;

import java.io.*;
import java.util.*;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import titan.messages.SerialMsg;
import titancommon.TitanCommand;

public class TitanBluetoothCom implements SerialPortEventListener {
	
	private static short START_BYTE = 0x3C;
	private static short STOP_BYTE = 0x3E;
	private static int TOSH_DATA_LENGTH = TitanCommand.TOSH_DATA_LENGTH;
	
	//private FileOutputStream m_Output;
	private OutputStream m_Output;
	private InputStream m_Input;
	private boolean m_IsConnected;

	private short m_data[];
	private int m_address;
	private int m_counter;
	private boolean m_bStuffed;
	private int m_iLength;
	private long m_msgCounter;
	
	private MessageListener m_MessageListener;
	
	private SerialPort m_serialPort;
	
	@SuppressWarnings("unchecked")
	public TitanBluetoothCom() {
		m_IsConnected = false;
		m_data = new short [TOSH_DATA_LENGTH+1];
		m_counter = 0;
		m_bStuffed = false;
		m_iLength = 0;
		
		m_msgCounter = 0;
		m_bStuffed = false;
		
		
/*		try {
			m_Output = new FileOutputStream("COM4:");
			m_IsConnected = true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/	
		try {
			Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
			CommPortIdentifier portID;
			while(portList.hasMoreElements()) {
				portID = portList.nextElement();
				if (portID.getPortType() == CommPortIdentifier.PORT_SERIAL ) {
					if (portID.getName().equals(LocalSettings.getBluetoothCOM())){
						
						m_serialPort = (SerialPort)portID.open("Titan",2000);
						m_serialPort.addEventListener(this);
						m_serialPort.notifyOnDataAvailable(true);
						m_serialPort.setInputBufferSize(256);
						m_serialPort.setSerialPortParams(9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
						//m_serialPort.disableReceiveFraming();
						//m_serialPort.disableReceiveThreshold();
						//m_serialPort.getFlowControlMode();
						//m_serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
					}
				}
			}
			
			if ( m_serialPort != null ) {
				m_Output = m_serialPort.getOutputStream();
				m_Input = m_serialPort.getInputStream();
			}
			
			//m_Input = new FileInputStream(LocalSettings.getBluetoothCOM());
		} catch (PortInUseException e) {
			System.err.println("Could not connect to: \""+LocalSettings.getBluetoothCOM()+"\"");
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			System.err.println("Could not connect to: \""+LocalSettings.getBluetoothCOM()+"\"");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Could not connect to: \""+LocalSettings.getBluetoothCOM()+"\"");
			e.printStackTrace();
		}
		catch (UnsupportedCommOperationException e) {
			System.err.println("Could not set commnication parameters on: \""+LocalSettings.getBluetoothCOM()+"\"");
			e.printStackTrace();
		}
		
		if (m_Output != null && m_Input != null ) {
			m_IsConnected = true;
		} else {
			System.err.println("Failed to connect to Gateway");
		}
		
	}
	
	public boolean send(SerialMsg msg) throws IOException {
		if(m_IsConnected) {
			m_Output.write(START_BYTE);
			m_Output.write(0);							// the destination in the Gateway: CC2420 = 0
			
			
			
			
			m_Output.write(msg.get_address());			// the nodeId
			m_Output.write(13); // only control messages can be sent
			//m_Output.write(msg.get_type());				// the message type: control=13/data=42

			m_Output.write(msg.get_length());			// the length of the message
			
			for(int i = 0; i < msg.get_length(); i++) {
				if(msg.getElement_data(i) == STOP_BYTE)
					m_Output.write(msg.getElement_data(STOP_BYTE));		// stuffing
					
				m_Output.write(msg.getElement_data(i));
			}

			m_Output.write(STOP_BYTE);
			m_Output.write(0);						// so the STOP_BYTE will be accepted
			
			return true;
		}
		else {
			System.err.println("Bluetooth: not connected");
			return false;
		}
	}
	
	
	public boolean send(String msg) throws IOException {
		
		if(m_IsConnected) {
			m_Output.write(START_BYTE);
			m_Output.write(0);							// the destination in the Gateway: CC2420 = 0
			m_Output.write(0);							// the nodeId
			m_Output.write(13 /*msg.get_type()*/);		// the message type: control=13/data=42
			m_Output.write(msg.length());				// length
			
			for(int i = 0; i < msg.length(); i++) {
				if(msg.charAt(i) == STOP_BYTE)
					m_Output.write(STOP_BYTE);		// stuffing
					
				m_Output.write(msg.charAt(i));
			}

			m_Output.write(STOP_BYTE);
			m_Output.write(0);						// so the STOP_BYTE will be accepted
			
			return true;
		} else {
			System.err.println("Bluetooth: not connected");
			return false;
		}
	}
	
	public void receive(byte c) {

		if(m_IsConnected) {
			if(m_counter == 0 && c == START_BYTE) {
				m_counter++;
			}
			else if(m_counter == 1) {
				m_counter++;
			}
			else if(m_counter == 2) {
				m_address = (short)c;
				m_counter++;
			}
			else if(m_counter == 3) {
				//m_msg.set_type((short)c); // message type - ignore
				m_counter++;
			}
			else if(m_counter == 4) {
				m_iLength = (int)c;
				m_counter++;
			}
			else if(m_counter > 4) {
				boolean stop = false;
					
				if(m_bStuffed) {
					if(c == STOP_BYTE) {
						m_data[m_counter-5] = STOP_BYTE;
						m_counter++;
						m_bStuffed = false;
					}
					else
						stop = true;
				}
				else if(c == STOP_BYTE)
					m_bStuffed = true;
				else {
					m_data[m_counter-5] = (short)c;
					m_counter++;
				}
				
				if (m_counter == TOSH_DATA_LENGTH + 6 || stop) { // stop byte received or packet length exceeded
					int len;
					
					if(stop)
						len = m_counter - 5;
					else
						len = TOSH_DATA_LENGTH;
					
					if(len == m_iLength) {
						SerialMsg msg = new SerialMsg(len+SerialMsg.DEFAULT_MESSAGE_SIZE);
						msg.set_length((short)len);
						msg.set_address(m_address);
						for(int i=0; i<len; i++) {
							msg.setElement_data(i, m_data[i]);
						}
						
						// TESTCODE: write received data on display //
						//String str = "";
						//for(int i=0; i<m_counter-5; i++) {
						//	str += " "  + m_data[i];
						//}
						//System.out.println(str);
						//////////////
						
						m_msgCounter++;
						m_MessageListener.messageReceived(0,msg);
					}
					else
						System.err.println("Error: Received message length doesn't match header length.");
					
					m_counter = 0;
					m_bStuffed = false;
		        }
			}
		}
	}
	
	
	public InputStream getInputStream() {
		if(m_IsConnected)
			return m_Input;
		else
			return null;
	}
	
	
	public long getMsgCounter() {
		return m_msgCounter;
	}
	
	
	public void close() throws IOException {
		if(m_IsConnected) {
			m_IsConnected = false;
			
			m_Output.close();
			m_Input.close();
			
			m_Output = null;
			m_Input = null;
			
			m_msgCounter = 0;
			m_counter = 0;
			m_bStuffed = false;
		}
	}
	
	public boolean isConnected() {
		return m_IsConnected;
	}


	public void registerListener(Message template, MessageListener listener) {
		m_MessageListener = listener;
	}

	public void serialEvent(SerialPortEvent event) {
	     switch (event.getEventType()) {
	      case SerialPortEvent.BI:
	      case SerialPortEvent.OE:
	      case SerialPortEvent.FE:
	      case SerialPortEvent.PE:
	      case SerialPortEvent.CD:
	      case SerialPortEvent.CTS:
	      case SerialPortEvent.DSR:
	      case SerialPortEvent.RI:
	      case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
	         break;
	      case SerialPortEvent.DATA_AVAILABLE:
	         // we get here if data has been received
	         byte[] readBuffer = new byte[256];
	         try {
	            // read data
	            while (m_Input.available() > 0) {
	               int iRead = m_Input.read(readBuffer);
	               for (int i=0; i<iRead; i++) {
	            	   receive(readBuffer[i]);
	               }
	            } 
	         } catch (IOException e) {}
	   
	         break;
	      }
	}
}
