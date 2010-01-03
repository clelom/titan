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
 * This class listens on the TitanSerial channel from TOSSIM
 * to the simulation environment and converts incoming data 
 * to SerialMsg.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package titan.simulation;

import java.util.Observable;
import java.util.Observer;

import titan.messages.SerialMsg;
import titan.simulation.ChannelNotices.*;

public class SimSerialListener extends Observable implements Observer {

    public SimSerialListener(SimChannel sc) {
        if (sc != null ) sc.registerListener(this);
    }
    
    public void update(Observable obs, Object msg) {
        if ( obs instanceof ChannelNotices ) {
            NoticeInfo ni = (NoticeInfo)msg;
            String strMsg = ni.strMessage;
            
            // get some indexes
            try {
//                int iSrc = strMsg.indexOf("(")+1;
//                int iSrcEnd= strMsg.indexOf(")");
                int iTo = strMsg.indexOf("To: ")+4;
                int iToEnd = strMsg.indexOf(", len: ",iTo);
                int iLenEnd = strMsg.indexOf(":",iToEnd+6);
                
                if ( iTo == -1 || iToEnd == -1 || iLenEnd == -1) {
                    System.err.println("Invalid message on TitanSerial channel: " + strMsg );
                    return;
                }
                
//                int iSrcAddr= Integer.parseInt(strMsg.substring(iSrc,iSrcEnd));
                int iAddr   = Integer.parseInt(strMsg.substring(iTo,iToEnd));
                int iLength = Integer.parseInt(strMsg.substring(iToEnd+7,iLenEnd));
                
                String [] strBytes = strMsg.substring(iLenEnd+2).split(" ");
                
                if ( strBytes.length != iLength ){
                    System.err.println("Something is wrong with the SerialMsg (length field="+iLength+",printed size="+strBytes.length+")");
                    iLength = Math.min(iLength, strBytes.length);
                }
                
                SerialMsg smsg = new SerialMsg(iLength+SerialMsg.DEFAULT_MESSAGE_SIZE);
                
                smsg.set_address(iAddr);
                smsg.set_length((short)iLength);
                for(int i=0;i<iLength;i++){
                   smsg.setElement_data(i, Short.parseShort(strBytes[i]));
                }
                
                setChanged();
                notifyObservers(smsg);
                clearChanged();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    /**
     * Test code
     *
     */
    public static void main(String [] args) {
        ChannelNotices cn = new ChannelNotices();
        SimSerialListener ssl = new SimSerialListener(null);
        ssl.update(cn, "DEBUG (0): To: 1, len: 3: 002 003 004");
    }

    
    
}
