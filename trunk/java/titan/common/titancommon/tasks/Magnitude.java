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
 * Magnitude.java
 * 
 * @author Andreas Breitenmoser
 *
 * Magnitude computes the (squared) magnitude from a number of n coordinates.
 * It accepts 6 configuration bytes:
 *    number of coordinates which the magnitude should be calculated for,
 *    number of sets of coordinates input to the task, i.e. in the end n = uiMagnPerPacket magnitudes are output, 
 *    overall offset of the input coordinates,
 *    overall scale factor of the input coordinates
 * 
 * Created: 21.01.2008
 * Last modified: 21.01.2008
 * 
 */

public class Magnitude extends Task {
    
    public final static String NAME   = "magnitude";
    public final static int    TASKID = 21;
    
    protected int m_iCoordNum;
    protected int m_iResultShift;
    protected int m_iOffset;
    protected int m_iScaleFactor;
    
    public Magnitude() {}
    
    public Magnitude( Magnitude m ) {
    	super(m);
        m_iCoordNum = m.m_iCoordNum;
        m_iResultShift = m.m_iResultShift;
        m_iOffset = m.m_iOffset;
        m_iScaleFactor = m.m_iScaleFactor;        
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new Magnitude(this);
    }
    
    public int getInPorts() {
        return m_iCoordNum;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public boolean setConfiguration(String[] strConfig) {

        m_iResultShift = 0;
        m_iOffset = 0;
        m_iScaleFactor = 1;
        
        if ( strConfig.length == 4 ) {
            m_iCoordNum = Integer.parseInt(strConfig[0]);
            m_iOffset = Integer.parseInt(strConfig[1]);
            m_iScaleFactor = Integer.parseInt(strConfig[2]);
            m_iResultShift = Integer.parseInt(strConfig[3]);
        } else if ( strConfig.length == 3 ) {
            m_iCoordNum = Integer.parseInt(strConfig[0]);
            m_iOffset = Integer.parseInt(strConfig[1]);
            m_iScaleFactor = Integer.parseInt(strConfig[2]);
        } else if ( strConfig.length == 1 ) {
            m_iCoordNum = Integer.parseInt(strConfig[0]);
        } else return false;
        
        
        boolean bValid = true;
        
        bValid &= ((0 < m_iCoordNum) && (m_iCoordNum < 256));
        bValid &= ((0 <= m_iResultShift) && (m_iResultShift < 32));
        bValid &= ((-32769 < m_iOffset) && (m_iScaleFactor < 32768));
        bValid &= ((-32769 < m_iScaleFactor) && (m_iScaleFactor < 32768));
        return bValid;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short [][] config =  {{ (short)m_iCoordNum, 
                                (short)((m_iOffset>>8)&0xFF),
                                (short)(m_iOffset&0xFF), 
                                (short)((m_iScaleFactor>>8)&0xFF), 
                                (short)(m_iScaleFactor&0xFF), 
                                (short)m_iResultShift }};
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
        tm.procCycles = (int)(m_iCoordNum*360.1287);
        tm.datapackets = new float [] { m_pktPerVar };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = 6;
        tm.instantiations = m_pktPerVar;
        tm.period = (int)(1/m_pktPerVar);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        float packetsNeededForVar = pktPerSecond*pktSize;
        
        if ( m_pktPerVar != packetsNeededForVar ) {
            m_pktPerVar = packetsNeededForVar;
            return true;
        } else return false;
        
    }
    
}
