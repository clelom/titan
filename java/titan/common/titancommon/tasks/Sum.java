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
 * Computes the sum over a defined length of data.
 * It accepts 2 configuration bytes, which define the window length uint8_t and the window overlap.
 * All input is assumed to be int16_t.
 * 
 * Created: 21.02.2008
 * Last modified: 21.02.2008
 * 
 */

public class Sum extends Task {
    
    public final static String NAME   = "sum";
    public final static int    TASKID = 27;
    
    protected int m_iWindowSize;
    protected int m_iWindowShift;
    
    public Sum() {}
    
    public Sum( Sum m ) {
    	super(m);
        m_iWindowSize = m.m_iWindowSize;
        m_iWindowShift = m.m_iWindowShift;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new Sum(this);
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

    float m_pktPerSum;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(m_iWindowSize*75.84175);
        tm.datapackets = new float [] { m_pktPerSum };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = 2*2*m_iWindowSize + 7;
        tm.instantiations = m_pktPerSum;
        tm.period = (int)(1/m_pktPerSum);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        float packetsNeededForSum = pktPerSecond*pktSize/m_iWindowSize;
        
        if ( m_pktPerSum != packetsNeededForSum ) {
            m_pktPerSum = packetsNeededForSum;
            return true;
        } else return false;
        
    }
    
}
