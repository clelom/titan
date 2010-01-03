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
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.message.SerialPacket;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PacketSource;
import net.tinyos.packet.PhoenixSource;
import net.tinyos.util.PrintStreamMessenger;


public class PCGateway implements MessageListener, SerialPortEventListener{

	// Mote communication (assumes a Titan configuration present on the devices)
	MoteIF m_MoteIF;
	
	// default values
	private static final String MOTECOM = new String("serial@COM11:telosb");
	private static final String BTPORT  = new String("COM21");
	
	// communication streams
	private SerialPort m_serialPort;
	private OutputStream m_Output;
	private InputStream m_Input;
	
	private Date m_startTime;
	
	// Bluetooth communication constants
	private static short START_BYTE = 0x3C;
	private static short STOP_BYTE = 0x3E;
	private static int TOSH_DATA_LENGTH = 50;
	
	// Bluetooth input fields
	private short m_data[] = new short [TOSH_DATA_LENGTH+1];
	private int m_address;
	private int m_counter;
	private boolean m_bStuffed;
	private int m_iLength;
	private long m_msgCounter;

	/**
	 * Constructor for the PCGateway
	 * @param strBTPORT serial port for Bluetooth communication
	 * @param strMOTECOM serial port definition for TinyOS MoteIF connection
	 */
	public PCGateway(String strBTPORT, String strMOTECOM) {
		
		// open connection to Tmote
        PacketSource pks = BuildSource.makePacketSource(strMOTECOM);
        PhoenixSource phx = BuildSource.makePhoenix(pks,PrintStreamMessenger.err);
        m_MoteIF = new MoteIF( phx );
        m_MoteIF.registerListener(new SerialMsg(), this);
        
        
        // open up bluetooth connection
		try {
			Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
			CommPortIdentifier portID;
			while(portList.hasMoreElements()) {
				portID = portList.nextElement();
				if (portID.getPortType() == CommPortIdentifier.PORT_SERIAL ) {
					if (portID.getName().equals(strBTPORT)){
						
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
			System.err.println("Could not connect to: \""+strBTPORT+"\"");
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			System.err.println("Could not connect to: \""+strBTPORT+"\"");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Could not connect to: \""+strBTPORT+"\"");
			e.printStackTrace();
		}
		catch (UnsupportedCommOperationException e) {
			System.err.println("Could not set commnication parameters on: \""+BTPORT+"\"");
			e.printStackTrace();
		}
		
		m_startTime = new Date();

		if (m_Output == null || m_Input == null ) {
			System.err.println("ERROR: failed to connect to Bluetooth port ("+m_Output+","+m_Input+")");
		} else {
			System.out.println("");
			System.out.println("*****************************************************");
			System.out.println("* Bluetooth to IEEE 802.15.4 gateway up and running *");
			System.out.println("*****************************************************");
			System.out.println("");
			System.out.println( "Started at " + m_startTime);
			System.out.println("Waiting for messages...");
		}
		
		
        
	}
	
	/**
	 * Handle message received from the Tmote
	 */
	public void messageReceived(int to, Message msg) {
		if ( msg.amType() == SerialMsg.AM_TYPE ) {
			try {
				
				// TESTCODE: write received data on display //
		        Date recvTime = new Date();
		        String strTime = String.format("%012d", recvTime.getTime()-m_startTime.getTime());
				System.out.print(strTime+"; RF->BT; " + ((SerialMsg)msg).get_length() + "; ");
				String str = "";
				for(int i=0; i<((SerialMsg)msg).get_length(); i++) {
					str += ((SerialMsg)msg).getElement_data(i) + "; ";
				}
				System.out.println(str);
				//////////////
				
				
				if ( bluetoothSend((SerialMsg)msg) == false ) {
					System.err.println("Failed to send over Bluetooth");
				}
				//System.out.print("Forwarding from Tmote to Bluetooth...");
				//System.out.println( bluetoothSend((SerialMsg)msg)? "done" : "failed");
			} catch (IOException e) {
				System.err.println("Failed to send over Bluetooth");
				e.printStackTrace();
			}
		} else {
			System.err.println("Unknown message type received from Tmote");
		}
	}
	
	public boolean bluetoothSend(SerialMsg msg) throws IOException {
		
		if ( m_Output == null ) return false;
		
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
	
	@Override
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
	               	   bluetoothReceive(readBuffer[i]);
	               }
	            } 
	         } catch (IOException e) {}
	   
	         break;
	      }
	}
	
	public void bluetoothReceive(byte c) {

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
			        Date recvTime = new Date();
			        String strTime = String.format("%012d", recvTime.getTime()-m_startTime.getTime());
					System.out.print(strTime+"; BT->RF; " + m_data.length + "; ");
					String str = "";
					for(int i=0; i<m_data.length; i++) {
						str += m_data[i] + "; ";
					}
					System.out.println(str);
					//////////////
					
					m_msgCounter++;
					
					try {
						//System.out.print("Forwarding from Bluetooth to Tmote...");
						m_MoteIF.send(0, msg );
						//System.out.println("done");
					} catch (IOException e) {
						System.err.println("Failed to forward from Bluetooth to Tmote");
						e.printStackTrace();
					}
				}
				else
					System.err.println("Error: Received message length doesn't match header length.");
				
				m_counter = 0;
				m_bStuffed = false;
	        }
		}
	}

	/**
	 * Instantiates a PCGateway object and runs it
	 * @param args command line arguments
	 */
	public static void main(String [] args) {
		
		PCGateway pcg;
		
		if (args.length == 0) {
			pcg = new PCGateway(BTPORT,MOTECOM);
		} else if (args.length == 2){
			pcg = new PCGateway(args[0],args[1]);
		} else {
			System.out.println("USAGE: arg1 = BTPORT, arg2 = MOTECOM");
		}
		
	}

	PhoenixSource sender;
	
	//////////////////////////////////////////////////////////////////////////
	// Test code

	/**
	 * Tries to bypass MoteIF
	 * @param moteId
	 * @param m
	 * @throws IOException
	 */
	private void tmoteSendDirectly(int moteId,Message m) throws IOException {
		int amType = m.amType();
		byte[] data = m.dataGet();
		SerialPacket packet =
		    new SerialPacket(SerialPacket.offset_data(0) + data.length);
		packet.set_header_dest(moteId);
		packet.set_header_type((short)amType);
		packet.set_header_length((short)data.length);
		packet.dataSet(data, 0, packet.offset_data(0), data.length);

		byte[] packetData = packet.dataGet();
		byte[] fullPacket = new byte[packetData.length + 1];
		fullPacket[0] = net.tinyos.packet.Serial.TOS_SERIAL_ACTIVE_MESSAGE_ID;
		System.arraycopy(packetData, 0, fullPacket, 1, packetData.length);
		sender.writePacket(fullPacket);

	}
}
