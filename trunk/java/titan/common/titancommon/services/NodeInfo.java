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

package titancommon.services;

import titancommon.compiler.NodeMetrics;

/**
 * Stores information about sensor nodes in the network
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */
public class NodeInfo {
    public int   address; ///< the address of the node
    public int   hops;    ///< distance away 
    public int[] path;    ///< path to the node
    public int[] tasks;   ///< tasks available on the node
    public InfoValue value; ///< the value of this information or how much it is up-to-date
    public boolean  bForeign;
    public int nodeType;
    
    public NodeInfo(int iAddress, int iNodeType, int[] iTasks, int curInquirySequencNumber) {
        address=iAddress;
        tasks=iTasks;
        hops=1;
        value = new InfoValue(curInquirySequencNumber);
        nodeType = iNodeType;
        bForeign = false;
    }
    
    /**
     * Returns the processing capabilities of the node, which are used to compute 
     * whether it can run all the required tasks.
     * 
     * @return NodeMetrics containing the characteristics of the node
     */
    public NodeMetrics getNodeMetrics() {
        
        // this data is given for a Tmote Sky sensor node
        NodeMetrics nm = new NodeMetrics();
        
        switch(nodeType) {
        case 0: // powerful
            nm.cyclesPerSecond = 10*5246913.58;
            nm.dynMemory = 10*4096; // bytes
            nm.transferTime = 195e-6; // seconds
            nm.transferForeignClusterPenalty = 1.0;
            nm.powerProcessing    =  10;  // mW
            nm.powerTransmit      = 26;  // mW
            nm.cyclesTransmit     = 10000; 
            nm.timeTransmit       =  5e-3; // seconds
            nm.powerAccelerometer =  5;  // mW
            nm.timeAccelerometer  =  10e-6; // seconds
            nm.maxTasks           = 100; 
            nm.maxConnections     = 200;
            nm.nodeType = 0;
        case 1: // mote type
            nm.cyclesPerSecond = 5246913.58;
            nm.dynMemory = 4096; // bytes
            nm.transferTime = 195e-6; // seconds
            nm.transferForeignClusterPenalty = 1.0;
            nm.powerProcessing    =  1;  // mW
            nm.powerTransmit      = 26;  // mW
            nm.cyclesTransmit     = 10000; 
            nm.timeTransmit       =  5e-3; // seconds
            nm.powerAccelerometer =  5;  // mW
            nm.timeAccelerometer  =  10e-6; // seconds
            nm.maxTasks           =  8; 
            nm.maxConnections     = 16;
            nm.nodeType = 1;
            nm.cyclesTransmit     = 1026;
        case 2: // low processing
            nm.cyclesPerSecond = 5246913.58/4;
            nm.dynMemory = 1024; // bytes
            nm.transferTime = 195e-6; // seconds
            nm.transferForeignClusterPenalty = 1.0;
            nm.powerProcessing    =  0.5;  // mW
            nm.powerTransmit      = 26;  // mW
            nm.cyclesTransmit     = 10000; 
            nm.timeTransmit       =  5e-3; // seconds
            nm.powerAccelerometer =  5;  // mW
            nm.timeAccelerometer  =  10e-6; // seconds
            nm.maxTasks           =  3; 
            nm.maxConnections     =  8;
        default:
            nm.nodeType = 2;
            nm.cyclesPerSecond = 5246913.58;
            nm.dynMemory = 4096; // bytes
            nm.transferTime = 195e-6; // seconds
            nm.transferForeignClusterPenalty = 1.0;
            nm.powerProcessing    =  1;  // mW
            nm.powerTransmit      = 26;  // mW
            nm.timeTransmit       =  5e-3; // seconds
            nm.powerAccelerometer =  5;  // mW
            nm.timeAccelerometer  =  10e-6; // seconds
            nm.maxTasks           = 10; 
            nm.maxConnections     = 20;
            nm.cyclesTransmit     = 1026;
            nm.nodeType = -1;
        }

        return nm;
    }

}
