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
 * The Duplicator configuration includes the number of output ports to be 
 * served. It produces for every incoming packet a packet on each of the 
 * output ports.
 * If no arguments are given, it initializes 2 output ports.
 *
 */
public class Duplicator extends Task {
    
    public final static String NAME   ="duplicator";
    public final static int    TASKID = 3;
    
    protected int m_iOutports=0;
    
    public Duplicator() {};
    
    public Duplicator(Duplicator d ) {
    	super(d);
        m_iOutports = d.m_iOutports;
    }
    
    public String getName() {
        return NAME;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public Object clone() {
        return new Duplicator(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return m_iOutports;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        
        if (strConfig == null){
            m_iOutports = 2;
            return true;
        } else if ( strConfig.length == 1 ) {
            m_iOutports = Integer.parseInt( strConfig[0]);
            return (m_iOutports != 0);
        }
        
        return (strConfig == null); // no configuration parameters
    }
    
    public int getConfigBytesNum() {
        return (m_iOutports==2)? 0:1;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        if ( m_iOutports == 2 ) {
            return null;
        } else {
            short [][] config = {{ (short)m_iOutports }};
            return config;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    float m_inputPPS = 0;
    int   m_inputPS = 0;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(m_inputPS*m_iOutports*22.8956);
        tm.datapackets = new float [m_iOutports];
        tm.packetsizes = new int [m_iOutports];
        for (int i=0; i<tm.datapackets.length; i++ ) {
            tm.datapackets[i] = m_inputPPS;
            tm.packetsizes[i] = m_inputPS;
        }
        tm.dynMemory = 0;
        tm.instantiations = m_inputPPS;
        tm.period = (int)(1/m_inputPPS);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        if ( (m_inputPPS == pktPerSecond) && (m_inputPS == pktSize)  ) {
            return false;
        } else {
            m_inputPPS = pktPerSecond;
            m_inputPS  = pktSize;
            return true;
        }
    }


}
