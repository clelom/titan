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
 * @author Andreas Breitenmoser
 *
 * Variance computes the variance over a specified data window length.
 * It accepts 2 configuration parameters, which define the window length and the window overlap.
 * An optional third indicates a shift of the 32bit resulting value, such as to use the result's 
 * 16 bits more optimally.
 * 
 * NOTE: The task uses two sums:
 *       SquareSum = sum x^2
 *       Sum       = sum x
 *       variance  = Sum^2/windowlength - SquareSum/windowlength
 *       Make sure that neither SquareSum^2 nor Sum^2 exceeds 32bit values!
 *       
 * 
 * Last modified: 30.01.08
 * 
 */

public class Variance extends Task {
    
    public final static String NAME   = "variance";
    public final static int    TASKID = 8;
    
    protected int m_iWindowSize;
    protected int m_iWindowShift;
    protected int m_iResultShift;
    
    public Variance() {}
    
    public Variance( Variance m ) {
    	super(m);
        m_iWindowSize = m.m_iWindowSize;
        m_iWindowShift = m.m_iWindowShift;
        m_iResultShift = m.m_iResultShift;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new Variance(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        
        // argument must be an integer and 0<ARG0<256
        if ( strConfig == null || ((strConfig.length != 2) && (strConfig.length != 3)) ) return false;
        
        m_iWindowSize = Integer.parseInt(strConfig[0]);
        m_iWindowShift = Integer.parseInt(strConfig[1]);
        m_iResultShift = (strConfig.length == 3)? Integer.parseInt(strConfig[2]) : 0;
        
        boolean bValid = true;
        bValid &= ((0 < m_iWindowSize) && (m_iWindowSize < 256));
        bValid &= ((0 < m_iWindowShift) && (m_iWindowShift < 256));
        bValid &= ((0 <= m_iResultShift) && (m_iResultShift < 256));
        

        return bValid;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        if ( m_iResultShift == 0 ) {
            short [][] config =  {{ (short)m_iWindowSize, (short)m_iWindowShift}};
            return config;
        } else {
            short [][] config =  {{ (short)m_iWindowSize, (short)m_iWindowShift, (short)m_iResultShift}};
            return config;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    float m_pktPerVar;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(m_iWindowSize*360.1287);
        tm.datapackets = new float [] { m_pktPerVar };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = 2*2*m_iWindowSize + 13;
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
