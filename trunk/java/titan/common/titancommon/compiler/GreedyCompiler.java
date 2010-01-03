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

import java.util.ArrayList;
import java.util.HashMap;
import titancommon.services.NodeInfo;
import titancommon.services.ServiceDirectory;
import titancommon.execution.TaskNetwork;
import titancommon.execution.TaskNetwork.NodeConfiguration;
import titancommon.tasks.Communicator;
import titancommon.tasks.Task;


/**
 * 
 * The Compiler class takes the ServiceDirectory and a task network and computes 
 * the optimal placement of nodes in the network.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 *
 */


public class GreedyCompiler implements Compiler {
    
    private ServiceDirectory m_ServiceDirectory;

    private class NetTaskToNodeTask {
        public int nodeAddress;
        public int taskNumber;
    };
    
    public GreedyCompiler( ServiceDirectory sd) {
        m_ServiceDirectory = sd;
    }
    
    /** Determines which node to select for the next task. The selection is based on the number of tasks on the current node,
     *  specified requirements plus whether the node actually contains that task.
     */
    
    private int getNextNode(TaskNetwork.NodeConfiguration tnet, NodeInfo[] nis, int curNodeAddr, HashMap tasksPerNode, int iCurTask) throws Exception
    {
        if (tnet.tasks[iCurTask].getAttribute("nodeID") != null)
        {
            // get the required nodeID
            int iNodeAddress = Integer.parseInt(tnet.tasks[iCurTask].getAttribute("nodeID"));
            
            // check whether the service directory knows the node and the task
            boolean bContainsTask = false;
            for (int i = 0; i < nis.length; i++)
            {
                if (nis[i].address == iNodeAddress) {
                    bContainsTask = true;
                    break;
                }
            }
            if (bContainsTask == false) throw new Exception("Could not place task #" + iCurTask + ": Node "+iNodeAddress+" not found or does not contain task");
            
            // seems to be all ok
            return iNodeAddress;
            
        }
        else
        { // no placement restrictions
            // check whether we need to select a new node
            // first, check whether curNodeAddr contains the task to be placed
            int curNodeIndex = -1; // index of the task in the node
            for (int j = 0; j < nis.length; j++)
            {
                if (nis[j].address == curNodeAddr)
                {
                    curNodeIndex = j;
                    break;
                }
            }
            // Select a new node, if curNodeAddr cannot be used
            if ((curNodeAddr == -1) ||
                    ( ! NetworkEvaluator.isExecutable(
                            tnet.tasks[iCurTask].getMetrics(m_ServiceDirectory.getNodeInfo(curNodeAddr).getNodeMetrics()),
                            (ArrayList)tasksPerNode.get(new Integer(curNodeAddr)),
                            m_ServiceDirectory.getNodeInfo(curNodeAddr).getNodeMetrics())) ||
                    (curNodeIndex == -1))
            {
                
                // curNode cannot take the new task -> select another node
                curNodeAddr = -1;
                NodeInfo foreignPreferred = null;
                for (int j = 0; j < nis.length; j++)
                {
                    
                    if (tasksPerNode.containsKey(new Integer(nis[j].address)))
                    {
                        if ( NetworkEvaluator.isExecutable(
                                tnet.tasks[iCurTask].getMetrics(m_ServiceDirectory.getNodeInfo(nis[j].address).getNodeMetrics()),
                                (ArrayList)tasksPerNode.get(new Integer(nis[j].address)),
                                m_ServiceDirectory.getNodeInfo(nis[j].address).getNodeMetrics()))
                        {
                            curNodeAddr = nis[j].address;
                            break;
                        }
                    }
                    else
                    {
                        if (nis[j].bForeign == true ) {
                            foreignPreferred = nis[j];
                        } else {
                            tasksPerNode.put(new Integer(nis[j].address), new ArrayList());
                            curNodeAddr = nis[j].address;
                        }
                    }
                } // foreach j in nis.length
                if ( curNodeAddr == -1 && foreignPreferred != null ) {
                	System.out.println("WARNING: Selecting out-of-cluster node (selection size="+nis.length+")!");
                    tasksPerNode.put(new Integer(foreignPreferred.address), new ArrayList());
                    curNodeAddr = foreignPreferred.address;
                }
                
                
            } // no placement restriction
            
            if (curNodeAddr == -1) {
                throw new Exception("ERROR: Could not place task " + iCurTask + ": no node has empty room.");
            }
            
            return curNodeAddr;
        }
    }
    
    public TaskNetwork compileNetwork( TaskNetwork taskNetwork ) throws Exception {
        
        // up to now, we only understand an initial task network
        if ( ((TaskNetwork.NodeConfiguration)(taskNetwork.m_Nodes.get(0))).address != -1) {
            throw new Exception("First node has not address -1: don't know what to do");
        }
        
        // get an initial cost estimation
//        NetworkEvaluator ne = new NetworkEvaluator(m_ServiceDirectory);
//        ne.evaluateNet(taskNetwork);
        
        // Get the task network to distribute
        TaskNetwork.NodeConfiguration tnet = (TaskNetwork.NodeConfiguration)(taskNetwork.m_Nodes.get(0));
        
        // evaluate which tasks are located where and store information in 
        // taskToNodes
        HashMap tasksPerNode = new HashMap();
        int curNodeAddr = -1; // address of the node currently to be filled
        NetTaskToNodeTask[] nttnt = new NetTaskToNodeTask[tnet.tasks.length]; // shows which task goes on which node
        int [] allocVector = new int[tnet.tasks.length];
		
        for(int i=0; i<tnet.tasks.length; i++ ) {
            allocVector[i] = -1;
            
            // search the service directory for the task
            NodeInfo[] nis = m_ServiceDirectory.queryTask( tnet.tasks[i].getID()  );
            
            // task not found?
            if ( nis == null ) {
                throw new Exception("No node has task "+tnet.tasks[i].getID() + "("+ tnet.tasks[i].getName() +"). Cannot compile task network");
            }
            
            // check whether there is a special location to put the node
            curNodeAddr = getNextNode(tnet,nis, curNodeAddr, tasksPerNode, i);
            
            // the new task will be placed on curNodeAddr
            nttnt[i] = new NetTaskToNodeTask();
            nttnt[i].nodeAddress = curNodeAddr;
            allocVector[i] = curNodeAddr;
            if (tasksPerNode.containsKey(new Integer(curNodeAddr)) == false) {
                tasksPerNode.put(new Integer(curNodeAddr), new ArrayList());
            }
            ArrayList taskList = (ArrayList)tasksPerNode.get(new Integer(curNodeAddr));
            nttnt[i].taskNumber = taskList.size();
            taskList.add(tnet.tasks[i].getMetrics(m_ServiceDirectory.getNodeInfo(curNodeAddr).getNodeMetrics()));
        }
        
        // place the tasks on the nodes
        TaskNetwork tno = new TaskNetwork();
        
        tno.m_bDebugOutput = taskNetwork.m_bDebugOutput;
        for(int i=0; i<tnet.tasks.length; i++) {
            
            // check whether there is any task on the node at all
            if (tasksPerNode.containsKey(new Integer(nttnt[i].nodeAddress)) == false) continue;
    		
            if (((ArrayList)tasksPerNode.get(new Integer(nttnt[i].nodeAddress))).size() == 0) continue;
            
            NodeConfiguration nc = tno.getNode( nttnt[i].nodeAddress );
            
            // not instantiated yet?
            if ( nc == null ) {
                
                nc = tno.addNode(m_ServiceDirectory.getNodeInfo(nttnt[i].nodeAddress));
                nc.tasks   = new Task[((ArrayList)tasksPerNode.get(new Integer(nttnt[i].nodeAddress))).size()];
            }
            
            // copy the task to the node configuration
            nc.tasks[nttnt[i].taskNumber] = (Task) tnet.tasks[i].clone();                       
        }

        // check whether broadcast channels are used
        int maxPort = -1;
        for (int i=0;i<tnet.connections.length;i++) {
          if (tnet.connections[i].StartTask == 255) {
        	  if (maxPort < tnet.connections[i].StartPort) {
        		  maxPort = tnet.connections[i].StartPort;
        	  }
          }
          if (tnet.connections[i].EndTask == 255) {
        	  if (maxPort < tnet.connections[i].EndPort) {
        		  maxPort = tnet.connections[i].EndPort;
        	  }
          }
        }
        if (maxPort != -1 ) {
        	System.out.println("Using "  + (maxPort+1) + " broadcast channel(s)");
        	
        	// add communication task to every node
        	for (int i=0; i<tno.m_Nodes.size();i++) {
        		NodeConfiguration nc = (NodeConfiguration)tno.m_Nodes.get(i);
        		
                // check whether a communication task is already available
                int iComTask = nc.getTaskIndex(Communicator.TASKID);
                Communicator comTask = (Communicator)nc.getTask(Communicator.TASKID);
                if ( iComTask == -1 ) {
                    comTask = new Communicator();
                    iComTask = nc.addTask( comTask );
                }

                // add broadcast output ports
                while(comTask.addOutport() < maxPort);
        	}
        	
        }
        
        // Add all the communication tasks that are needed
        for (int i=0;i<tnet.connections.length;i++) {
            
        	if ( tnet.connections[i].StartTask == 255 ) { // receiving from broadcast address
        		int iEndNode   = nttnt[tnet.connections[i].EndTask].nodeAddress;
        		
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

        		
        	} else if (tnet.connections[i].EndTask == 255 ) { // going to broadcast address

        		int iStartNode = nttnt[tnet.connections[i].StartTask].nodeAddress;
                /////////// Sender side
                // first care about the start node
                NodeConfiguration nc = tno.getNode( iStartNode );
                
                // check whether a communication task is already available
                Communicator comTask = (Communicator)nc.getTask(Communicator.TASKID);
                if ( comTask == null ) {
                    comTask = new Communicator();
                    nc.addTask( comTask );
                }
                
                int comIndex = nc.getTaskIndex(Communicator.TASKID);
                
                // add remote connection
                int portIndex = comTask.addConnection( titancommon.TitanCommand.BROADCAST_ADDR, tnet.connections[i].EndPort ); // broadcast ports remain the same
                
                // add internal connection
                nc.addConnection( nttnt[tnet.connections[i].StartTask].taskNumber,
                        tnet.connections[i].StartPort,
                        comIndex, portIndex );
        	} else {
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
         }
            
        } // foreach connection
        
        tno.setAllocVector(allocVector);
        
        return tno;
    }

    /**
     * The update is solved easily be recomputing a new distribution of the 
     * task network.
     */
    public TaskNetwork updateNetwork(TaskNetwork templateNetwork, TaskNetwork currentNetwork) throws Exception {
        return compileNetwork(templateNetwork);
    }
    
 
}
