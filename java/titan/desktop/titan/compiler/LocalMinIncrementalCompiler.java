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

package titan.compiler;

import java.io.FileNotFoundException;
import java.util.Vector;

import titan.ConfigReader;
import titancommon.execution.TaskNetwork;
import titancommon.compiler.Compiler;
import titancommon.compiler.GreedyCompiler;
import titancommon.compiler.TaskNetworkConverter;
import titancommon.compiler.*;
import titancommon.services.ServiceDirectory;

/**
 * This compiler takes a task network that is already distributed and finds 
 * a local minimum of transmission / processing power consumption by moving 
 * tasks between nodes until no more improvements can be made to the task 
 * graph distribution.
 * 
 * LocalMinIncrementalCompiler uses a NetworkEvaluator instance to estimate 
 * whether or not tasks can be moved from one node to another. 
 * 
 * This compiler also takes care of lost sensor nodes.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */

public class LocalMinIncrementalCompiler implements Compiler {

    ServiceDirectory m_ServiceDirectory;
    
    public LocalMinIncrementalCompiler(ServiceDirectory sd) {
        m_ServiceDirectory = sd;
    }
    
    /**
     * LocalMinIncrementalCompiler is only able to modify an already existing 
     * and working TaskNetwork, which it will modify. If calling this function 
     * it will instantiate a GreedyCompiler to get an initial placement.
     * 
     * @param templateNetwork The task network to distribute on the network.  
     *                        All tasks are assigned to a single virtual node.
     * @return An optimized TaskNetwork to be sent to the network.
     */
    public TaskNetwork compileNetwork(TaskNetwork templateNetwork) throws Exception {

        // get an initial configuration by another compiler
        Compiler comp = new GreedyCompiler(m_ServiceDirectory);
        TaskNetwork tn = comp.compileNetwork(templateNetwork);
        
        return localMinSearchRandom(templateNetwork,tn.getAllocVector());
    }

    public TaskNetwork updateNetwork(TaskNetwork templateNetwork, TaskNetwork currentNetwork) throws Exception {

        // get current distribution
        int [] allocVector = currentNetwork.getAllocVector();
        if (allocVector == null ) throw new Exception("allocVector not set");
        
        // check whether all nodes still exist
        int iTaskMissingNodes = 0;
        for ( int i=0; i < allocVector.length; i++ ) {
            if ( m_ServiceDirectory.nodeExists(allocVector[i]) == false ) {
                iTaskMissingNodes++;
                allocVector[i] = -1;
            }
        }
        
        // handle missing nodes
        if ( iTaskMissingNodes > 0 ) throw new Exception("Replacing missing nodes not yet implemented");
        
        return null;
    }

    /**
     * Shifts random tasks around to find the local minimum
     * @param tn The current task network. Must contain an allocVector
     * @return The optimized task network.
     * @throws Exception 
     */
    private TaskNetwork localMinSearchRandom(TaskNetwork templateNetwork, int [] allocVector) throws Exception {
        if (allocVector == null ) throw new Exception("Allocation vector missing");
        
        TaskNetworkConverter tnc = new TaskNetworkConverter(m_ServiceDirectory);
        NetworkEvaluator     ne = new NetworkEvaluator(m_ServiceDirectory);

        // tasks get only shifted between participatin nodes. Get participating nodes
        Vector<Integer> partNodes = new Vector<Integer>();
        for (int i=0; i < allocVector.length; i++ ) {
            if ( ! partNodes.contains(new Integer(allocVector[i])) ) {
                partNodes.add(new Integer(allocVector[i]));
            }
        }
        
        double   bestValue = Double.MAX_VALUE;
        TaskNetwork bestTN = tnc.VectorToTaskNetwork(templateNetwork, allocVector);
        int []      bestAV = allocVector.clone();
        for (int i=0; i < 100; i++ ) {
            
            // create new distribution
            int taskIndex = (int)Math.floor(Math.random()*allocVector.length);
            int nodeIndex = partNodes.get((int)Math.floor(Math.random()*partNodes.size()));
            int [] newAlloc = bestAV.clone();
            newAlloc[taskIndex] = nodeIndex;

            // evaluate distribution
            System.out.print("Evaluating ("+taskIndex+","+nodeIndex+") [");
            for(int j=0;j<allocVector.length;j++) System.out.print(" "+allocVector[j]+" ");
            System.out.println("]");
            TaskNetwork tn = tnc.VectorToTaskNetwork(templateNetwork, allocVector);
            double value = ne.evaluateNet(tn); 
            
            if ( value < bestValue ) {
                bestValue = value; 
                bestTN = tn;
                bestAV = newAlloc;
                
                System.out.println("New value: " + value );
            }
        }
        
        
        return bestTN;
    }

    
    /**
     * 
     * This is just for testing and does not contain any functionality needed 
     * in actual program.
     * 
     * @param args
     */
    public static void main(String[] args) {
        
        // read the test configuration file
        ConfigReader cfgFile;
        try {
            cfgFile = new ConfigReader("Titan/test_configreader.txt");
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Could not open configuration file.");
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
        
        // compile the network
        try {
            Compiler cmp = new LocalMinIncrementalCompiler(sd);
            TaskNetwork tno = cmp.compileNetwork(tn);
            if ( tno == null ) {
                System.err.println("ERROR: Compilation failed!");
                return;
            }
            System.out.println("Compiled network onto "+tno.m_Nodes.size()+" motes:\n");
            
            // print configuration
            tno.printTaskNetwork();
            
            // test sending
            //tno.configureNetwork(null);

        } catch( Exception e ) {
            System.err.println("ERROR: Compilation failed: reason\""+e.getMessage()+"\"");
        }
        
        
    }

}
