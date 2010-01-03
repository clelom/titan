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
 * SimpleWriter generates a data packet every 250 milliseconds on its output 
 * port. It accepts no configuration bytes.
 * 
 */
public class SimpleWriter extends Task {
    
    public final static String NAME   = "simplewriter";
    public final static int    TASKID = 1;
    
    private int m_Period = -1; //< Period to wait between two samples
    
    public SimpleWriter() {}
    
    public SimpleWriter(SimpleWriter sw) {
    	super(sw);
        m_Period = sw.m_Period;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getName()
     */
    public String getName() {
        return NAME.toLowerCase();
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getID()
     */
    public int getID() {
        return TASKID;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#copy()
     */
    public Object clone() {
        return new SimpleWriter(this);
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getInPorts()
     */
    public int getInPorts() {
        return 0;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getOutPorts()
     */
    public int getOutPorts() {
        return 1;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#setConfiguration(java.lang.String)
     */
    public boolean setConfiguration(String[] strConfig) {
        
        if (strConfig == null) return true;
        if (strConfig.length != 1 ) return false;
        
        m_Period = Integer.parseInt(strConfig[0]);
        
        // range check
        if ( (m_Period <= 0) || ((1<<16)<= m_Period)) m_Period = -1; 
        
        return (m_Period != -1);
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getConfigBytesNum()
     */
    public int getConfigBytesNum() {
        
        return (m_Period != -1)? 2 : 0;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getConfigBytes()
     */
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        if (m_Period == -1 ) return null;
        
        // parse value
        short[][] config = {{ (short)((m_Period>>8)&0xFF), (short)(m_Period&0xFF) }};
        return config;
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        float period = (m_Period==-1)? 250 : m_Period;
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = 189;
        tm.datapackets = new float [] { 1/period*1000  };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = 4;
        tm.instantiations = 1/period*1000;
        tm.period = (int)period;
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {

        System.err.println("ERROR: SimpleWriter setInputPortDatarate: no input ports available!");
        return false;
    }
}
