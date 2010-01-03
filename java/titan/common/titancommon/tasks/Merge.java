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

import titancommon.*;
import titancommon.compiler.NodeMetrics;
import titancommon.compiler.TaskMetrics;

/**
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * Merges the data coming in from a number of ports and sends the merged 
 * data over the output port.
 *
 */
public class Merge extends Task {
    
    
    public final static String NAME   = "merge";
    public final static int    TASKID = 13;
    
    protected int m_iInports;
    protected int m_iBytesPerMessage;
    
    public Merge() {}
    
    public Merge( Merge m ) {
    	super(m);
        m_iInports = m.m_iInports;
        m_iBytesPerMessage = m.m_iBytesPerMessage;
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
        return new Merge(this);
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getInPorts()
     */
    public int getInPorts() {
        return m_iInports;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getOutPorts()
     */
    public int getOutPorts() {
        return 1;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#setConfiguration(java.lang.String[])
     */
    public boolean setConfiguration(String[] strConfig) {
        
        if (  strConfig == null || strConfig.length < 1 || 2 < strConfig.length  ) return false;
        
        m_iInports = Integer.parseInt(strConfig[0]);
        m_iBytesPerMessage = (strConfig.length==2)?Integer.parseInt(strConfig[1]): -1; 
        
        boolean bValid = true;
        
        bValid &= ((0 < m_iInports) && (m_iInports < 256)); 
        bValid &= (m_iInports*m_iBytesPerMessage <= titancommon.TitanCommand.TITAN_PACKET_SIZE);
        
        return bValid;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getConfigBytesNum()
     */
    public int getConfigBytesNum() {
        return 1;
    }
    
    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getConfigBytes()
     */
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        if ( m_iBytesPerMessage == -1 ) {
            short [][] config = {{ (short)m_iInports }};
            return config;
        } else {
            short [][] config = {{ (short)m_iInports,(short)m_iBytesPerMessage }};
            return config;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    private float [] m_pktPerSecond;
    private int []   m_pktSize;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        int   totalSize=0;
        float minFreq = Float.MAX_VALUE;
        float maxFreq = Float.MIN_VALUE;
        if (m_pktSize != null ) {
            for (int i=0; i<m_pktSize.length;i++) {
                totalSize+=m_pktSize[i];
                if ( m_pktPerSecond[i] <= minFreq ) {
                    minFreq = m_pktPerSecond[i];
                }
                if ( m_pktPerSecond[i] >= maxFreq ) {
                    maxFreq = m_pktPerSecond[i];
                }
            }
        }
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(totalSize*78.2267);
        tm.datapackets = new float[] { minFreq };
        tm.packetsizes = new int[] {totalSize};
        tm.dynMemory = 5;
        tm.instantiations = maxFreq;
        tm.period = (int)(1/maxFreq);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {

        boolean bChanged = false;
        
        if (m_pktPerSecond == null ) {
            m_pktPerSecond = new float[m_iInports];
            m_pktSize      = new int[m_iInports];
            bChanged = true;
        }
        
        if ( m_pktPerSecond[port] != pktPerSecond ) {
            m_pktPerSecond[port] = pktPerSecond;
            bChanged = true;
        }
        
        if ( m_pktSize[port] != pktSize ) {
            m_pktSize[port] = pktSize;
            bChanged = true;
        }
        
        return bChanged;
    }

}
