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
 * Threshold.java
 * 
 * @author Andreas Breitenmoser
 *
 * Does a thresholding of the input data. Incoming are 16 bit, outgoing 8 bit 
 * data thresholded and numbered by the thresholds. Thresholds are ordered in
 * ascending order.
 * 
 * Created: 21.01.2008
 * Last modified: 21.01.2008
 * 
 */

public class Threshold extends Task {
    
    public final static String NAME   = "threshold";
    public final static int    TASKID = 7;
    
    protected int uiNum;
    protected short [] thresholds;
    
    public Threshold() {}
    
    public Threshold( Threshold m ) {
    	super(m);
        uiNum = m.uiNum;
        if ( m.thresholds != null ) {
          thresholds = new short[m.thresholds.length];
          for(int i=0; i<m.thresholds.length; i++) {
              thresholds[i] = m.thresholds[i];
          }
        }
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new Threshold(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        
        // argument must be an integer and 0<ARG0<256
        if ( strConfig == null || strConfig.length < 2 ) return false;
        
        uiNum = Integer.parseInt(strConfig[0]);
        
        thresholds = new short[strConfig.length-1];
        for (int i=1; i<strConfig.length;i++) {
            thresholds[i-1] = (short)Short.parseShort(strConfig[i]);
        }
        
        return ((0 < uiNum) && (uiNum < 256));
    }
    
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short [][] config = {new short[uiNum*2+1]};
        
        config[0][0] = (short)uiNum;

        for (int i=0; i<uiNum; i++) {        
            config[0][i*2+1] = (short)((thresholds[i]>>8) & 0xFF);
            config[0][i*2+2] = (short)((thresholds[i]) & 0xFF);
        }
        
        return config;
        
        
        /* TODO: consider that there may be more thresholds as can fit in one message.
        
        * // compute number of messages needed
        int nDataBytesPerLine =  Math.min((maxBytesPerMsg-1)/2, uiNum);
        short [][] config = new short[1 + nDataBytesPerLine*2][];
        
        config[0][0] = (short)uiNum;

        int x, y;
        for (int i=0; i<uiNum; i++) {
            x = int(i/nDataBytesPerLine);
            y = i - x*nDataBytesPerLine;
            config[x][y*2+1] = (short)((thresholds[i]>>8) & 0xFF);
            config[x][y*2+2] = (short)((thresholds[i]) & 0xFF);
        }
        
        return config;
        */
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
        tm.procCycles = (int)(uiNum*360.1287);
        tm.datapackets = new float [] { m_pktPerVar };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = uiNum + 1;
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
