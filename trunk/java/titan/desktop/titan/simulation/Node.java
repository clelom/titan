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
 * Handles a node during simulation
 * 
 * @author Urs Hunkeler <urs.hunkeler@epfl.ch>
 *
 */

package titan.simulation;

import java.util.Observable;
import java.util.Observer;

import titan.simulation.ChannelNotices.NoticeInfo;

public class Node implements Observer {
    public static int ID = 0;
    
    private double x;
    private double y;
    private int id = ID++;
    private Tossim tossim = null;
    private Mote mote = null;
    
    public boolean[] leds = new boolean[3];
    
    public Node(Tossim tossim, double x, double y) {
        this.x = x;
        this.y = y;
        this.tossim = tossim;
        this.mote = tossim.getNode(id);
        tossim.radio().setNoise(mote, -100, 5);
        mote.bootAtTime(id * 1000 + 234);
        SimChannel sc = tossim.addChannel("LedsC",false);
        sc.registerListener(this);
    }
    
    public void connect(Node neighbor) {
        tossim.radio().add(mote, neighbor.mote, -50);
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public int getID() {
        return id;
    }

    public void update(Observable obs, Object param) {
        if ( obs instanceof titan.simulation.ChannelNotices ) {
            NoticeInfo ni = (NoticeInfo) param;
            
            // make sure we have the right channel
            if (ni.strChannel.compareTo("LedsC")!=0) {
                System.err.println("Node: Unknown notification received!");
                return;
            }
//            else if (ni.strMessage.compareTo("Channel LedsC")==0) {
//                // filter welcome message
//                return;
//            }
            
            // format: "DEBUG (x): LEDS: LEDy [on/off]"
            try {
                String strNode = ni.strMessage.substring(7);
                strNode = strNode.substring(0,strNode.indexOf(")"));
                int iNode = Integer.parseInt(strNode);
                
                if (iNode == id) {
                    String strLed = ni.strMessage.substring(ni.strMessage.indexOf("Led")+3);
                    strLed = strLed.substring(0,strLed.indexOf(" "));
                    int iLed = Integer.parseInt(strLed);
    
                    String strStatus = ni.strMessage.substring(ni.strMessage.indexOf(" ",ni.strMessage.indexOf("Led"))+1);
                    strStatus = strStatus.substring(0,strStatus.indexOf("."));
                    
                    leds[iLed] = (strStatus.compareTo("on") == 0);
                }
            } catch (Exception e) {
                // something's wrong with the format - just ignore it
            }
            
        }        
        
    }
}
