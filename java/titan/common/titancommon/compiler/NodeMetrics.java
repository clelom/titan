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

/**
 *
 * Default class for metrics defining a node's capabilities. Used to compute 
 * the cost for task execution on the node.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

public class NodeMetrics {

    /** number of cycles performed per second */
    public double cyclesPerSecond;
    
    /** number of bytes of dynamic memory */
    public int dynMemory;
    
    /** Seconds to transmit a packet from one task to another */
    public double transferTime;

    /** Additional cost of not being in the same cluster */
    public double transferForeignClusterPenalty;

    /** Power in mW needed just for processing (microcontroller only) */
    public double powerProcessing;
    
    /** Power in mW needed for transmission */
    public double powerTransmit;
    
    /** Time in seconds needed to transmit a packet */
    public double timeTransmit;
    
    /** Power in mW for accelerometer */
    public double powerAccelerometer;
    
    /** Time in seconds needed for wakeup and sampling of the accelerometer */
    public double timeAccelerometer;
    
    /** Maximum number of tasks a node can host */
    public int maxTasks;
    
    /** Maximum number of connections a node can host */
    public int maxConnections;
    
    /** Node type identifier */
    public int nodeType;

    /** Cycles needed to transfer a packet from one task to another */
    public int cyclesTransmit;
}
