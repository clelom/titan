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

package titan;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import titan.messages.SerialMsg;
import titan.simulation.SimChannel;
import titan.simulation.SimSerialListener;
import titan.simulation.Simulation;
import titan.simulation.Tossim;

import net.tinyos.message.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import titancommon.route.TitanRouter;

/**
 * Exchanges messages with the sensor network, be it a real 
 * physical one, or a simulation. This class is a wrapper for 
 * MoteIF, as it does not (yet?) support communication with 
 * TOSSIM in TinyOS 2.0.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 *
 *
 */


public class TitanCommunicate implements Observer{

    // real world
    MoteIF m_MoteIF;

    // simulation
    Tossim m_Tossim;
    Simulation m_Simulation;

    // Bluetooth connection
    TitanBluetoothCom m_BTCom;
    
    MessageListener m_Listener = null;

    private int m_Mode;
    
    public static final int TMOTE = 0;
    public static final int SIM = 1;
    public static final int BLUETOOTH = 2;
	  public static final int TCP_ONLY = 3;
    public static final int DEFAULT = 0;
    
    public static final int TOS_BCAST_ADDR = 0xFFFF;

    /**
     * Instantiates a TitanCommunicate object
     * @param iType Communication time. Can be one of TMOTE,SIM,BLUETOOTH
     */
    public TitanCommunicate( int iType ) {
      m_Mode = iType;

    	switch(iType) {
	    	case TMOTE:
	            PacketSource pks = BuildSource.makePacketSource(LocalSettings.getMOTECOM());
	            PhoenixSource phx = BuildSource.makePhoenix(pks,PrintStreamMessenger.err);
	            m_MoteIF = new MoteIF( phx );
	    		break;
	    	case SIM:
	            m_Tossim = new Tossim(LocalSettings.getTitanTossimPath());
	            
	            // set up channel to receive serial messages produced
	            SimChannel sc = m_Tossim.addChannel("TitanSerial",false);
	            SimSerialListener ssl = new SimSerialListener(sc);
	            ssl.addObserver(this);
	            
	            // set up debugging channels
//	            m_Tossim.addChannel("Titan",false);
//	            m_Tossim.addChannel("TitanTask",true);
//	            m_Tossim.addChannel("TitanConfig",true);
	            
	            
	            m_Simulation = new Simulation(m_Tossim);
	            m_Tossim.setDebugListener(m_Simulation);
	            m_Simulation.openFrame();
	            
	            m_Tossim.runFor(5);
	    		break;
	    	case BLUETOOTH:
	    		m_BTCom = new TitanBluetoothCom();
	    		break;
    		default:
    			break;
    	
    	}
    }
    
    public void SimContinue(float fSeconds) {
        if (m_Simulation == null ) 
            System.err.println("ERROR: no simulation running");
        else
            m_Simulation.runFor(fSeconds);
    }
    
    public void SimAddChannel(String strChannel ) {
        if ( m_Tossim == null )
            System.err.println("ERROR: no simulation running");
        else
            m_Tossim.addChannel(strChannel, true);
    }
    
    /**
     * Send m to moteId via this mote interface
     * @param moteId message destination
     * @param m message
     * @exception IOException thrown if message could not be sent
     */
    synchronized public boolean send(int moteId, SerialMsg m) {
        // remove net_id from destination addr. for mote / bt communication
        int addr = m.get_address();
        if ((addr != 0xFFFF) && (addr != -1)) {
          m.set_address(addr & TitanRouter.CLIENT_MASK);
        }
        
        try {
            if (m_MoteIF != null ) {
                m_MoteIF.send(addr,m);
                Thread.sleep(50);
            } else if (m_Simulation != null ) {
                m_Simulation.deliverMsg(m);
                m_Simulation.runFor(0.050f);
            } else if (m_BTCom != null) {
            	m_BTCom.send(m);
            }
        } catch(InterruptedException e) {
            // do nothing
        } catch(IOException e) {
            System.err.println("ERROR: Could not send packet: ");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * Register a listener for given messages type. The message m should be
     * an instance of a subclass of Message (generated by mig). When a
     * message of the corresponding type is received, a new instance of m's
     * class is created with the received message as data. This message is
     * then passed to the given MessageListener.
     * 
     * Note that multiple MessageListeners can be registered for the same
     * message type, and in fact each listener can use a different template
     * type if it wishes (the only requirement is that m.getType() matches
     * the received message).
     * WARNING: This only holds true for the real implementation. For 
     * simulation, only one listener is supported!!!
     *
     * @param m message template specifying which message to receive
     * @param l listener to which received messages are dispatched
     */
    synchronized public void registerListener(Message template, MessageListener listener) {
        
        if (m_MoteIF != null ) {
            m_MoteIF.registerListener(template,listener);
        } else if (m_BTCom != null) {
        	m_BTCom.registerListener(template,listener);
        } else {
            m_Listener = listener;
        }
        
    }
    
    /**
     * Deregister a listener for a given message type.
     * @param m message template specifying which message to receive
     * @param l listener to which received messages are dispatched
     */
    synchronized public void deregisterListener(Message template, MessageListener listener) {
        
        if (m_MoteIF != null ) {
            m_MoteIF.deregisterListener(template,listener);
        } else {
            if (m_Listener == listener) m_Listener = null;
        }
        
    }

    public void update(Observable arg0, Object arg1) {

        if( m_Listener != null ) {
            m_Listener.messageReceived(-1, (SerialMsg)arg1);
        }
    }

    public boolean isConnected() {
      if (m_Mode == TCP_ONLY)
        return true;

        //TODO: Check whether it is really connected
        return false;
    }
}
