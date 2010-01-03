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

package titancommon.execution;

import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import titan.TitanCommunicate;
import titancommon.messages.ConfigurationSuccessMsg;
import titancommon.messages.DataMsg;
import titancommon.messages.MessageDispatcher;
import titan.messages.SerialMsg;
import titancommon.TitanCommand;

/**
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

public class RecoverNode extends TimerTask implements Observer {

    /** Communication to the sensor network */
    private TitanCommunicate m_Comm;
    private TaskNetwork      m_TaskNetwork;
    private MessageDispatcher m_MessageDispatcher;
    private int              m_nodeAddr;
    private Timer            m_Timer;
    private boolean          m_bRunning = true;
    private int              m_nodeResponses; ///< messages seen from the node in the last 3 seconds

    private final int RETRY_PERIOD = 3000;
    
    public boolean m_bDebugOutput = false;
    
    public RecoverNode (TitanCommunicate comm, TaskNetwork tn, MessageDispatcher md, int nodeAddr) {
        
        // store objects
        m_Comm = comm;
        m_TaskNetwork = tn;
        m_MessageDispatcher = md;
        m_nodeAddr = nodeAddr;
        
        m_bRunning = true;

        // register for notifications on new nodes
        m_MessageDispatcher.addObserver(this);

    }

    private static boolean bBusy = false;
    private /*synchronized*/ void reconfigureNode() {
       if (bBusy == false) bBusy = true;
       else {
          m_bRunning = false;
          while(bBusy) {
             try {
               Thread.sleep(50);
               if (m_bDebugOutput) System.out.print(".");
             } catch (InterruptedException ex) {
               ex.printStackTrace();
               bBusy = false;
               return;
             }
          }
          m_bRunning = true;
          bBusy = true;
       }
       Date now = new Date();
       System.out.println(now.getTime() + " RecoverNode: received no answer from node: " + m_nodeAddr + ": reconfiguring");
       
       
       try {
            clearNode();
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            //Logger.getLogger(RecoverNode.class.getName()).log(Level.SEVERE, null, ex);
        }
       
       m_TaskNetwork.reconfigureNode(m_Comm,m_nodeAddr);
       bBusy = false;
    }

    private void clearNode(){
                    // set up the configuration messages
            short [] fwdconfig = { 
                    (short)((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_CONFIG), // msg type
                    0, 0, 0, 0,     // task num, conn num, master ID
                    0, 0  // indicate nothing more
            };
               
            SerialMsg msg = new SerialMsg(fwdconfig.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
            
            msg.set_length((short)fwdconfig.length);
            msg.set_address(m_nodeAddr);
            msg.set_data(fwdconfig);
            
            System.out.print("Sending clear message 1/1 (" + fwdconfig.length + " bytes) ...");
            m_Comm.send( 0, msg );
            System.out.println("ok"); 
    }
    
    /**
     * Starts the RecoverNode object
     */
    public void start() {
       if( m_nodeAddr == 0 ) return;
        // instantiate timer
        m_Timer = new Timer();
        m_Timer.scheduleAtFixedRate(this, RETRY_PERIOD, RETRY_PERIOD);
    }
    
    /**
     * Listen for any incoming message from a node 
     */
    public void update(Observable obs, Object param) {

      int nodeAddress = -1;

      // get node message sources
      if (param instanceof titancommon.messages.DataMsg) {
         nodeAddress = m_TaskNetwork.getNodeAddrByPort(m_TaskNetwork.m_iMasterAddr,((DataMsg) param).destPort);
      } else if (param instanceof titancommon.messages.ConfigurationSuccessMsg) {
         nodeAddress = ((ConfigurationSuccessMsg) param).nodeID;
      }

      //System.out.println("RecoverNode(" + m_nodeAddr + "): Got message from node " + nodeAddress + " responses="+m_nodeResponses);

      // relevant message?
      if (nodeAddress == m_nodeAddr) m_nodeResponses++;
    }

    /**
     * Called periodically by the timer. Tries every time to reconfigure the node.
     * Is stopped by a call to update with a registration of the node
     */
    public void run() {
        if ( m_bRunning ) {
           
           if ( m_nodeResponses == 0) { 
               //m_Timer.cancel();
               reconfigureNode();
               //m_Timer = new Timer();
               //m_Timer.scheduleAtFixedRate(this, RETRY_PERIOD, RETRY_PERIOD);
           } else {
              m_nodeResponses = 0;
           }
        } else {
            m_Timer.cancel();
            m_MessageDispatcher.deleteObserver(this);
        }
    }
    
    public void stop() {
       if(m_Timer != null ) m_Timer.cancel();
       m_MessageDispatcher.deleteObserver(this);
    }

}
