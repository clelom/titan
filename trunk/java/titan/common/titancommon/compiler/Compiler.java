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

import titancommon.execution.TaskNetwork;

/**
 * The Compiler interface can be used to assign different types of 
 * task network compilers for the NetworkManager. The interface defines 
 * two functions: compileNetwork() for a first compilation with only 
 * compiler specific constraints, and a second function updateNetwork(),
 * which is called on the loss of a participating node, which would need 
 * an adaptation of the current configuration. 
 * 
 * The compilers need to have a reference to the ServiceDirectory containing 
 * the nodes currently available in the sensor network. This will be needed 
 * to actually assign tasks to specific sensor network nodes.
 * 
 * An additional task of the compiler is to also add and configure 
 * Communicator tasks, which deliver packets between tasks on different 
 * sensor nodes.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public interface Compiler {

    /**
     * Generates a mapping between the given template network, where the 
     * complete application task graph is allocated in one node, and the 
     * actually existing network.
     * 
     * @param templateNetwork The application task graph on one node
     * @return A TaskNetwork containing the tasks and their configurations 
     *         to sensor nodes as well as connection tasks configured to 
     *         send data from one task to the other.
     */
    public TaskNetwork compileNetwork(TaskNetwork templateNetwork) throws Exception;
    
    /**
     * This function is called when a change in the network occurs, which 
     * makes a recompilation of the task graph necessary. This may i.e. be  
     * the addition or loss of a sensor node.
     * @param templateNetwork The original application task graph on one node
     * @param currentNetwork  The current configuration of the network
     * @return A new TaskNetwork containing a new mapping of tasks to nodes 
     *         and newly configured interconnections between nodes.
     * @throws Exception 
     */
    public TaskNetwork updateNetwork(TaskNetwork templateNetwork, TaskNetwork currentNetwork) throws Exception;

}
