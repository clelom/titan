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
 * Main TOSSIM simulation class. Starts and runs TOSSIM and sets 
 * up the environment.
 * 
 * @author Urs Hunkeler <urs.hunkeler@epfl.ch>
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */

package titan.simulation;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import titan.TitanCommunicate;
import titan.messages.SerialMsg;


public class Simulation extends Canvas implements DebugListener, WindowListener {
    
    private static final long serialVersionUID = -3327331854019070621L;
    
    public static final int SIZE_X = 100;
    public static final int SIZE_Y = 100;
    public static final int DEFAULT_NUMBER_OF_NODES = 10;
    public static final int RANGE = 100;
    
    private Node[] nodes;
    private Tossim tossim = null;
    
    private Frame m_Frame;
    
    /**
     * Constructs a simulation object with the default 
     * number of nodes
     * @param tossim Connection to the Tossim object the simulation controls
     */
    public Simulation(Tossim tossim) {
        this(tossim,DEFAULT_NUMBER_OF_NODES);
    }
    /**
     * Constructs a simulation object that will be hooked 
     * up with the given TOSSIM simulation.
     * @param tossim
     */
    public Simulation(Tossim tossim, int iNodes) {
        this.tossim = tossim;
        nodes = new Node[iNodes];
        
        // add mouse listener to window
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if(me.getButton() == 1) {
                    iterate(); // on LButtonClick, continue simulation for some time
                    repaint();
                } else {
                    repaint();
                }
            }
        });

        // set up initial (random) node placement
        for(int i = 0; i < nodes.length; i++) {
            boolean ok = false;
            while(!ok) {
                double x = Math.random() * SIZE_X;
                double y = Math.random() * SIZE_Y;
                ok = true;
                for(int j = 0; j < i; j++) {
                    double x1 = nodes[j].getX() - x;
                    double y1 = nodes[j].getY() - y;
                    // make sure nodes have a minimum distance between them
                    if(Math.sqrt(x1 * x1 + y1 * y1) < 2) {
                        ok = false;
                        break;
                    }
                }
                if(ok) {
                    nodes[i] = new Node(tossim, x, y);
                }
            }
        }
        
        // set up interconnections between nodes
        System.out.println("Connectivity model: All connected, no loss");
        for(int i = 0; i < nodes.length; i++) {
            for(int j = 0; j < nodes.length; j++) {
/*cl                double x = nodes[i].getX() - nodes[j].getX();
                double y = nodes[i].getY() - nodes[j].getY();
                double distance = Math.sqrt(x * x + y * y);
                if(distance <= RANGE) {
*/
                nodes[i].connect(nodes[j]);
//                  }
            }

        }
    }
    
    /**
     * Continues the simulation by 200ms
     *
     */
    private void iterate() {
        tossim.runFor(0.2f);
    }
    
    public void runFor(float dSeconds) {
        tossim.runFor(dSeconds);
    }
    
    /**
     * Paints the simulated nodes onto the canvas
     */
    public void paint(Graphics g) {
        int height = getSize().height;
        int width  = getSize().width;

        int ledradius = 5;
        int noderadius= 18;
        
        // iterate through nodes
        for(int i = 0; i < nodes.length; i++) {
            int rx = (int)(nodes[i].getX() / SIZE_X * width);
            int ry = (int)(nodes[i].getY() / SIZE_Y * height);
            
            // draw node circle
            g.setColor(Color.black);
            g.drawOval(rx - noderadius, ry - noderadius, 2*noderadius, 2*noderadius);

            // draw node id
            String strID = "id " + i; //String.format("ID %2i", i);
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D r2d = fm.getStringBounds(strID, g);
            g.drawString("id "+i, rx-(int)(r2d.getWidth()/2), ry);
            
            // led 0
            if(nodes[i].leds[0]) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.red.darker());
            }
            g.fillOval(rx - 3*ledradius, ry, 2*ledradius, 2*ledradius);
            
            // led 1
            if(nodes[i].leds[1]) {
                g.setColor(Color.green);
            } else {
                g.setColor(Color.green.darker());
            }
            g.fillOval(rx - ledradius, ry, 2*ledradius, 2*ledradius);
            
            // led 2
            if(nodes[i].leds[2]) {
                g.setColor(Color.blue.brighter());
            } else {
                g.setColor(Color.blue.darker());
            }
            g.fillOval(rx + ledradius, ry, 2*ledradius, 2*ledradius);
            
        }
    }
    
    public void debugMsg(int node, String msg) {
        if(msg.startsWith("LEDS:")) {
            Node n = null;
            for(int i = 0; i < nodes.length; i++) {
                if(nodes[i].getID() == node) {
                    n = nodes[i];
                    break;
                }
            }
            
            String[] tokens = msg.split(" ");
            int l = -1;
            if(tokens[1].equals("Led0")) {
                l = 0;
            } else if(tokens[1].equals("Led1")) {
                l = 1;
            } else if(tokens[1].equals("Led2")) {
                l = 2;
            } else {
                System.out.println("Error Led Message: " + msg);
            }
            
            if(tokens[2].equals("on.")) {
                n.leds[l] = true;
            } else if(tokens[2].equals("off.")) {
                n.leds[l] = false;
            }
        } else {
            System.out.println("==> Debug (" + node + "): " + msg);
        }
    }
    
    public void errorMsg(int node, String msg) {
        System.out.println("==> Error (" + node + "): " + msg);
    }
    
    public void openFrame() {
        m_Frame = new Frame();
        m_Frame.add(this);
        /*    f.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent we) {
         System.exit(0);
         }
         });
         */
        m_Frame.addWindowListener( this );
        
        m_Frame.setSize(300, 300);
        m_Frame.setVisible(true);
    }
    
    public void windowOpened(WindowEvent arg0) {
    }
    
    public void windowClosing(WindowEvent arg0) {
        System.out.println("\nStopping simulation...");
        this.tossim.close();
        System.exit(0);
    }
    
    public void windowClosed(WindowEvent arg0) {
    }
    
    public void windowIconified(WindowEvent arg0) {
    }
    
    public void windowDeiconified(WindowEvent arg0) {
    }
    
    public void windowActivated(WindowEvent arg0) {
    }
    
    public void windowDeactivated(WindowEvent arg0) {
    }

    //////////////////////////////////////////////////////////////////////////
    // Titan specific

    /**
     * Delivers the message to each of the nodes in the network
     * @param msg The message to be delivered, the address is ignored
     */
    private void broadcastMsg( SerialMsg msg ) {
        for (int i=0; i<nodes.length;i++) {
            msg.set_address(nodes[i].getID());
            deliverMsg(msg);
        }
    }
    
    public void deliverMsg( SerialMsg msg ) {
        if (tossim==null) return;
        
        // if this is a broadcast message, send it individually, 
        // TOSSIM does not support broadcast.
        if (msg.get_address() == TitanCommunicate.TOS_BCAST_ADDR) {
            broadcastMsg(msg);
            return;
        }
        
        // create message
        tossim.runFunc("from SerialMsg import *");
        tossim.runFunc("msg=SerialMsg()");
        
        // set data
        tossim.runFunc("msg.set_address("+msg.get_address()+")");
        tossim.runFunc("msg.set_length("+msg.get_length()+")");
        
        // set buffer
        for(int i=0;i<msg.get_length();i++) {
            tossim.runFunc("msg.setElement_data("+i+","+msg.getElement_data(i)+")");
        }
        
        // now fill up the packet
        tossim.runFunc("pkt="+tossim.getID()+".newPacket()");
        tossim.runFunc("pkt.setData(msg.data)");
        tossim.runFunc("pkt.setType(msg.get_amType())");
        tossim.runFunc("pkt.setDestination("+msg.get_address()+")");
//cl        tossim.runFunc("pkt.setDestination(0)"); // always send to node 0 -> same as on PC with real network
        
        // deliver the packet
        tossim.runFunc("pkt.deliver("+msg.get_address()+","+tossim.getID()+".time()+1)");
        
        // run for a short while
        tossim.runNextEvent();
        
    }

    //////////////////////////////////////////////////////////////////////////
    // Testing code
    
    /**
     * Runs the simulation.
     * 
     * @param Path to directory where the TOSSIM python script lies.
     */
    static public final short TC_VERSION = 1;
    static public final short TITANCOMM_CONFIG  = 0;

    public static void main(String[] args) {
        
        Tossim t = new Tossim(args[0]);
        
        t.addChannel("TitanTask",true);
//      t.addChannel("TitanComm");
        t.addChannel("TitanConfig",true);
        t.addChannel("Titan",true);
//      t.addChannel("TitanPacket");
//      t.addChannel("TitanQueue");
//      t.addChannel("TitanMemory");
        t.addChannel("TitanSerial",true);
        t.addChannel("TitanTaskSS",true);
        
        Simulation sim = new Simulation(t);
        t.setDebugListener(sim);
        sim.openFrame();
        
        t.runFor(5);
/*
        short [] fwdconfig = 
        { (short)((TC_VERSION << 4) | TITANCOMM_CONFIG), // msg type
                2, 1, 0, 0,     // task num, conn num, master ID
                (5<<4)|2,       // config ID, num tasks
                0, 1, 0, 0,     // task 0 - SimpleWriter
                0, 4, 1, 0,     // task 2 - LEDs
                (5<<4)|1,       // config ID, num tasks connections
                0, 0, 1, 0,     // connection
                0
        };
        
        SerialMsg msg = new SerialMsg(fwdconfig.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
        
        msg.set_length((short)fwdconfig.length);
        msg.set_address(1);
        msg.set_data(fwdconfig);

        sim.deliverMsg(msg);
*/        
        t.runFor(5);
    }
    
}
