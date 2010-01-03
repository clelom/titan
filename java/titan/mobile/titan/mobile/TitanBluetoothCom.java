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
 * This Class implements the mobile phones Bluetooth interface to be able to talk to the Gateway.
 * 
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 */

package titan.mobile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import titan.messages.SerialMsg;

public class TitanBluetoothCom {
	
	private static short START_BYTE = 0x3C;
	private static short STOP_BYTE = 0x3E;
	private static int TOSH_DATA_LENGTH = 28;
	
	private StreamConnection m_StrmConn;
	private OutputStream m_Output;
	private InputStream m_Input;
	private boolean m_IsConnected;

	private short m_data[];
	private int m_counter;
	private boolean m_bStuffed;
	private int m_iLength;
	private SerialMsg m_msg;
	private long m_msgCounter;
	
	public TitanBluetoothCom() {
		m_IsConnected = false;
		m_data = new short [TOSH_DATA_LENGTH+1];
		m_counter = 0;
		m_bStuffed = false;
		m_iLength = 0;
	}

	public boolean connect(String url) throws IOException {
            
            if(!m_IsConnected) {
			m_counter = 0;
			m_msgCounter = 0;
			m_bStuffed = false;
			m_StrmConn = (StreamConnection) Connector.open(url);
			
			m_Output = m_StrmConn.openOutputStream();
			m_Input = m_StrmConn.openInputStream();
			
			m_IsConnected = true;
			
			return true;
		}
		else
			return false;
	}
	
	
	public boolean send(SerialMsg msg) throws IOException {
		if(m_IsConnected) {
			m_Output.write(START_BYTE);
			m_Output.write(0);							// the destination in the Gateway: CC2420 = 0
			m_Output.write(msg.get_address());			// the nodeId
			m_Output.write(msg.get_type());				// the message type: control=13/data=42
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
		else
			return false;
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
		}
		else
			return false;
	}
	
	public void receive(char c) {
		if(m_IsConnected) {
			if(m_counter == 0 && c == START_BYTE) {
				m_msg = new SerialMsg(TOSH_DATA_LENGTH);
				m_counter++;
			}
			else if(m_counter == 1) {
				m_counter++;
			}
			else if(m_counter == 2) {
				m_msg.set_address((short)c);
				m_counter++;
			}
			else if(m_counter == 3) {
				m_msg.set_type((short)c);
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
						m_msg.set_length((short)len);
						m_msg.set_data(m_data);
						
						// TESTCODE: write received data on display //
//						String str = "";
//						for(int i=0; i<m_counter-5; i++) {
//							str += " "  + m_data[i];
//						}
//						System.out.println(str);
						//////////////
						
						m_msgCounter++;
						TitanMobile.getTitanCommand().messageReceived(-1, m_msg);
					}
					else
						System.out.println("Error: Received message length doesn't match header length.");
					
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
			
			m_StrmConn.close();
			
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
}
