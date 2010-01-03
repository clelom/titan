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

package titancommon.tasks;

import titancommon.compiler.NodeMetrics;
import titancommon.compiler.TaskMetrics;

/**
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class Led extends Task {
    
    public final static String NAME   = "led";
    public final static int    TASKID = 4;
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public int getID() {
        return TASKID;
    }
    
    public Led() {}
    
    public Led(Led l) {
    	super(l);
    }
    
    public Object clone() {
        return new Led(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 0;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        return ( strConfig == null );
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        return null;
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    float m_frequency;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = 6;
        tm.datapackets = null;
        tm.packetsizes = null;
        tm.dynMemory = 0;
        tm.instantiations = m_frequency;
        tm.period = (int)(1/m_frequency);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        if ( m_frequency != pktPerSecond ) {
            m_frequency = pktPerSecond;
            return true;
        } else return false;
        
    }
}
