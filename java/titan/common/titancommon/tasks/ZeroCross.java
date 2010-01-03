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
 * ZeroCross counts the number of zero crossings inside a sliding window.
 * The sliding window is continuously shifted with m_iWindowShift.
 * Number of zero crossings is incremented each time the input signal
 * crosses the band [mean-m_iThreshLow, mean+m_iThreshUp] around the average of the current window.
 *  
 * ZeroCross accepts 4 configuration parameters, which define the window length, the window overlap
 * and the thresholds uiThreshUp and uiThreshLow.
 * 
 * Created: 27.02.2008
 * Last modified: 28.02.08
 * 
 */

public class ZeroCross extends Task {
    
    public final static String NAME   = "zerocross";
    public final static int    TASKID = 10;
    
    protected int m_iWindowSize;
    protected int m_iWindowShift;
    protected int m_iThreshUp;
    protected int m_iThreshLow;
   
    
    public ZeroCross() {}
    
    public ZeroCross( ZeroCross m ) {
    	super(m);
        m_iWindowSize = m.m_iWindowSize;
        m_iWindowShift = m.m_iWindowShift;
        m_iThreshLow = m.m_iThreshLow;
        m_iThreshUp = m.m_iThreshUp;    
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new ZeroCross(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        
        // argument must be an integer and 0<ARG0<256
        if ( strConfig == null || strConfig.length != 4 ) return false;
        
        m_iWindowSize = Integer.parseInt(strConfig[0]);
        m_iWindowShift = Integer.parseInt(strConfig[1]);
        m_iThreshLow = Integer.parseInt(strConfig[2]);
        m_iThreshUp = Integer.parseInt(strConfig[3]);
        
        boolean bValid = true;
        bValid &= ((0 < m_iWindowSize) && (m_iWindowSize < 256));
        bValid &= ((0 < m_iWindowShift) && (m_iWindowShift < 256));
        bValid &= ((0 < m_iThreshLow) && (m_iThreshLow < 32768));
        bValid &= ((0 < m_iThreshUp) && (m_iThreshUp < 32768));

        return bValid;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
      
        short [][] config = {new short[6]};
        config[0][0] =  (short)m_iWindowSize;
        config[0][1] =  (short)m_iWindowShift;
        config[0][2] =  (short)((m_iThreshLow>>8) & 0xFF);
        config[0][3] =  (short)((m_iThreshLow) & 0xFF);
        config[0][4] =  (short)((m_iThreshUp>>8) & 0xFF);
        config[0][5] =  (short)((m_iThreshUp) & 0xFF);
        
        return config;

    }
    
    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    float m_pktPerZC;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(m_iWindowSize*360.1287);
        tm.datapackets = new float [] { m_pktPerZC };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = 2*2*m_iWindowSize + 16;
        tm.instantiations = m_pktPerZC;
        tm.period = (int)(1/m_pktPerZC);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        float packetsNeededForZC = pktPerSecond*pktSize/m_iWindowSize;
        
        if ( m_pktPerZC != packetsNeededForZC ) {
            m_pktPerZC = packetsNeededForZC;
            return true;
        } else return false;
        
    }
    
}
