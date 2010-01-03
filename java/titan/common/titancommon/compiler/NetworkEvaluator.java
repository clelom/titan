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
import titancommon.execution.TaskNetwork;
import titancommon.execution.TaskNetwork.NodeConfiguration;
import titancommon.services.NodeInfo;
import titancommon.services.ServiceDirectory;
import titancommon.tasks.Communicator;
import titancommon.tasks.Task;
import titancommon.tasks.Communicator.CommConfig;

/**
 * Evaluates a compiled task network and computes an estimation of the energy 
 * consumed by single nodes and the whole network when it is executing.
 * 
 * TODO: A network model would allow to compute energy on forwarding nodes
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 *
 */
public class NetworkEvaluator {

    protected ServiceDirectory m_ServiceDirectory;
    
    /** Multiplicator for the processing cost estimation, assumes 66% underestimation */
    private final double PROCESSING_MULTIPLICATOR = 1.5;
    
    public NetworkEvaluator(ServiceDirectory sd) {
        m_ServiceDirectory = sd;
    }
    
    public double evaluateNet( TaskNetwork tn ) {
        return evaluateNet(tn,true);
    }
    
    /**
     * Evaluates the cost of implementing this task network on the 
     * network described in the service directory given with the initialization.
     * 
     * @param tn
     * @param bPropagateData Propagates the data from task to task to estimate the data actually produced. Can be set to false to reduce evaluation time.
     * @return
     */
    public double evaluateNet( TaskNetwork tn, boolean bPropagateData ) {
        
        if (tn==null) return Double.MAX_VALUE;
        
        // first propagate the datarates through the network
        if (bPropagateData) {
            propagateDatarates(tn);
        }
        
        // now compute the cost of the network
        double costTotal = 0;
        double [] costProcessing    = new double[tn.m_Nodes.size()];
        double [] costCommunication = new double[tn.m_Nodes.size()];
        double [] costSensors       = new double[tn.m_Nodes.size()];
        
        // iterate through the nodes
        for (int i=0; i < tn.m_Nodes.size(); i++ ) {
            NodeConfiguration nc = (NodeConfiguration)tn.m_Nodes.get(i);
            costProcessing[i]=0;
            costCommunication[i]=0;
            
            // check whether there are too many tasks allocated
            // this stems from implementation limits
            NodeMetrics nm = nc.getNodeMetrics();
            if ( nc.tasks.length > nm.maxTasks || nc.connections.length > nm.maxConnections ) {
                costTotal = Double.MAX_VALUE;
                break;
            }

            // iterate through the tasks
            for(int j=0; j < nc.tasks.length; j++ ) {
                
                // communication tasks store the interconnections between nodes
                if ( nc.tasks[j] instanceof Communicator ) {
                    Communicator com = (Communicator)nc.tasks[j];
                    TaskMetrics tm = com.getMetrics(nc.getNodeMetrics());

                    if (tm.datapackets==null) {
                        if(!(com instanceof Communicator)) {
                        	System.out.println( "WARNING: NetworkEvaluator: Task ("+com.getName()+") input data not set!");
                        }
                        break; // communication settings not set yet -> only one instance per node -> wait until next round
                    }

                    for(int k=0; k < com.m_Connections.size(); k++ ) {
                       
                       costCommunication[i] += evaluateConnection( nc.address, ((CommConfig)com.m_Connections.get(k)).destAddr, 
                                                                tm.datapackets[k], tm.packetsizes[k], nc.getNodeMetrics() );
                       costProcessing[i] += tm.datapackets[k]*nm.cyclesTransmit; 

                   }
                } else {
                    // get the task metrics
                    TaskMetrics tm = nc.tasks[j].getMetrics(nm);
                    
                    // estimate computation cost on this node
                    costProcessing[i] += evaluateTask( tm, nm );
                    
                    // add cost of internal communication
                    if (tm.datapackets != null) {
                        for ( int k=0; k< tm.datapackets.length; k++ ) {
                            costProcessing[i] += tm.datapackets[k] * nm.transferTime*nm.cyclesPerSecond;
                        }
                    } // does produce packets
                    
                    costSensors[i] += tm.sensorEnergy;
                } // not a Communicator
            } // foreach task on the node
            
            if (costProcessing[i]*PROCESSING_MULTIPLICATOR >= nm.cyclesPerSecond ){
            	costProcessing[i] = Double.MAX_VALUE;
            }
            
            //System.out.format("Node %1$2d: Proc=%2$f Com=%3$03.3f Sens=%4$3.3f\n", tn.m_Nodes.get(i).address,costProcessing[i],costCommunication[i], costSensors[i] );
            costTotal += costProcessing[i]/nm.cyclesPerSecond*nm.powerProcessing; // convert to energy
            costTotal += costCommunication[i];
            
            // don't run unneccessarily
            if ( costTotal >= Double.MAX_VALUE ) break;
            
        } // foreach node
        
        
        return costTotal;
    }

    /**
     * Propagates all datarates from sensor tasks producing the data all the way down to 
     * the sinks. Tasks compute their output datarates after having received them on their 
     * inputs.
     * 
     * @param tn The task network for which to calculate the propagated datarates
     */
    private void propagateDatarates(TaskNetwork tn) {
        // we have to propagate all datarate values through the network before being
        // able to read out the datarate information needed for the communication costs
        boolean bChanged;
        do {
            bChanged = false;
            
            // iterate through the nodes
            for (int i=0; i < tn.m_Nodes.size(); i++ ) {
                NodeConfiguration nc = (NodeConfiguration)tn.m_Nodes.get(i);
                
                // iterate through the connections
                for(int j=0; j < nc.connections.length; j++ ) {
                    
                    Task tstart = nc.tasks[nc.connections[j].StartTask];
                    Task tend   = nc.tasks[nc.connections[j].EndTask];
                    
                    TaskMetrics tm = tstart.getMetrics(null);
                    
                    // check whether there is an indication of how many messages are generated
                    // communicator tasks only provide data on what comes out on the wireless link
                    if ( (tm.datapackets == null) || (tm.packetsizes == null) || (tstart instanceof Communicator) ) continue;
                    
                    // make sure we can work with something
                    if (tm == null) continue;
                    
                    // check whether we're going to a different node
                    if ( tend instanceof titancommon.tasks.Communicator ) {
                        
                        // get communicator information
                        Communicator com = (Communicator)tend;
                        CommConfig comcfg = (CommConfig)com.m_Connections.get(nc.connections[j].EndPort);
                        
                        // find communicator on other node
                        NodeConfiguration ncRemote = tn.getNode( comcfg.destAddr );
                        int iRemoteComTaskNumber = -1;
                        for(int k=0; k<ncRemote.tasks.length; k++ ) {
                            if ( ncRemote.tasks[k].getID() == Communicator.TASKID ) {
                                iRemoteComTaskNumber=k;
                                break;
                            }
                        }
                        
                        if ( iRemoteComTaskNumber == -1 ) System.out.println( "WARNING: NetworkEvaluator: Could not find Communicator on node "+comcfg.destAddr);

                        // find connection on remote that goes from the communicator 
                        // to the task we are looking for
                        for(int k=0; k<ncRemote.connections.length; k++ ) {
                            if ((ncRemote.connections[k].StartTask == iRemoteComTaskNumber) &&
                                (ncRemote.connections[k].StartPort == comcfg.destPort ) ) {
                                
                                // found! now add the connection datarate information
                                Task tRemoteEnd = ncRemote.tasks[ncRemote.connections[k].EndTask];
                                if ( tRemoteEnd.setInputPortDatarate(ncRemote.connections[k].EndPort, 
                                                                     tm.datapackets[nc.connections[j].StartPort],
                                                                     tm.packetsizes[nc.connections[j].StartPort]) ){
                                    bChanged = true;
                                    break;
                                }
                            } // if port and task match, add info
                        } // find remote internode connection
                        
                    }

                    // in any case: register the transfer data
                    if ( tend.setInputPortDatarate(nc.connections[j].EndPort, tm.datapackets[nc.connections[j].StartPort], tm.packetsizes[nc.connections[j].StartPort] ) ) {
                        bChanged = true;
                    }
                    
                } // foreach connection on the node
                
            } // foreach node
            
//            System.out.println("Communication data forwarding iteration complete");
        } while (bChanged);
    }
    
    /**
     * Computes the energy cost of sending a packet over the wireless link from 
     * one node to another (no multihop followed)
     * @param srcAddr  Data packet source address
     * @param destAddr Data packet destination address
     * @param datarate Rate at which data is sent over this link in bytes per second
     * @return energy cost value for sending all the data
     */
    protected double evaluateConnection( int srcAddr, int destAddr, float packets, int size, NodeMetrics nm  ) {
        
        // this here only counts the number of packets that need to be transmitted and 
        // adds an offset
        double dCost = packets*nm.powerTransmit*nm.timeTransmit; 
        
        NodeInfo srcNI  = m_ServiceDirectory.getNodeInfo(srcAddr);
        NodeInfo destNI = m_ServiceDirectory.getNodeInfo(destAddr);
        
        if (srcNI.bForeign == true && destNI.bForeign == false) {
            dCost +=  srcNI.getNodeMetrics().transferForeignClusterPenalty;
        }
        
        return dCost;
    }
    
    /**
     * Evaluate the cost of running this task. The cost is indicated in cycles needed 
     * to complete the task.
     * 
     * @param tm The task metrics associated with the task
     * @return The number of cycles per second needed to execute the tasks
     */
    protected double evaluateTask( TaskMetrics tm, NodeMetrics nm ) {
        return tm.instantiations*tm.procCycles;
    }
    
    /** Evaluates whether the additional task will be executable on a node.
     * @param tm     Task metrics of the task to be added
     * @param tasks  List of task metrics already issued to that node
     * @param nm     Node metric identifying the node
     * @return       True if sufficient resources exists, else false
     */
    public static boolean isExecutable( TaskMetrics newTM, ArrayList tasks, NodeMetrics nm ) {

        double totalCycles = 0; // cycles needed per second
        double totalMemory = 0; // memory needed per second
        double totalPackets = 0;
        
        // summarize task information
        for ( int tmi = 0; tmi < tasks.size(); tmi++ ) {
            TaskMetrics tm = (TaskMetrics)tasks.get(tmi);
            totalCycles += tm.instantiations*tm.procCycles;
            totalMemory += tm.dynMemory;
            if (tm.datapackets != null) {
                for (int i=0; i<tm.datapackets.length; i++) {
                    totalPackets += tm.datapackets[i];
                }
            }
        }
        
        // add up with additional task
        totalCycles += newTM.instantiations*newTM.procCycles;
        totalMemory += newTM.dynMemory;
        if (newTM.datapackets != null) {
            for (int i=0; i<newTM.datapackets.length; i++) {
                totalPackets += newTM.datapackets[i];
            }
        }
        
        // add up total processing cost
        totalCycles += totalPackets*nm.cyclesTransmit;
        
        totalCycles *= 1.5;
        
        // return true if executable
        //return (totalCycles < nm.cyclesPerSecond) && 
        //       (totalMemory < nm.dynMemory); 
    
        
        //old version
        return tasks.size() <= 8;
    }
}
