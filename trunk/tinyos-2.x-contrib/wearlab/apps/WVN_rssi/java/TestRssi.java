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

import net.tinyos.message.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;

class TestRssi implements MessageListener{
	
	MoteIF m_MoteIF;
	
	public TestRssi() {
        PacketSource pks = BuildSource.makePacketSource("serial@COM11:telosb");
        PhoenixSource phx = BuildSource.makePhoenix(pks,PrintStreamMessenger.err);
        m_MoteIF = new MoteIF( phx );
        
        m_MoteIF.registerListener(new WVN_RssiMsg(), this);
        
        System.out.println("Source;Destination;Power;RSSI;Counter");
	}
	
	public void messageReceived(int addr, Message msg) {
		WVN_RssiMsg rssiMsg = (WVN_RssiMsg)msg;
		
		System.out.println(rssiMsg.get_sender() + ";" + rssiMsg.get_receiver() + ";" + 
		                   rssiMsg.get_power_level() + ";" + rssiMsg.get_rssi_value() + ";" +
                       rssiMsg.get_counter() );
	}
	
	public static void main(String[] args) {
        TestRssi tr = new TestRssi();
	}
	
}