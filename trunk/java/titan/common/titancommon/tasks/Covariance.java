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
 * @author Andreas Breitenmoser
 *
 * Covariance computes the covariance over a specified data window length.
 * It accepts 2 configuration bytes, which define the window length uint8_t and the window overlap.
 * 
 * Last modified: 30.01.08
 */

public class Covariance extends Task {
    
    public final static String NAME   = "covariance";
    public final static int    TASKID = 25;
    
    protected int m_iWindowSize;
    protected int m_iWindowShift;
    
    public Covariance() {}
    
    public Covariance( Covariance m ) {
    	super(m);
        m_iWindowSize = m.m_iWindowSize;
        m_iWindowShift = m.m_iWindowShift;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new Covariance(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        
        // argument must be an integer and 0<ARG0<256
        if ( strConfig == null || strConfig.length != 2 ) return false;
        
        m_iWindowSize = Integer.parseInt(strConfig[0]);
        m_iWindowShift = Integer.parseInt(strConfig[1]);
        
        boolean bValid = true;
        bValid &= ((0 < m_iWindowSize) && (m_iWindowSize < 256));
        bValid &= ((0 < m_iWindowShift) && (m_iWindowShift < 256));

        return bValid;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short [][] config =  {{ (short)m_iWindowSize, (short)m_iWindowShift}};
        return config;
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    //TODO: The metrics have not been evaluated for this task yet
    
    float m_pktPerVar;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(m_iWindowSize*360.1287);
        tm.datapackets = new float [] { m_pktPerVar };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = 2*2*m_iWindowSize + 9;
        tm.instantiations = m_pktPerVar;
        tm.period = (int)(1/m_pktPerVar);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        float packetsNeededForVar = pktPerSecond*pktSize/m_iWindowSize;
        
        if ( m_pktPerVar != packetsNeededForVar ) {
            m_pktPerVar = packetsNeededForVar;
            return true;
        } else return false;
        
    }
    
}
