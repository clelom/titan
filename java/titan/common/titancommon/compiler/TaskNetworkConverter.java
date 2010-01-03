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

package titancommon.compiler;

import java.util.HashMap;
import titan.ConfigReader;
import titancommon.execution.TaskNetwork;
import titancommon.execution.TaskNetwork.NodeConfiguration;
import titancommon.services.ServiceDirectory;
import titancommon.tasks.Communicator;
import titancommon.tasks.Task;

/**
 * The TaskNetworkConverter converts different representations of task graphs 
 * between each other.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 *
 */
public class TaskNetworkConverter {

    private class NetTaskToNodeTask {
        public int nodeAddress;
        public int taskNumber;
    };
    
    ServiceDirectory m_ServiceDirectory;
    
    public TaskNetworkConverter(ServiceDirectory sd) {
        m_ServiceDirectory = sd;
    }

    /**
     * Converts the task network on a single node (addr=-1) onto a number of nodes, takes 
     * as input an assignment vector telling which task goes to which node. 
     * @param original A task network on a single node, must be on node -1
     * @param assignments A vector indicating where the tasks should be moved
     * @return A task network putting each task at its assigned position. Returns null, if 
     *         the configuration is not feasible!
     */
    public TaskNetwork VectorToTaskNetwork( TaskNetwork tnOriginal, int[] assignments ) throws Exception {

        if (m_ServiceDirectory == null ) return null;
        
        // up to now, we only understand an initial task network
        if ( ((TaskNetwork.NodeConfiguration)(tnOriginal.m_Nodes.get(0))).address != -1) throw new Exception("VectorToTaskNetwork: First node has not address -1: don't know what to do");
        
        // Get the task network to distribute
        TaskNetwork.NodeConfiguration tnet = (TaskNetwork.NodeConfiguration)(tnOriginal.m_Nodes.get(0));
        
        if (tnet.tasks.length != assignments.length ) throw new Exception("VectorToTaskNetwork: Assignment number is not equal to task number");
        
        // evaluate which tasks are located where and store information in 
        // taskToNodes
        HashMap tasksPerNode = new HashMap();
        int curNodeAddr = -1; // address of the node currently to be filled
        NetTaskToNodeTask[] nttnt = new NetTaskToNodeTask[tnet.tasks.length]; // shows which task goes on which node
        for(int i=0; i<tnet.tasks.length; i++ ) {
            
            // get the assigned node address
            curNodeAddr = assignments[i];
            
            // check whether the task has special placement restrictions
            if (tnet.tasks[i].getAttribute("nodeID") != null)
            {
                int iForcedNode = Integer.parseInt(tnet.tasks[i].getAttribute("nodeID"));
                if ( curNodeAddr != iForcedNode) {
                    return null;
                }
            }

            // check whether the task is actually available on the node
            if ( m_ServiceDirectory.hasTask(curNodeAddr,tnet.tasks[i].getID()) == false ) {
                return null;
            }
            
            // the new task will be placed on curNodeAddr
            nttnt[i] = new NetTaskToNodeTask();
            nttnt[i].nodeAddress = curNodeAddr;
            if (tasksPerNode.containsKey(new Integer(curNodeAddr)) == false) {
                tasksPerNode.put(new Integer(curNodeAddr), new Integer(0));
            }
            Integer curTaskNumber = ((Integer)tasksPerNode.get(new Integer(curNodeAddr)));
            nttnt[i].taskNumber = curTaskNumber.intValue();
            tasksPerNode.put(new Integer(curNodeAddr), new Integer(curTaskNumber.intValue()+1));
        }
        
        // place the tasks on the nodes
        TaskNetwork tno = new TaskNetwork();
        for(int i=0; i<tnet.tasks.length; i++) {
            
            // check whether there is any task on the node at all
            if (tasksPerNode.containsKey(new Integer(nttnt[i].nodeAddress)) == false) continue;
            if (((Integer)tasksPerNode.get(new Integer(nttnt[i].nodeAddress))).intValue() == 0) continue;
            
            NodeConfiguration nc = tno.getNode( nttnt[i].nodeAddress );
            
            // not instantiated yet?
            if ( nc == null ) {
                nc = tno.addNode(m_ServiceDirectory.getNodeInfo(nttnt[i].nodeAddress));
                nc.tasks   = new Task[((Integer)tasksPerNode.get(new Integer(nttnt[i].nodeAddress))).intValue()];
            }
            
            // copy the task to the node configuration
            nc.tasks[nttnt[i].taskNumber] = (Task)tnet.tasks[i].clone();           
        }
        
        
        // Add all the communication tasks that are needed
        for (int i=0;i<tnet.connections.length;i++) {
            
            int iStartNode = nttnt[tnet.connections[i].StartTask].nodeAddress;
            int iEndNode   = nttnt[tnet.connections[i].EndTask].nodeAddress;
            
            // check whether tasks are on the same node
            if ( iStartNode == iEndNode ) {
                NodeConfiguration nc = tno.getNode( iStartNode );
                
                // append connection
                nc.addConnection( nttnt[tnet.connections[i].StartTask].taskNumber, 
                        tnet.connections[i].StartPort,
                        nttnt[tnet.connections[i].EndTask].taskNumber,
                        tnet.connections[i].EndPort );
                
                
            } else { // connection goes over two nodes -> add communication task
                
                
                /////// Receiver side
                // on the receiver side, the comTask also needs to be instantiated
                NodeConfiguration nc = tno.getNode( iEndNode );
                
                // check whether a communication task is already available
                int iComTask = nc.getTaskIndex(Communicator.TASKID);
                Communicator comTask = (Communicator)nc.getTask(Communicator.TASKID);
                if ( iComTask == -1 ) {
                    comTask = new Communicator();
                    iComTask = nc.addTask( comTask );
                }
                
                int iNewComPort = comTask.addOutport();
                
                // append connection
                nc.addConnection( iComTask, 
                        iNewComPort,
                        nttnt[tnet.connections[i].EndTask].taskNumber,
                        tnet.connections[i].EndPort );
                
                
                
                /////////// Sender side
                // first care about the start node
                nc = tno.getNode( iStartNode );
                
                // check whether a communication task is already available
                comTask = (Communicator)nc.getTask(Communicator.TASKID);
                if ( comTask == null ) {
                    comTask = new Communicator();
                    nc.addTask( comTask );
                }
                
                int comIndex = nc.getTaskIndex(Communicator.TASKID);
                
                // add remote connection
                int portIndex = comTask.addConnection( iEndNode, iNewComPort );
                
                // add internal connection
                nc.addConnection( nttnt[tnet.connections[i].StartTask].taskNumber,
                        tnet.connections[i].StartPort,
                        comIndex, portIndex );
                
                
                
            }
            
            
        } // foreach connection

        tno.setAllocVector(assignments);
        
        return tno;        
    }
    
    
    
    
    //////////////////////////////////////////////////////////////////////////
    // Testing
    /**
     * Tests the functionality of the TaskNetworkConverter
     * 
     * @param args no arguments evaluated
     */
    public static void main(String[] args) {
        // read the test configuration file
        ConfigReader cfgFile;
        try {
            cfgFile = new ConfigReader("Titan/test_configreader.txt");
        } catch (Exception e) {
        	System.out.println("ERROR: Could not open configuration file.");
            return;
        }
        
        // get the resulting task network
        TaskNetwork tn = cfgFile.getTaskNetwork();
        
        // open up the service directory
        ServiceDirectory sd = new ServiceDirectory(null);
        int[] testtasks = { 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17 };
//      sd.updateNodeInfo(0, 1, testtasks);
        sd.updateNodeInfo(1, 1, testtasks);
//      sd.updateNodeInfo(2, 1, testtasks);
        sd.updateNodeInfo(3, 1, testtasks);
        sd.updateNodeInfo(4, 1, testtasks);
        sd.updateNodeInfo(5, 1, testtasks);
        sd.updateNodeInfo(6, 1, testtasks);
        sd.updateNodeInfo(7, 1, testtasks);
        sd.updateNodeInfo(8, 1, testtasks);
        
        // set up two possible configurations
        int[] config0 = new int[6];
        config0[0] = 6;
        config0[1] = 6;
        config0[2] = 4;
        config0[3] = 4;
        config0[4] = 4;// here should an error be generated
        config0[5] = 0;
        int[] config1 = new int[6];
        config1[0] = 1;
        config1[1] = 1;
        config1[2] = 2;// here an error should be generated
        config1[3] = 2;
        config1[4] = 1;
        config1[5] = 0;
        int[] config2 = new int[6];
        config2[0] = 4;
        config2[1] = 3;
        config2[2] = 4;
        config2[3] = 4;
        config2[4] = 1;
        config2[5] = 0;
        
        // compile the network
        try{
            TaskNetworkConverter tnc = new TaskNetworkConverter(sd);
            TaskNetwork tno = tnc.VectorToTaskNetwork(tn,config0);
            if ( tno == null ) {
            	System.out.println("Failed to convert first task network: CORRECT");
            } else {
            	System.out.println("Successfully compiled first task network: FAILED");
                tno.printTaskNetwork();
            }
            tno = tnc.VectorToTaskNetwork(tn,config1);
            if ( tno == null ) {
            	System.out.println("Failed to convert second task network: CORRECT");
            } else {
            	System.out.println("Successfully compiled second task network: FAILED");
                tno.printTaskNetwork();
            }
            tno = tnc.VectorToTaskNetwork(tn,config2);
            if ( tno == null ) {
            	System.out.println("Failed to convert third task network: FAILED");
            } else {
            	System.out.println("Successfully compiled second task network: CORRECT");
                tno.printTaskNetwork();
            }
        } catch (Exception e ) {
            System.err.println("ERROR: "+e.getMessage());
        }
        
    }

}
