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
 * TaskMetrics gives indications about the resources a task consumes. For this purpose, 
 * it indicates needs on processing cycles, and the data rate it produces as output. This 
 * data is used for computing the energy needed on the processor, as well as the energy 
 * consumed by a wireless link (if the output data is actually sent over the wireless 
 * channel).
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class TaskMetrics {

    //////////////////////////////////////////////////////////////////////////
    // Processing

    public TaskMetrics() {}
    
    // copy constructor
    public TaskMetrics(TaskMetrics tm) {
        
        procCycles = tm.procCycles;
        period = tm.period;
        instantiations = tm.instantiations;
        dynMemory = tm.dynMemory;
        sensorEnergy = tm.sensorEnergy;
        if (tm.datapackets != null) {
            datapackets = new float[tm.datapackets.length];
            for(int i=0;i<datapackets.length;i++) {
                datapackets[i] = tm.datapackets[i];
            }
            
        }
        if (tm.packetsizes != null) {
            packetsizes = new int[tm.packetsizes.length];
            for(int i=0;i<packetsizes.length;i++) {
                packetsizes[i] = tm.packetsizes[i];
            }
            
        }
        
    }

    /** number of cycles per task execution */
    public int procCycles;
    
    /** For periodic tasks the average delay between tasks is indicated in miliseconds. */
    public int period;
    
    /** Average number of instantiations per second */
    public double instantiations;
    
    /** bytes of dynamic memory needed for instantiation */
    public int dynMemory;

    //////////////////////////////////////////////////////////////////////////
    // Sensor energy

    public double sensorEnergy;
    
    //////////////////////////////////////////////////////////////////////////
    // Communication
    
    /** total number of packets per second produced at the output ports */
    public float [] datapackets;
    
    /** Average size of packets */
    public int [] packetsizes;
    
}
