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

import java.util.ArrayList;

import titancommon.Connection;
import titancommon.TitanCommand;
import titan.TitanCommunicate;
import titancommon.compiler.NodeMetrics;
import titan.messages.SerialMsg;
import titancommon.services.NodeInfo;
import titancommon.tasks.Communicator;
import titancommon.tasks.Task;

/**
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class TaskNetwork {
    
    ////////////////////////////////////////////////////////////////////////////
    // TaskNetwork class
    
    public class NodeConfiguration {
        public int          address;
        
        protected NodeInfo  m_nodeInfo;
        
        public Task[]       tasks;
        public Connection[] connections;
        
        public NodeConfiguration(NodeInfo ni) {
            m_nodeInfo = ni;
        }
        
        /** returns a copy of itself */
        public Object clone() {
        	NodeConfiguration nc = new NodeConfiguration(m_nodeInfo);
        	nc.address = address;
        	nc.tasks = new Task[tasks.length];
        	for (int i=0; i< tasks.length; i++) {
        		nc.tasks[i] = (Task) tasks[i].clone();
        	}
        	nc.connections = new Connection[connections.length];
        	for (int i=0; i< connections.length; i++) {
        		nc.connections[i] = (Connection) connections[i].clone();
        	}
        	
        	return nc;
        }
        
        /** 
         * Adds a connection to the connection array
         * @param StartTask 
         * @param StartPort
         * @param EndTask
         * @param EndPort
         */
        public void addConnection( int StartTask, int StartPort, int EndTask, int EndPort ){
            
            // check whether array exists
            if ( connections == null ) {
                connections = new Connection[1];
                connections[0] = new Connection( StartTask, StartPort, EndTask,   EndPort );
                
            } else {
                // rearrange the array
                Connection[] nc2 = new Connection[connections.length+1];
                for(int j=0; j < connections.length; j++ ) {
                    nc2[j] = connections[j];
                }
                nc2[nc2.length-1] = new Connection( StartTask, StartPort, EndTask,   EndPort );
                
                connections = nc2;
                
            }
        } // method NodeConfiguration.addConnection
        
        /**
         * Adds a task to the array of tasks
         * @param newTask
         * @return The index at which the task has been inserted
         */
        public int addTask( Task newTask ) {
            
            // create if not exists, else enlarge
            if ( tasks == null ) {
                tasks = new Task[1];
            } else { // append an entry
                Task[] tsk2 = new Task[tasks.length+1];
                for(int i=0; i<tasks.length;i++) {
                    tsk2[i] = tasks[i];
                }
                tasks = tsk2;
            }
            
            // add task to task list
            tasks[tasks.length-1] = newTask;
            
            return tasks.length-1;
        }
        
        /**
         * Returns the first instance of a task with the taskID found
         * @param taskID
         * @return Task if successful, else null
         */
        public Task getTask( int taskID ) {
            for(int i=0; i<tasks.length; i++ ) {
                if ( tasks[i].getID() == taskID ) return tasks[i];
            }
            return null;
        }
        
        /**
         * Returns the first index at which the task with the id taskID is 
         * encountered
         * @param taskID
         * @return index
         */
        public int getTaskIndex( int taskID ) {
            for(int i=0; i<tasks.length; i++ ) {
                if ( tasks[i].getID() == taskID ) return i;
            }
            return -1;
        }
        
        /**
         * Returns the processing capabilities of the node, which are used to compute 
         * whether it can run all the required tasks.
         * 
         * @return NodeMetrics containing the characteristics of the node
         */
        public NodeMetrics getNodeMetrics() {
            if (m_nodeInfo==null) return null;
            return m_nodeInfo.getNodeMetrics();
        }
        
    } // class NodeConfiguration
    
    
    ////////////////////////////////////////////////////////////////////////////
    // TaskNetwork class
    
    public ArrayList/*<NodeConfiguration>*/ m_Nodes = new ArrayList();
    protected ArrayList/*<short[]>*/ m_MsgList;
    protected short m_ConfigID;
    protected short m_iMasterAddr;
    
    public boolean m_bDebugOutput = false;
    
    /** Stores the allocation of tasks to nodes */
    private int [] m_AllocVector;
    /** Configuration is only started after a configuration start message has been sent */
    private boolean m_bDelayedReconfig = true;
	
    /** returns a vector displaying the allocation created during the last run */
    public void setAllocVector(int[] allocVector) {
        m_AllocVector = (int[]) ((allocVector != null) ? allocVector.clone() : null);
    }
    public int [] getAllocVector() {
        return (int[]) ((m_AllocVector != null) ? m_AllocVector.clone() : null);
    }

    public Object clone() {
    
    	TaskNetwork newTN = new TaskNetwork();
    	
    	for (int i=0; i<m_Nodes.size(); i++) {
    		newTN.m_Nodes.add( ((NodeConfiguration)(m_Nodes.get(i))).clone() );
    	}
    	
    	newTN.m_ConfigID = m_ConfigID;
    	newTN.m_iMasterAddr = m_iMasterAddr;
    	newTN.m_bDebugOutput = m_bDebugOutput;

    	newTN.setAllocVector(m_AllocVector);

    	return newTN;
    }
    
    public TaskNetwork() {}
    
    public void setConfigID(short iConfigID) {
    	m_ConfigID = iConfigID;
    }
    
    /**
     * Creates a task network with an initial configuration, which is stored in 
     * a virtual node with address -1
     * @param tsk The tasks to be added to the task network
     * @param cnt 
     */
    public TaskNetwork( Task[] tsk, Connection[] cnt ) {
        
        NodeConfiguration node = new NodeConfiguration(null);
        
        node.address = -1;
        node.tasks = tsk;
        node.connections = cnt;
        
        m_Nodes.add(node);
    }
    
    public NodeConfiguration getNode( int address ) {
        
        for(int i=0; i<m_Nodes.size(); i++) {
            NodeConfiguration curNode = (NodeConfiguration)m_Nodes.get(i);
            if ( curNode.address == address ) {
                return curNode;
            }
        }
        
        return null;
    }
    
    public NodeConfiguration addNode( NodeInfo ni ) {
        NodeConfiguration node = new NodeConfiguration(ni);
        node.address = ni.address;
        m_Nodes.add(node);
        return node;
    }
    
    public void printTaskNetwork() {
        for (int i=0;i<m_Nodes.size();i++) {
            NodeConfiguration nc = (NodeConfiguration)m_Nodes.get(i);
            
            System.out.println("Node "+nc.address+" configuration:");
            
            
            // print task info
            for (int j=0; j<nc.tasks.length; j++) {
                System.out.println("  Task " + j + " is " + nc.tasks[j].getName());
                
                // print contents of COM task
                if ( nc.tasks[j].getID() == Communicator.TASKID ){
                    Communicator com = (Communicator)nc.tasks[j];
                    for (int k=0; k<com.m_Connections.size(); k++ ){
                        Communicator.CommConfig ccfg = (Communicator.CommConfig)com.m_Connections.get(k);
                        System.out.println("    Port "+k+" mapped to ("+ccfg.destAddr+","+ ccfg.destPort+")");
                    }
                }
            }
            
            if (nc.connections == null) {
                System.out.println("No connections on node " + nc.address + "!\n");
            }
            
            for (int j=0; j<nc.connections.length; j++)  {
                System.out.println("  Connection " + j + " from ("+
                        nc.connections[j].StartTask + "," + nc.connections[j].StartPort +
                        ") to (" +
                        nc.connections[j].EndTask + "," + nc.connections[j].EndPort +
                ")");
            }
            
        } // foreach node
        
    }
    
    /**
     * Prints the task network configuration into a one line string. The format 
     * is:
     * 
     * [nodeNum] ([nodeID] [serviceNum] {[serviceID]}*)*
     * 
     * nodeNum     number of nodes ()*
     * nodeID      node address
     * serviceNum  number of services on the node {}*
     * serviceID   serviceID on the node
     * 
     * @return A string without newlines describing the task configuration
     */
    public String getConfigurationLine() {
        String strLine = m_Nodes.size() + ";";
        for (int i=0;i<m_Nodes.size();i++) {
            NodeConfiguration nc = (NodeConfiguration)m_Nodes.get(i);
            int iNodeServices=0;
            String strServices="";
            // print task info
            for (int j=0; j<nc.tasks.length; j++) {
                if ( nc.tasks[j].getID() != 0 ) { // do not count com task
                    strServices += nc.tasks[j].getID() + ";";
                    iNodeServices++;
                }
            }
            strLine += nc.address + ";" + iNodeServices + ";" + strServices;
        }
        return strLine;
    }
    
    /**
     * Checks whether the node is part of network description.
     * @param address NodeID of the node of interest
     * @return true if the node is in the task network
     */
    public boolean containsNode(int address) {
        for(int i=0; i< m_Nodes.size(); i++) {
            if ( ((NodeConfiguration)m_Nodes.get(i)).address == address ) return true;
        }
        return false;
    }
    
    
    
    
    /* ********************************************************************************************** */
    /*                           COMMUNICATION WITH THE NETWORK                                       */
    /* ********************************************************************************************** */
    
    final static int FWD_HEADER_SIZE = 4;
    // TODO: MAX_CFGFWD_PAYLOAD_SIZE depends on hopcount
    final static int MAX_CFGFWD_PAYLOAD_SIZE = TitanCommand.TOSH_DATA_LENGTH-FWD_HEADER_SIZE;
    
    public boolean configureNetwork(TitanCommunicate comm) {
        
        m_MsgList = new ArrayList();
        
        // go through all the nodes
        for (int i=0; i<m_Nodes.size();i++) {
            NodeConfiguration nc = (NodeConfiguration)m_Nodes.get(i);
            
            // create the messages
            tasksToMsg(m_MsgList, nc);
            connsToMsg(m_MsgList, nc);
            
        } // foreach node
        
        
        // now actually send the configuration to the motes
        if (comm == null) return false;
        // foreach node
        for ( int i=0; i< m_MsgList.size(); i++ ) {
            
            // get data
            short[] cfgData = (short[])m_MsgList.get(i);
            
            // send message
            if (m_bDebugOutput) {
            	System.out.print("Sending configuration message "+(i+1)+"/"+m_MsgList.size()+" ...");
            }
            
            if ( cfgData[0] != 1 ) {
                System.err.println("ERROR: Cannot handle multihop messages!");
                return false;
            }
            
            SerialMsg msg = new SerialMsg(cfgData.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
            msg.set_length( cfgData[1] );
            msg.set_address( cfgData[2]*256 + cfgData[3] );
            
            //if (m_bDebugOutput) System.out.print("\nTN: Message to "+ (cfgData[2]*256 + cfgData[3]) + "size: " + cfgData[1] + ": " );

            short[] msgData = new short[cfgData.length-4];
            for( int j=0; j<msgData.length; j++ ) {
                msgData[j] = cfgData[j+4];
                //if (m_bDebugOutput)  System.out.print(" " + msgData[j]);
            }
            msg.set_data(msgData);
            
            comm.send(0, msg);
            if (m_bDebugOutput) System.out.println("ok");
            
        }
        
        if (m_bDelayedReconfig) {
           sendStartConfigMsg(comm,65535); // broadcast start configuration
        }
            
        return true;
    }

    private void sendStartConfigMsg(TitanCommunicate comm, int nodeAddr) {
        short [] fwdconfig = 
        { (short)((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_CFGSTART), // msg type
        		0,0,(short)m_ConfigID};
        
        SerialMsg msg = new SerialMsg(fwdconfig.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
        
        msg.set_length((short)fwdconfig.length);
        msg.set_address(nodeAddr); // always broadcast
        msg.set_data(fwdconfig);
        
        System.out.print("Sending configuration "+ m_ConfigID +" start message ("+fwdconfig.length+" bytes) ...");
        comm.send( 0, msg );
        System.out.println("ok");
    }

    /**
     * Reconfigures a node with the same configuration as when the execution started
     * @param comm Communication object to connect to the sensor network
     */
    public boolean reconfigureNode(TitanCommunicate comm, int nodeAddr ) {
        
        // now actually send the configuration to the motes
        if (comm == null) return false;
        if (m_MsgList == null ) return false;
        
        // foreach node
        int msgCount=1;
        for ( int i=0; i< m_MsgList.size(); i++ ) {
            
            // get data
            short[] cfgData = (short[])m_MsgList.get(i);
            
            int msgDest = cfgData[2]*256 + cfgData[3];
            
            // only send messages to that particular node
            if ( msgDest != nodeAddr ) continue;
            
            // send message
            //System.out.print("Reconfiguring node "+nodeAddr+": Message "+msgCount+" ...");
            msgCount++;
            
            if ( cfgData[0] != 1 ) {
                System.err.println("ERROR: Cannot handle multihop messages!");
                return false;
            }
            
            SerialMsg msg = new SerialMsg(cfgData.length-4+SerialMsg.DEFAULT_MESSAGE_SIZE);
            msg.set_length( cfgData[1] );
            msg.set_address( msgDest );
            
            //System.out.print("\nTNR: Message to "+ msgDest + "size: " + cfgData[1] + ": " );

            short[] msgData = new short[cfgData.length-4];
            for( int j=0; j<msgData.length; j++ ) {
                msgData[j] = cfgData[j+4];
                //System.out.print(" " + msgData[j]);
            }
            msg.set_data(msgData);
            
            comm.send(0, msg);
            //System.out.println("ok");
            
        }
        
        sendStartConfigMsg(comm, nodeAddr);

        return true;
    }
    
    final static int FWD_CONFIG_MSG_HEADER_SIZE  = 6+1; // +1 footer
    final static int FWD_CFGTASK_MSG_HEADER_SIZE = 2+1; // +1 footer
    
    /**
     * 
     * Packs the tasks into configuration messages
     * 
     * @param MsgList
     * @param nc
     */
    
    private boolean tasksToMsg(ArrayList MsgList, NodeConfiguration nc) {

        ArrayList/*<short[]>*/ configPackets = new ArrayList();
        
        int iMaxCfgSize = MAX_CFGFWD_PAYLOAD_SIZE-4-FWD_CFGTASK_MSG_HEADER_SIZE;
        
        // collect all configuration packets from the tasks
        for(int i=0; i < nc.tasks.length; i++ ) {
            short [][] configData = nc.tasks[i].getConfigBytes(iMaxCfgSize);
            
            if ( configData == null ) configData = new short[1][0];
            
            // go through all configuration packets
            for (int j=0; j < configData.length; j++ ) {
                
                // check the sizes
                if ( configData[j].length > iMaxCfgSize ) {
                    System.err.println("ERROR: Configuration packet "+j+ " of task "+nc.tasks[i].getName()+" too large for message");
                    continue;
                }
                
                // add header with the number of tasks
                short [] taskInfo = new short[configData[j].length + 4];
                taskInfo[0] = (short)((nc.tasks[i].getID()>>8)&0xFF);
                taskInfo[1] = (short)(nc.tasks[i].getID()&0xFF);
                taskInfo[2] = (short)i;
                taskInfo[3] = (short)configData[j].length;

                // copy configuration data
                for (int k=0; k < configData[j].length; k++ ) {
                    taskInfo[k+4] = configData[j][k];
                }

                // add to configuration buffer
                configPackets.add( taskInfo );
            }
        } // for: get configuration packets
        
        // now fit packets into messages. First message contains 
        // additional configuration data
        int iMsgSize = FWD_CONFIG_MSG_HEADER_SIZE+FWD_HEADER_SIZE;
        int iFirstPacket = 0;
        boolean bFirstMessage = true;
        for (int iCurCfgPacket=0; iCurCfgPacket < configPackets.size(); iCurCfgPacket++ ) {
            
            // check whether the packet is full, or it is the last
            if ( iMsgSize + ((short[])configPackets.get(iCurCfgPacket)).length > MAX_CFGFWD_PAYLOAD_SIZE ) {
                
                createMessage(MsgList, nc, configPackets, iMsgSize, iFirstPacket, bFirstMessage, iCurCfgPacket);
                
                bFirstMessage = false;
                
                // set data for next iteration
                iFirstPacket = iCurCfgPacket;
                iMsgSize = FWD_HEADER_SIZE+FWD_CFGTASK_MSG_HEADER_SIZE + ((short[])configPackets.get(iCurCfgPacket)).length;

            } else {
                iMsgSize += ((short[])configPackets.get(iCurCfgPacket)).length;
            }
            
        }

        // send also the last message
        if ( configPackets.size() > 0 ) {
            createMessage(MsgList, nc, configPackets, iMsgSize, iFirstPacket, bFirstMessage, configPackets.size());
        }
        
        return true;
        
    }

    private void createMessage(ArrayList MsgList, NodeConfiguration nc, ArrayList configPackets, int iMsgSize, int iFirstPacket, boolean bFirstMessage, int iCurCfgPacket) {
        short[] cfgData = new short[iMsgSize];
        
        int iMsgIndex = 0;
        
        // CFGFORWARD header
        cfgData[iMsgIndex++] = 1;        // hop count
        cfgData[iMsgIndex++] = (short)(iMsgSize-FWD_HEADER_SIZE); // payload size
        cfgData[iMsgIndex++] = (short)((nc.address>>8)&0xFF); // address high byte
        cfgData[iMsgIndex++] = (short)(nc.address&0xFF);      // address low  byte

        if (bFirstMessage) {

            // internal message header: CONFIG
            cfgData[iMsgIndex++] =(short)((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_CONFIG);
            cfgData[iMsgIndex++] = (short) ((short)(m_bDelayedReconfig? 0x80 : 0x00) | (short)nc.tasks.length);              // number of tasks in the configuration
            cfgData[iMsgIndex++] = (short)nc.connections.length;// number of connections in the configuration 
            cfgData[iMsgIndex++] = (short)((m_iMasterAddr>>8)&0xFF);
            cfgData[iMsgIndex++] = (short)((m_iMasterAddr)&0xFF);
        } else {
            
            // internal message header: CFGTASK
            cfgData[iMsgIndex++] = (short)((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_CFGTASK);
        }
        
        cfgData[iMsgIndex++] = (short)((m_ConfigID<<4)|(iCurCfgPacket-iFirstPacket)); // configuration ID, number of tasks 

        
        // headers set, now copy data
        for ( int j=iFirstPacket; j < iCurCfgPacket; j++ ) {
            short[] curPacket = (short[])configPackets.get(j); 
            for ( int k=0; k < curPacket.length; k++ ) {
                cfgData[iMsgIndex++] = curPacket[k];
            }
        }
        
        // add a last zero byte, indicating there are no connections following
        cfgData[iMsgIndex++] =0;
        
        if ( iMsgIndex != cfgData.length ) {
            System.err.println("CreateMessage(): Error: message buffer not filled!!! Only "+iMsgIndex+" of "+cfgData.length);
        }

        //////////////////
        // Debug output
/*        System.out.println("Created message with "+iMsgIndex+"/"+cfgData.length+" bytes");
        for (int k=0; k<cfgData.length; k++ ) {
          System.out.print(k+":"+cfgData[k]+ "\t");
        }
        System.out.println("");
*/        //////////////////
        
        // now store the message
        MsgList.add(cfgData);
    }
    
    
    final static int FWD_CFGCONN_MSG_HEADER_SIZE = 2;
    /**
     * 
     * Packs the connections into configuration messages
     * 
     * @param MsgList list of messages where the configuration messages are added
     * @param nc NodeConfiguration of the node to produce the configuration messages for
     * @param nc
     */
    private boolean connsToMsg(ArrayList MsgList, NodeConfiguration nc) {
        
        int CON_SIZE = 4;
        
        // create task configuration messages
        int iConns = 0;
        int iMsgSize  = FWD_CFGCONN_MSG_HEADER_SIZE;
        int iMsgConns = 0;
        while ( iConns < nc.connections.length ) {
            
            // full enough?
            if ( iMsgSize + CON_SIZE > MAX_CFGFWD_PAYLOAD_SIZE ) {
                
                createConnMessage(MsgList, nc, iConns, iMsgSize, iMsgConns);
                
                // reset the message trackers
                iMsgSize = FWD_CFGCONN_MSG_HEADER_SIZE;
                iMsgConns = 0;
                
            } else { // connection fits into the message -> update trackers
                iMsgSize += CON_SIZE;
                iConns++;
                iMsgConns++;
            }
        } // foreach connection
        
        if ( iMsgSize > 0 ) {
            createConnMessage(MsgList, nc, iConns, iMsgSize, iMsgConns);
        }
        
        return true;
    }
    
    /**
     * Embedds a connection connfiguration message into a CFGForward message
     * (refactored to make connsToMsg a little clearer)
     * 
     * @param MsgList   Array of messages where the message will be added
     * @param nc        Node configuration to be transmitted
     * @param iConns    Number of remaining messages
     * @param iMsgSize  Total number of bytes available in a message
     * @param iMsgConns Total number of messages that can be saved in one message
     */
    private void createConnMessage(ArrayList MsgList, NodeConfiguration nc, int iConns, int iMsgSize, int iMsgConns) {
        int iMsgIndex=0;
        
        // create the packet
        short[] cfgData = new short[iMsgSize+FWD_HEADER_SIZE];
        
        // CFGFORWARD header
        cfgData[iMsgIndex++] = 1;        // hop count
        cfgData[iMsgIndex++] = (short)iMsgSize; // payload size
        cfgData[iMsgIndex++] = (short)((nc.address>>8)&0xFF); // address high byte
        cfgData[iMsgIndex++] = (short)(nc.address&0xFF);
        
        // internal message header: CFGCONN
        cfgData[iMsgIndex++] = (short)((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_CFGCONN);
        cfgData[iMsgIndex++] = (short)((m_ConfigID<<4)|iMsgConns); // configuration ID, number of connections 
        
        // add connection configuration
        for (int j=iConns-iMsgConns; j<iConns; j++ ) {
            
            // write taskID
            cfgData[iMsgIndex++] = (short)nc.connections[j].StartTask;
            cfgData[iMsgIndex++] = (short)nc.connections[j].StartPort;
            cfgData[iMsgIndex++] = (short)nc.connections[j].EndTask;
            cfgData[iMsgIndex++] = (short)nc.connections[j].EndPort;
            
        }
        
//      System.out.println("Created message with "+iMsgIndex+"/"+cfgData.length+" bytes");
//      for (int k=0; k<cfgData.length; k++ ) {
//      System.out.println("Byte "+k+" is "+cfgData[k]);
//      }
        
        
        // add to message list
        MsgList.add( cfgData );
    }
    
    
    /**
     * Clears all node configurations on the nodes in the task network
     *
     */
    public boolean clearConfig(TitanCommunicate comm) {
        
        for(int i=0; i< m_Nodes.size(); i++) {
            
            short [] fwdconfig = { 
                    (short)((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_CONFIG), // msg type
                    0, 0, 0, 0,     // task num, conn num, master ID
                    0, 0  // indicate nothing more
            };
            
            
            SerialMsg msg = new SerialMsg(fwdconfig.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
            
            msg.set_length((short)fwdconfig.length);
            msg.set_address(((NodeConfiguration)m_Nodes.get(i)).address);
            msg.set_data(fwdconfig);
            
            if (m_bDebugOutput) System.out.print("Sending configuration message 1/1 (" + fwdconfig.length + " bytes) ...");
            comm.send( 0, msg );
            if (m_bDebugOutput) System.out.println("ok");
            
        }
        return true;
    } // forall nodes

    /**
     * Searches the node address of the node that sends data to the specified port 
     * at the specified node.
     * @param nodeAddr  The node receiving data
     * @param port      The port on which the node is receiving data
     * @return Node address
     */
    public int getNodeAddrByPort(int nodeAddr, int port) {

        // go through all nodes and check their communication tasks
        for(int i=0; i< m_Nodes.size(); i++) {

            // get the communication task
            Task commTask = ((NodeConfiguration)m_Nodes.get(i)).getTask(Communicator.TASKID);
            if (commTask == null) continue; // no communication here
            
            // check whether it has a connection to nodeAddr and port port
            Communicator comm = (Communicator)commTask;
            if ( comm.hasConnection(nodeAddr,port) ) {
                return ((NodeConfiguration)m_Nodes.get(i)).address;
            }
        }
        
        return -1;
    }
    
    
}
