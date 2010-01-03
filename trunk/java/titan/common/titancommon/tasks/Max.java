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
 * Max accepts one number identifying the window size and a shift it operates on. These 
 * are two short (2 byte) values.
 *
 */
public class Max extends Task {
    
    public final static String NAME   = "max";
    public final static int    TASKID = 11;
    
    private int m_iWindowSize;
    private int m_iWindowShift;
    
    public Max() {}
    
    public Max( Max m ) {
    	super(m);
        m_iWindowSize = m.m_iWindowSize;
        m_iWindowShift= m.m_iWindowShift;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public int getID() {
        return TASKID;
    }
    
    public Object clone() {
        return new Max(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        if (  strConfig == null || strConfig.length != 2 ) return false;
        
        m_iWindowSize = Integer.parseInt(strConfig[0]);
        m_iWindowShift= Integer.parseInt(strConfig[1]);
        
        return (0 < m_iWindowSize) && (m_iWindowSize < (1<<16));
        
    }
    
    public int getConfigBytesNum() {
        return 2;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short [][] config = {{ (short)((m_iWindowSize)&0xFF), (short)(m_iWindowShift&0xFF) }}; 
        return config;
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    float m_pktPerRun;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(m_iWindowSize*46.0297);
        tm.datapackets = new float [] { m_pktPerRun };
        tm.packetsizes = new int [] {4};
        tm.dynMemory = 8;
        tm.instantiations = m_pktPerRun;
        tm.period = (int)(1/m_pktPerRun);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        float packetsNeededForMean = pktPerSecond*pktSize/m_iWindowSize;
        
        if ( m_pktPerRun != packetsNeededForMean ) {
            m_pktPerRun = packetsNeededForMean;
            return true;
        } else return false;
        
    }
 }
