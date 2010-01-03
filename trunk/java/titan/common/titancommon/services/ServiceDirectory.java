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
 * ServiceDirectory.java
 * 
 * This object stores information about the nodes available in the sensor network. 
 * It periodically broadcasts messages asking nodes to submit the TaskIDs of the 
 * tasks they provide.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 */
package titancommon.services;

import java.util.*;

import titancommon.TitanCommand;
import titan.TitanCommunicate;
import titan.messages.SerialMsg;
import titancommon.route.TitanRouter;
import titancommon.services.ServiceNotices.*;

public class ServiceDirectory extends TimerTask {
    
    /** Enable active service discovery by issuing service query messages */
    private boolean m_bActiveSearch = true;
    
    private int m_curInquirySequenceNumber = 0;
    
    //TODO: Reimplement foreign directory search for Java 1.3
    //private List m_ForeignDirectories = new ArrayList();
    
    TitanCommunicate m_Comm; ///< Connector to the network. Used to send messages
    Timer  m_Timer;  ///< Periodic timer for issuing service discovery messages
    HashMap    m_NodeInfo = new HashMap();  ///< Service database linking nodeAddress->information class  //CHANGE_MR: Map...
 //   @SuppressWarnings("unused")
//    private static int INQUIRE_PERIOD = 1000;
    
    // adding observers to ServiceDirectory changes
    private ServiceNotices m_Observers = new ServiceNotices();  ///< Observable object that notifies clients of changes
    
    public ServiceDirectory( TitanCommunicate comm ) {
        m_Comm = comm;
        
        if (m_Comm != null) {
            m_Timer = new Timer();
//cl            m_Timer.scheduleAtFixedRate(this, 0, INQUIRE_PERIOD);

            /* **********************************************/
            /*                 TEST CODE!!!                 */
            /* **********************************************/
            
          int[] testtasks = { 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43 };
          m_NodeInfo.put( new Integer( 0), new NodeInfo( 0,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 1), new NodeInfo( 1,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 2), new NodeInfo( 2,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 3), new NodeInfo( 3,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 4), new NodeInfo( 4,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 5), new NodeInfo( 5,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 6), new NodeInfo( 6,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 7), new NodeInfo( 7,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 8), new NodeInfo( 8,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 9), new NodeInfo( 9,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer(10), new NodeInfo(10,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer(11), new NodeInfo(11,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 12), new NodeInfo( 12,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 13), new NodeInfo( 13,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 14), new NodeInfo( 14,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer( 15), new NodeInfo( 15,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer(16), new NodeInfo(16,0, testtasks,1 ) );
          m_NodeInfo.put( new Integer(17), new NodeInfo(17,0, testtasks,1 ) ); 

          // local nodes, id 100++
          m_NodeInfo.put( new Integer(100), new NodeInfo(100,0, testtasks, 1 ) );
          m_NodeInfo.put( new Integer(101), new NodeInfo(101,0, testtasks, 1 ) );
          m_NodeInfo.put( new Integer(102), new NodeInfo(102,0, testtasks, 1 ) );
          for (int i = 103; i < (1 << TitanRouter.CLIENT_BITS); i++ ) {
            m_NodeInfo.put( new Integer(i), new NodeInfo(i,0, testtasks, 1 ) );
          }

          int net_id = 1 << TitanRouter.CLIENT_BITS;
          m_NodeInfo.put( new Integer(net_id + 1),   new NodeInfo(net_id + 1, 0, testtasks, 1) );
          m_NodeInfo.put( new Integer(net_id + 2),   new NodeInfo(net_id + 2, 0, testtasks, 1) );
          m_NodeInfo.put( new Integer(net_id + 100), new NodeInfo(net_id + 100, 0, testtasks, 1) );
          m_NodeInfo.put( new Integer(net_id + 101), new NodeInfo(net_id + 101, 0, testtasks, 1) );

          net_id = 2 << TitanRouter.CLIENT_BITS;
          m_NodeInfo.put( new Integer(net_id + 1),   new NodeInfo(net_id + 1, 0, testtasks, 1) );
          m_NodeInfo.put( new Integer(net_id + 2),   new NodeInfo(net_id + 2, 0, testtasks, 1) );
          m_NodeInfo.put( new Integer(net_id + 100), new NodeInfo(net_id + 100, 0, testtasks, 1) );
          m_NodeInfo.put( new Integer(net_id + 101), new NodeInfo(net_id + 101, 0, testtasks, 1) );
        }
        
    }
    
    /**
     * This is called by TitanCommand to indicate that a service discovery 
     * reply message has been received. It is usually an answer to a service 
     * discovery message sent in the run() function.
     * @param addr Address of the replying node
     * @param msg  Message received
     */
    public void messageReceived(int addr, SerialMsg msg) {
        
        // do some checks
        SerialMsg SMsg = msg;
        if ( (SMsg.getElement_data(0)&0xF) != TitanCommand.TITANCOMM_DICS_REP ) return;
        
        // parse message
        int nodeAddress = SMsg.getElement_data(1)*256 + SMsg.getElement_data(2);
        int iNumTasks = SMsg.getElement_data(3);
        int[] taskIDs = new int[iNumTasks];
        
        // extract task identifiers
        for ( int i=0; i < iNumTasks; i++ ) {
            taskIDs[i] = (SMsg.getElement_data( 4 + i*2 )*256 + SMsg.getElement_data(5+i*2));
        }
        
        //TODO: NodeType information should be included into the task registration messages
        updateNodeInfo( nodeAddress,-1, taskIDs );
    }
    
    /**
     * Updates the information on nodes and their available tasks.
     * @param nodeAddr  Node address that contains the tasks
     * @param nodeType  Type of the node to update
     * @param tasks     List of task IDs specifying the tasks on that node
     */
    public void updateNodeInfo( int nodeAddr, int nodeType, int[] tasks ) {
        
        NodeInfo ni = (NodeInfo)m_NodeInfo.get( new Integer(nodeAddr));
        
        // check whether data is available
        if ( ni == null ) {
            // node does not yet exist - create node
            m_NodeInfo.put( new Integer(nodeAddr), new NodeInfo(nodeAddr, nodeType, tasks, m_curInquirySequenceNumber ));
            m_Observers.notifyObservers(nodeAddr, NoticeReason.NODE_ADDED);
        } else {
            
            // fuse data
            // for every entry check whether it already exists
            for (int i=0; i<tasks.length; i++ ) {
                boolean bFound = false;
                for (int j=0; j<ni.tasks.length; j++ ) {
                    if ( ni.tasks[j] == tasks[i] ) {
                        bFound = true;
                        break;
                    }
                }
                if (bFound == false ) {
                    int [] temp = new int[ni.tasks.length+1];
                    for (int j=0; j< ni.tasks.length; j++ ){
                        temp[j] = ni.tasks[j];
                    }
                    temp[temp.length-1] = tasks[i];
                    ni.tasks = temp;
                }
            }
            
            if (ni.nodeType!=nodeType && nodeType!=-1) {
                ni.nodeType = nodeType;
            }
            
            // update value
            ni.value.updateValue( m_curInquirySequenceNumber );
        }
    }
    
    /**
     * Removes all stored data about the node with the given address
     * @param nodeAddr Node address to be removed
     * @return Whether the node could successfully be removed
     */
    public boolean removeNodeInfo( int nodeAddr ) {
        NodeInfo ni = (NodeInfo)m_NodeInfo.remove(new Integer(nodeAddr));
        return (ni != null);
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Retrieve information
    
    /**
     * Queries the service database for a task with the ID iTaskID
     * @param iTaskID Identifier of the task to be looked for
     * @return A List of node descriptors that contain the task asked for
     */
    public NodeInfo[] queryTask( int iTaskID ) {
  
        ArrayList/*<NodeInfo>*/ resNodes = (ArrayList)queryTaskCollection(iTaskID);
        
        if ( resNodes.size() == 0 ) return null;
        
        return (NodeInfo[])resNodes.toArray( new NodeInfo[resNodes.size()]);

//    	// drop whole HashMap m_NodeInfo content into NodeInfo[] structure
//    	// where one of the tasks is a iTaskID -> nodeinfo[i].tasks[j] == iTaskID
//        
//    	NodeInfo[] nodeinfo = (NodeInfo[]) m_NodeInfo.values(new NodeInfo[m_NodeInfo.size()]);
//    	
//    	ArrayList resNodes = new ArrayList();
//    	
//    	for (int i=0; i<nodeinfo.length; i++) {
//    		
//          // search through all registered tasks
//          for (int j=0; j<nodeinfo[i].tasks.length; j++ ) {
//              if ( nodeinfo[i].tasks[j] == iTaskID ) {
//                  resNodes.add( nodeinfo[i] );
//              }
//          }
//    	}
//    	
//    	NodeInfo[] returnNodes = new NodeInfo[resNodes.size()];
//    	for (int i=0; i<resNodes.size(); i++) {
//    		returnNodes[i] = (NodeInfo)resNodes.get(i);
//    	}
//    	
//    	return returnNodes;
    }
    
    /**
     * Queries the service database for a task with the ID iTaskID
     * @param iTaskID Identifier of the task to be looked for
     * @return A List of node descriptors that contain the task asked for
     */
    public Collection queryTaskCollection(int iTaskID) {
        NodeInfo[] nodeinfo = (NodeInfo[])m_NodeInfo.values().toArray( new NodeInfo[m_NodeInfo.size()]);
        
        ArrayList resNodes = new ArrayList();
        
        
        // search through all saved nodes
        for (int i=0; i<nodeinfo.length; i++) {
            
            // search through all registered tasks
            for (int j=0; j<nodeinfo[i].tasks.length; j++ ) {
                if ( nodeinfo[i].tasks[j] == iTaskID ) {
                    resNodes.add( nodeinfo[i] );
                }
            }
        }
        
/*        // add nodes from foreign directories
        for(int i=0; i<m_ForeignDirectories.size();i++){
            ArrayList<NodeInfo> foreign = (ArrayList<NodeInfo>)m_ForeignDirectories.get(i).queryTaskCollection(iTaskID);
            if( foreign != null) {
                for (int j=0; j<foreign.size();j++){
                    foreign.get(j).bForeign = true;
                }
                resNodes.addAll(foreign);
            }
        }
*/
        return resNodes;
    }

    /**
     * Gets a map of which nodes have which taskIDs. First index 
     * distinguishes the TaskIDs, second index goes through all nodes 
     * that contain the task
     * @param iTaskIDs tasks to be looked for
     * @return A map of tasks to nodes
     */
    public int[][] queryTasksToNodeMap(int[] taskIDs) {
        int[][] result = new int[taskIDs.length][];
        
        for(int i=0; i<taskIDs.length; i++ ) {
            NodeInfo[] nis = queryTask(taskIDs[i]);
            result[i] = new int[nis.length];
            for(int j=0; j<nis.length; j++) {
                result[i][j] = nis[j].address;
            }
        }
        
        return result;
    }
    
    /**
     * Returns a map of nodes containing a range of taskIDs. This method provides 
     * a shortcut for queryTaskToNodeMap(), where iFirstTaskID is the first ID to 
     * be retrieved, and iLastTaskID the last. Both IDs are included in the range.
     * @param iFirstTaskID first taskID to be
     * @param iLastTaskID  end taskID to be retrieved
     * @return A map from TaskID to possible NodeIDs
     */
    public int[][] queryTaskRangeToNodeMap(int iFirstTaskID, int iLastTaskID) {
        if ( iLastTaskID < iFirstTaskID ) return null;
        int [] taskIDs = new int[iLastTaskID-iFirstTaskID+1];
        for (int i=iFirstTaskID; i<=iLastTaskID; i++) {
            taskIDs[i-iFirstTaskID]=i;
        }
        return queryTasksToNodeMap(taskIDs);
    }

    public int queryMostTasks( int[] taskIDs ) {

        //NodeInfo[] nodeinfo = (NodeInfo[])m_NodeInfo.values().toArray( new NodeInfo[m_NodeInfo.size()]);
    	
    	NodeInfo[] nodeinfo = transformNodeInfoToArray(m_NodeInfo);
        
        int iMaxTasks = 0;
        int iNodeAddress = -1;
        
        // search through all saved nodes
        for (int i=0; i<nodeinfo.length; i++) {
            int iNodeTasks=0;
            // search through all registered tasks on the nodes
            for (int j=0; j<nodeinfo[i].tasks.length; j++ ) {
                // compare to the task list
                for (int k=0;k<taskIDs.length;k++) {
                    if ( nodeinfo[i].tasks[j] == taskIDs[k] ) {
                        iNodeTasks++;
                    }
                } // for k
            } // for j
            if (iNodeTasks > iMaxTasks) {
                iNodeAddress = nodeinfo[i].address;
                iMaxTasks = iNodeTasks;
            } else if ((iNodeTasks == iMaxTasks) && (nodeinfo[i].bForeign == false)) { // foreign node?
                iNodeAddress = nodeinfo[i].address;
                iMaxTasks = iNodeTasks;
            }
        } // for i
        
        return iNodeAddress;
    }
    
    /**
     * Checks whether a node has registered the given task
     * @param iNodeAddress Node that should contain the task
     * @param iTaskID ID of the task to be found
     * @return Whether the task is available
     */
    public boolean hasTask( int iNodeAddress, int iTaskID ) {
        
        NodeInfo ni = (NodeInfo) m_NodeInfo.get(new Integer(iNodeAddress));

        // locally available?
        if (ni == null) {
            // check foreign directories
//            for(int i=0; i<m_ForeignDirectories.size();i++){
//                if (m_ForeignDirectories.get(i).hasTask(iNodeAddress,iTaskID)) {
//                    return true;
//                }
//            }
            
            // not found on any other directory
        	return false;
        }
        
        for(int i=0; i < ni.tasks.length; i++) {
            if ( ni.tasks[i] == iTaskID ) return true;
        }
        
        return false;
    }
    
    /** 
     * Queries for the node information to the address of a certain node
     * @param Node ID for which NodeInfo should be retrieved
     * @return the NodeInfo structure for the given node ID 
     */
    public NodeInfo getNodeInfo( int nodeAddr ) {
        NodeInfo ni = (NodeInfo)m_NodeInfo.get(new Integer(nodeAddr));
        
        // maybe try other service directories
//        if (ni == null) {
//            for(int i=0; i<m_ForeignDirectories.size();i++){
//                ni = m_ForeignDirectories.get(i).getNodeInfo(nodeAddr);
//                
//                // found it? set the foreign bit!
//                if (ni!=null) {
//                    ni.bForeign = true;
//                    break;
//                }
//            }
//        }
        
        return ni;
    }

    /**
     * Notifies the service directory that a node is alive. This could come from a message 
     * received by this particular node, or similar.
     */
    public void nodeIsAlive( int nodeAddr ) {
        NodeInfo ni = (NodeInfo)m_NodeInfo.get( new Integer(nodeAddr));
        if ( ni != null ) {
            ni.value.isAlive();
        } else {
            updateNodeInfo( nodeAddr,-1, new int [0] );
        }
    }
    
    /**
     * Checks whether a given node exists in the service directory
     */
    public boolean nodeExists(int nodeAddr) {
        return m_NodeInfo.containsKey( new Integer(nodeAddr) );
    }
    
//    public Set getAllNodeAddr() {
//        Set allnodes = m_NodeInfo.keySet();
//        
////        for(int i=0; i<m_ForeignDirectories.size();i++){
////            Set foreignnodes = m_ForeignDirectories.get(i).getAllNodeAddr();
////            
////            try {
////                if (allnodes.size() == 0 ) {
////                    allnodes= foreignnodes;
////                }else {
////                    allnodes.addAll(foreignnodes);
////                }
////            } catch (UnsupportedOperationException e) {
////                e.printStackTrace();
////            }
////        }
//
//        return allnodes;
//    }

    
    /**
     * Adds an observer that will obtain information about changes in the service directory.
     * @param o New observer to be added
     */
    public void addObserver( Observer o ) {
        m_Observers.addObserver(o);
    }
    
    /**
     * Removes an observer from the list
     * @param o Observer to be removed
     */
    public void deleteObserver( Observer o ) {
        m_Observers.deleteObserver(o);
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Periodic updates / checks
    
    /**
     * Called periodically by the timer function. Issues service discovery 
     * messages
     */
    public void run() {
        
        // if no communication exists, don't do anything
        if (m_Comm == null) return;
        
        if (m_bActiveSearch) {
            short [] msgData = {
                    (short)((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_DISCOVER), //msg type
                    0, 0, // source ID
                    0 };  // scope
            
            SerialMsg msg = new SerialMsg(msgData.length+SerialMsg.DEFAULT_MESSAGE_SIZE);
            msg.set_address(TitanCommunicate.TOS_BCAST_ADDR);
            msg.set_length((short)msgData.length);
            msg.set_data( msgData );
            
//            try
//            {
//                m_Comm.send(MoteIF.TOS_BCAST_ADDR, msg);
//            }
//            catch (IOException e)
//            {
//                System.err.println("Warning: could not send service discovery message");
//            }
//            catch (InterruptedException e)
//            {
//                System.err.println("Warning: timed out on service discovery message!");
//            }
            
            // this is a hack, since TOSSIM does not support broadcast
			for ( int i=0; i < 6; i++ ) {
			  msg.set_address(i);
			  m_Comm.send( i, msg );
			}

        }
        
        // update nodeinfo database value
        CheckDatabaseEntries();
        m_curInquirySequenceNumber++;
        
    }
    
    /**
     * Goes through all service database entries and checks whether they should still be 
     * kept in the database.
     *
     */
    private void CheckDatabaseEntries() {
        
        // Transform NodeInfo into a list for easy iteration
        NodeInfo[] nodeinfo = transformNodeInfoToArray(m_NodeInfo);
        for (int i=0; i<nodeinfo.length; i++) {
            
            if ( ! nodeinfo[i].value.checkValue(m_curInquirySequenceNumber) ) {
            	
            	//TODO: do something for root node
            	if (nodeinfo[i].address == 0 ) continue;
                
                // this information lost its value - the node is probably dead/gone
                m_NodeInfo.remove(new Integer(nodeinfo[i].address));
                
                // give notice to anybody interested
                m_Observers.notifyObservers( nodeinfo[i].address, NoticeReason.NODE_REMOVED );
                
                System.out.println("ServiceDirectory: Removing node "+ nodeinfo[i].address + " from database");
                
            }
            
        } // foreach nodeinfo
        
    }
    
    /**
     * Sets service discovery to active or passive mode. In active mode, it issues periodically 
     * a service discovery message, while in passive mode it only reacts to incoming messages.
     * @param bActive True to set active mode, false to set passive mode
     * @return True if service directory is from now on in active mode
     */
    public boolean setActiveSearch(boolean bActive) {
    	return m_bActiveSearch = bActive;
    }

    /** Connects to a foreign service directory */
    public void addForeignSD(ServiceDirectory directory) {
 //       m_ForeignDirectories.add(directory);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Test code
    
    /**
     * Prints the content of the service database
     */
    public void printInfo(java.io.PrintStream out) {
        out.println("Service directory contents:\n");
        
        NodeInfo[] nodeinfo = transformNodeInfoToArray(m_NodeInfo);
        
        // go through all nodes //wor
        for (int i=0; i<nodeinfo.length; i++) {
            
        	out.println("  Node "+nodeinfo[i].address+" with value " + nodeinfo[i].value.getValue() + " contains:");
            
            // search through all registered tasks
        	String str = "Tasks: ";
            for (int j=0; j<nodeinfo[i].tasks.length; j++ ) {
                str += nodeinfo[i].tasks[j];
                if (j != nodeinfo[i].tasks.length - 1) str += ", ";
            }
        	out.println(str);
        } // foreach node
        
    }
    
    public static void main( String[] args ) {
        
        ServiceDirectory sd = new ServiceDirectory( null );
        
        NodeInfo ni[] = sd.queryTask( 1 );
        
        for ( int i=0; i<ni.length; i++ ) {
        	System.out.println("Task 1 found on node "+ni[i].address);
        }
    }

    private NodeInfo[] transformNodeInfoToArray(HashMap ndInfo) { //ChangeMR: (Map ndInfo)

        return (NodeInfo[])ndInfo.values().toArray( new NodeInfo[m_NodeInfo.size()]);       

/*        NodeInfo[] nodeinfo = new NodeInfo[ndInfo.size()];
        
        int a=0;
        ndInfo.keySet().iterator();
        for(Iterator i = ndInfo.keysIterator(); i.hasNext();) {
        	nodeinfo[a] = (NodeInfo) ndInfo.get(i.next());
        	a++;
        }
        
    	return nodeinfo;
 */
    }
}
